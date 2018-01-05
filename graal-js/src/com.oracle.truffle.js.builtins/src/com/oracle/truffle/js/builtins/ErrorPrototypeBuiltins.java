/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeGetStackTraceNodeGen;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltinsFactory.ErrorPrototypeToStringNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSError}.prototype.
 */
public final class ErrorPrototypeBuiltins extends JSBuiltinsContainer.Switch {
    public ErrorPrototypeBuiltins() {
        super(JSError.PROTOTYPE_NAME);
        defineFunction("toString", 0);

        if (JSTruffleOptions.NashornExtensions) {
            defineFunction("getStackTrace", 0);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        switch (builtin.getName()) {
            case "toString":
                return ErrorPrototypeToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case "getStackTrace":
                return ErrorPrototypeGetStackTraceNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ErrorPrototypeToStringNode extends JSBuiltinNode {

        @Child private PropertyGetNode getNameNode;
        @Child private PropertyGetNode getMessageNode;

        public ErrorPrototypeToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected String toString(Object thisObj,
                        @Cached("create()") JSToStringNode toStringNode) {
            String name = toStringNode.executeString(thisObj);
            String message = JSRuntime.stringConcat("Method Error.prototype.toString called on incompatible receiver ", name);
            throw Errors.createTypeError(message, this);
        }

        @Specialization(guards = "isJSObject(errorObj)")
        protected String toString(DynamicObject errorObj, //
                        @Cached("create()") JSToStringNode toStringNode) {
            Object objName = getName(errorObj);
            Object objMessage = getMessage(errorObj);
            String strName = (objName == Undefined.instance) ? "Error" : toStringNode.executeString(objName);
            String strMessage = (objMessage == Undefined.instance) ? "" : toStringNode.executeString(objMessage);
            if (strName.length() == 0) {
                return strMessage;
            }
            if (strMessage.length() == 0) {
                return strName;
            }
            return toStringIntl(strName, strMessage);
        }

        @TruffleBoundary
        private static String toStringIntl(String strName, String strMessage) {
            return strName + ": " + strMessage;
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
            Object exception = thisObj.get(JSError.EXCEPTION_PROPERTY_NAME);
            Object[] stackTrace = getStackTraceFromThrowable(exception);
            return JSArray.createConstant(getContext(), stackTrace);
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
