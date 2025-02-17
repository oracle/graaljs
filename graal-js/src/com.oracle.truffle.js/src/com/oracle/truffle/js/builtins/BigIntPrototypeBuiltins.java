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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToLocaleStringIntlNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToLocaleStringNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntToStringNodeGen;
import com.oracle.truffle.js.builtins.BigIntPrototypeBuiltinsFactory.JSBigIntValueOfNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsIntNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.intl.InitializeNumberFormatNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBigIntObject;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormat;
import com.oracle.truffle.js.runtime.builtins.intl.JSNumberFormatObject;

/**
 * Contains builtins for {@linkplain JSBigInt}.prototype.
 */
public final class BigIntPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<BigIntPrototypeBuiltins.BigIntPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new BigIntPrototypeBuiltins();

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

        @Child private JSToIntegerAsIntNode toIntegerNode;

        public JSBigIntOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        protected JSException noBigIntFailure(Object value) {
            throw Errors.createTypeError(JSRuntime.safeToString(value) + " is not a BigInt");
        }

        protected int toIntegerAsInt(Object target) {
            if (toIntegerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntegerNode = insert(JSToIntegerAsIntNode.create());
            }
            return toIntegerNode.executeInt(target);
        }
    }

    public abstract static class JSBigIntToStringNode extends JSBigIntOperation {

        public JSBigIntToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(radix)"})
        protected TruffleString toStringBigIntRadix10(BigInt thisObj, Object radix,
                        @Cached @Shared InlinedBranchProfile radixErrorBranch) {
            return toStringImpl(thisObj, 10, radixErrorBranch);
        }

        @Specialization(guards = {"!isUndefined(radix)"})
        protected TruffleString toStringBigInt(BigInt thisObj, Object radix,
                        @Cached @Shared InlinedBranchProfile radixErrorBranch) {
            return toStringImpl(thisObj, radix, radixErrorBranch);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isUndefined(radix)"})
        protected TruffleString toStringRadix10(JSBigIntObject thisObj, Object radix,
                        @Cached @Shared InlinedBranchProfile radixErrorBranch) {
            return toStringImpl(JSBigInt.valueOf(thisObj), 10, radixErrorBranch);
        }

        @Specialization(guards = {"!isUndefined(radix)"})
        protected TruffleString toString(JSBigIntObject thisObj, Object radix,
                        @Cached @Shared InlinedBranchProfile radixErrorBranch) {
            return toStringImpl(JSBigInt.valueOf(thisObj), radix, radixErrorBranch);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected void toStringNoBigInt(Object thisObj, Object radix) {
            throw Errors.createTypeError("BigInt.prototype.toString requires that 'this' be a BigInt");
        }

        private TruffleString toStringImpl(BigInt numberVal, Object radix, InlinedBranchProfile radixErrorBranch) {
            int radixVal = toIntegerAsInt(radix);
            if (radixVal < 2 || radixVal > 36) {
                radixErrorBranch.enter(this);
                throw Errors.createRangeError("toString() expects radix in range 2-36");
            }
            return Strings.fromBigInt(numberVal, radixVal);
        }
    }

    public abstract static class JSBigIntToLocaleStringIntlNode extends JSBigIntOperation {

        @Child InitializeNumberFormatNode initNumberFormatNode;

        public JSBigIntToLocaleStringIntlNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.initNumberFormatNode = InitializeNumberFormatNode.createInitalizeNumberFormatNode(context);
        }

        @TruffleBoundary
        private JSNumberFormatObject createNumberFormat(Object locales, Object options) {
            JSNumberFormatObject numberFormatObj = JSNumberFormat.create(getContext(), getRealm());
            initNumberFormatNode.executeInit(numberFormatObj, locales, options);
            return numberFormatObj;
        }

        @Specialization
        protected TruffleString bigIntToLocaleString(BigInt thisObj, Object locales, Object options) {
            JSNumberFormatObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, thisObj);
        }

        @Specialization
        protected TruffleString jsBigIntToLocaleString(JSBigIntObject thisObj, Object locales, Object options) {
            JSNumberFormatObject numberFormatObj = createNumberFormat(locales, options);
            return JSNumberFormat.format(numberFormatObj, JSBigInt.valueOf(thisObj));
        }

        @Fallback
        @SuppressWarnings("unused")
        protected Object failForNonBigInts(Object notANumber, Object locales, Object options) {
            throw noBigIntFailure(notANumber);
        }

    }

    public abstract static class JSBigIntToLocaleStringNode extends JSBigIntOperation {

        public JSBigIntToLocaleStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected TruffleString toLocaleStringBigInt(BigInt thisObj) {
            return toLocaleStringImpl(thisObj);
        }

        @Specialization
        protected TruffleString toLocaleStringJSBigInt(JSBigIntObject thisObj) {
            return toLocaleStringImpl(JSBigInt.valueOf(thisObj));
        }

        private static TruffleString toLocaleStringImpl(BigInt bi) {
            return Strings.fromBigInt(bi);
        }

        @Fallback
        protected Object failForNonBigInts(Object thisObject) {
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

        @Specialization
        protected BigInt valueOf(JSBigIntObject thisObj) {
            return JSBigInt.valueOf(thisObj);
        }

        @Fallback
        protected void valueOf(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeError("BigInt.prototype.valueOf requires that 'this' be a BigInt");
        }
    }
}
