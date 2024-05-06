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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Completion.Type;
import com.oracle.truffle.js.runtime.objects.Null;

public final class JSArguments {
    public static final Object[] EMPTY_ARGUMENTS_ARRAY = new Object[0];
    public static final int RUNTIME_ARGUMENT_COUNT = 2;

    private static final int THIS_OBJECT_INDEX = 0;
    private static final int FUNCTION_OBJECT_INDEX = 1;
    private static final int NEW_TARGET_INDEX = RUNTIME_ARGUMENT_COUNT;

    private static final int RESUME_EXECUTION_CONTEXT = RUNTIME_ARGUMENT_COUNT;
    private static final int RESUME_GENERATOR_OR_PROMISE = RUNTIME_ARGUMENT_COUNT + 1;
    private static final int RESUME_COMPLETION_TYPE = RUNTIME_ARGUMENT_COUNT + 2;
    private static final int RESUME_COMPLETION_VALUE = RUNTIME_ARGUMENT_COUNT + 3;

    private JSArguments() {
        // should not be constructed
    }

    static Object[] createNullArguments() {
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

    public static void setThisObject(Object[] arguments, Object value) {
        arguments[THIS_OBJECT_INDEX] = value;
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

    public static void setUserArguments(Object[] arguments, int index, Object[] userArguments) {
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
        return ((JSFunctionObject.Unbound) getFunctionObject(arguments)).getEnclosingFrame();
    }

    public static void arraycopy(Object[] src, int srcPos, Object[] dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
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

    public static Object[] createResumeArguments(Object executionContext, Object generatorOrPromiseCapability, Completion.Type completionType, Object completionValue) {
        MaterializedFrame contextFrame = JSFrameUtil.castMaterializedFrame(executionContext);
        Object[] arguments = contextFrame.getArguments();
        return new Object[]{arguments[0], arguments[1], contextFrame, generatorOrPromiseCapability, completionType, completionValue};
    }

    public static Object[] createResumeArguments(Object executionContext, Object generator, Completion completion) {
        return createResumeArguments(executionContext, generator, completion.getType(), completion.getValue());
    }

    public static MaterializedFrame getResumeExecutionContext(Object[] arguments) {
        return JSFrameUtil.castMaterializedFrame(arguments[RESUME_EXECUTION_CONTEXT]);
    }

    public static Object getResumeGeneratorOrPromiseCapability(Object[] arguments) {
        return arguments[RESUME_GENERATOR_OR_PROMISE];
    }

    public static Completion.Type getResumeCompletionType(Object[] arguments) {
        return (Type) arguments[RESUME_COMPLETION_TYPE];
    }

    public static Object getResumeCompletionValue(Object[] arguments) {
        return arguments[RESUME_COMPLETION_VALUE];
    }

    public static Completion getResumeCompletion(Object[] arguments) {
        return Completion.create(getResumeCompletionType(arguments), getResumeCompletionValue(arguments));
    }
}
