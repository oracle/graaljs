/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.PropertyNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;

/**
 * Implements OrdinaryToPrimitive (O, hint).
 */
public class OrdinaryToPrimitiveNode extends JavaScriptBaseNode {
    private final Hint hint;
    private final JSContext context;
    private final ConditionProfile toStringFunctionProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile valueOfFunctionProfile = ConditionProfile.createBinaryProfile();
    @Child private PropertyNode getToStringNode;
    @Child private PropertyNode getValueOfNode;
    @Child private IsCallableNode isCallableNode;
    @Child private JSFunctionCallNode callToStringNode;
    @Child private JSFunctionCallNode callValueOfNode;
    @Child private IsPrimitiveNode isPrimitiveNode;

    protected OrdinaryToPrimitiveNode(JSContext context, Hint hint) {
        assert hint == Hint.String || hint == Hint.Number;
        this.hint = hint;
        this.context = context;
        this.isCallableNode = IsCallableNode.create();
        this.isPrimitiveNode = IsPrimitiveNode.create();
    }

    public Object execute(DynamicObject object) {
        assert JSGuards.isJSObject(object);
        if (hint == Hint.String) {
            return doHintString(object);
        } else {
            assert hint == Hint.Number;
            return doHintNumber(object);
        }
    }

    public static OrdinaryToPrimitiveNode createHintString(JSContext context) {
        return create(context, Hint.String);
    }

    public static OrdinaryToPrimitiveNode createHintNumber(JSContext context) {
        return create(context, Hint.Number);
    }

    public static OrdinaryToPrimitiveNode create(JSContext context, Hint hint) {
        return new OrdinaryToPrimitiveNode(context, hint);
    }

    protected Object doHintString(DynamicObject object) {
        Object toString = getToString().executeWithTarget(object);
        if (toStringFunctionProfile.profile(isCallableNode.executeBoolean(toString))) {
            Object result = callToStringNode.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        Object valueOf = getValueOf().executeWithTarget(object);
        if (valueOfFunctionProfile.profile(isCallableNode.executeBoolean(valueOf))) {
            Object result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    protected Object doHintNumber(DynamicObject object) {
        assert JSGuards.isJSObject(object);
        Object valueOf = getValueOf().executeWithTarget(object);
        if (valueOfFunctionProfile.profile(isCallableNode.executeBoolean(valueOf))) {
            Object result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        Object toString = getToString().executeWithTarget(object);
        if (toStringFunctionProfile.profile(isCallableNode.executeBoolean(toString))) {
            Object result = callToStringNode.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    private PropertyNode getToString() {
        if (getToStringNode == null || callToStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToStringNode = insert(PropertyNode.createMethod(context, null, JSRuntime.TO_STRING));
            callToStringNode = insert(JSFunctionCallNode.createCall());
        }
        return getToStringNode;
    }

    private PropertyNode getValueOf() {
        if (getValueOfNode == null || callValueOfNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getValueOfNode = insert(PropertyNode.createMethod(context, null, JSRuntime.VALUE_OF));
            callValueOfNode = insert(JSFunctionCallNode.createCall());
        }
        return getValueOfNode;
    }
}
