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
package com.oracle.truffle.js.builtins.intl;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.intl.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.intl.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesSelectNodeGen;
import com.oracle.truffle.js.builtins.intl.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesSelectRangeNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRules;
import com.oracle.truffle.js.runtime.builtins.intl.JSPluralRulesObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class PluralRulesPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<PluralRulesPrototypeBuiltins.PluralRulesPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new PluralRulesPrototypeBuiltins();

    protected PluralRulesPrototypeBuiltins() {
        super(JSPluralRules.PROTOTYPE_NAME, PluralRulesPrototype.class);
    }

    public enum PluralRulesPrototype implements BuiltinEnum<PluralRulesPrototype> {

        resolvedOptions(0),
        select(1),
        selectRange(2);

        private final int length;

        PluralRulesPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (selectRange == this) {
                return JSConfig.ECMAScript2023;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PluralRulesPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSPluralRulesResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case select:
                return JSPluralRulesSelectNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case selectRange:
                return JSPluralRulesSelectRangeNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSPluralRulesResolvedOptionsNode extends JSBuiltinNode {

        public JSPluralRulesResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(JSPluralRulesObject pluralRules) {
            return JSPluralRules.resolvedOptions(getContext(), getRealm(), pluralRules);
        }

        @Fallback
        public Object throwTypeError(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorTypeXExpected(JSPluralRules.CLASS_NAME);
        }
    }

    public abstract static class JSPluralRulesSelectNode extends JSBuiltinNode {

        public JSPluralRulesSelectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doSelect(JSPluralRulesObject pluralRules, Object value,
                        @Cached JSToDoubleNode toDoubleNode) {
            return JSPluralRules.select(pluralRules, toDoubleNode.executeDouble(value));
        }

        @Fallback
        @SuppressWarnings("unused")
        public Object throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorTypeXExpected(JSPluralRules.CLASS_NAME);
        }
    }

    public abstract static class JSPluralRulesSelectRangeNode extends JSBuiltinNode {

        public JSPluralRulesSelectRangeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doSelectRange(JSPluralRulesObject pluralRules, Object start, Object end,
                        @Cached JSToDoubleNode startToDouble,
                        @Cached JSToDoubleNode endToDouble,
                        @Cached InlinedBranchProfile errorBranch) {
            if (start == Undefined.instance || end == Undefined.instance) {
                errorBranch.enter(this);
                throw Errors.createTypeError("invalid range");
            }
            double x = startToDouble.executeDouble(start);
            double y = endToDouble.executeDouble(end);
            if (Double.isNaN(x) || Double.isNaN(y)) {
                errorBranch.enter(this);
                throw Errors.createRangeError("invalid range");
            }
            return JSPluralRules.selectRange(pluralRules, x, y);
        }

        @Fallback
        @SuppressWarnings("unused")
        public Object throwTypeError(Object bummer, Object start, Object end) {
            throw Errors.createTypeErrorTypeXExpected(JSPluralRules.CLASS_NAME);
        }
    }
}
