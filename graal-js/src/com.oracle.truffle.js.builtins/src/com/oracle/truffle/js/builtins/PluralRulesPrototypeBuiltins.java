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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesResolvedOptionsNodeGen;
import com.oracle.truffle.js.builtins.PluralRulesPrototypeBuiltinsFactory.JSPluralRulesSelectNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;

public final class PluralRulesPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<PluralRulesPrototypeBuiltins.PluralRulesPrototype> {

    protected PluralRulesPrototypeBuiltins() {
        super(JSPluralRules.PROTOTYPE_NAME, PluralRulesPrototype.class);
    }

    public enum PluralRulesPrototype implements BuiltinEnum<PluralRulesPrototype> {

        resolvedOptions(0),
        select(1);

        private final int length;

        PluralRulesPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PluralRulesPrototype builtinEnum) {
        switch (builtinEnum) {
            case resolvedOptions:
                return JSPluralRulesResolvedOptionsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case select:
                return JSPluralRulesSelectNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSPluralRulesResolvedOptionsNode extends JSBuiltinNode {

        public JSPluralRulesResolvedOptionsNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        public Object doResolvedOptions(DynamicObject pluralRules) {
            return JSPluralRules.resolvedOptions(getContext(), pluralRules);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        public void doResolvedOptions(@SuppressWarnings("unused") Object bummer) {
            throw Errors.createTypeErrorPluralRulesExpected();
        }
    }

    public abstract static class JSPluralRulesSelectNode extends JSBuiltinNode {

        public JSPluralRulesSelectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isDynamicObject(pluralRules)"})
        public Object doSelect(DynamicObject pluralRules, Object value) {
            return JSPluralRules.select(pluralRules, value);
        }

        @Specialization(guards = "!isDynamicObject(bummer)")
        @SuppressWarnings("unused")
        public void throwTypeError(Object bummer, Object value) {
            throw Errors.createTypeErrorPluralRulesExpected();
        }
    }
}
