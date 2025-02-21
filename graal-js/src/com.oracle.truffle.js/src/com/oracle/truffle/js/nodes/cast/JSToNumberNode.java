/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNodeGen.JSToNumberUnaryNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * This implements the abstract operation ToNumber.
 */
@GenerateUncached
public abstract class JSToNumberNode extends JavaScriptBaseNode {

    public abstract Object execute(Object value);

    public final Number executeNumber(Object value) {
        return (Number) execute(value);
    }

    @NeverDefault
    public static JSToNumberNode create() {
        return JSToNumberNodeGen.create();
    }

    public static JavaScriptNode create(JavaScriptNode child) {
        if (child.isResultAlwaysOfType(Number.class) || child.isResultAlwaysOfType(int.class) || child.isResultAlwaysOfType(double.class)) {
            return child;
        }
        return JSToNumberUnaryNodeGen.create(child);
    }

    @Specialization
    protected static int doInteger(int value) {
        return value;
    }

    @Specialization
    protected static int doBoolean(boolean value) {
        return JSRuntime.booleanToNumber(value);
    }

    @Specialization
    protected static double doDouble(double value) {
        return value;
    }

    @Specialization(guards = "isJSNull(value)")
    protected static int doNull(@SuppressWarnings("unused") Object value) {
        return 0;
    }

    @Specialization(guards = "isUndefined(value)")
    protected static double doUndefined(@SuppressWarnings("unused") Object value) {
        return Double.NaN;
    }

    @Specialization
    protected Number doString(TruffleString value,
                    @Cached JSStringToNumberNode stringToNumberNode) {
        double doubleValue = stringToNumberNode.execute(value);
        return JSRuntime.doubleToNarrowestNumber(doubleValue);
    }

    @InliningCutoff
    @Specialization
    protected Number doJSObject(JSObject value,
                    @Shared @Cached JSToPrimitiveNode toPrimitiveNode,
                    @Shared @Cached JSToNumberNode toNumberNode) {
        return toNumberNode.executeNumber(toPrimitiveNode.executeHintNumber(value));
    }

    @Specialization
    protected final Number doSymbol(@SuppressWarnings("unused") Symbol value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a Symbol value", this);
    }

    @Specialization(guards = "!value.isForeign()")
    protected final Number doBigInt(@SuppressWarnings("unused") BigInt value) {
        throw Errors.createTypeErrorCannotConvertToNumber("a BigInt value", this);
    }

    @Specialization(guards = "value.isForeign()")
    protected static Number doForeignBigInt(BigInt value) {
        return value.doubleValue();
    }

    @Specialization
    protected static double doLong(long value) {
        return value;
    }

    @InliningCutoff
    @Specialization(guards = "isJSObject(value) || isForeignObject(value)", replaces = "doJSObject")
    protected Number doJSOrForeignObject(Object value,
                    @Shared @Cached JSToPrimitiveNode toPrimitiveNode,
                    @Shared @Cached JSToNumberNode toNumberNode) {
        return toNumberNode.executeNumber(toPrimitiveNode.executeHintNumber(value));
    }

    public abstract static class JSToNumberUnaryNode extends JSUnaryNode {

        protected JSToNumberUnaryNode(JavaScriptNode operand) {
            super(operand);
        }

        @Specialization
        protected static Object doDefault(Object value,
                        @Cached JSToNumberNode toNumberNode) {
            return toNumberNode.executeNumber(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return create(cloneUninitialized(getOperand(), materializedTags));
        }

        @Override
        public boolean isResultAlwaysOfType(Class<?> clazz) {
            return super.isResultAlwaysOfType(Number.class);
        }

        @Override
        public String expressionToString() {
            return getOperand().expressionToString();
        }
    }
}
