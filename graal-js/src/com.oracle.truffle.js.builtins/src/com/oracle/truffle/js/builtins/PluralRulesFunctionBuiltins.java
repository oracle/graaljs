/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.js.builtins.CollatorFunctionBuiltinsFactory.SupportedLocalesOfNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSPluralRules;

/**
 * Contains builtins for {@linkplain JSPluralRules} function (constructor).
 */
public final class PluralRulesFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<PluralRulesFunctionBuiltins.PluralRulesFunction> {
    protected PluralRulesFunctionBuiltins() {
        super(JSPluralRules.CLASS_NAME, PluralRulesFunction.class);
    }

    public enum PluralRulesFunction implements BuiltinEnum<PluralRulesFunction> {
        supportedLocalesOf(1);

        private final int length;

        PluralRulesFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, PluralRulesFunction builtinEnum) {
        switch (builtinEnum) {
            case supportedLocalesOf:
                return SupportedLocalesOfNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }
}
