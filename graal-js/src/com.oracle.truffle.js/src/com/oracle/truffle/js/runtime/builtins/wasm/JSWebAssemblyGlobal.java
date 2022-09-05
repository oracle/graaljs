/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyGlobalPrototypeBuiltins;
import com.oracle.truffle.js.nodes.wasm.ToJSValueNode;
import com.oracle.truffle.js.nodes.wasm.ToWebAssemblyValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSWebAssemblyGlobal extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {
    public static final TruffleString CLASS_NAME = Strings.constant("Global");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Global.prototype");
    public static final TruffleString VALUE = Strings.constant("value");

    public static final TruffleString WEB_ASSEMBLY_GLOBAL = Strings.constant("WebAssembly.Global");

    public static final JSWebAssemblyGlobal INSTANCE = new JSWebAssemblyGlobal();

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

    public static boolean isJSWebAssemblyGlobal(Object object) {
        return object instanceof JSWebAssemblyGlobalObject;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSContext ctx = realm.getContext();
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, constructor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, WebAssemblyGlobalPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorProperty(ctx, prototype, VALUE, createValueGetterFunction(realm), createValueSetterFunction(realm), JSAttributes.configurableEnumerableWritable());
        JSObjectUtil.putToStringTag(prototype, WEB_ASSEMBLY_GLOBAL);
        return prototype;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWebAssemblyGlobalPrototype();
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSWebAssemblyGlobalObject create(JSContext context, JSRealm realm, Object wasmGlobal, TruffleString valueType, boolean mutable) {
        Object embedderData = JSWebAssembly.getEmbedderData(realm, wasmGlobal);
        if (embedderData instanceof JSWebAssemblyGlobalObject) {
            return (JSWebAssemblyGlobalObject) embedderData;
        }
        JSObjectFactory factory = context.getWebAssemblyGlobalFactory();
        JSWebAssemblyGlobalObject object = new JSWebAssemblyGlobalObject(factory.getShape(realm), wasmGlobal, valueType, mutable);
        factory.initProto(object, realm);
        JSWebAssembly.setEmbedderData(realm, wasmGlobal, object);
        return context.trackAllocation(object);
    }

    private static JSFunctionObject createValueGetterFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.WebAssemblyGlobalGetValue, (c) -> {
            CallTarget callTarget = new JavaScriptRootNode(c.getLanguage(), null, null) {
                @Child ToJSValueNode toJSValueNode = ToJSValueNode.create();
                @Child InteropLibrary globalReadLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object thiz = JSFrameUtil.getThisObj(frame);
                    if (isJSWebAssemblyGlobal(thiz)) {
                        JSWebAssemblyGlobalObject object = (JSWebAssemblyGlobalObject) thiz;
                        Object wasmGlobal = object.getWASMGlobal();
                        Object globalRead = realm.getWASMGlobalRead();
                        try {
                            return toJSValueNode.execute(globalReadLib.execute(globalRead, wasmGlobal));
                        } catch (InteropException ex) {
                            throw Errors.shouldNotReachHere(ex);
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeError("get WebAssembly.Global.value: Receiver is not a WebAssembly.Global", this);
                    }
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(c, callTarget, 0, Strings.concat(Strings.GET_SPC, VALUE));
        });

        return JSFunction.create(realm, getterData);
    }

    private static JSFunctionObject createValueSetterFunction(JSRealm realm) {
        JSFunctionData setterData = realm.getContext().getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.WebAssemblyGlobalSetValue, (c) -> {
            CallTarget callTarget = new JavaScriptRootNode(c.getLanguage(), null, null) {
                @Child ToWebAssemblyValueNode toWebAssemblyValueNode = ToWebAssemblyValueNode.create();
                @Child InteropLibrary globalWriteLib = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object[] args = frame.getArguments();
                    Object thiz = JSArguments.getThisObject(args);
                    if (isJSWebAssemblyGlobal(thiz)) {
                        JSWebAssemblyGlobalObject global = (JSWebAssemblyGlobalObject) thiz;
                        if(!global.isMutable()) {
                            errorBranch.enter();
                            throw Errors.createTypeError("set WebAssembly.Global.value: Global is constant");
                        }
                        Object wasmGlobal = global.getWASMGlobal();
                        try {
                            Object value;
                            if (JSArguments.getUserArgumentCount(args) == 0) {
                                errorBranch.enter();
                                throw Errors.createTypeError("set WebAssembly.Global.value: Argument 0 is required");
                            } else {
                                value = JSArguments.getUserArgument(args, 0);
                            }
                            Object webAssemblyValue = toWebAssemblyValueNode.execute(value, global.getValueType());
                            Object globalWrite = realm.getWASMGlobalWrite();
                            globalWriteLib.execute(globalWrite, wasmGlobal, webAssemblyValue);
                            return Undefined.instance;
                        } catch (InteropException ex) {
                            throw Errors.shouldNotReachHere(ex);
                        } catch (AbstractTruffleException ex) {
                            errorBranch.enter();
                            throw Errors.createTypeError(ex, this);
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeError("set WebAssembly.Global.value: Receiver is not a WebAssembly.Global", this);
                    }
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(c, callTarget, 1, Strings.concat(Strings.SET_SPC, VALUE));
        });

        return JSFunction.create(realm, setterData);
    }

}
