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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeGetStackTraceNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeToStringNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
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

    public static final class ErrorPrototypeNashornCompatBuiltins extends JSBuiltinsContainer.SwitchEnum<ErrorPrototypeNashornCompatBuiltins.ErrorNashornCompat> {
        public static final JSBuiltinsContainer BUILTINS = new ErrorPrototypeNashornCompatBuiltins();

        protected ErrorPrototypeNashornCompatBuiltins() {
            super(JSError.PROTOTYPE_NAME, ErrorNashornCompat.class);
        }

        public enum ErrorNashornCompat implements BuiltinEnum<ErrorNashornCompat> {
            getStackTrace(0);

            private final int length;

            ErrorNashornCompat(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ErrorNashornCompat builtinEnum) {
            switch (builtinEnum) {
                case getStackTrace:
                    return ErrorPrototypeGetStackTraceNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            }
            return null;
        }
    }

    public abstract static class ErrorPrototypeToStringNode extends JSBuiltinNode {

        @Child private PropertyGetNode getNameNode;
        @Child private PropertyGetNode getMessageNode;
        @Child private JSToStringNode toStringNode;

        public ErrorPrototypeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object toStringNonObject(Object thisObj,
                        @Cached TruffleString.ConcatNode concatNode) {
            TruffleString name = toStringConv(thisObj);
            String message = Strings.toJavaString(Strings.concat(concatNode, Strings.METHOD_ERROR_PROTOTYPE_TO_STRING_CALLED_ON_INCOMPATIBLE_RECEIVER, name));
            throw Errors.createTypeError(message, this);
        }

        @Specialization(guards = "isJSObject(errorObj)")
        protected Object toStringObject(DynamicObject errorObj) {
            Object objName = getName(errorObj);
            TruffleString strName = (objName == Undefined.instance) ? Strings.UC_ERROR : toStringConv(objName);
            Object objMessage = getMessage(errorObj);
            TruffleString strMessage = (objMessage == Undefined.instance) ? Strings.EMPTY_STRING : toStringConv(objMessage);
            if (Strings.length(strName) == 0) {
                return strMessage;
            }
            if (Strings.length(strMessage) == 0) {
                return strName;
            }
            return toStringIntl(strName, strMessage);
        }

        private TruffleString toStringConv(Object value) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(value);
        }

        @TruffleBoundary
        private static Object toStringIntl(TruffleString strName, TruffleString strMessage) {
            return Strings.concatAll(strName, Strings.COLON_SPACE, strMessage);
        }

        protected Object getName(DynamicObject errObj) {
            if (getNameNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNameNode = insert(PropertyGetNode.create(JSError.NAME, false, getContext()));
            }
            return getNameNode.getValue(errObj);
        }

        protected Object getMessage(DynamicObject errObj) {
            if (getMessageNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getMessageNode = insert(PropertyGetNode.create(JSError.MESSAGE, false, getContext()));
            }
            return getMessageNode.getValue(errObj);
        }
    }

    public abstract static class ErrorPrototypeGetStackTraceNode extends JSBuiltinNode {

        public ErrorPrototypeGetStackTraceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject getStackTrace(Object thisObj) {
            throw Errors.createTypeErrorNotAnObject(thisObj);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject getStackTrace(DynamicObject thisObj) {
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
    }
}
