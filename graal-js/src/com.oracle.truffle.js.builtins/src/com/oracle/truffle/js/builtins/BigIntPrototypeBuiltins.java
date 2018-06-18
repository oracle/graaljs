/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToLocaleStringIntlNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToStringNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntValueOfNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToIntegerNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSNumberFormat;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSBigInt}.prototype.
 */
public final class BigIntPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<BigIntPrototypeBuiltins.BigIntPrototype> {
    protected BigIntPrototypeBuiltins() {
        super(JSBigInt.PROTOTYPE_NAME, BigIntPrototype.class);
    }

    public enum BigIntPrototype implements BuiltinEnum<BigIntPrototype> {
        toLocaleString(0),
        toString(0),
        valueOf(0);

        private final int length;

        BigIntPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, BigIntPrototype builtinEnum) {
        switch (builtinEnum) {
            case toLocaleString:
                if (context.isOptionIntl402()) {
                    return JSBigIntToLocaleStringIntlNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
                } else {
                    return JSBigIntToLocaleStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
                }
            case toString:
                return JSBigIntToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case valueOf:
                return JSBigIntValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSBigIntOperation extends JSBuiltinNode {

        @Child private JSToIntegerNode toIntegerNode;

        public JSBigIntOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        protected JSException noBigIntFailure(Object value) {
            throw Errors.createTypeError(String.format("%s is not a BigInt", JSRuntime.safeToString(value)));
        }

        protected BigInt getBigIntValue(DynamicObject obj) {
            return JSBigInt.valueOf(obj);
        }

        protected int toInteger(Object target) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerNode.create());
            }
            return toIntegerNode.executeInt(target);
        }
    }

    public abstract static class JSBigIntToStringNode extends JSBigIntOperation {

        public JSBigIntToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile radixErrorBranch = BranchProfile.create();

        protected boolean isRadix10(Object radix) {
            return radix == Undefined.instance || (radix instanceof Integer && ((Integer) radix) == 10);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(radix)"})
        protected String toStringBigIntRadix10(BigInt thisObj, Object radix) {
            return toStringImpl(thisObj, 10);
        }

        @Specialization(guards = {"!isUndefined(radix)"})
        protected String toStringBigInt(BigInt thisObj, Object radix) {
            return toStringImpl(thisObj, radix);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSBigInt(thisObj)", "isUndefined(radix)"})
        protected String toStringRadix10(DynamicObject thisObj, Object radix) {
            return toStringImpl(JSBigInt.valueOf(thisObj), 10);
        }

        @Specialization(guards = {"isJSBigInt(thisObj)", "!isUndefined(radix)"})
        protected String toString(DynamicObject thisObj, Object radix) {
            return toStringImpl(JSBigInt.valueOf(thisObj), radix);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected void toStringNoBigInt(Object thisObj, Object radix) {
            throw Errors.createTypeError("BigInt.prototype.toString requires that 'this' be a BigInt");
        }

        private String toStringImpl(BigInt numberVal, Object radix) {
            int radixVal = toInteger(radix);
            if (radixVal < 2 || radixVal > 36) {
                radixErrorBranch.enter();
                throw Errors.createRangeError("toString() expects radix in range 2-36");
            }
            return numberVal.toString(radixVal);
        }
    }

    public abstract static class JSBigIntToLocaleStringIntlNode extends JSBigIntOperation {

        @Child InitializeNumberFormatNode initNumberFormatNode;

        public JSBigIntToLocaleStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @TruffleBoundary
        private DynamicObject createNumberFormat(Object locales, Object options) {
            DynamicObject numberFormatObj = JSNumberFormat.create(getContext());
            initNumberFormatNode.executeInit(numberFormatObj, locales, options);
            return numberFormatObj;
        }

        @Specialization
        protected String bigIntToLocaleString(BigInt thisObj, Object locales, Object options) {
            DynamicObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, thisObj);
        }

        @Specialization(guards = "isJSBigInt(thisObj)")
        protected String jsBigIntToLocaleString(DynamicObject thisObj, Object locales, Object options) {
            DynamicObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, getBigIntValue(thisObj));
        }

        @Fallback
        @SuppressWarnings("unused")
        protected String failForNonBigInts(Object notANumber, Object locales, Object options) {
            throw noBigIntFailure(notANumber);
        }

    }

    public abstract static class JSBigIntToLocaleStringNode extends JSBigIntOperation {

        public JSBigIntToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toLocaleStringBigInt(BigInt thisObj) {
            return toLocaleStringImpl(thisObj);
        }

        @Specialization(guards = "isJSBigInt(thisObj)")
        protected String toLocaleStringJSBigInt(DynamicObject thisObj) {
            return toLocaleStringImpl(getBigIntValue(thisObj));
        }

        private static String toLocaleStringImpl(BigInt bi) {
            return bi.toString();
        }

        @Fallback
        protected String failForNonBigInts(Object thisObject) {
            throw noBigIntFailure(thisObject);
        }
    }

    public abstract static class JSBigIntValueOfNode extends JSBigIntOperation {

        public JSBigIntValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected BigInt valueOfBigInt(BigInt thisObj) {
            return thisObj;
        }

        @Specialization(guards = "isJSBigInt(thisObj)")
        protected BigInt valueOf(DynamicObject thisObj) {
            return JSBigInt.valueOf(thisObj);
        }

        @Fallback
        protected void valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("BigInt.prototype.valueOf requires that 'this' be a BigInt");
        }
    }
}
