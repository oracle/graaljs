/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSWebAssemblyInstance extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final String CLASS_NAME = "Instance";
    public static final String EXPORTS = "exports";

    public static final JSWebAssemblyInstance INSTANCE = new JSWebAssemblyInstance();

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    public static boolean isJSWebAssemblyInstance(Object object) {
        return object instanceof JSWebAssemblyInstanceObject;
    }

    @Override
    public DynamicObject createPrototype(JSRealm realm, DynamicObject constructor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);
        JSObjectUtil.putAccessorProperty(ctx, prototype, EXPORTS, createExportsGetterFunction(realm), Undefined.instance, JSAttributes.configurableEnumerableWritable());
        JSObjectUtil.putToStringTag(prototype, "WebAssembly.Instance");
        return prototype;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWebAssemblyInstancePrototype();
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSWebAssemblyInstanceObject create(JSContext context, JSRealm realm, Object wasmInstance, Object wasmModule) {
        JSObjectFactory factory = context.getWebAssemblyInstanceFactory();
        Object exportsObject = createExportsObject(context, realm, wasmInstance, wasmModule);
        JSWebAssemblyInstanceObject object = new JSWebAssemblyInstanceObject(factory.getShape(realm), wasmInstance, exportsObject);
        factory.initProto(object, realm);
        return context.trackAllocation(object);
    }

    private static DynamicObject createExportsGetterFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.WebAssemblyInstanceGetExports, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    if (isJSWebAssemblyInstance(thiz)) {
                        return ((JSWebAssemblyInstanceObject) thiz).getExports();
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeError("WebAssembly.Instance.exports(): Receiver is not a WebAssembly.Instance", this);
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get " + EXPORTS);
        });

        return JSFunction.create(realm, getterData);
    }

    private static Object createExportsObject(JSContext context, JSRealm realm, Object wasmInstance, Object wasmModule) {
        DynamicObject exports = JSOrdinary.createWithNullPrototype(context);
        try {
            Object exportsFunction = realm.getWASMModuleExports();
            Object exportsInfo = InteropLibrary.getUncached(exportsFunction).execute(exportsFunction, wasmModule);
            Object instanceExport = realm.getWASMInstanceExport();
            InteropLibrary exportsInterop = InteropLibrary.getUncached(exportsInfo);
            long size = exportsInterop.getArraySize(exportsInfo);

            for (long i = 0; i < size; i++) {
                Object exportInfo = exportsInterop.readArrayElement(exportsInfo, i);
                InteropLibrary exportInterop = InteropLibrary.getUncached(exportInfo);
                String name = asString(exportInterop.readMember(exportInfo, "name"));
                String externtype = asString(exportInterop.readMember(exportInfo, "kind"));
                Object externval = InteropLibrary.getUncached().execute(instanceExport, wasmInstance, name);
                Object value;

                if ("function".equals(externtype)) {
                    String typeInfo = asString(exportInterop.readMember(exportInfo, "type"));
                    value = exportFunction(context, realm, externval, typeInfo);
                } else if ("global".equals(externtype)) {
                    String valueType = asString(exportInterop.readMember(exportInfo, "type"));
                    value = JSWebAssemblyGlobal.create(context, realm, externval, valueType);
                } else if ("memory".equals(externtype)) {
                    value = JSWebAssemblyMemory.create(context, realm, externval);
                } else {
                    assert "table".equals(externtype);
                    value = JSWebAssemblyTable.create(context, realm, externval);
                }

                JSObject.set(exports, name, value);
            }
        } catch (InteropException ex) {
            throw Errors.shouldNotReachHere(ex);
        }
        JSObject.setIntegrityLevel(exports, true);
        return exports;
    }

    @CompilerDirectives.TruffleBoundary
    public static Object exportFunction(JSContext context, JSRealm realm, Object export, String typeInfo) {
        Object embedderData = JSWebAssembly.getEmbedderData(realm, export);
        if (embedderData instanceof JSFunctionObject) {
            return embedderData;
        }

        int idxOpen = typeInfo.indexOf('(');
        int idxClose = typeInfo.indexOf(')');
        String name = typeInfo.substring(0, idxOpen);
        String argTypes = typeInfo.substring(idxOpen + 1, idxClose);
        String returnType = typeInfo.substring(idxClose + 1);
        String[] paramTypes = argTypes.length() != 0 ? argTypes.split(" ") : new String[0];
        int argCount = paramTypes.length;
        boolean returnTypeIsI64 = JSWebAssemblyValueTypes.isI64(returnType);
        boolean anyArgTypeIsI64 = argTypes.contains(JSWebAssemblyValueTypes.I64);

        CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
            @Child ToWebAssemblyValueNode toWebAssemblyValueNode = ToWebAssemblyValueNode.create();
            @Child ToJSValueNode toJSValueNode = ToJSValueNode.create();
            private final BranchProfile errorBranch = BranchProfile.create();
            @Child InteropLibrary exportFunctionLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
            @CompilationFinal(dimensions = 1) String[] argTypesArray = paramTypes;

            @Override
            public Object execute(VirtualFrame frame) {
                if (!context.getContextOptions().isWasmBigInt() && (returnTypeIsI64 || anyArgTypeIsI64)) {
                    errorBranch.enter();
                    throw Errors.createTypeError("wasm function signature contains illegal type");
                }

                Object[] frameArguments = frame.getArguments();
                int userArgumentCount = JSArguments.getUserArgumentCount(frameArguments);
                Object[] wasmArgs = new Object[argCount];
                for (int i = 0; i < argCount; i++) {
                    Object wasmArg;
                    if (i < userArgumentCount) {
                        wasmArg = JSArguments.getUserArgument(frameArguments, i);
                    } else {
                        wasmArg = Undefined.instance;
                    }
                    wasmArgs[i] = toWebAssemblyValueNode.execute(wasmArg, argTypesArray[i]);
                }

                try {
                    Object wasmResult;
                    try {
                        wasmResult = exportFunctionLib.execute(export, wasmArgs);
                    } catch (GraalJSException jsex) {
                        errorBranch.enter();
                        throw jsex;
                    } catch (AbstractTruffleException tex) {
                        errorBranch.enter();
                        ExceptionType type = InteropLibrary.getUncached(tex).getExceptionType(tex);
                        if (type == ExceptionType.INTERRUPT || type == ExceptionType.EXIT) {
                            throw tex;
                        } else {
                            throw Errors.createRuntimeError(tex, this);
                        }
                    }

                    if (returnType.isEmpty()) {
                        return Undefined.instance;
                    } else {
                        return toJSValueNode.execute(wasmResult);
                    }
                } catch (InteropException ex) {
                    throw Errors.shouldNotReachHere(ex);
                }
            }
        });

        JSFunctionData functionData = JSFunctionData.createCallOnly(context, callTarget, argCount, name);
        DynamicObject result = JSFunction.create(realm, functionData);
        JSObjectUtil.putHiddenProperty(result, JSWebAssembly.FUNCTION_ADDRESS, export);
        JSWebAssembly.setEmbedderData(realm, export, result);
        return result;
    }

    @CompilerDirectives.TruffleBoundary
    public static Object transformImportObject(JSContext context, JSRealm realm, Object wasmModule, Object importObject) {
        try {
            DynamicObject transformedImportObject = JSOrdinary.create(context, realm);

            Object importsFn = realm.getWASMModuleImports();
            Object imports = InteropLibrary.getUncached(importsFn).execute(importsFn, wasmModule);
            InteropLibrary importsInterop = InteropLibrary.getUncached(imports);
            long size = importsInterop.getArraySize(imports);
            for (long i = 0; i < size; i++) {
                Object descriptor = importsInterop.readArrayElement(imports, i);
                InteropLibrary descriptorInterop = InteropLibrary.getUncached(descriptor);

                String module = asString(descriptorInterop.readMember(descriptor, "module"));
                Object moduleImportObject = JSInteropUtil.get(importObject, module);
                InteropLibrary moduleImportObjectInterop = InteropLibrary.getUncached(moduleImportObject);
                if (!moduleImportObjectInterop.hasMembers(moduleImportObject)) {
                    throw Errors.createTypeErrorNotAnObject(moduleImportObject);
                }

                String name = asString(descriptorInterop.readMember(descriptor, "name"));
                Object value = JSInteropUtil.get(moduleImportObject, name);
                String externType = asString(descriptorInterop.readMember(descriptor, "kind"));
                Object wasmValue;

                if ("function".equals(externType)) {
                    if (!JSRuntime.isCallable(value)) {
                        throw Errors.createLinkError("Imported value is not callable");
                    }
                    if (JSWebAssembly.isExportedFunction(value)) {
                        wasmValue = JSWebAssembly.getExportedFunction((DynamicObject) value);
                    } else {
                        String typeInfo = asString(descriptorInterop.readMember(descriptor, "type"));
                        wasmValue = createHostFunction(value, typeInfo);
                    }
                } else if ("global".equals(externType)) {
                    boolean isNumber = JSRuntime.isNumber(value);
                    boolean isBigInt = JSRuntime.isBigInt(value);
                    if (isNumber || context.getContextOptions().isWasmBigInt() && isBigInt) {
                        String valueType = asString(descriptorInterop.readMember(descriptor, "type"));
                        boolean isI64 = JSWebAssemblyValueTypes.isI64(valueType);
                        if (!context.getContextOptions().isWasmBigInt() && isI64) {
                            throw Errors.createLinkError("Can't import the value of i64 WebAssembly.Global");
                        }
                        if (isI64 && isNumber) {
                            throw Errors.createLinkError("Value of valtype i64 must be BigInt");
                        }
                        if (!isI64 && isBigInt) {
                            throw Errors.createLinkError("BigInt can only be stored in valtype i64");
                        }
                        Object webAssemblyValue = ToWebAssemblyValueNode.getUncached().execute(value, valueType);
                        try {
                            Object createGlobal = realm.getWASMGlobalAlloc();
                            wasmValue = InteropLibrary.getUncached(createGlobal).execute(createGlobal, valueType, false, webAssemblyValue);
                        } catch (InteropException ex) {
                            throw Errors.shouldNotReachHere(ex);
                        }
                    } else if (JSWebAssemblyGlobal.isJSWebAssemblyGlobal(value)) {
                        wasmValue = ((JSWebAssemblyGlobalObject) value).getWASMGlobal();
                    } else {
                        throw Errors.createLinkError("Imported value is not WebAssembly.Global object");
                    }
                } else if ("memory".equals(externType)) {
                    if (JSWebAssemblyMemory.isJSWebAssemblyMemory(value)) {
                        wasmValue = ((JSWebAssemblyMemoryObject) value).getWASMMemory();
                    } else {
                        throw Errors.createLinkError("Imported value is not WebAssembly.Memory object");
                    }
                } else {
                    assert "table".equals(externType) : externType;
                    if (JSWebAssemblyTable.isJSWebAssemblyTable(value)) {
                        wasmValue = ((JSWebAssemblyTableObject) value).getWASMTable();
                    } else {
                        throw Errors.createLinkError("Imported value is not WebAssembly.Table object");
                    }
                }

                DynamicObject transformedModule;
                if (JSObject.hasOwnProperty(transformedImportObject, module)) {
                    transformedModule = (DynamicObject) JSObject.get(transformedImportObject, module);
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

    @CompilerDirectives.TruffleBoundary
    private static Object createHostFunction(Object fn, String typeInfo) {
        return new WebAssemblyHostFunction(fn, typeInfo);
    }

    private static String asString(Object string) throws UnsupportedMessageException {
        if (string instanceof String) {
            return (String) string;
        }
        return InteropLibrary.getUncached(string).asString(string);
    }

}
