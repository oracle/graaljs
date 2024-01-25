/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeGetStackTraceNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeToStringNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ForeignErrorPrototypeCauseNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ForeignErrorPrototypeMessageNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ForeignErrorPrototypeNameNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ForeignErrorPrototypeStackNodeGen;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSError}.prototype.
 */
public final class ErrorPrototypeBuiltins extends JSBuiltinsContainer.Switch {
    public static final JSBuiltinsContainer BUILTINS = new ErrorPrototypeBuiltins();

    protected ErrorPrototypeBuiltins() {
        super(JSError.PROTOTYPE_NAME);
        defineFunction(Strings.TO_STRING, 0);
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        if (Strings.equals(Strings.TO_STRING, builtin.getName())) {
            return ErrorPrototypeToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public static final class ErrorPrototypeNashornCompatBuiltins extends JSBuiltinsContainer.Switch {
        private static final TruffleString GET_STACK_TRACE = Strings.constant("getStackTrace");

        public static final JSBuiltinsContainer BUILTINS = new ErrorPrototypeNashornCompatBuiltins();

        protected ErrorPrototypeNashornCompatBuiltins() {
            super(JSError.PROTOTYPE_NAME);
            defineFunction(GET_STACK_TRACE, 0);
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
            if (Strings.equals(GET_STACK_TRACE, builtin.getName())) {
                return ErrorPrototypeGetStackTraceNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            }
            return null;
        }
    }

    @ImportStatic(Strings.class)
    public abstract static class ErrorPrototypeToStringNode extends JSBuiltinNode {

        public ErrorPrototypeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization(guards = "!isObjectNode.executeBoolean(thisObj)")
        protected final Object toStringNonObject(Object thisObj,
                        @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObjectNode) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getFullName(), thisObj);
        }

        @Specialization(guards = "isObjectNode.executeBoolean(errorObj)")
        protected static Object toStringObject(Object errorObj,
                        @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObjectNode,
                        @Cached("create(NAME, false, getContext())") PropertyGetNode getNameNode,
                        @Cached("create(MESSAGE, false, getContext())") PropertyGetNode getMessageNode,
                        @Cached JSToStringNode toStringNode) {
            Object objName = getNameNode.getValue(errorObj);
            TruffleString strName = (objName == Undefined.instance) ? Strings.UC_ERROR : toStringNode.executeString(objName);
            Object objMessage = getMessageNode.getValue(errorObj);
            TruffleString strMessage = (objMessage == Undefined.instance) ? Strings.EMPTY_STRING : toStringNode.executeString(objMessage);
            if (Strings.length(strName) == 0) {
                return strMessage;
            }
            if (Strings.length(strMessage) == 0) {
                return strName;
            }
            return concatNameAndMessage(strName, strMessage);
        }

        @TruffleBoundary
        private static Object concatNameAndMessage(TruffleString strName, TruffleString strMessage) {
            return Strings.concatAll(strName, Strings.COLON_SPACE, strMessage);
        }

    }

    public abstract static class ErrorPrototypeGetStackTraceNode extends JSBuiltinNode {

        public ErrorPrototypeGetStackTraceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject getStackTrace(JSObject thisObj) {
            // get original exception from special exception property; call
            // Throwable#getStackTrace(), transform it a bit and turn it into a JSArray
            Object exception = JSDynamicObject.getOrNull(thisObj, JSError.EXCEPTION_PROPERTY_NAME);
            Object[] stackTrace = getStackTraceFromThrowable(exception);
            return JSArray.createConstant(getContext(), getRealm(), stackTrace);
        }

        @TruffleBoundary
        private static Object[] getStackTraceFromThrowable(Object exception) {
            if (exception instanceof GraalJSException) {
                return ((GraalJSException) exception).getJSStackTrace();
            } else {
                return new StackTraceElement[0];
            }
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected JSDynamicObject getStackTrace(Object thisObj) {
            throw Errors.createTypeErrorNotAnObject(thisObj);
        }
    }

    public static final class ForeignErrorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ForeignErrorPrototypeBuiltins.ForeignError> {
        public static final JSBuiltinsContainer BUILTINS = new ForeignErrorPrototypeBuiltins();

        protected ForeignErrorPrototypeBuiltins() {
            super(JSError.PROTOTYPE_NAME, ForeignError.class);
        }

        public enum ForeignError implements BuiltinEnum<ForeignError> {
            cause(0),
            message(0),
            name(0),
            stack(0);

            private final int length;

            ForeignError(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }

            @Override
            public boolean isGetter() {
                return true;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ForeignError builtinEnum) {
            switch (builtinEnum) {
                case cause:
                    return ForeignErrorPrototypeCauseNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case message:
                    return ForeignErrorPrototypeMessageNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case name:
                    return ForeignErrorPrototypeNameNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                case stack:
                    return ForeignErrorPrototypeStackNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            }
            return null;
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class ForeignErrorPrototypeMessageNode extends JSBuiltinNode {

        public ForeignErrorPrototypeMessageNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getMessage(Object error,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached ImportValueNode importNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (interop.hasExceptionMessage(error)) {
                try {
                    Object message = interop.getExceptionMessage(error);
                    return importNode.executeWithTarget(message);
                } catch (UnsupportedMessageException umex) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorInteropException(error, umex, "getExceptionMessage", this);
                }
            }
            return Strings.EMPTY_STRING;
        }

    }

    @ImportStatic(JSConfig.class)
    public abstract static class ForeignErrorPrototypeNameNode extends JSBuiltinNode {

        public ForeignErrorPrototypeNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "InteropLibraryLimit")
        protected Object getName(Object error,
                        @CachedLibrary("error") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interopMeta,
                        @Cached ImportValueNode importNode) {
            try {
                if (interop.isException(error) && interop.hasMetaObject(error)) {
                    return importNode.executeWithTarget(interopMeta.getMetaQualifiedName(interop.getMetaObject(error)));
                }
            } catch (UnsupportedMessageException e) {
                // Interop contract violation
                assert false : e;
            }
            return Strings.UC_ERROR;
        }

    }

    @ImportStatic(JSConfig.class)
    public abstract static class ForeignErrorPrototypeStackNode extends JSBuiltinNode {

        public ForeignErrorPrototypeStackNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getStack(Object error,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interopStr,
                        @Cached TruffleString.FromJavaStringNode fromJavaStringNode) {
            if (interop.hasExceptionStackTrace(error)) {
                String stack = JSInteropUtil.formatError(error, interop, interopStr);
                return Strings.fromJavaString(fromJavaStringNode, stack);
            }
            return Undefined.instance;
        }

    }

    @ImportStatic(JSConfig.class)
    public abstract static class ForeignErrorPrototypeCauseNode extends JSBuiltinNode {

        public ForeignErrorPrototypeCauseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getCause(Object error,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached ImportValueNode importNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (interop.hasExceptionCause(error)) {
                try {
                    Object cause = interop.getExceptionCause(error);
                    return importNode.executeWithTarget(cause);
                } catch (UnsupportedMessageException umex) {
                    errorBranch.enter(this);
                    throw Errors.createTypeErrorInteropException(error, umex, "getExceptionCause", this);
                }
            }
            return Undefined.instance;
        }

    }

}
