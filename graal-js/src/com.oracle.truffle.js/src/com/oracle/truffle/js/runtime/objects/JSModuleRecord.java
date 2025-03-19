/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.objects;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.module.CreateImportMetaNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

/**
 * Source Text Module Record.
 */
public class JSModuleRecord extends ScriptOrModule {

    public enum Status {
        Unlinked,
        Linking,
        Linked,
        Evaluating,
        EvaluatingAsync,
        Evaluated,
    }

    private final JSModuleData parsedModule;
    private final JSModuleLoader moduleLoader;

    /** Module's linking/evaluation status. */
    private Status status;

    /** Exception that occurred during evaluation. */
    private Throwable evaluationError;
    /** Implementation-specific: The result of ModuleExecution if no exception occurred. */
    private Object executionResult;

    /** Lazily initialized Module Namespace object ({@code [[Namespace]]}). */
    private JSDynamicObject namespace;
    /** Lazily initialized frame ({@code [[Environment]]}). */
    private MaterializedFrame environment;
    /** Lazily initialized import.meta object ({@code [[ImportMeta]]}). */
    private JSObject importMeta;

    // [HostDefined]
    private Object hostDefined;

    /**
     * Auxiliary field used during Link and Evaluate only. If [[Status]] is "linking" or
     * "evaluating", this non-negative number records the point at which the module was first
     * visited during the ongoing depth-first traversal of the dependency graph.
     */
    private int dfsIndex;
    /**
     * Auxiliary field used during Link and Evaluate only. If [[Status]] is "linking" or
     * "evaluating", this is either the module's own [[DFSIndex]] or that of an "earlier" module in
     * the same strongly connected component.
     */
    private int dfsAncestorIndex;

    public JSModuleRecord(JSModuleData parsedModule, JSModuleLoader moduleLoader) {
        super(parsedModule.getContext(), parsedModule.getSource());
        this.parsedModule = parsedModule;
        this.moduleLoader = moduleLoader;
        this.hasTLA = parsedModule.isTopLevelAsync();
        this.hostDefined = null;
        setUnlinked();
    }

    public JSModuleRecord(JSModuleData moduleData, JSModuleLoader moduleLoader, Object hostDefined) {
        this(moduleData, moduleLoader);
        this.hostDefined = hostDefined;
    }

    public com.oracle.js.parser.ir.Module getModule() {
        return parsedModule.getModule();
    }

    public JSModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public JSFunctionData getFunctionData() {
        return parsedModule.getFunctionData();
    }

    public FrameDescriptor getFrameDescriptor() {
        return parsedModule.getFrameDescriptor();
    }

    public JSModuleData getModuleData() {
        return parsedModule;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean hasBeenEvaluated() {
        return getStatus() == Status.Evaluated || getStatus() == Status.EvaluatingAsync;
    }

    public Throwable getEvaluationError() {
        assert hasBeenEvaluated();
        return evaluationError;
    }

    public void setEvaluationError(Throwable evaluationError) {
        assert hasBeenEvaluated();
        this.evaluationError = evaluationError;
    }

    public JSDynamicObject getNamespace() {
        return namespace;
    }

    public void setNamespace(JSDynamicObject namespace) {
        assert this.namespace == null;
        this.namespace = namespace;
    }

    public MaterializedFrame getEnvironment() {
        return environment;
    }

    public void setEnvironment(MaterializedFrame environment) {
        assert this.environment == null;
        assert this.getFrameDescriptor() == environment.getFrameDescriptor();
        this.environment = environment;
    }

    public Object getHostDefined() {
        return this.hostDefined;
    }

    public int getDFSIndex() {
        assert dfsIndex >= 0;
        return dfsIndex;
    }

    public void setDFSIndex(int dfsIndex) {
        this.dfsIndex = dfsIndex;
    }

    public int getDFSAncestorIndex() {
        assert dfsAncestorIndex >= 0;
        return dfsAncestorIndex;
    }

    public void setDFSAncestorIndex(int dfsAncestorIndex) {
        this.dfsAncestorIndex = dfsAncestorIndex;
    }

    public Object getExecutionResult() {
        assert hasBeenEvaluated();
        return executionResult;
    }

    public void setExecutionResult(Object executionResult) {
        this.executionResult = executionResult;
    }

    public Object getExecutionResultOrThrow() {
        assert hasBeenEvaluated();
        Throwable error = getEvaluationError();
        if (error != null) {
            throw JSRuntime.rethrow(error);
        } else {
            Object result = getExecutionResult();
            assert result != null;
            return result;
        }
    }

    public JSObject getImportMetaOrNull() {
        return importMeta;
    }

    public JSObject getImportMeta(CreateImportMetaNode createImportMeta) {
        JSObject metaObj = importMeta;
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.SLOWPATH_PROBABILITY, metaObj == null)) {
            importMeta = metaObj = createImportMeta.execute(this);
        }
        return metaObj;
    }

    @TruffleBoundary
    public String getURL() {
        return getSource().getURI().toString();
    }

    public void setUnlinked() {
        setStatus(Status.Unlinked);
        this.environment = null;
        this.dfsIndex = -1;
        this.dfsAncestorIndex = -1;
    }

    // ##### Top-level await

    // [[CycleRoot]]
    private JSModuleRecord cycleRoot = this;
    // [[HasTLA]]
    private final boolean hasTLA;
    // [[AsyncEvaluation]] (true when asyncEvaluationOrder > 0)
    private long asyncEvaluationOrder;
    // [[TopLevelCapability]]
    private PromiseCapabilityRecord topLevelPromiseCapability = null;
    // [[AsyncParentModules]]
    private List<JSModuleRecord> asyncParentModules = null;
    // [[PendingAsyncDependencies]]
    private int pendingAsyncDependencies = 0;

    public PromiseCapabilityRecord getTopLevelCapability() {
        return topLevelPromiseCapability;
    }

    public void setTopLevelCapability(PromiseCapabilityRecord capability) {
        this.topLevelPromiseCapability = capability;
    }

    public boolean isAsyncEvaluation() {
        return asyncEvaluationOrder > 0;
    }

    public List<JSModuleRecord> getAsyncParentModules() {
        return asyncParentModules;
    }

    public void setPendingAsyncDependencies(int value) {
        pendingAsyncDependencies = value;
    }

    public void initAsyncParentModules() {
        assert asyncParentModules == null;
        asyncParentModules = new ArrayList<>();
    }

    public void incPendingAsyncDependencies() {
        pendingAsyncDependencies++;
    }

    public void decPendingAsyncDependencies() {
        pendingAsyncDependencies--;
    }

    public void appendAsyncParentModules(JSModuleRecord moduleRecord) {
        asyncParentModules.add(moduleRecord);
    }

    public int getPendingAsyncDependencies() {
        return pendingAsyncDependencies;
    }

    public void setAsyncEvaluatingOrder(long order) {
        asyncEvaluationOrder = order;
    }

    public long getAsyncEvaluatingOrder() {
        return asyncEvaluationOrder;
    }

    public boolean hasTLA() {
        return hasTLA;
    }

    public void setCycleRoot(JSModuleRecord module) {
        cycleRoot = module;
    }

    public JSModuleRecord getCycleRoot() {
        return cycleRoot;
    }

    @Override
    public void rememberImportedModuleSource(TruffleString moduleSpecifier, Source moduleSource) {
        parsedModule.rememberImportedModuleSource(moduleSpecifier, moduleSource);
    }

}
