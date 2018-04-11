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
            throw Errors.createTypeErrorFormat("Not a symbol: %s", JSRuntime.safeToString(argument));
        }
    }
}
