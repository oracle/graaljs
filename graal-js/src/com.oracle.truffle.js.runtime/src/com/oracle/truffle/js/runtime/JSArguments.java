/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

public final class JSArguments {
    public static final Object[] EMPTY_ARGUMENTS_ARRAY = new Object[0];
    public static final int RUNTIME_ARGUMENT_COUNT = 2;

    private static final int THIS_OBJECT_INDEX = 0;
    private static final int FUNCTION_OBJECT_INDEX = 1;
    private static final int NEW_TARGET_INDEX = RUNTIME_ARGUMENT_COUNT;

    public static Object[] createNullArguments() {
        return createZeroArg(Null.instance, null);
    }

    public static Object[] create(Object target, Object function, Object... userArguments) {
        Object[] arguments = createInitial(target, function, userArguments.length);
        setUserArguments(arguments, 0, userArguments);
        return arguments;
    }

    public static Object[] createInitial(Object target, Object function, int userArgumentCount) {
        Object[] result = new Object[RUNTIME_ARGUMENT_COUNT + userArgumentCount];
        result[THIS_OBJECT_INDEX] = target;
        result[FUNCTION_OBJECT_INDEX] = function;
        return result;
    }

    public static Object[] createZeroArg(Object target, Object function) {
        return createInitial(target, function, 0);
    }

    public static Object[] createOneArg(Object target, Object function, Object userArgument) {
        Object[] arguments = createInitial(target, function, 1);
        setUserArgument(arguments, 0, userArgument);
        return arguments;
    }

    public static Object getThisObject(Object[] arguments) {
        return arguments[THIS_OBJECT_INDEX];
    }

    public static Object getFunctionObject(Object[] arguments) {
        return arguments[FUNCTION_OBJECT_INDEX];
    }

    public static Object getUserArgument(Object[] arguments, int index) {
        return arguments[index + RUNTIME_ARGUMENT_COUNT];
    }

    public static void setUserArgument(Object[] arguments, int index, Object value) {
        arguments[index + RUNTIME_ARGUMENT_COUNT] = value;
    }

    public static int getUserArgumentCount(Object[] arguments) {
        return arguments.length - RUNTIME_ARGUMENT_COUNT;
    }

    public static void setUserArguments(Object[] arguments, int index, Object... userArguments) {
        arraycopy(userArguments, 0, arguments, RUNTIME_ARGUMENT_COUNT + index, userArguments.length);
    }

    public static Object[] extractUserArguments(Object[] arguments) {
        // Do not use Arrays.copyOfRange(..) to reduce code side.
        Object[] userArguments = new Object[arguments.length - RUNTIME_ARGUMENT_COUNT];
        arraycopy(arguments, RUNTIME_ARGUMENT_COUNT, userArguments, 0, userArguments.length);
        return userArguments;
    }

    public static Object[] extractUserArguments(Object[] arguments, int skip) {
        return extractUserArguments(arguments, skip, 0);
    }

    public static Object[] extractUserArguments(Object[] arguments, int skip, int skipEnd) {
        int offset = RUNTIME_ARGUMENT_COUNT + skip;
        Object[] userArguments = new Object[arguments.length - offset - skipEnd];
        arraycopy(arguments, offset, userArguments, 0, userArguments.length);
        return userArguments;
    }

    public static MaterializedFrame getEnclosingFrame(Object[] arguments) {
        DynamicObject functionObject = JSObject.castJSObject(getFunctionObject(arguments));
        if (!JSFunction.isJSFunction(functionObject)) {
            throw Errors.shouldNotReachHere();
        }
        return JSFunction.getEnclosingFrame(functionObject);
    }

    public static void arraycopy(Object[] src, int srcPos, Object[] dest, int destPos, int length) {
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = src[srcPos + i];
        }
    }

    public static Object[] createWithNewTarget(Object target, Object function, Object newTarget, Object... userArguments) {
        Object[] arguments = createInitialWithNewTarget(target, function, newTarget, userArguments.length);
        setUserArguments(arguments, 1, userArguments);
        return arguments;
    }

    public static Object[] createInitialWithNewTarget(Object target, Object function, Object newTarget, int userArgumentCount) {
        Object[] arguments = createInitial(target, function, userArgumentCount + 1);
        arguments[NEW_TARGET_INDEX] = newTarget;
        return arguments;
    }

    public static Object getNewTarget(Object[] arguments) {
        return arguments[NEW_TARGET_INDEX];
    }
}
