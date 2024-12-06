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

import com.oracle.js.parser.ir.Module;
import com.oracle.js.parser.ir.Module.ExportEntry;
import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespace;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespaceObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * Source Text Module Record.
 */
public class JSModuleRecord extends CyclicModuleRecord {

    private final JSModuleData parsedModule;
    private final JSModuleLoader moduleLoader;

    /** Lazily initialized Module Namespace object ({@code [[Namespace]]}). */
    private JSModuleNamespaceObject namespace;
    /** Lazily initialized import.meta object ({@code [[ImportMeta]]}). */
    private JSDynamicObject importMeta;

    public JSModuleRecord(JSModuleData parsedModule, JSModuleLoader moduleLoader) {
        this(parsedModule, moduleLoader, null);
    }

    public JSModuleRecord(JSModuleData parsedModule, JSModuleLoader moduleLoader, Object hostDefined) {
        super(parsedModule.getContext(), parsedModule.getSource(), hostDefined, parsedModule.isTopLevelAsync());
        this.parsedModule = parsedModule;
        this.moduleLoader = moduleLoader;
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

    @Override
    public FrameDescriptor getFrameDescriptor() {
        return parsedModule.getFrameDescriptor();
    }

    public JSModuleData getModuleData() {
        return parsedModule;
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
            AbstractModuleRecord requestedModule = getImportedModule(exportEntry.getModuleRequest());
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
                AbstractModuleRecord importedModule = getImportedModule(exportEntry.getModuleRequest());
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
            AbstractModuleRecord importedModule = getImportedModule(exportEntry.getModuleRequest());
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

    @Override
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
    public List<ModuleRequest> getRequestedModules() {
        return getModule().getRequestedModules();
    }

    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, Object hostDefinedArg) {
        return context.getEvaluator().loadRequestedModules(realm, this, hostDefinedArg);
    }

    @TruffleBoundary
    @Override
    public void initializeEnvironment(JSRealm realm) {
        assert getStatus() == Status.Linking : getStatus();
        Module module = this.getModule();
        for (ExportEntry exportEntry : module.getIndirectExportEntries()) {
            ExportResolution resolution = this.resolveExport(exportEntry.getExportName());
            if (resolution.isNull() || resolution.isAmbiguous()) {
                throw Errors.createSyntaxError("Could not resolve indirect export entry");
            }
        }

        // Initialize the environment by executing the module function.
        // It will automatically yield when the module is linked.
        var moduleFunction = JSFunction.create(realm, this.getFunctionData());
        Object[] arguments = JSArguments.create(Undefined.instance, moduleFunction, this);
        // The [[Construct]] target of a module is used to initialize the environment.
        JSFunction.getConstructTarget(moduleFunction).call(arguments);
    }

    @Override
    public Object executeModule(JSRealm realm, PromiseCapabilityRecord capability) {
        JSFunctionObject moduleFunction = JSFunction.create(realm, this.getFunctionData());
        if (!this.hasTLA()) {
            assert capability == null;
            return JSFunction.call(JSArguments.create(Undefined.instance, moduleFunction, this));
        } else {
            assert capability != null;
            return JSFunction.call(JSArguments.create(Undefined.instance, moduleFunction, this, capability));
        }
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
