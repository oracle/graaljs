/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;

/**
 * This implements ECMA 9.3 ToNumber, but always converting the result to a double value.
 *
 * @see JSToNumberNode
 */
public abstract class JSToDoubleNode extends JavaScriptBaseNode {

    @Child private JSToDoubleNode toDoubleNode;

    public abstract Object execute(Object value);

    public abstract double executeDouble(Object value);

    public static JSToDoubleNode create() {
        return JSToDoubleNodeGen.create();
    }

    @Specialization
    protected static double doInteger(int value) {
        return value;
    }

    @Specialization
    protected static double doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization
    protected final double doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertBigIntToNumber(this);
    }

    @Specialization(guards = "isJSNull(value)")
    protected static double doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization
    protected static double doStringDouble(String value,
                    @Cached("create()") JSStringToNumberNode stringToNumberNode) {
        return stringToNumberNode.executeString(value);
    }

    @Specialization(guards = "isJSObject(value)")
    protected double doJSObject(DynamicObject value,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
        return getToDoubleNode().executeDouble(toPrimitiveNode.execute(value));
    }

    @Specialization
    protected final double doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization
    protected final double doRecord(@SuppressWarnings("unused") Record value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Record value", this);
    }

    @Specialization
    protected final double doTuple(@SuppressWarnings("unused") Tuple value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Tuple value", this);
    }

    @Specialization(guards = "isForeignObject(object)")
    protected double doForeignObject(Object object,
                    @Cached("createHintNumber()") JSToPrimitiveNode toPrimitiveNode) {
        return getToDoubleNode().executeDouble(toPrimitiveNode.execute(object));
    }

    private JSToDoubleNode getToDoubleNode() {
        if (toDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toDoubleNode = insert(JSToDoubleNode.create());
        }
        return toDoubleNode;
    }

    @Specialization(guards = "isJavaNumber(value)")
    protected static double doJavaNumber(Object value) {
        return JSRuntime.doubleValue((Number) value);
    }
}
