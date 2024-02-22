/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import com.oracle.truffle.api.strings.TruffleString;

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
    private static final int ARROW_FUNCTION_PARAMETER_SCOPE = 1 << 11;

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
    /** Use map. */
    protected EconomicMap<String, UseInfo> uses;

    private boolean closed;
    private boolean hasBlockScopedOrRedeclaredSymbols;
    private boolean hasPrivateNames;
    private boolean hasClosures;
    private boolean hasEval;
    private boolean hasNestedEval;

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
            return parent == null ? 0 : parent.flags;
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
        return new Scope(parent, FUNCTION_PARAMETER_SCOPE | FUNCTION_TOP_SCOPE |
                        ((functionFlags & FunctionNode.IS_ARROW) != 0 ? ARROW_FUNCTION_PARAMETER_SCOPE : 0),
                        computeFlags(parent, functionFlags));
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
                    // https://tc39.es/ecma262/#sec-variablestatements-in-catch-blocks
                    /*
                     * The Block of a Catch clause may contain var declarations that bind a name
                     * that is also bound by the CatchParameter. At runtime, such bindings are
                     * instantiated in the VariableDeclarationEnvironment. They do not shadow the
                     * same-named bindings introduced by the CatchParameter.
                     */
                    continue;
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
    public boolean addPrivateName(TruffleString name, int symbolFlags) {
        assert isClassBodyScope();
        // Register a declared private name.
        if (hasSymbol(name.toJavaStringUncached())) {
            assert getExistingSymbol(name.toJavaStringUncached()).isPrivateName();
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

    public boolean isArrowFunctionParameterScope() {
        return (type & ARROW_FUNCTION_PARAMETER_SCOPE) != 0;
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
        resolveUses();
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Clears defined symbols and moves any local uses into the parent scope.
     */
    public void kill() {
        assert !closed : "scope is closed";
        assert isKillable() : "must not be killed";
        symbols.clear();
        if (uses != null) {
            if (parent != null && !parent.closed) {
                MapCursor<String, UseInfo> cursor = uses.getEntries();
                while (cursor.advance()) {
                    String usedName = cursor.getKey();
                    UseInfo useInfo = cursor.getValue();
                    assert useInfo.isUnresolved();
                    if (useInfo.use == this) {
                        parent.addLocalUse(usedName);
                    }
                    if (useInfo.hasInnerUse()) {
                        useInfo.use = null;
                        parent.addUsesFromInnerScope(usedName, useInfo);
                    }
                }
            }
            uses = null;
        }
        closed = true;
    }

    /**
     * Records the use of an identifier reference in this scope to be resolved.
     */
    public void addIdentifierReference(String name) {
        addLocalUse(name);
    }

    private UseInfo getUseInfo(String name) {
        if (uses == null) {
            return null;
        }
        return uses.get(name);
    }

    private void putUseInfo(String name, UseInfo useInfo) {
        if (uses == null) {
            uses = EconomicMap.create();
        }
        uses.put(name, useInfo);
    }

    private void removeUseInfo(String name) {
        if (uses == null) {
            return;
        }
        uses.removeKey(name);
    }

    /**
     * Records or resolves a use in this scope.
     */
    private void addLocalUse(String name) {
        assert !closed : "scope is closed";
        UseInfo foundUse = getUseInfo(name);
        Symbol foundSymbol = symbols.get(name);
        if (foundSymbol != null) {
            // declared in this scope, can be resolved immediately.
            if (foundUse == null) {
                foundUse = UseInfo.resolvedLocal(name, this);
                putUseInfo(name, foundUse);
                assert foundUse.use == this;
            } else {
                assert foundUse.use == this || foundUse.use == null;
                foundUse.use = this;
            }
            resolveUse(foundUse, this, foundSymbol);
        } else {
            // as of yet unresolved, could still be resolved in the current scope later
            // or in one of the outer scopes eventually.
            if (foundUse == null) {
                foundUse = UseInfo.unresolved(name);
                putUseInfo(name, foundUse);
                assert foundUse.use == null;
            } else {
                assert foundUse.def == null;
                assert foundUse.use == this || foundUse.use == null;
            }
            foundUse.use = this;
        }
    }

    /**
     * Used to resolve or forward unresolved uses from inner scopes.
     */
    private void addUsesFromInnerScope(String name, UseInfo useInfo) {
        assert !closed : "scope is closed";
        if (useInfo.use == null && useInfo.innerUseScopes == null) {
            return;
        }

        Symbol foundSymbol = symbols.get(name);
        if (foundSymbol != null && !isKillable()) {
            // declared here, so we can resolve it immediately.
            resolveUse(useInfo, this, foundSymbol);
            return;
        }
        // merge unresolved use into the parent scope
        UseInfo foundUse = getUseInfo(name);
        if (foundUse == null) {
            foundUse = UseInfo.unresolved(name);
            putUseInfo(name, foundUse);
        } else {
            assert foundUse.use == this || foundUse.use == null;
        }

        if (useInfo.innerUseScopes != null) {
            for (Scope innerScope : useInfo.innerUseScopes) {
                foundUse.addInnerUse(innerScope);
            }
        }
        if (useInfo.use != null) {
            foundUse.addInnerUse(useInfo.use);
        }
    }

    private boolean isKillable() {
        // Delay resolution while parsing cover arrow parameter list in case it will be revoked.
        return isArrowFunctionParameterScope();
    }

    /**
     * Resolves free variables in this scope and forwards unresolved uses to the parent scope.
     */
    public void resolveUses() {
        if (uses == null || uses.isEmpty()) {
            return;
        }

        boolean hasDeclarations = hasDeclarations();
        MapCursor<String, UseInfo> cursor = uses.getEntries();
        while (cursor.advance()) {
            String usedName = cursor.getKey();
            UseInfo useInfo = cursor.getValue();
            if (useInfo.isUnresolved()) {
                Symbol foundSymbol;
                if (hasDeclarations && (foundSymbol = symbols.get(usedName)) != null) {
                    // resolved in this scope
                    resolveUse(useInfo, this, foundSymbol);
                } else {
                    // unresolved, pass on to parent scope, if possible
                    // no use in this scope, skip this scope and remove the use here.
                    if (parent == null || parent.closed) {
                        // A closed parent scope implies a parsing boundary, e.g. eval().
                        // We cannot make any symbols available that are not already captured.
                        // We could resolve these symbols statically but there is currently
                        // no advantage in doing so early.
                        markUseUnresolvable(useInfo);
                    } else {
                        parent.addUsesFromInnerScope(usedName, useInfo);
                    }
                    if (useInfo.use == null) {
                        cursor.remove();
                    }
                }
            }
        }
        if (uses.isEmpty()) {
            uses = null; // free unused memory
        }

        // Note: In case of nested eval, we would still not have to capture variables that are
        // shadowed in all inner scopes that contain eval.
        // Consider e.g.: `{ let x; { let x; { eval("..."); } } }`
        // Here the outer x would not have to be captured (only the inner x). For simplicity,
        // we currently assume all variables in scopes that contain (nested) eval may be captured.
    }

    /**
     * Resolve use in the local and any inner scopes to the scope defining the symbol.
     */
    private static void resolveUse(UseInfo useInfo, Scope defScope, Symbol foundSymbol) {
        String name = useInfo.name;
        // cannot change a resolved scope
        assert useInfo.def == null || useInfo.def == defScope;
        assert name.equals(foundSymbol.getName());
        assert defScope.hasSymbol(foundSymbol.getName());
        if (useInfo.use != null) {
            markSymbolUsed(foundSymbol, defScope, useInfo.use);
        }
        if (useInfo.innerUseScopes != null) {
            for (Scope inner : useInfo.innerUseScopes) {
                UseInfo innerUse = inner.getUseInfo(name);
                if (innerUse == null) {
                    innerUse = UseInfo.unresolved(name);
                    innerUse.use = inner;
                    inner.putUseInfo(name, innerUse);
                }
                innerUse.def = defScope;
                innerUse.innerUseScopes = null;

                markSymbolUsed(foundSymbol, defScope, inner);
            }
            useInfo.innerUseScopes = null;
        }
        useInfo.def = defScope;
    }

    private static void markSymbolUsed(Symbol foundSymbol, Scope defScope, Scope useScope) {
        foundSymbol.setUsed();
        if (defScope != useScope) {
            boolean inClosure = isInClosure(defScope, useScope);
            if (inClosure) {
                foundSymbol.setClosedOver();
                defScope.hasClosures = true;
            } else {
                foundSymbol.setUsedInInnerScope();
            }
        }
    }

    private static void markUseUnresolvable(UseInfo useInfo) {
        String name = useInfo.name;
        assert useInfo.def == null : name; // must not be resolved

        if (useInfo.innerUseScopes != null) {
            for (Scope inner : useInfo.innerUseScopes) {
                UseInfo innerUse = inner.getUseInfo(name);
                if (innerUse != null) {
                    assert innerUse.def == null : name; // must not be resolved
                    if (innerUse.use != null) {
                        innerUse.use.removeUseInfo(name);
                    }
                }
            }
            useInfo.innerUseScopes = null;
        }
    }

    /**
     * Checks if there is a function boundary between the inner and outer scopes.
     */
    private static boolean isInClosure(Scope outer, Scope inner) {
        assert inner != null && outer != null;
        Scope current = inner;
        while (current != null && current != outer) {
            if (current.isFunctionTopScope()) {
                // scopes cross a function boundary
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Returns true if the scope has closed over variables.
     */
    public boolean hasClosures() {
        return hasClosures;
    }

    /**
     * Does this scope contain a direct eval.
     */
    public boolean hasEval() {
        return hasEval;
    }

    /**
     * Marks this scope as containing a direct eval.
     */
    public void setHasEval() {
        if (!hasEval) {
            hasEval = true;
            setHasNestedEval();
        }
    }

    /**
     * Does this scope or any nested scopes contain a direct eval.
     */
    public boolean hasNestedEval() {
        return hasNestedEval;
    }

    public void setHasNestedEval() {
        // taint all parent scopes
        for (Scope current = this; current != null; current = current.parent) {
            if (current.hasNestedEval || current.closed) {
                break;
            }
            current.hasNestedEval = true;
        }
    }

    @Override
    public String toString() {
        StringJoiner names = new StringJoiner(",", "(", ")");
        for (Symbol symbol : symbols.getValues()) {
            String name = symbol.getName();
            String mark = "";
            if (symbol.isClosedOver()) {
                mark += "'";
            } else if (symbol.isUsedInInnerScope()) {
                mark += "\"";
            } else if (hasNestedEval) {
                if (!symbol.isUsed()) {
                    mark += "*";
                }
            } else if (!symbol.isUsed()) {
                mark += "-";
            }
            names.add(name + mark);
        }

        String usedNames = "";
        if (uses != null) {
            StringJoiner sj = new StringJoiner(",", ">(", ")").setEmptyValue("");
            for (String use : uses.getKeys()) {
                if (!symbols.containsKey(use)) {
                    sj.add(use);
                }
            }
            usedNames = sj.toString();
        }

        String taint = "";
        if (hasClosures) {
            taint += "'";
        } else if (hasNestedEval) {
            taint += "*";
        }
        return "[" + getScopeKindName() + "Scope" + taint + names + usedNames + (parent == null ? "" : ", " + parent + "") + "]";
    }

    private String getScopeKindName() {
        if (isGlobalScope()) {
            return "Global";
        } else if (isModuleScope()) {
            return "Module";
        } else if (isEvalScope()) {
            return "Eval";
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
        }
        return "";
    }

    /**
     * Tracks use of symbol in the current and any nested scopes.
     */
    static final class UseInfo {
        /** Used name. */
        final String name;
        /** Resolved scope in which the symbol has been defined. Must be a parent scope. */
        Scope def;
        /** Local scope in which the symbol is used. */
        Scope use;
        /** Inner scopes with unresolved uses of the symbol. */
        List<Scope> innerUseScopes;

        private UseInfo(String name, Scope use, Scope def) {
            this.name = Objects.requireNonNull(name);
            this.use = use;
            this.def = def;
        }

        static UseInfo resolvedLocal(String name, Scope local) {
            return new UseInfo(name, local, local);
        }

        static UseInfo unresolved(String name) {
            return new UseInfo(name, null, null);
        }

        /**
         * Add an unresolved use of the symbol in an inner scope.
         */
        void addInnerUse(Scope useScope) {
            if (innerUseScopes == null) {
                innerUseScopes = new ArrayList<>();
            }
            // there should not be any duplicate scopes in the list
            assert innerUseScopes.stream().noneMatch(s -> s == useScope) : name;
            innerUseScopes.add(useScope);
        }

        boolean isResolved() {
            return !isUnresolved();
        }

        boolean isUnresolved() {
            return def == null || innerUseScopes != null;
        }

        boolean hasInnerUse() {
            return innerUseScopes != null;
        }
    }
}
