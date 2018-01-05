/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.CollatorFunctionBuiltinsFactory.SupportedLocalesOfNodeGen;
import com.oracle.truffle.js.nodes.intl.JSToCanonicalizedLocaleListNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSCollator;
import com.oracle.truffle.js.runtime.util.IntlUtil;

/**
 * Contains builtins for {@linkplain JSCollator} function (constructor).
 */
public final class CollatorFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<CollatorFunctionBuiltins.CollatorFunction> {
    protected CollatorFunctionBuiltins() {
        super(JSCollator.CLASS_NAME, CollatorFunction.class);
    }

    public enum CollatorFunction implements BuiltinEnum<CollatorFunction> {
        supportedLocalesOf(1);

        private final int length;

        CollatorFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, CollatorFunction builtinEnum) {
        switch (builtinEnum) {
            case supportedLocalesOf:
                return SupportedLocalesOfNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SupportedLocalesOfNode extends JSBuiltinNode {

        @Child JSToCanonicalizedLocaleListNode toCanonicalizedLocaleListNode;

        public SupportedLocalesOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toCanonicalizedLocaleListNode = JSToCanonicalizedLocaleListNode.create(context);
        }

        @Specialization
        protected Object getSupportedLocales(Object locales) {
            return JSRuntime.createArrayFromList(getContext(), IntlUtil.supportedLocales(toCanonicalizedLocaleListNode.executeLanguageTags(locales)));
        }
    }
}
