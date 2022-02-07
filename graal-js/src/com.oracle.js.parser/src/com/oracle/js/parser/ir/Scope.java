/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser.ir;

import java.util.StringJoiner;

import org.graalvm.collections.EconomicMap;

/**
 * Represents a binding scope (corresponds to LexicalEnvironment or VariableEnvironment).
 */
public final class Scope {
    private final Scope parent;
    private final int type;
    private final int flags;

    private static final int BLOCK_SCOPE = 1 << 0;
    private static final int FUNCTION_BODY_SCOPE = 1 << 1;
    private static final int FUNCTION_PARAMETER_SCOPE = 1 << 2;
    private static final int CATCH_PARAMETER_SCOPE = 1 << 3;
    private static final int GLOBAL_SCOPE = 1 << 4;
    private static final int MODULE_SCOPE = 1 << 5;
    private static final int FUNCTION_TOP_SCOPE = 1 << 6;
    private static final int SWITCH_BLOCK_SCOPE = 1 << 7;
    /** Class head scope = LexicalEnvironment of the class (containing the class name binding). */
    private static final int CLASS_HEAD_SCOPE = 1 << 8;
    /** Class body scope = PrivateEnvironment of the class (containing private names and brands). */
    private static final int CLASS_BODY_SCOPE = 1 << 9;
    private static final int EVAL_SCOPE = 1 << 10;

    /** Scope is in a function context. {@code new.target} is available. */
    private static final int IN_FUNCTION = 1 << 16;
    /** Scope is in a method context. Super property accesses are allowed. */
    private static final int IN_METHOD = 1 << 17;
    /** Scope is in a derived class constructor. Super calls are allowed. */
    private static final int IN_DERIVED_CONSTRUCTOR = 1 << 18;
    /** Scope is in a class field initializer. 'arguments' is not allowed. */
    private static final int IS_CLASS_FIELD_INITIALIZER = 1 << 19;

    /** Symbol table - keys must be returned in the order they were put in. */
    protected final EconomicMap<String, Symbol> symbols;

    private boolean closed;
    private boolean hasBlockScopedOrRedeclaredSymbols;
    private boolean hasPrivateNames;

    private Scope(Scope parent, int type, int flags) {
        this.parent = parent;
        this.type = type;
        this.symbols = EconomicMap.create();
        this.flags = flags;
    }

    private Scope(Scope parent, int type) {
        this(parent, type, parent == null ? 0 : parent.flags);
    }

    private static int computeFlags(Scope parent, int functionFlags) {
        if ((functionFlags & FunctionNode.IS_ARROW) != 0) {
            // propagate flags from enclosing function scope.
            return parent.flags;
        } else {
            int flags = 0;
            flags |= IN_FUNCTION;
            flags |= ((functionFlags & FunctionNode.IS_METHOD) != 0) ? IN_METHOD : 0;
            flags |= ((functionFlags & FunctionNode.IS_DERIVED_CONSTRUCTOR) != 0) ? IN_DERIVED_CONSTRUCTOR : 0;
            flags |= ((functionFlags & FunctionNode.IS_CLASS_FIELD_INITIALIZER) != 0) ? IS_CLASS_FIELD_INITIALIZER : 0;
            return flags;
        }
    }

    public static Scope createGlobal() {
        return new Scope(null, FUNCTION_BODY_SCOPE | GLOBAL_SCOPE | FUNCTION_TOP_SCOPE);
    }

    public static Scope createModule() {
        return new Scope(null, FUNCTION_BODY_SCOPE | MODULE_SCOPE | FUNCTION_TOP_SCOPE);
    }

    public static Scope createFunctionBody(Scope parent, int functionFlags, boolean functionTopScope) {
        assert functionTopScope || (parent.isFunctionParameterScope() && parent.isFunctionTopScope());
        return new Scope(parent, FUNCTION_BODY_SCOPE | (functionTopScope ? FUNCTION_TOP_SCOPE : 0), computeFlags(parent, functionFlags));
    }

    public static Scope createFunctionBody(Scope parent) {
        return createFunctionBody(parent, 0, true);
    }

    public static Scope createBlock(Scope parent) {
        return new Scope(parent, BLOCK_SCOPE);
    }

    public static Scope createCatchParameter(Scope parent) {
        return new Scope(parent, CATCH_PARAMETER_SCOPE);
    }

    public static Scope createFunctionParameter(Scope parent, int functionFlags) {
        return new Scope(parent, FUNCTION_PARAMETER_SCOPE | FUNCTION_TOP_SCOPE, computeFlags(parent, functionFlags));
    }

    public static Scope createSwitchBlock(Scope parent) {
        return new Scope(parent, BLOCK_SCOPE | SWITCH_BLOCK_SCOPE);
    }

    public static Scope createClassHead(Scope parent) {
        return new Scope(parent, BLOCK_SCOPE | CLASS_HEAD_SCOPE);
    }

    public static Scope createClassBody(Scope parent) {
        return new Scope(parent, CLASS_BODY_SCOPE);
    }

    public static Scope createEval(Scope parent, boolean strict) {
        return new Scope(parent, EVAL_SCOPE | (strict ? FUNCTION_BODY_SCOPE | FUNCTION_TOP_SCOPE : 0));
    }

    public Scope getParent() {
        return parent;
    }

    /**
     * Get all the symbols defined in this block, in definition order.
     *
     * @return symbol iterator
     */
    public Iterable<Symbol> getSymbols() {
        return symbols.getValues();
    }

    /**
     * Retrieves an existing symbol defined in the current block.
     *
     * @param name the name of the symbol
     * @return an existing symbol with the specified name defined in the current block, or null if
     *         this block doesn't define a symbol with this name.
     */
    public Symbol getExistingSymbol(final String name) {
        return symbols.get(name);
    }

    /**
     * Test if a symbol with this name is defined in the current block.
     *
     * @param name the name of the symbol
     */
    public boolean hasSymbol(final String name) {
        return symbols.containsKey(name);
    }

    /**
     * Get the number of symbols defined in this block.
     */
    public int getSymbolCount() {
        return symbols.size();
    }

    /**
     * Add symbol to the scope if it does not already exist.
     */
    public Symbol putSymbol(final Symbol symbol) {
        assert !closed : "scope is closed";
        Symbol existing = symbols.putIfAbsent(symbol.getName(), symbol);
        if (existing != null) {
            assert (existing.getFlags() & Symbol.KINDMASK) == (symbol.getFlags() & Symbol.KINDMASK) : symbol;
            return existing;
        }
        if (symbol.isBlockScoped() || symbol.isVarRedeclaredHere()) {
            hasBlockScopedOrRedeclaredSymbols = true;
        }
        if (symbol.isPrivateName()) {
            hasPrivateNames = true;
        }
        return null;
    }

    public boolean hasBlockScopedOrRedeclaredSymbols() {
        return hasBlockScopedOrRedeclaredSymbols;
    }

    public boolean hasPrivateNames() {
        return hasPrivateNames;
    }

    public boolean hasDeclarations() {
        return !symbols.isEmpty();
    }

    /**
     * Returns true if the name is lexically declared in this scope or any of its enclosing scopes
     * within this function.
     *
     * @param varName the declared name
     * @param annexB if true, ignore catch parameters
     * @param includeParameters include parameter scope?
     */
    public boolean isLexicallyDeclaredName(final String varName, final boolean annexB, final boolean includeParameters) {
        for (Scope current = this; current != null; current = current.getParent()) {
            Symbol existingSymbol = current.getExistingSymbol(varName);
            if (existingSymbol != null && existingSymbol.isBlockScoped()) {
                if (existingSymbol.isCatchParameter() && annexB) {
                    continue; // B.3.5 VariableStatements in Catch Blocks
                }
                return true;
            }
            if (includeParameters ? current.isFunctionTopScope() : current.isFunctionBodyScope()) {
                break;
            }
        }
        return false;
    }

    /**
     * Returns a block scoped symbol in this scope or any of its enclosing scopes within this
     * function.
     *
     * @param varName the symbol name
     */
    public Symbol findBlockScopedSymbolInFunction(String varName) {
        for (Scope current = this; current != null; current = current.getParent()) {
            Symbol existingSymbol = current.getExistingSymbol(varName);
            if (existingSymbol != null) {
                if (existingSymbol.isBlockScoped()) {
                    return existingSymbol;
                } else {
                    // early exit
                    break;
                }
            }
            if (current.isFunctionTopScope()) {
                break;
            }
        }
        return null;
    }

    /**
     * Add a private bound identifier.
     *
     * @return true if the private name was added, false if it was already declared (duplicate name)
     */
    public boolean addPrivateName(String name, int symbolFlags) {
        assert isClassBodyScope();
        // Register a declared private name.
        if (hasSymbol(name)) {
            assert getExistingSymbol(name).isPrivateName();
            return false;
        } else {
            putSymbol(new Symbol(name, Symbol.IS_CONST | Symbol.IS_PRIVATE_NAME | Symbol.HAS_BEEN_DECLARED | symbolFlags));
            return true;
        }
    }

    public boolean findPrivateName(String name) {
        for (Scope current = this; current != null; current = current.parent) {
            if (current.hasSymbol(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockScope() {
        return (type & BLOCK_SCOPE) != 0;
    }

    public boolean isFunctionBodyScope() {
        return (type & FUNCTION_BODY_SCOPE) != 0;
    }

    public boolean isFunctionParameterScope() {
        return (type & FUNCTION_PARAMETER_SCOPE) != 0;
    }

    public boolean isCatchParameterScope() {
        return (type & CATCH_PARAMETER_SCOPE) != 0;
    }

    public boolean isGlobalScope() {
        return (type & GLOBAL_SCOPE) != 0;
    }

    public boolean isModuleScope() {
        return (type & MODULE_SCOPE) != 0;
    }

    public boolean isFunctionTopScope() {
        return (type & FUNCTION_TOP_SCOPE) != 0;
    }

    public boolean isSwitchBlockScope() {
        return (type & SWITCH_BLOCK_SCOPE) != 0;
    }

    public boolean isClassBodyScope() {
        return (type & CLASS_BODY_SCOPE) != 0;
    }

    public boolean isClassHeadScope() {
        return (type & CLASS_HEAD_SCOPE) != 0;
    }

    public boolean isEvalScope() {
        return (type & EVAL_SCOPE) != 0;
    }

    public boolean inFunction() {
        return (flags & IN_FUNCTION) != 0;
    }

    public boolean inMethod() {
        return (flags & IN_METHOD) != 0;
    }

    public boolean inDerivedConstructor() {
        return (flags & IN_DERIVED_CONSTRUCTOR) != 0;
    }

    public boolean inClassFieldInitializer() {
        return (flags & IS_CLASS_FIELD_INITIALIZER) != 0;
    }

    /**
     * Closes the scope for symbol registration.
     */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
    }

    @Override
    public String toString() {
        StringJoiner names = new StringJoiner(",", "(", ")");
        for (String name : symbols.getKeys()) {
            names.add(name);
        }
        return "[" + getScopeKindName() + "Scope" + names + (parent == null ? "" : ", " + parent + "") + "]";
    }

    private String getScopeKindName() {
        if (isGlobalScope()) {
            return "Global";
        } else if (isModuleScope()) {
            return "Module";
        } else if (isFunctionBodyScope()) {
            return "Var";
        } else if (isFunctionParameterScope()) {
            return "Param";
        } else if (isCatchParameterScope()) {
            return "Catch";
        } else if (isSwitchBlockScope()) {
            return "Switch";
        } else if (isClassHeadScope()) {
            return "Class";
        } else if (isClassBodyScope()) {
            return "Private";
        } else if (isEvalScope()) {
            return "Eval";
        }
        return "";
    }
}
