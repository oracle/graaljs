/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
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
import com.oracle.truffle.js.runtime.interop.InteropFunction;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ExportLibrary(InteropLibrary.class)
public abstract class JSFunctionObject extends JSNonProxyObject {

    protected JSFunctionObject(Shape shape, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
        super(shape);
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
    public String getClassName() {
        return JSFunction.INSTANCE.getClassName(this);
    }

    @Override
    public String getBuiltinToStringTag() {
        return JSFunction.INSTANCE.getBuiltinToStringTag(this);
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
                    @Shared("exportValue") @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
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
                    @Shared("exportValue") @Cached ExportValueNode exportNode) throws UnsupportedMessageException {
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
    private static SourceSection getSourceLocationImpl(DynamicObject receiver) {
        if (JSFunction.isJSFunction(receiver)) {
            DynamicObject func = receiver;
            CallTarget ct = JSFunction.getCallTarget(func);
            if (JSFunction.isBoundFunction(func)) {
                func = JSFunction.getBoundTargetFunction(func);
                ct = JSFunction.getCallTarget(func);
            }

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
        if (JSRuntime.isString(name)) {
            return JSRuntime.javaToString(name);
        }
        return "";
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
                obj = ((JSException) obj).getErrorObjectEager();
            }
            if (JSGuards.isJSObject(obj) && !JSProxy.isJSProxy(obj)) {
                DynamicObject proto = JSObject.getPrototype((DynamicObject) obj);
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

    public static JSFunctionObject create(Shape shape, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
        return new Unbound(shape, functionData, enclosingFrame, realm, classPrototype);
    }

    public static JSFunctionObject createBound(Shape shape, JSFunctionData functionData, JSRealm realm, Object classPrototype,
                    DynamicObject boundTargetFunction, Object boundThis, Object[] boundArguments) {
        return new Bound(shape, functionData, realm, classPrototype, boundTargetFunction, boundThis, boundArguments);
    }

    public static final class Unbound extends JSFunctionObject {
        protected Unbound(Shape shape, JSFunctionData functionData, MaterializedFrame enclosingFrame, JSRealm realm, Object classPrototype) {
            super(shape, functionData, enclosingFrame, realm, classPrototype);
        }
    }

    public static final class Bound extends JSFunctionObject {
        protected Bound(Shape shape, JSFunctionData functionData, JSRealm realm, Object classPrototype,
                        DynamicObject boundTargetFunction, Object boundThis, Object[] boundArguments) {
            super(shape, functionData, JSFrameUtil.NULL_MATERIALIZED_FRAME, realm, classPrototype);
            this.boundTargetFunction = boundTargetFunction;
            this.boundThis = boundThis;
            this.boundArguments = boundArguments;
            this.boundLength = calculateBoundLength();
        }

        private final DynamicObject boundTargetFunction;
        private final Object boundThis;
        private final Object[] boundArguments;
        private final int boundLength;
        private CharSequence boundName;

        public DynamicObject getBoundTargetFunction() {
            return boundTargetFunction;
        }

        public Object getBoundThis() {
            return boundThis;
        }

        public Object[] getBoundArguments() {
            return boundArguments;
        }

        public CharSequence getBoundName() {
            if (boundName == null) {
                initializeBoundName();
            }
            return boundName;
        }

        public void setTargetName(CharSequence targetName) {
            boundName = JSLazyString.create("bound ", targetName);
        }

        @TruffleBoundary
        private void initializeBoundName() {
            setTargetName(getFunctionName(boundTargetFunction));
        }

        private static CharSequence getFunctionName(DynamicObject function) {
            if (JSFunction.isBoundFunction(function)) {
                return ((JSFunctionObject.Bound) function).getBoundName();
            } else {
                return JSFunction.getName(function);
            }
        }

        public int getBoundLength() {
            return boundLength;
        }

        private int calculateBoundLength() {
            return Math.max(0, getBoundFunctionLength(boundTargetFunction) - boundArguments.length);
        }

        private static int getBoundFunctionLength(DynamicObject function) {
            if (JSFunction.isBoundFunction(function)) {
                return ((JSFunctionObject.Bound) function).getBoundLength();
            } else {
                return JSFunction.getLength(function);
            }
        }

    }

}
