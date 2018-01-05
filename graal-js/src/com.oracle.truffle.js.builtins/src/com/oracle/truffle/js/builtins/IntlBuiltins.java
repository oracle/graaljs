/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.IntlBuiltinsFactory.GetCanonicalLocalesNodeGen;
import com.oracle.truffle.js.nodes.intl.JSToCanonicalizedLocaleListNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSIntl;

/**
 * Contains builtins for {@linkplain Intl} function (constructor).
 */
public final class IntlBuiltins extends JSBuiltinsContainer.SwitchEnum<IntlBuiltins.Intl> {

    protected IntlBuiltins() {
        super(JSIntl.CLASS_NAME, Intl.class);
    }

    public enum Intl implements BuiltinEnum<Intl> {
        getCanonicalLocales(1);

        private final int length;

        Intl(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Intl builtinEnum) {
        switch (builtinEnum) {
            case getCanonicalLocales:
                return GetCanonicalLocalesNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class GetCanonicalLocalesNode extends JSBuiltinNode {

        @Child JSToCanonicalizedLocaleListNode canonicalizeLocaleListNode;

        public GetCanonicalLocalesNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @CompilerDirectives.TruffleBoundary
        protected Object getCanonicalLocales(Object locales) {
            if (canonicalizeLocaleListNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                canonicalizeLocaleListNode = insert(JSToCanonicalizedLocaleListNode.create(getContext()));
            }
            String[] languageTags = canonicalizeLocaleListNode.executeLanguageTags(locales);
            return JSRuntime.createArrayFromList(getContext(), Arrays.asList((Object[]) languageTags));
        }
    }
}
