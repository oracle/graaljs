/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.SymbolFunctionBuiltins;
import com.oracle.truffle.js.builtins.SymbolPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContext.BuiltinFunctionKey;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Object wrapper around a primitive symbol.
 *
 * @see Symbol
 */
public final class JSSymbol extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {

    public static final JSSymbol INSTANCE = new JSSymbol();

    public static final String TYPE_NAME = "symbol";
    public static final String CLASS_NAME = "Symbol";
    public static final String PROTOTYPE_NAME = CLASS_NAME + ".prototype";
    public static final String DESCRIPTION = "description";

    private JSSymbol() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, Symbol symbol) {
        DynamicObject mapObj = JSSymbolObject.create(realm, context.getSymbolFactory(), symbol);
        assert isJSSymbol(mapObj);
        return context.trackAllocation(mapObj);
    }

    public static Symbol getSymbolData(DynamicObject symbolWrapper) {
        assert JSSymbol.isJSSymbol(symbolWrapper);
        return ((JSSymbolObject) symbolWrapper).getSymbol();
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, SymbolPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        if (ctx.getContextOptions().getEcmaScriptVersion() >= JSConfig.ECMAScript2019) {
            JSObjectUtil.putBuiltinAccessorProperty(prototype, DESCRIPTION, createDescriptionGetterFunction(realm), Undefined.instance);
        }
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        Shape initialShape = JSObjectUtil.getProtoChildShape(prototype, JSSymbol.INSTANCE, context);
        return initialShape;
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, SymbolFunctionBuiltins.BUILTINS);
    }

    private static DynamicObject createDescriptionGetterFunction(JSRealm realm) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(BuiltinFunctionKey.SymbolGetDescription, (c) -> {
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final ConditionProfile isSymbolProfile = ConditionProfile.createBinaryProfile();
                private final ConditionProfile isJSSymbolProfile = ConditionProfile.createBinaryProfile();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (isSymbolProfile.profile(obj instanceof Symbol)) {
                        return ((Symbol) obj).getDescription();
                    } else if (isJSSymbolProfile.profile(isJSSymbol(obj))) {
                        return JSSymbol.getSymbolData((DynamicObject) obj).getDescription();
                    } else {
                        throw Errors.createTypeErrorSymbolExpected();
                    }
                }
            });
            return JSFunctionData.createCallOnly(c, callTarget, 0, "get " + DESCRIPTION);
        });

        return JSFunction.create(realm, getterData);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    @TruffleBoundary
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        return "[" + CLASS_NAME + "]";
    }

    public static boolean isJSSymbol(Object obj) {
        return obj instanceof JSSymbolObject;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getSymbolPrototype();
    }

}
