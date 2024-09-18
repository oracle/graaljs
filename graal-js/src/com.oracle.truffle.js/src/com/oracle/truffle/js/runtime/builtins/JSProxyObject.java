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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInstantiateNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.interop.JSMetaType;
import com.oracle.truffle.js.runtime.objects.JSClassObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public final class JSProxyObject extends JSClassObject {

    private Object proxyTarget;
    private JSDynamicObject proxyHandler;

    protected JSProxyObject(Shape shape, JSDynamicObject proto, Object proxyTarget, JSDynamicObject proxyHandler) {
        super(shape, proto);
        this.proxyTarget = proxyTarget;
        this.proxyHandler = proxyHandler;
    }

    public JSDynamicObject getProxyHandler() {
        return proxyHandler;
    }

    public Object getProxyTarget() {
        return proxyTarget;
    }

    public void revoke(boolean isCallable, boolean isConstructor) {
        this.proxyHandler = Null.instance;
        this.proxyTarget = RevokedTarget.lookup(isCallable, isConstructor);
    }

    @Override
    public TruffleString getClassName() {
        return JSProxy.CLASS_NAME;
    }

    @Override
    public TruffleString getBuiltinToStringTag() {
        Object targetNonProxy = JSProxy.getTargetNonProxy(this);
        if (JSDynamicObject.isJSDynamicObject(targetNonProxy)) {
            if (JSArray.isJSArray(targetNonProxy)) {
                return JSArray.CLASS_NAME;
            } else if (JSFunction.isJSFunction(targetNonProxy)) {
                return JSFunction.CLASS_NAME;
            } else {
                return Strings.UC_OBJECT;
            }
        } else {
            InteropLibrary interop = InteropLibrary.getUncached(targetNonProxy);
            if (interop.hasArrayElements(targetNonProxy)) {
                return JSArray.CLASS_NAME;
            } else if (interop.isExecutable(targetNonProxy) || interop.isInstantiable(targetNonProxy)) {
                return JSFunction.CLASS_NAME;
            } else {
                return Strings.UC_OBJECT;
            }
        }
    }

    @ExportMessage
    public boolean isExecutable(
                    @Cached IsCallableNode isCallable) {
        return isCallable.executeBoolean(this);
    }

    @ExportMessage
    public Object execute(Object[] args,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropExecuteNode callNode,
                    @Shared @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, Undefined.instance, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public boolean isInstantiable() {
        return JSRuntime.isConstructor(this);
    }

    @ExportMessage
    public Object instantiate(Object[] args,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropInstantiateNode callNode,
                    @Shared @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public Object getMetaObject() {
        return JSMetaType.JS_PROXY;
    }

    @Override
    public boolean isExtensible() {
        return JSProxy.INSTANCE.isExtensible(this);
    }

    @Override
    public boolean preventExtensions(boolean doThrow) {
        return JSProxy.INSTANCE.preventExtensions(this, doThrow);
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return defaultToString();
        } else {
            Object target = JSProxy.getTarget(this);
            Object handler = JSProxy.getHandler(this);
            return Strings.concatAll(Strings.PROXY_PAREN,
                            JSRuntime.toDisplayStringInner(target, allowSideEffects, format, depth, this),
                            Strings.COMMA_SPC,
                            JSRuntime.toDisplayStringInner(handler, allowSideEffects, format, depth, this),
                            Strings.PAREN_CLOSE);
        }
    }

    @ExportMessage
    public void removeMember(String key,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached @Shared TruffleString.FromJavaStringNode fromJavaString) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            super.removeMember(key, fromJavaString);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") boolean internal,
                    @CachedLibrary("this") InteropLibrary self) {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        JSRealm realm = JSRealm.get(self);
        language.interopBoundaryEnter(realm);
        try {
            return super.getMembers(internal);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class RevokedTarget implements TruffleObject {
        private final boolean isCallable;
        private final boolean isConstructor;

        RevokedTarget(boolean isCallable, boolean isConstructor) {
            this.isCallable = isCallable;
            this.isConstructor = isConstructor;
        }

        @ExportMessage
        public boolean isExecutable() {
            return isCallable;
        }

        @ExportMessage
        public Object execute(@SuppressWarnings("unused") Object[] args) throws UnsupportedMessageException {
            if (isExecutable()) {
                throw Errors.createTypeErrorProxyRevoked(JSProxy.APPLY, null);
            } else {
                throw UnsupportedMessageException.create();
            }
        }

        @ExportMessage
        public boolean isInstantiable() {
            return isConstructor;
        }

        @ExportMessage
        public Object instantiate(@SuppressWarnings("unused") Object[] args) throws UnsupportedMessageException {
            if (isInstantiable()) {
                throw Errors.createTypeErrorProxyRevoked(JSProxy.CONSTRUCT, null);
            } else {
                throw UnsupportedMessageException.create();
            }
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public TruffleString toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
            return Null.NAME;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean hasLanguage() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Class<? extends TruffleLanguage<?>> getLanguage() {
            return JavaScriptLanguage.class;
        }

        static Object lookup(boolean callable, boolean constructor) {
            return REVOKED_TARGET[(callable ? 1 : 0) + (constructor ? 2 : 0)];
        }

        @CompilationFinal(dimensions = 1) static final Object[] REVOKED_TARGET = {
                        Null.instance,
                        new RevokedTarget(true, false),
                        new RevokedTarget(false, true),
                        new RevokedTarget(true, true),
        };
    }
}
