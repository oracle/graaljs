/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.wasm;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyInstancePrototypeBuiltins;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNodeGen;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNodeGen;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSWebAssemblyInstance extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final TruffleString CLASS_NAME = Strings.constant("Instance");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Instance.prototype");
    private static final TruffleString MUT = Strings.constant("mut");

    public static final TruffleString WEB_ASSEMBLY_INSTANCE = Strings.constant("WebAssembly.Instance");

    public static final JSWebAssemblyInstance INSTANCE = new JSWebAssemblyInstance();
    static final TruffleString FUNCTION_ADAPTER_NAME = Strings.constant("wasm-function-adapter");

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, WebAssemblyInstancePrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, WEB_ASSEMBLY_INSTANCE);
        return prototype;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWebAssemblyInstancePrototype();
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSWebAssemblyInstanceObject create(JSContext context, JSRealm realm, Object wasmInstance, Object wasmModule) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm), wasmInstance, wasmModule);
    }

    public static JSWebAssemblyInstanceObject create(JSContext context, JSRealm realm, JSDynamicObject proto, Object wasmInstance, Object wasmModule) {
        JSObjectFactory factory = context.getWebAssemblyInstanceFactory();
        JSObject exportsObject = createExportsObject(context, realm, wasmInstance, wasmModule);
        var shape = factory.getShape(realm, proto);
        var newObj = factory.initProto(new JSWebAssemblyInstanceObject(shape, proto, wasmInstance, exportsObject), realm, proto);
        return factory.trackAllocation(newObj);
    }

    @TruffleBoundary
    private static JSObject createExportsObject(JSContext context, JSRealm realm, Object wasmInstance, Object wasmModule) {
        JSObject exports = JSOrdinary.createWithNullPrototype(context);
        try {
            Object exportsFunction = realm.getWASMModuleExports();
            Object exportsInfo = InteropLibrary.getUncached(exportsFunction).execute(exportsFunction, wasmModule);
            Object instanceExport = realm.getWASMInstanceExport();
            InteropLibrary exportsInterop = InteropLibrary.getUncached(exportsInfo);
            long size = exportsInterop.getArraySize(exportsInfo);

            for (long i = 0; i < size; i++) {
                Object exportInfo = exportsInterop.readArrayElement(exportsInfo, i);
                InteropLibrary exportInterop = InteropLibrary.getUncached(exportInfo);
                TruffleString name = asTString(exportInterop.readMember(exportInfo, "name"));
                TruffleString externtype = asTString(exportInterop.readMember(exportInfo, "kind"));
                Object externval = InteropLibrary.getUncached().execute(instanceExport, wasmInstance, Strings.toJavaString(name));
                Object value;

                if (Strings.equals(Strings.FUNCTION, externtype)) {
                    TruffleString typeInfo = asTString(exportInterop.readMember(exportInfo, "type"));
                    value = exportFunction(context, realm, externval, typeInfo);
                } else if (Strings.equals(Strings.GLOBAL, externtype)) {
                    TruffleString type = asTString(exportInterop.readMember(exportInfo, "type"));
                    int sepIndex = Strings.indexOf(type, ' ');
                    final TruffleString valueTypeStr = Strings.substring(context, type, 0, sepIndex);
                    WebAssemblyValueType valueType = WebAssemblyValueType.valueOf(valueTypeStr.toJavaStringUncached());
                    final boolean mutable = Strings.regionEquals(type, sepIndex + 1, MUT, 0, 3);
                    value = JSWebAssemblyGlobal.create(context, realm, externval, valueType, mutable);
                } else if (Strings.MEMORY.equals(externtype)) {
                    TruffleString type = asTString(exportInterop.readMember(exportInfo, "type"));
                    final boolean shared = Strings.regionEquals(type, 0, Strings.SHARED, 0, 6);
                    value = JSWebAssemblyMemory.create(context, realm, externval, shared);
                } else {
                    assert Strings.TABLE.equals(externtype);
                    TruffleString typeStr = asTString(exportInterop.readMember(exportInfo, "type"));
                    WebAssemblyValueType type = WebAssemblyValueType.valueOf(typeStr.toJavaStringUncached());
                    value = JSWebAssemblyTable.create(context, realm, externval, type);
                }

                JSObject.set(exports, name, value);
            }
        } catch (InteropException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
        exports.setIntegrityLevel(true, true);
        return exports;
    }

    @TruffleBoundary
    public static Object exportFunction(JSContext context, JSRealm realm, Object export, TruffleString typeInfo) {
        Object embedderData = JSWebAssembly.getEmbedderData(realm, export);
        if (embedderData instanceof JSFunctionObject) {
            return embedderData;
        }

        WasmFunctionTypeInfo typeInfoKey = parseWasmFunctionTypeInfo(context, typeInfo);
        TruffleString name = Strings.substring(context, typeInfo, 0, Strings.indexOf(typeInfo, '('));
        JSFunctionData functionData = getOrCreateExportedWasmFunctionAdapter(context, typeInfoKey);

        JSFunctionObject result = JSFunction.create(realm, functionData);
        JSFunction.setFunctionName(result, name);
        JSObjectUtil.putHiddenProperty(result, JSWebAssembly.FUNCTION_ADDRESS, export);
        JSWebAssembly.setEmbedderData(realm, export, result);
        return result;
    }

    private static WasmFunctionTypeInfo parseWasmFunctionTypeInfo(JSContext context, TruffleString typeInfo) {
        int idxOpen = Strings.indexOf(typeInfo, '(');
        int idxClose = Strings.indexOf(typeInfo, ')');
        TruffleString argTypes = Strings.lazySubstring(typeInfo, idxOpen + 1, idxClose - (idxOpen + 1));
        TruffleString returnTypes = Strings.lazySubstring(typeInfo, idxClose + 1);
        WebAssemblyValueType[] paramTypes = parseTypeSequence(context, argTypes);
        WebAssemblyValueType[] resultTypes = parseTypeSequence(context, returnTypes);
        boolean anyReturnTypeIsI64 = Arrays.asList(resultTypes).contains(WebAssemblyValueType.i64);
        boolean anyArgTypeIsI64 = Arrays.asList(paramTypes).contains(WebAssemblyValueType.i64);
        boolean anyReturnTypeIsV128 = Arrays.asList(resultTypes).contains(WebAssemblyValueType.v128);
        boolean anyArgTypeIsV128 = Arrays.asList(paramTypes).contains(WebAssemblyValueType.v128);
        return new WasmFunctionTypeInfo(paramTypes, resultTypes, anyReturnTypeIsI64 || anyArgTypeIsI64, anyReturnTypeIsV128 || anyArgTypeIsV128);
    }

    private static WebAssemblyValueType[] parseTypeSequence(JSContext context, TruffleString typeString) {
        TruffleString[] types = Strings.split(context, typeString, Strings.SPACE);
        WebAssemblyValueType[] result = new WebAssemblyValueType[types.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = WebAssemblyValueType.lookupType(types[i].toJavaStringUncached());
        }
        return result;
    }

    private static JSFunctionData getOrCreateExportedWasmFunctionAdapter(JSContext context, WasmFunctionTypeInfo funcType) {
        Map<WasmFunctionTypeInfo, JSFunctionData> cache = context.getWebAssemblyCache().wasmFunctionAdapterCache;
        JSFunctionData functionData = cache.get(funcType);
        if (functionData == null) {
            functionData = cache.computeIfAbsent(funcType, k -> {
                CallTarget callTarget = new WasmToJSFunctionAdapterRootNode(context, funcType).getCallTarget();
                return JSFunctionData.createCallOnly(context, callTarget, funcType.paramLength(), FUNCTION_ADAPTER_NAME);
            });
        }
        return functionData;
    }

    private static class WasmToJSFunctionAdapterRootNode extends JavaScriptRootNode {
        private static final int MAX_UNROLL = 32;
        private final JSContext context;
        private final WasmFunctionTypeInfo type;

        @Child ToWebAssemblyValueNode toWebAssemblyValueNode = ToWebAssemblyValueNodeGen.create();
        @Child ToJSValueNode toJSValueNode = ToJSValueNodeGen.create();
        private final BranchProfile errorBranch = BranchProfile.create();
        @Child InteropLibrary exportFunctionLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child InteropLibrary readArrayElementLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
        @Child DynamicObjectLibrary getExportedFunctionLib = JSObjectUtil.createDispatched(JSWebAssembly.FUNCTION_ADDRESS);

        WasmToJSFunctionAdapterRootNode(JSContext context, WasmFunctionTypeInfo type) {
            super(context.getLanguage(), null, null);
            this.context = context;
            this.type = type;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if ((!context.getLanguageOptions().wasmBigInt() && type.anyTypeIsI64()) || type.anyTypeIsV128()) {
                errorBranch.enter();
                throw Errors.createTypeError("wasm function signature contains illegal type");
            }
            int argCount = type.paramLength();
            int returnLength = type.resultLength();

            Object[] frameArguments = frame.getArguments();
            Object[] wasmArgs = convertArgsToWasm(frameArguments, argCount);

            Object export = getExportedFunctionLib.getOrDefault(JSFrameUtil.getFunctionObject(frame), JSWebAssembly.FUNCTION_ADDRESS, null);
            try {
                Object wasmResult;
                try {
                    wasmResult = exportFunctionLib.execute(export, wasmArgs);
                } catch (GraalJSException jsex) {
                    errorBranch.enter();
                    throw jsex;
                } catch (AbstractTruffleException tex) {
                    errorBranch.enter();
                    ExceptionType exceptionType = InteropLibrary.getUncached(tex).getExceptionType(tex);
                    if (exceptionType == ExceptionType.INTERRUPT || exceptionType == ExceptionType.EXIT) {
                        throw tex;
                    } else {
                        throw Errors.createRuntimeError(tex, this);
                    }
                }

                if (returnLength == 0) {
                    return Undefined.instance;
                } else if (returnLength == 1) {
                    return toJSValueNode.execute(wasmResult);
                } else {
                    Object[] values = new Object[returnLength];
                    for (int i = 0; i < returnLength; i++) {
                        values[i] = toJSValueNode.execute(readArrayElementLib.readArrayElement(wasmResult, i));
                    }
                    return JSArray.createConstantObjectArray(context, getRealm(), values);
                }
            } catch (InteropException ex) {
                throw Errors.shouldNotReachHere(ex);
            }
        }

        @ExplodeLoop
        private Object[] convertArgsToWasm(Object[] frameArguments, int paramCount) {
            CompilerAsserts.partialEvaluationConstant(paramCount);
            if (paramCount > MAX_UNROLL) {
                return convertArgsToWasmNoUnroll(frameArguments, paramCount);
            }
            Object[] wasmArgs = new Object[paramCount];
            for (int i = 0; i < paramCount; i++) {
                wasmArgs[i] = convertArgToWasm(frameArguments, i);
            }
            return wasmArgs;
        }

        private Object[] convertArgsToWasmNoUnroll(Object[] frameArguments, int paramCount) {
            Object[] wasmArgs = new Object[paramCount];
            for (int i = 0; i < paramCount; i++) {
                wasmArgs[i] = convertArgToWasm(frameArguments, i);
            }
            return wasmArgs;
        }

        private Object convertArgToWasm(Object[] frameArguments, int i) {
            Object wasmArg;
            if (i < JSArguments.getUserArgumentCount(frameArguments)) {
                wasmArg = JSArguments.getUserArgument(frameArguments, i);
            } else {
                wasmArg = Undefined.instance;
            }
            return toWebAssemblyValueNode.execute(wasmArg, type.paramTypes()[i]);
        }

        @Override
        public String toString() {
            return FUNCTION_ADAPTER_NAME.toJavaStringUncached() + ":" + type.toString();
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Object transformImportObject(JSContext context, JSRealm realm, Object wasmModule, Object importObject) {
        try {
            JSObject transformedImportObject = JSOrdinary.createWithNullPrototype(context);

            Object importsFn = realm.getWASMModuleImports();
            Object imports = InteropLibrary.getUncached(importsFn).execute(importsFn, wasmModule);
            InteropLibrary importsInterop = InteropLibrary.getUncached(imports);
            long size = importsInterop.getArraySize(imports);
            for (long i = 0; i < size; i++) {
                Object descriptor = importsInterop.readArrayElement(imports, i);
                InteropLibrary descriptorInterop = InteropLibrary.getUncached(descriptor);

                TruffleString module = asTString(descriptorInterop.readMember(descriptor, "module"));
                Object moduleImportObject = JSRuntime.get(importObject, module);
                InteropLibrary moduleImportObjectInterop = InteropLibrary.getUncached(moduleImportObject);
                if (!moduleImportObjectInterop.hasMembers(moduleImportObject)) {
                    throw Errors.createTypeError("Imported module \"" + module + "\" is not an object: " + JSRuntime.safeToString(moduleImportObject));
                }

                TruffleString name = asTString(descriptorInterop.readMember(descriptor, "name"));
                Object value = JSRuntime.get(moduleImportObject, name);
                TruffleString externType = asTString(descriptorInterop.readMember(descriptor, "kind"));
                Object wasmValue;

                if (Strings.equals(Strings.FUNCTION, externType)) {
                    if (!JSRuntime.isCallable(value)) {
                        throw createLinkErrorImport(i, module, name, "Imported value is not callable");
                    }
                    if (JSWebAssembly.isExportedFunction(value)) {
                        wasmValue = JSWebAssembly.getExportedFunction((JSDynamicObject) value);
                    } else {
                        TruffleString typeInfo = asTString(descriptorInterop.readMember(descriptor, "type"));
                        wasmValue = createHostFunction(value, parseWasmFunctionTypeInfo(context, typeInfo));
                    }
                } else if (Strings.equals(Strings.GLOBAL, externType)) {
                    boolean isNumber = JSRuntime.isNumber(value);
                    boolean isBigInt = JSRuntime.isBigInt(value);
                    if (isNumber || context.getLanguageOptions().wasmBigInt() && isBigInt) {
                        TruffleString valueTypeStr = asTString(descriptorInterop.readMember(descriptor, "type"));
                        WebAssemblyValueType valueType = WebAssemblyValueType.lookupType(valueTypeStr.toJavaStringUncached());
                        boolean isI64 = valueType == WebAssemblyValueType.i64;
                        if (!context.getLanguageOptions().wasmBigInt() && isI64) {
                            throw createLinkErrorImport(i, module, name, "Can't import the value of i64 WebAssembly.Global");
                        }
                        if (isI64 && isNumber) {
                            throw createLinkErrorImport(i, module, name, "Value of valtype i64 must be BigInt");
                        }
                        if (!isI64 && isBigInt) {
                            throw createLinkErrorImport(i, module, name, "BigInt can only be stored in valtype i64");
                        }
                        if (valueType == WebAssemblyValueType.v128) {
                            throw createLinkErrorImport(i, module, name, "Values of valtype v128 cannot be imported from JS");
                        }
                        Object webAssemblyValue = ToWebAssemblyValueNodeGen.getUncached().execute(value, valueType);
                        try {
                            Object createGlobal = realm.getWASMGlobalAlloc();
                            wasmValue = InteropLibrary.getUncached(createGlobal).execute(createGlobal, valueTypeStr, false, webAssemblyValue);
                        } catch (InteropException ex) {
                            throw Errors.shouldNotReachHere(ex);
                        }
                    } else if (JSWebAssemblyGlobal.isJSWebAssemblyGlobal(value)) {
                        wasmValue = ((JSWebAssemblyGlobalObject) value).getWASMGlobal();
                    } else {
                        throw createLinkErrorImport(i, module, name, "Imported value is not a WebAssembly.Global object");
                    }
                } else if (Strings.equals(Strings.MEMORY, externType)) {
                    if (JSWebAssemblyMemory.isJSWebAssemblyMemory(value)) {
                        wasmValue = ((JSWebAssemblyMemoryObject) value).getWASMMemory();
                    } else {
                        throw createLinkErrorImport(i, module, name, "Imported value is not a WebAssembly.Memory object");
                    }
                } else {
                    assert Strings.equals(Strings.TABLE, externType) : externType;
                    if (JSWebAssemblyTable.isJSWebAssemblyTable(value)) {
                        wasmValue = ((JSWebAssemblyTableObject) value).getWASMTable();
                    } else {
                        throw createLinkErrorImport(i, module, name, "Imported value is not a WebAssembly.Table object");
                    }
                }

                JSDynamicObject transformedModule;
                if (JSObject.hasOwnProperty(transformedImportObject, module)) {
                    transformedModule = (JSDynamicObject) JSObject.get(transformedImportObject, module);
                } else {
                    transformedModule = JSOrdinary.create(context, realm);
                    JSObject.set(transformedImportObject, module, transformedModule);
                }
                JSObject.set(transformedModule, name, wasmValue);
            }
            return transformedImportObject;
        } catch (InteropException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
    }

    @TruffleBoundary
    private static JSException createLinkErrorImport(long index, TruffleString module, TruffleString name, String message) {
        return Errors.createLinkError("Import #" + index + " \"" + module + "\" \"" + name + "\": " + message);
    }

    @TruffleBoundary
    private static Object createHostFunction(Object fn, WasmFunctionTypeInfo typeInfo) {
        return new WebAssemblyHostFunction(fn, typeInfo);
    }

    private static TruffleString asTString(Object string) {
        if (string instanceof String) {
            return Strings.fromJavaString((String) string);
        }
        return Strings.interopAsTruffleString(string);
    }

    public static final class Cache {
        final Map<WasmFunctionTypeInfo, JSFunctionData> wasmFunctionAdapterCache = new ConcurrentHashMap<>();
    }
}
