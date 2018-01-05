/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.SymbolFunctionBuiltinsFactory.SymbolForNodeGen;
import com.oracle.truffle.js.builtins.SymbolFunctionBuiltinsFactory.SymbolKeyForNodeGen;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for Symbol function.
 */
public final class SymbolFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<SymbolFunctionBuiltins.SymbolFunction> {
    protected SymbolFunctionBuiltins() {
        super(JSSymbol.CLASS_NAME, SymbolFunction.class);
    }

    public enum SymbolFunction implements BuiltinEnum<SymbolFunction> {
        for_(1),
        keyFor(1);

        private final int length;

        SymbolFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SymbolFunction builtinEnum) {
        switch (builtinEnum) {
            case for_:
                return SymbolForNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case keyFor:
                return SymbolKeyForNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class SymbolForNode extends JSBuiltinNode {
        @Child private JSToStringNode toStringNode;

        public SymbolForNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toStringNode = JSToStringNode.create();
        }

        @Specialization
        protected Symbol symbolFor(Object key) {
            String stringKey = toStringNode.executeString(key);
            return getOrCreateSymbol(getContext().getSymbolRegistry(), stringKey);
        }

        @TruffleBoundary
        private static Symbol getOrCreateSymbol(Map<String, Symbol> symbolRegistry, String stringKey) {
            Symbol symbol = symbolRegistry.get(stringKey);
            if (symbol == null) {
                symbol = Symbol.create(stringKey);
                symbolRegistry.put(stringKey, symbol);
            }
            return symbol;
        }
    }

    public abstract static class SymbolKeyForNode extends JSBuiltinNode {
        public SymbolKeyForNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isSymbol(symbol)")
        protected Object symbolKeyFor(Symbol symbol) {
            return getKeyFor(getContext().getSymbolRegistry(), symbol);
        }

        @TruffleBoundary
        private static Object getKeyFor(Map<String, Symbol> symbolRegistry, Symbol symbol) {
            return symbolRegistry.entrySet().stream().filter(entry -> entry.getValue().equals(symbol)).findFirst().<Object> map(entry -> entry.getKey()).orElse(Undefined.instance);
        }

        @TruffleBoundary
        @Specialization(guards = {"!isSymbol(argument)"})
        protected static Symbol valueOf(Object argument) {
            throw Errors.createTypeError("Not a symbol: %s", JSRuntime.objectToString(argument));
        }
    }
}
