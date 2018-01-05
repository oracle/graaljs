/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.SymbolPrototypeBuiltinsFactory.SymbolToStringNodeGen;
import com.oracle.truffle.js.builtins.SymbolPrototypeBuiltinsFactory.SymbolValueOfNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;

/**
 * Contains builtins for Symbol.prototype.
 */
public final class SymbolPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SymbolPrototypeBuiltins.SymbolPrototype> {
    protected SymbolPrototypeBuiltins() {
        super(JSSymbol.PROTOTYPE_NAME, SymbolPrototype.class);
    }

    public enum SymbolPrototype implements BuiltinEnum<SymbolPrototype> {
        toString(0),
        valueOf(0);

        private final int length;

        SymbolPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SymbolPrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
                return SymbolToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case valueOf:
                return SymbolValueOfNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SymbolToStringNode extends JSBuiltinNode {
        public SymbolToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile isSymbolObjectProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isPrimitiveSymbolProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected String toString(Object thisObj) {
            if (isSymbolObjectProfile.profile(JSSymbol.isJSSymbol(thisObj))) {
                return JSSymbol.getSymbolData((DynamicObject) thisObj).toString();
            } else if (isPrimitiveSymbolProfile.profile(thisObj instanceof Symbol)) {
                return ((Symbol) thisObj).toString();
            } else {
                throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
            }
        }
    }

    public abstract static class SymbolValueOfNode extends JSBuiltinNode {
        public SymbolValueOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile isSymbolObjectProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isPrimitiveSymbolProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        protected Symbol valueOf(Object thisObj) {
            if (isSymbolObjectProfile.profile(JSSymbol.isJSSymbol(thisObj))) {
                return JSSymbol.getSymbolData((DynamicObject) thisObj);
            } else if (isPrimitiveSymbolProfile.profile(thisObj instanceof Symbol)) {
                return (Symbol) thisObj;
            } else {
                throw Errors.createTypeErrorIncompatibleReceiver(thisObj);
            }
        }
    }
}
