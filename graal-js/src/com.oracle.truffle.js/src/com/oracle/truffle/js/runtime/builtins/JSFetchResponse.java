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
import com.oracle.truffle.js.builtins.FetchResponseFunctionBuiltins;
import com.oracle.truffle.js.builtins.FetchResponsePrototypeBuiltins;
import com.oracle.truffle.js.builtins.helper.FetchHeaders;
import com.oracle.truffle.js.builtins.helper.FetchResponse;
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
 * https://fetch.spec.whatwg.org/#response-class.
 */
public final class JSFetchResponse extends JSNonProxy implements JSConstructorFactory.Default.WithFunctions, PrototypeSupplier {
    public static final JSFetchResponse INSTANCE = new JSFetchResponse();
    public static final TruffleString CLASS_NAME = Strings.constant("Response");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Response.prototype");

    // getter names
    private static final TruffleString TYPE = Strings.constant("type");
    private static final TruffleString URL = Strings.constant("url");
    private static final TruffleString REDIRECTED = Strings.constant("redirected");
    private static final TruffleString STATUS = Strings.constant("status");
    private static final TruffleString STATUS_TEXT = Strings.constant("statusText");
    private static final TruffleString OK = Strings.constant("ok");
    private static final TruffleString HEADERS = Strings.constant("headers");
    // body getter names
    private static final TruffleString BODY = Strings.constant("body");
    private static final TruffleString BODY_USED = Strings.constant("bodyUsed");

    private JSFetchResponse() { }

    public static boolean isJSFetchResponse(Object obj) {
        return obj instanceof JSFetchResponseObject;
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

    private static FetchResponse getInternalData(JSDynamicObject obj) {
        assert isJSFetchResponse(obj);
        return ((JSFetchResponseObject) obj).getResponseMap();
    }

    private static TruffleString getType(JSDynamicObject obj) {
        String type = getInternalData(obj).getType().toString();
        type = type.replace("_", "");
        return TruffleString.fromJavaStringUncached(type, TruffleString.Encoding.UTF_8);
    }

    private static TruffleString getUrl(JSDynamicObject obj) {
        String url = getInternalData(obj).getUrl() == null ? "" : getInternalData(obj).getUrl().toString();
        return TruffleString.fromJavaStringUncached(url, TruffleString.Encoding.UTF_8);
    }

    private static boolean getRedirected(JSDynamicObject obj) {
        return getInternalData(obj).getRedirected();
    }

    private static int getStatus(JSDynamicObject obj) {  // su
        return getInternalData(obj).getStatus();
    }

    private static TruffleString getStatusText(JSDynamicObject obj) {
        String statusText = getInternalData(obj).getStatusText();
        return TruffleString.fromJavaStringUncached(statusText, TruffleString.Encoding.UTF_8);
    }

    private static boolean getOk(JSDynamicObject obj) {
        return getInternalData(obj).getOk();
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
                    if (JSFetchResponse.isJSFetchResponse(obj)) {
                        switch (key) {
                            case FetchResponseGetUrl:
                                return JSFetchResponse.getUrl((JSFetchResponseObject) obj);
                            case FetchResponseGetRedirected:
                                return JSFetchResponse.getRedirected((JSFetchResponseObject) obj);
                            case FetchResponseGetType:
                                return JSFetchResponse.getType((JSFetchResponseObject) obj);
                            case FetchResponseGetStatus:
                                return JSFetchResponse.getStatus((JSFetchResponseObject) obj);
                            case FetchResponseGetStatusText:
                                return JSFetchResponse.getStatusText((JSFetchResponseObject) obj);
                            case FetchResponseGetOk:
                                return JSFetchResponse.getOk((JSFetchResponseObject) obj);
                            case FetchResponseGetHeaders:
                                FetchHeaders headers = getHeaders((JSFetchResponseObject) obj);
                                return JSFetchHeaders.create(realm.getContext(), realm, headers);
                            case FetchResponseGetBody:
                                return JSFetchResponse.getBody((JSFetchResponseObject) obj);
                            case FetchResponseGetBodyUsed:
                                return JSFetchResponse.getBodyUsed((JSFetchResponseObject) obj);
                            default:
                                throw new IllegalArgumentException("FetchResponse getter function key expected");
                        }
                    } else {
                        errorBranch.enter();
                        throw Errors.createTypeError("Response expected");
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

        JSObjectUtil.putBuiltinAccessorProperty(prototype, TYPE, createGetterFunction(realm, TYPE, JSContext.BuiltinFunctionKey.FetchResponseGetType), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, URL, createGetterFunction(realm, URL, JSContext.BuiltinFunctionKey.FetchResponseGetUrl), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, REDIRECTED, createGetterFunction(realm, REDIRECTED, JSContext.BuiltinFunctionKey.FetchResponseGetRedirected), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, STATUS, createGetterFunction(realm, STATUS, JSContext.BuiltinFunctionKey.FetchResponseGetStatus), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, STATUS_TEXT, createGetterFunction(realm, STATUS_TEXT, JSContext.BuiltinFunctionKey.FetchResponseGetStatusText), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, OK, createGetterFunction(realm, OK, JSContext.BuiltinFunctionKey.FetchResponseGetOk), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, HEADERS, createGetterFunction(realm, HEADERS, JSContext.BuiltinFunctionKey.FetchResponseGetHeaders), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BODY, createGetterFunction(realm, BODY, JSContext.BuiltinFunctionKey.FetchResponseGetBody), Undefined.instance);
        JSObjectUtil.putBuiltinAccessorProperty(prototype, BODY_USED, createGetterFunction(realm, BODY_USED, JSContext.BuiltinFunctionKey.FetchResponseGetBodyUsed), Undefined.instance);

        JSObjectUtil.putFunctionsFromContainer(realm, prototype, FetchResponsePrototypeBuiltins.BUILTINS);

        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getFetchResponsePrototype();
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm, FetchResponseFunctionBuiltins.BUILTINS);
    }

    public static JSFetchResponseObject create(JSContext context, JSRealm realm, FetchResponse response) {
        JSObjectFactory factory = context.getFetchResponseFactory();
        JSFetchResponseObject obj = JSFetchResponseObject.create(factory.getShape(realm), response);
        factory.initProto(obj, realm);
        return context.trackAllocation(obj);
    }
}
