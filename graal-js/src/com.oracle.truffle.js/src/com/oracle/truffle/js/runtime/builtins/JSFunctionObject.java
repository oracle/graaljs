/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.JSInteropExecuteNode;
import com.oracle.truffle.js.nodes.interop.JSInteropInstantiateNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.interop.InteropFunction;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public abstract class JSFunctionObject extends JSNonProxyObject {

    protected JSFunctionObject(Shape shape, JSDynamicObject proto, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
        super(shape, proto);
        this.functionData = functionData;
        this.enclosingFrame = enclosingFrame;
        this.realm = realm;
        this.classPrototype = classPrototype;
    }

    /** Shared function data. */
    private final JSFunctionData functionData;
    /** Materialized frame of the enclosing function. */
    private final MaterializedFrame enclosingFrame;
    private final JSRealm realm;
    /** The {@code prototype} property of the function object. Lazily initialized. */
    private Object classPrototype;

    public final JSFunctionData getFunctionData() {
        return functionData;
    }

    public final MaterializedFrame getEnclosingFrame() {
        return enclosingFrame;
    }

    public final JSRealm getRealm() {
        return realm;
    }

    public final Object getClassPrototype() {
        return classPrototype;
    }

    public void setClassPrototype(Object classPrototype) {
        this.classPrototype = classPrototype;
    }

    public Object getLexicalThis() {
        return classPrototype;
    }

    @Override
    public final TruffleString getClassName() {
        return getBuiltinToStringTag();
    }

    @Override
    public final TruffleString getBuiltinToStringTag() {
        return JSFunction.CLASS_NAME;
    }

    @ExportMessage
    public final boolean isExecutable(
                    @Cached IsCallableNode isCallable) {
        return isCallable.executeBoolean(this);
    }

    @ExportMessage
    public final Object execute(Object[] args,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropExecuteNode callNode,
                    @Shared @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, Undefined.instance, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean isInstantiable() {
        return JSRuntime.isConstructor(this);
    }

    @ExportMessage
    public final Object instantiate(Object[] args,
                    @CachedLibrary("this") InteropLibrary self,
                    @Cached JSInteropInstantiateNode callNode,
                    @Shared @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
        JavaScriptLanguage language = JavaScriptLanguage.get(self);
        language.interopBoundaryEnter(realm);
        try {
            Object result = callNode.execute(this, args);
            return exportNode.execute(result);
        } finally {
            language.interopBoundaryExit(realm);
        }
    }

    @ExportMessage
    public final boolean hasSourceLocation() {
        return getSourceLocationImpl(this) != null;
    }

    @ExportMessage
    public final SourceSection getSourceLocation() throws UnsupportedMessageException {
        SourceSection sourceSection = getSourceLocationImpl(this);
        if (sourceSection == null) {
            throw UnsupportedMessageException.create();
        }
        return sourceSection;
    }

    @TruffleBoundary
    private static SourceSection getSourceLocationImpl(JSDynamicObject receiver) {
        Object function = receiver;
        while (function instanceof JSFunctionObject.Bound boundFunction) {
            function = JSFunction.getBoundTargetFunction(boundFunction);
        }
        if (JSFunction.isJSFunction(function)) {
            CallTarget ct = JSFunction.getCallTarget((JSFunctionObject) function);

            if (ct instanceof RootCallTarget) {
                return ((RootCallTarget) ct).getRootNode().getSourceSection();
            }
        }
        return null;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final boolean isMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @TruffleBoundary
    @ExportMessage(name = "getMetaQualifiedName")
    @ExportMessage(name = "getMetaSimpleName")
    public final Object getMetaObjectName() {
        Object name = JSRuntime.getDataProperty(this, JSFunction.NAME);
        if (Strings.isTString(name)) {
            return name;
        }
        return Strings.EMPTY_STRING;
    }

    @SuppressWarnings("static-method")
    @TruffleBoundary
    @ExportMessage
    public final boolean isMetaInstance(Object instance) {
        Object constructorPrototype = JSRuntime.getDataProperty(this, JSObject.PROTOTYPE);
        if (JSGuards.isJSObject(constructorPrototype)) {
            Object obj = instance;
            if (obj instanceof InteropFunction) {
                obj = ((InteropFunction) obj).getFunction();
            }
            if (obj instanceof JSException) {
                obj = ((JSException) obj).getErrorObject();
            }
            if (JSGuards.isJSObject(obj) && !JSProxy.isJSProxy(obj)) {
                JSDynamicObject proto = JSObject.getPrototype((JSDynamicObject) obj);
                while (proto != Null.instance) {
                    if (proto == constructorPrototype) {
                        return true;
                    }
                    if (JSProxy.isJSProxy(proto)) {
                        break;
                    }
                    proto = JSObject.getPrototype(proto);
                }
            }
        }
        return false;
    }

    @Override
    @TruffleBoundary
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        RootNode rn = ((RootCallTarget) JSFunction.getCallTarget(this)).getRootNode();
        SourceSection ssect = rn.getSourceSection();
        TruffleString source;
        if (ssect == null || !ssect.isAvailable() || ssect.getSource().isInternal()) {
            source = Strings.concatAll(Strings.FUNCTION_SPC, JSFunction.getName(this), Strings.FUNCTION_NATIVE_CODE_BODY);
        } else if (depth >= format.getMaxDepth()) {
            source = Strings.concatAll(Strings.FUNCTION_SPC, JSFunction.getName(this), Strings.FUNCTION_BODY_DOTS);
        } else {
            if (ssect.getCharacters().length() > 200) {
                source = Strings.concat(Strings.fromJavaString(ssect.getCharacters().subSequence(0, 195).toString()), Strings.FUNCTION_BODY_OMITTED);
            } else {
                source = Strings.fromJavaString(ssect.getCharacters().toString());
            }
        }
        return source;
    }

    public static JSFunctionObject create(Shape shape, JSDynamicObject proto, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
        return new Unbound(shape, proto, functionData, enclosingFrame, realm, classPrototype);
    }

    public static JSFunctionObject createBound(Shape shape, JSDynamicObject proto, JSFunctionData functionData, JSRealm realm, Object classPrototype,
                    Object boundTargetFunction, Object boundThis, Object[] boundArguments) {
        return new Bound(shape, proto, functionData, realm, classPrototype, boundTargetFunction, boundThis, boundArguments);
    }

    public static JSFunctionObject createWrapped(Shape shape, JSDynamicObject proto, JSFunctionData functionData, JSRealm realm, Object boundTargetFunction) {
        return new Wrapped(shape, proto, functionData, realm, boundTargetFunction);
    }

    public static final class Unbound extends JSFunctionObject {
        protected Unbound(Shape shape, JSDynamicObject proto, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
            super(shape, proto, functionData, enclosingFrame, realm, classPrototype);
        }
    }

    /**
     * Bound or wrapped function exotic object.
     */
    public abstract static class BoundOrWrapped extends JSFunctionObject {

        private int boundLength;

        private TruffleString boundName;

        protected BoundOrWrapped(Shape shape, JSDynamicObject proto, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
            super(shape, proto, functionData, enclosingFrame, realm, classPrototype);
        }

        public final TruffleString getBoundName() {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, boundName == null)) {
                initializeName();
            }
            return boundName;
        }

        public final void setBoundName(TruffleString targetName, TruffleString prefix) {
            boundName = Strings.isEmpty(prefix) ? targetName : concat(prefix, targetName);
        }

        @TruffleBoundary
        private static TruffleString concat(TruffleString first, TruffleString second) {
            return Strings.concat(first, second);
        }

        protected abstract void initializeName();

        @TruffleBoundary
        protected static TruffleString getFunctionName(JSFunctionObject function) {
            if (function instanceof BoundOrWrapped) {
                return ((BoundOrWrapped) function).getBoundName();
            } else {
                return JSFunction.getName(function);
            }
        }

        public final int getBoundLength() {
            return boundLength;
        }

        public final void setBoundLength(int length) {
            assert this.boundLength == 0;
            this.boundLength = length;
        }
    }

    /**
     * Bound function exotic object.
     */
    public static final class Bound extends BoundOrWrapped {

        private final Object boundTargetFunction;
        private final Object boundThis;
        private final Object[] boundArguments;

        protected Bound(Shape shape, JSDynamicObject proto, JSFunctionData functionData, JSRealm realm, Object classPrototype,
                        Object boundTargetFunction, Object boundThis, Object[] boundArguments) {
            super(shape, proto, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, realm, classPrototype);
            this.boundTargetFunction = boundTargetFunction;
            this.boundThis = boundThis;
            this.boundArguments = boundArguments;
        }

        public Object getBoundTargetFunction() {
            return boundTargetFunction;
        }

        public Object getBoundThis() {
            return boundThis;
        }

        public Object[] getBoundArguments() {
            return boundArguments;
        }

        @Override
        protected void initializeName() {
            // This method should not be called if the wrapped target function is not a JS function.
            setBoundName(getFunctionName((JSFunctionObject) getBoundTargetFunction()), Strings.BOUND_SPC);
        }
    }

    /**
     * Wrapped function exotic object.
     *
     * @see JSShadowRealmObject
     */
    public static final class Wrapped extends BoundOrWrapped {
        private final Object wrappedTargetFunction;

        protected Wrapped(Shape shape, JSDynamicObject proto, JSFunctionData functionData, JSRealm realm, Object wrappedTargetFunction) {
            super(shape, proto, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, realm, JSFunction.CLASS_PROTOTYPE_PLACEHOLDER);
            this.wrappedTargetFunction = wrappedTargetFunction;
        }

        public Object getWrappedTargetFunction() {
            return wrappedTargetFunction;
        }

        @Override
        protected void initializeName() {
            // This method should not be called if the wrapped target function is not a JS function.
            setBoundName(getFunctionName((JSFunctionObject) getWrappedTargetFunction()), Strings.EMPTY_STRING);
        }
    }
}
