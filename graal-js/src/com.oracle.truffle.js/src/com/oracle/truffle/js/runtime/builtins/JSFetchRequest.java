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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.FetchRequestPrototypeBuiltins;
import com.oracle.truffle.js.builtins.helper.FetchHeaders;
import com.oracle.truffle.js.builtins.helper.FetchRequest;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * https://fetch.spec.whatwg.org/#request-class.
 */
public final class JSFetchRequest extends JSNonProxy implements JSConstructorFactory.Default.Default, PrototypeSupplier {
    public static final JSFetchRequest INSTANCE = new JSFetchRequest();
    public static final TruffleString CLASS_NAME = Strings.constant("Request");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Request.prototype");

    // getter names
    private static final TruffleString METHOD = Strings.constant("method");
    private static final TruffleString URL = Strings.constant("url");
    private static final TruffleString REFERRER = Strings.constant("referrer");
    private static final TruffleString REFERRER_POLICY = Strings.constant("referrerPolicy");
    private static final TruffleString REDIRECT = Strings.constant("redirect");
    private static final TruffleString HEADERS = Strings.constant("headers");
    // body getter names
    private static final TruffleString BODY = Strings.constant("body");
    private static final TruffleString BODY_USED = Strings.constant("bodyUsed");

    private JSFetchRequest() { }

    public static boolean isJSFetchRequest(Object obj) {
        return obj instanceof JSFetchRequestObject;
    }

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    @Override
    public TruffleString getClassName(JSDynamicObject object) {
        return getClassName();
    }

    @Override
    public TruffleString getBuiltinToStringTag(JSDynamicObject object) {
        return getClassName(object);
    }

    public static FetchRequest getInternalData(JSDynamicObject obj) {
        assert isJSFetchRequest(obj);
        return ((JSFetchRequestObject) obj).getRequestMap();
    }

    private static TruffleString getMethod(JSDynamicObject obj) {
        String method = getInternalData(obj).getMethod();
        return TruffleString.fromJavaStringUncached(method, TruffleString.Encoding.UTF_8);
    }

    private static TruffleString getUrl(JSDynamicObject obj) {
        String url = getInternalData(obj).getUrl().toString();
        return TruffleString.fromJavaStringUncached(url, TruffleString.Encoding.UTF_8);
    }

    // https://fetch.spec.whatwg.org/#dom-request-referrer
    private static Object getReferrer(JSDynamicObject obj) {
        String referrer = getInternalData(obj).getReferrer();

        if (referrer == null) {
            return Undefined.instance;
        }
        if (referrer.equals("no-referrer")) {
            return TruffleString.fromJavaStringUncached("", TruffleString.Encoding.UTF_8);
        }
        if (referrer.equals("client")) {
            return TruffleString.fromJavaStringUncached("about:client", TruffleString.Encoding.UTF_8);
        }

        return TruffleString.fromJavaStringUncached(referrer, TruffleString.Encoding.UTF_8);
    }

    private static TruffleString getReferrerPolicy(JSDynamicObject obj) {
        String referrerPolicy = getInternalData(obj).getReferrerPolicy();
        return TruffleString.fromJavaStringUncached(referrerPolicy, TruffleString.Encoding.UTF_8);
    }

    private static TruffleString getRedirectMode(JSDynamicObject obj) {
        String redirect = getInternalData(obj).getRedirectMode();
        return TruffleString.fromJavaStringUncached(redirect, TruffleString.Encoding.UTF_8);
    }

    private static FetchHeaders getHeaders(JSDynamicObject obj) {
        return getInternalData(obj).getHeaders();
    }

    private static Object getBody(JSDynamicObject obj) {
        String body = getInternalData(obj).getBody();
        if (body == null) {
            return Null.instance;
        }
        return TruffleString.fromJavaStringUncached(body, TruffleString.Encoding.UTF_8);
    }

    private static boolean getBodyUsed(JSDynamicObject obj) {
        return getInternalData(obj).isBodyUsed();
    }

    private static JSDynamicObject createGetterFunction(JSRealm realm, TruffleString name, JSContext.BuiltinFunctionKey key) {
        JSFunctionData getterData = realm.getContext().getOrCreateBuiltinFunctionData(key, (c) -> {
            CallTarget callTarget = new JavaScriptRootNode(c.getLanguage(), null, null) {
                private final BranchProfile errorBranch = BranchProfile.create();

                @Override
                public Object execute(VirtualFrame frame) {
                    Object obj = frame.getArguments()[0];
                    if (JSFetchRequest.isJSFetchRequest(obj)) {
                        switch (key) {
                            case FetchRequestGetUrl:
                                return JSFetchRequest.getUrl((JSFetchRequestObject) obj);
                            case FetchRequestGetMethod:
                                return JSFetchRequest.getMethod((JSFetchRequestObject) obj);
                            case FetchRequestGetReferrer:
                                return JSFetchRequest.getReferrer((JSFetchRequestObject) obj);
                            case FetchRequestGetReferrerPolicy:
                                return JSFetchRequest.getReferrerPolicy((JSFetchRequestObject) obj);
                            case FetchRequestGetRedirect:
                                return JSFetchRequest.getRedirectMode((JSFetchRequestObject) obj);
                            case FetchRequestGetHeaders:
                                FetchHeaders headers = getHeaders((JSFetchRequestObject) obj);
                                return JSFetchHeaders.create(realm.getContext(), realm, headers);
                            case FetchRequestGetBody:
                                return JSFetchRequest.getBody((JSFetchRequestObject) obj);
                            case FetchRequestGetBodyUsed:
                                return JSFetchRequest.getBodyUsed((JSFetchRequestObject) obj);
                            default:
                                throw new IllegalArgumentException("FetchRequest getter function key expected");
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeError("Request expected");
                    }
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(c, callTarget, 0, Strings.concat(Strings.GET_SPC, name));
        });
        return JSFunction.create(realm, getterData);
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject ctor) {
        JSContext ctx = realm.getContext();

        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);

        JSObjectUtil.putBuiltinAccessorProperty(prototype, URL, createGetterFunction(realm, URL, JSContext.BuiltinFunctionKey.FetchRequestGetUrl), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, METHOD, createGetterFunction(realm, METHOD, JSContext.BuiltinFunctionKey.FetchRequestGetMethod), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, REDIRECT, createGetterFunction(realm, REDIRECT, JSContext.BuiltinFunctionKey.FetchRequestGetRedirect), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, REFERRER, createGetterFunction(realm, REFERRER, JSContext.BuiltinFunctionKey.FetchRequestGetReferrer), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, REFERRER_POLICY, createGetterFunction(realm, REFERRER_POLICY, JSContext.BuiltinFunctionKey.FetchRequestGetReferrerPolicy), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HEADERS, createGetterFunction(realm, HEADERS, JSContext.BuiltinFunctionKey.FetchRequestGetHeaders), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BODY, createGetterFunction(realm, BODY, JSContext.BuiltinFunctionKey.FetchRequestGetBody), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BODY_USED, createGetterFunction(realm, BODY_USED, JSContext.BuiltinFunctionKey.FetchRequestGetBodyUsed), Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, FetchRequestPrototypeBuiltins.BUILTINS);

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getFetchRequestPrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSFetchRequestObject create(JSContext context, JSRealm realm, FetchRequest request) {
        JSObjectFactory factory = context.getFetchRequestFactory();
        JSFetchRequestObject obj = JSFetchRequestObject.create(factory.getShape(realm), request);
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }
}
