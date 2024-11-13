/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.collections.EconomicMap;

import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespaceObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * Source Text Module Record.
 */
public class JSModuleRecord extends AbstractModuleRecord {

    public enum Status {
        New,
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
    private JSModuleNamespaceObject namespace;
    /** Lazily initialized frame ({@code [[Environment]]}). */
    private MaterializedFrame environment;
    /** Lazily initialized import.meta object ({@code [[ImportMeta]]}). */
    private JSDynamicObject importMeta;

    // [HostDefined]
    private Object hostDefined;

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

    private EconomicMap<ModuleRequest, AbstractModuleRecord> loadedModules = EconomicMap.create();

    public JSModuleRecord(JSModuleData parsedModule, JSModuleLoader moduleLoader) {
        super(parsedModule.getContext(), parsedModule.getSource());
        this.parsedModule = parsedModule;
        this.moduleLoader = moduleLoader;
        this.hasTLA = parsedModule.isTopLevelAsync();
        this.hostDefined = null;
        this.status = Status.New;
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

    public JSDynamicObject getImportMeta() {
        if (importMeta == null) {
            importMeta = createMetaObject();
        }
        return importMeta;
    }

    private JSDynamicObject createMetaObject() {
        JSObject metaObj = JSOrdinary.createWithNullPrototype(context);
        if (context.hasImportMetaInitializerBeenSet()) {
            context.notifyImportMetaInitializer(metaObj, this);
        } else {
            initializeMetaObject(metaObj);
        }
        return metaObj;
    }

    @TruffleBoundary
    private void initializeMetaObject(JSObject metaObj) {
        JSObject.set(metaObj, Strings.URL, Strings.fromJavaString(getSource().getURI().toString()));
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
    // [[AsyncEvaluation]]
    private boolean asyncEvaluation;
    // Order in which [[AsyncEvaluation]] was set (if > 0)
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
        return asyncEvaluation;
    }

    public void setAsyncEvaluation(boolean asyncEvaluation) {
        this.asyncEvaluation = asyncEvaluation;
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

    @TruffleBoundary
    @Override
    public AbstractModuleRecord getLoadedModule(JSRealm realm, ModuleRequest moduleRequest) {
        return loadedModules.get(moduleRequest);
    }

    @TruffleBoundary
    @Override
    public AbstractModuleRecord addLoadedModule(JSRealm realm, ModuleRequest moduleRequest, AbstractModuleRecord module) {
        return loadedModules.putIfAbsent(moduleRequest, module);
    }

    @TruffleBoundary
    public AbstractModuleRecord getImportedModule(ModuleRequest moduleRequest) {
        assert loadedModules.containsKey(moduleRequest) : moduleRequest;
        return loadedModules.get(moduleRequest);
    }

    @Override
    public Object getModuleSource() {
        /*
         * Source Text Module Record provides a GetModuleSource implementation that always returns
         * an abrupt completion indicating that a source phase import is not available.
         */
        throw Errors.createSyntaxError("Source phase import is not available for Source Text Module");
    }

    @Override
    public Collection<TruffleString> getExportedNames(Set<JSModuleRecord> exportStarSet) {
        CompilerAsserts.neverPartOfCompilation();
        if (exportStarSet.contains(this)) {
            // Assert: We've reached the starting point of an import * circularity.
            return Collections.emptySortedSet();
        }
        exportStarSet.add(this);
        Collection<TruffleString> exportedNames = new HashSet<>();
        for (ExportEntry exportEntry : getModule().getLocalExportEntries()) {
            // Assert: module provides the direct binding for this export.
            exportedNames.add(exportEntry.getExportName());
        }
        for (ExportEntry exportEntry : getModule().getIndirectExportEntries()) {
            // Assert: module imports a specific binding for this export.
            exportedNames.add(exportEntry.getExportName());
        }
        for (ExportEntry exportEntry : getModule().getStarExportEntries()) {
            JSModuleRecord requestedModule = (JSModuleRecord) getImportedModule(exportEntry.getModuleRequest());
            Collection<TruffleString> starNames = requestedModule.getExportedNames(exportStarSet);
            for (TruffleString starName : starNames) {
                if (!starName.equals(com.oracle.js.parser.ir.Module.DEFAULT_NAME)) {
                    if (!exportedNames.contains(starName)) {
                        exportedNames.add(starName);
                    }
                }
            }
        }
        return exportedNames;
    }

    /**
     * ResolveExport attempts to resolve an imported binding to the actual defining module and local
     * binding name. The defining module may be the module represented by the Module Record this
     * method was invoked on or some other module that is imported by that module. The parameter
     * resolveSet is use to detect unresolved circular import/export paths. If a pair consisting of
     * specific Module Record and exportName is reached that is already in resolveSet, an import
     * circularity has been encountered. Before recursively calling ResolveExport, a pair consisting
     * of module and exportName is added to resolveSet.
     *
     * If a defining module is found a Record {[[module]], [[bindingName]]} is returned. This record
     * identifies the resolved binding of the originally requested export. If no definition was
     * found or the request is found to be circular, null is returned. If the request is found to be
     * ambiguous, the string "ambiguous" is returned.
     */
    @Override
    public ExportResolution resolveExport(TruffleString exportName, Set<Pair<? extends AbstractModuleRecord, TruffleString>> resolveSet) {
        CompilerAsserts.neverPartOfCompilation();
        Pair<JSModuleRecord, TruffleString> resolved = new Pair<>(this, exportName);
        if (resolveSet.contains(resolved)) {
            // Assert: this is a circular import request.
            return ExportResolution.notFound();
        }
        resolveSet.add(resolved);
        for (ExportEntry exportEntry : getModule().getLocalExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                // Assert: module provides the direct binding for this export.
                return ExportResolution.resolved(this, exportEntry.getLocalName());
            }
        }
        for (ExportEntry exportEntry : getModule().getIndirectExportEntries()) {
            if (exportEntry.getExportName().equals(exportName)) {
                JSModuleRecord importedModule = (JSModuleRecord) getImportedModule(exportEntry.getModuleRequest());
                if (exportEntry.getImportName().equals(Module.STAR_NAME)) {
                    // Assert: module does not provide the direct binding for this export.
                    return ExportResolution.resolved(importedModule, Module.NAMESPACE_EXPORT_BINDING_NAME);
                } else {
                    // Assert: module imports a specific binding for this export.
                    return importedModule.resolveExport(exportEntry.getImportName(), resolveSet);
                }
            }
        }
        if (exportName.equals(Module.DEFAULT_NAME)) {
            // Assert: A default export was not explicitly defined by this module.
            return ExportResolution.notFound();
            // NOTE: A default export cannot be provided by an `export *` or `export * from "mod"`.
        }
        ExportResolution starResolution = ExportResolution.notFound();
        for (ExportEntry exportEntry : getModule().getStarExportEntries()) {
            JSModuleRecord importedModule = (JSModuleRecord) getImportedModule(exportEntry.getModuleRequest());
            ExportResolution resolution = importedModule.resolveExport(exportName, resolveSet);
            if (resolution.isAmbiguous()) {
                return resolution;
            }
            if (!resolution.isNull()) {
                if (starResolution.isNull()) {
                    starResolution = resolution;
                } else {
                    // Assert: there is more than one * import that includes the requested name.
                    if (!resolution.equals(starResolution)) {
                        return ExportResolution.ambiguous();
                    }
                }
            }
        }
        return starResolution;
    }

    public JSDynamicObject getModuleNamespaceOrNull() {
        return namespace;
    }

    @TruffleBoundary
    @Override
    public JSDynamicObject getModuleNamespace() {
        if (namespace != null) {
            return namespace;
        }
        Collection<TruffleString> exportedNames = getExportedNames();
        List<Map.Entry<TruffleString, ExportResolution>> unambiguousNames = new ArrayList<>();
        for (TruffleString exportedName : exportedNames) {
            ExportResolution resolution = resolveExport(exportedName);
            if (resolution.isNull()) {
                throw Errors.createSyntaxError("Could not resolve export");
            } else if (!resolution.isAmbiguous()) {
                unambiguousNames.add(Map.entry(exportedName, resolution));
            }
        }
        unambiguousNames.sort((a, b) -> a.getKey().compareCharsUTF16Uncached(b.getKey()));
        JSModuleNamespaceObject ns = JSModuleNamespace.create(getContext(), JSRealm.get(null), this, unambiguousNames);
        this.namespace = ns;
        return ns;
    }

    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, Object hostDefinedArg) {
        return context.getEvaluator().loadRequestedModules(realm, this, hostDefinedArg);
    }

    @Override
    public void link(JSRealm realm) {
        context.getEvaluator().moduleLinking(realm, this);
    }

    @Override
    public Object evaluate(JSRealm realm) {
        return context.getEvaluator().moduleEvaluation(realm, this);
    }

    @Override
    public void rememberImportedModuleSource(TruffleString moduleSpecifier, Source moduleSource) {
        parsedModule.rememberImportedModuleSource(moduleSpecifier, moduleSource);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "SourceTextModule" + "@" + Integer.toHexString(System.identityHashCode(this)) + "[status=" + getStatus() + ", source=" + getSource() + "]";
    }

}
