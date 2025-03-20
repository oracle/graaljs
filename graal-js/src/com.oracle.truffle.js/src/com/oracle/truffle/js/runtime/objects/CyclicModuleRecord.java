/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.collections.EconomicMap;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;

/**
 * A Cyclic Module Record is used to represent information about a module that can participate in
 * dependency cycles with other modules that are subclasses of the Cyclic Module Record type. Module
 * Records that are not subclasses of the Cyclic Module Record type must not participate in
 * dependency cycles with Source Text Module Records.
 */
public abstract class CyclicModuleRecord extends AbstractModuleRecord {

    public enum Status {
        New,
        Unlinked,
        Linking,
        Linked,
        Evaluating,
        EvaluatingAsync,
        Evaluated,
    }

    /** Module's linking/evaluation status. */
    private Status status;

    /**
     * Auxiliary field used during Link and Evaluate only. If [[Status]] is "linking" or
     * "evaluating", this non-negative number records the point at which the module was first
     * visited during the ongoing depth-first traversal of the dependency graph.
     */
    private int dfsIndex = -1;
    /**
     * Auxiliary field used during Link and Evaluate only. If [[Status]] is "linking" or
     * "evaluating", this is either the module's own [[DFSIndex]] or that of an "earlier" module in
     * the same strongly connected component.
     */
    private int dfsAncestorIndex = -1;

    private final EconomicMap<LoadedModuleRequest, AbstractModuleRecord> loadedModules = EconomicMap.create();

    // [[CycleRoot]]
    private CyclicModuleRecord cycleRoot = this;
    // [[HasTLA]]
    private final boolean hasTLA;
    // [[AsyncEvaluation]]
    private boolean asyncEvaluation;
    // Order in which [[AsyncEvaluation]] was set (if > 0)
    private long asyncEvaluationOrder;
    // [[TopLevelCapability]]
    private PromiseCapabilityRecord topLevelPromiseCapability = null;
    // [[AsyncParentModules]]
    private List<CyclicModuleRecord> asyncParentModules = null;
    // [[PendingAsyncDependencies]]
    private int pendingAsyncDependencies = 0;

    /** Exception that occurred during evaluation. */
    private Throwable evaluationError;
    /** Implementation-specific: The result of ModuleExecution if no exception occurred. */
    private Object executionResult;

    protected CyclicModuleRecord(JSContext context, Source source, Object hostDefined, FrameDescriptor frameDescriptor) {
        this(context, source, hostDefined, frameDescriptor, false);
    }

    protected CyclicModuleRecord(JSContext context, Source source, Object hostDefined, FrameDescriptor frameDescriptor, boolean hasTLA) {
        super(context, source, hostDefined, frameDescriptor);
        this.hasTLA = hasTLA;
        this.status = Status.New;
    }

    /**
     * A list of all the ModuleSpecifier strings and import attributes used by the module
     * represented by this record to request the importation of a module, in occurrence order.
     */
    public abstract List<ModuleRequest> getRequestedModules();

    /**
     * Initialize the Environment Record of the module, including resolving all imported bindings,
     * and create the module's execution context.
     */
    public abstract void initializeEnvironment(JSRealm realm);

    /**
     * Evaluate the module's code within its execution context. If this module has true in
     * [[HasTLA]], then a PromiseCapability Record is passed as an argument, and the method is
     * expected to resolve or reject the given capability. In this case, the method must not throw
     * an exception, but instead reject the PromiseCapability Record if necessary.
     */
    public abstract Object executeModule(JSRealm realm, PromiseCapabilityRecord capability);

    @Override
    public final JSPromiseObject evaluate(JSRealm realm) {
        return context.getEvaluator().moduleEvaluation(realm, this);
    }

    @Override
    public final void link(JSRealm realm) {
        context.getEvaluator().moduleLinking(realm, this);
    }

    @TruffleBoundary
    public final AbstractModuleRecord getLoadedModule(ModuleRequest moduleRequest) {
        return loadedModules.get(LoadedModuleRequest.of(moduleRequest));
    }

    @TruffleBoundary
    @Override
    public final AbstractModuleRecord addLoadedModule(JSRealm realm, ModuleRequest moduleRequest, AbstractModuleRecord module) {
        return loadedModules.putIfAbsent(LoadedModuleRequest.of(moduleRequest), module);
    }

    @TruffleBoundary
    public final AbstractModuleRecord getImportedModule(ModuleRequest moduleRequest) {
        AbstractModuleRecord loadedModule = getLoadedModule(moduleRequest);
        assert loadedModule != null : moduleRequest;
        return loadedModule;
    }

    @Override
    public final Status getStatus() {
        return status;
    }

    public final void setStatus(Status status) {
        this.status = status;
    }

    public final boolean isLinked() {
        return getStatus().compareTo(Status.Linked) >= 0;
    }

    public final boolean hasBeenEvaluated() {
        return getStatus() == Status.Evaluated || getStatus() == Status.EvaluatingAsync;
    }

    public final void setUnlinked() {
        setStatus(Status.Unlinked);
        this.clearEnvironment();
        this.dfsIndex = -1;
        this.dfsAncestorIndex = -1;
    }

    public final Throwable getEvaluationError() {
        assert hasBeenEvaluated();
        return evaluationError;
    }

    public final void setEvaluationError(Throwable evaluationError) {
        assert hasBeenEvaluated();
        this.evaluationError = evaluationError;
    }

    public final Object getExecutionResult() {
        assert hasBeenEvaluated();
        return executionResult;
    }

    public final void setExecutionResult(Object executionResult) {
        this.executionResult = executionResult;
    }

    public final Object getExecutionResultOrThrow() {
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

    public final int getDFSIndex() {
        assert dfsIndex >= 0;
        return dfsIndex;
    }

    public final void setDFSIndex(int dfsIndex) {
        this.dfsIndex = dfsIndex;
    }

    public final int getDFSAncestorIndex() {
        assert dfsAncestorIndex >= 0;
        return dfsAncestorIndex;
    }

    public final void setDFSAncestorIndex(int dfsAncestorIndex) {
        this.dfsAncestorIndex = dfsAncestorIndex;
    }

    public final PromiseCapabilityRecord getTopLevelCapability() {
        return topLevelPromiseCapability;
    }

    public final void setTopLevelCapability(PromiseCapabilityRecord capability) {
        this.topLevelPromiseCapability = capability;
    }

    public final boolean isAsyncEvaluation() {
        return asyncEvaluation;
    }

    public final void setAsyncEvaluation(boolean asyncEvaluation) {
        this.asyncEvaluation = asyncEvaluation;
    }

    public final List<CyclicModuleRecord> getAsyncParentModules() {
        return asyncParentModules;
    }

    public final void setPendingAsyncDependencies(int value) {
        pendingAsyncDependencies = value;
    }

    public final void initAsyncParentModules() {
        assert asyncParentModules == null;
        asyncParentModules = new ArrayList<>();
    }

    public final void incPendingAsyncDependencies() {
        pendingAsyncDependencies++;
    }

    public final void decPendingAsyncDependencies() {
        pendingAsyncDependencies--;
    }

    public final void appendAsyncParentModules(CyclicModuleRecord moduleRecord) {
        asyncParentModules.add(moduleRecord);
    }

    public final int getPendingAsyncDependencies() {
        return pendingAsyncDependencies;
    }

    public final void setAsyncEvaluatingOrder(long order) {
        asyncEvaluationOrder = order;
    }

    public final long getAsyncEvaluatingOrder() {
        return asyncEvaluationOrder;
    }

    public final boolean hasTLA() {
        return hasTLA;
    }

    public final void setCycleRoot(CyclicModuleRecord module) {
        cycleRoot = module;
    }

    public final CyclicModuleRecord getCycleRoot() {
        return cycleRoot;
    }

    @TruffleBoundary
    public final boolean isReadyForSyncExecution() {
        return isReadyForSyncExecution(new HashSet<>());
    }

    private boolean isReadyForSyncExecution(Set<AbstractModuleRecord> seen) {
        CompilerAsserts.neverPartOfCompilation();
        if (!seen.add(this)) {
            return true;
        }
        if (getStatus() == Status.Evaluated) {
            return true;
        } else if (getStatus() == Status.Evaluating || getStatus() == Status.EvaluatingAsync) {
            return false;
        } else {
            assert getStatus() == Status.Linked : getStatus();
            if (hasTLA()) {
                return false;
            }
            for (ModuleRequest request : getRequestedModules()) {
                var requiredModule = getImportedModule(request);
                if (requiredModule instanceof CyclicModuleRecord requiredCyclicModule && !requiredCyclicModule.isReadyForSyncExecution(seen)) {
                    return false;
                }
            }
            return true;
        }
    }

    public record LoadedModuleRequest(
                    TruffleString specifier,
                    Map<TruffleString, TruffleString> attributes) {
        /**
         * Only keep specifier and import attributes for loaded module lookup, drop import phase.
         * See ModuleRequestsEqual.
         */
        public static LoadedModuleRequest of(ModuleRequest request) {
            return new LoadedModuleRequest(request.specifier(), request.attributes());
        }
    }
}
