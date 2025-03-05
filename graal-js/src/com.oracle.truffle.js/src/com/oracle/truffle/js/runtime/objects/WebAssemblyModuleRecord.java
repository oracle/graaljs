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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.oracle.js.parser.ir.Module.ModuleRequest;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyBuiltins.WebAssemblyInstantiateNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * WebAssembly Module Record.
 */
public class WebAssemblyModuleRecord extends CyclicModuleRecord {

    private final JSWebAssemblyModuleObject webAssemblyModule;

    private List<ModuleRequest> requestedModules;
    private List<TruffleString> exportNameList;

    public WebAssemblyModuleRecord(JSContext context, Source source, JSWebAssemblyModuleObject webAssemblyModule) {
        super(context, source, null, null);
        this.webAssemblyModule = webAssemblyModule;
    }

    @Override
    public Object getModuleSource() {
        return webAssemblyModule;
    }

    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, Object hostDefined) {
        return context.getEvaluator().loadRequestedModules(realm, this, hostDefined);
    }

    @Override
    public List<ModuleRequest> getRequestedModules() {
        if (requestedModules != null) {
            return requestedModules;
        } else {
            return requestedModules = readRequestedModules();
        }
    }

    @TruffleBoundary
    private List<ModuleRequest> readRequestedModules() {
        JSRealm realm = JSRealm.get(null);
        try {
            Object importsArray = InteropLibrary.getUncached().execute(realm.getWASMModuleImports(), webAssemblyModule.getWASMModule());

            InteropLibrary importsArrayLib = InteropLibrary.getUncached(importsArray);
            int importsArrayLength = (int) importsArrayLib.getArraySize(importsArray);
            Set<ModuleRequest> requestedModuleSet = new LinkedHashSet<>();

            for (int i = 0; i < importsArrayLength; i++) {
                Object importDesc = importsArrayLib.readArrayElement(importsArray, i);

                TruffleString importedModuleName = Strings.interopAsTruffleString(InteropLibrary.getUncached().readMember(importDesc, "module"));
                requestedModuleSet.add(ModuleRequest.create(importedModuleName));
            }
            return List.copyOf(requestedModuleSet);
        } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | InvalidArrayIndexException | UnknownIdentifierException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
    }

    @Override
    public Collection<TruffleString> getExportedNames(Set<JSModuleRecord> exportStarSet) {
        if (exportNameList != null) {
            return exportNameList;
        } else {
            return exportNameList = readExportedNames();
        }
    }

    @TruffleBoundary
    private List<TruffleString> readExportedNames() {
        JSRealm realm = JSRealm.get(null);
        Object exportsFunction = realm.getWASMModuleExports();
        try {
            Object exportsArray = InteropLibrary.getUncached().execute(exportsFunction, webAssemblyModule.getWASMModule());

            InteropLibrary exportsArrayLib = InteropLibrary.getUncached(exportsArray);
            int exportsArrayLength = (int) exportsArrayLib.getArraySize(exportsArray);
            TruffleString[] exportNames = new TruffleString[exportsArrayLength];
            for (int i = 0; i < exportsArrayLength; i++) {
                Object exportDesc = exportsArrayLib.readArrayElement(exportsArray, i);
                TruffleString exportName = Strings.interopAsTruffleString(InteropLibrary.getUncached().readMember(exportDesc, "name"));
                exportNames[i] = exportName;
            }
            return List.of(exportNames);
        } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | InvalidArrayIndexException | UnknownIdentifierException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
    }

    @TruffleBoundary
    @Override
    public ExportResolution resolveExport(TruffleString exportName, Set<Pair<? extends AbstractModuleRecord, TruffleString>> resolveSet) {
        for (TruffleString name : getExportedNames()) {
            if (name.equals(exportName)) {
                return ExportResolution.resolved(this, name);
            }
        }
        return ExportResolution.notFound();
    }

    @TruffleBoundary
    @Override
    public void initializeEnvironment(JSRealm realm) {
        var exportNames = getExportedNames();
        int exportNameCount = exportNames.size();
        FrameDescriptor.Builder b = FrameDescriptor.newBuilder(exportNameCount);
        for (TruffleString name : exportNames) {
            b.addSlot(FrameSlotKind.Illegal, name, null);
        }
        FrameDescriptor desc = b.build();
        setFrameDescriptor(desc);
        MaterializedFrame env = Truffle.getRuntime().createMaterializedFrame(JSArguments.EMPTY_ARGUMENTS_ARRAY, desc);
        for (int i = 0; i < exportNameCount; i++) {
            env.clear(i); // mark as dead in temporal dead zone
        }
        setEnvironment(env);
    }

    @TruffleBoundary
    @Override
    public Object executeModule(JSRealm realm, PromiseCapabilityRecord promiseCapability) {
        assert promiseCapability == null;
        var importObject = JSOrdinary.createWithNullPrototype(context);
        for (ModuleRequest requestedModule : getRequestedModules()) {
            JSRuntime.createDataProperty(importObject, requestedModule.specifier(), JSOrdinary.createWithNullPrototype(context));
        }

        try {
            Object importsArray = InteropLibrary.getUncached().execute(realm.getWASMModuleImports(), webAssemblyModule.getWASMModule());

            InteropLibrary importsArrayLib = InteropLibrary.getUncached(importsArray);
            int importsArrayLength = (int) importsArrayLib.getArraySize(importsArray);

            for (int i = 0; i < importsArrayLength; i++) {
                Object importDesc = importsArrayLib.readArrayElement(importsArray, i);

                TruffleString importedModuleName = Strings.interopAsTruffleString(InteropLibrary.getUncached().readMember(importDesc, "module"));
                TruffleString name = Strings.interopAsTruffleString(InteropLibrary.getUncached().readMember(importDesc, "name"));

                AbstractModuleRecord importedModule = getImportedModule(ModuleRequest.create(importedModuleName));
                Object value = importedModule.getModuleNamespace().getValue(name);

                JSObject moduleImportsObject = (JSObject) importObject.getValue(importedModuleName);
                JSRuntime.createDataProperty(moduleImportsObject, name, value);
            }

            var instance = WebAssemblyInstantiateNode.instantiateModule(context, realm, webAssemblyModule.getWASMModule(), importObject, InteropLibrary.getUncached());
            MaterializedFrame environment = getEnvironment();
            int i = 0;
            for (var name : getExportedNames()) {
                environment.setObject(i++, instance.getExports().getValue(name));
            }
        } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException | InvalidArrayIndexException | UnknownIdentifierException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
        return Undefined.instance;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "WebAssembly.Module" + "@" + Integer.toHexString(System.identityHashCode(this)) + "[status=" + getStatus() + ", source=" + getSource() + "]";
    }
}
