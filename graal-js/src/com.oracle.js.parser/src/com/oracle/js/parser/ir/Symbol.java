/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Symbol is a symbolic address for a value ("variable" if you wish). Identifiers in JavaScript
 * source, as well as certain synthetic variables created by the compiler are represented by Symbol
 * objects. Symbols can address either local variable slots in bytecode ("slotted symbol"), or
 * properties in scope objects ("scoped symbol"). A symbol can also end up being defined but then
 * not used during symbol assignment calculations; such symbol will be neither scoped, nor slotted;
 * it represents a dead variable (it might be written to, but is never read). Finally, a symbol can
 * be both slotted and in scope. This special case can only occur with bytecode method parameters.
 * They all come in as slotted, but if they are used by a nested function (or eval) then they will
 * be copied into the scope object, and used from there onwards. Two further special cases are
 * parameters stored in {@code NativeArguments} objects and parameters stored in {@code Object[]}
 * parameter to variable-arity functions. Those use the {@code #getFieldIndex()} property to refer
 * to their location.
 */
public final class Symbol implements Comparable<Symbol> {
    /** Is this a let binding */
    public static final int IS_LET = 1 << 0;
    /** Is this a const binding */
    public static final int IS_CONST = 1 << 1;
    /** Is this a var binding */
    public static final int IS_VAR = 1 << 2;
    /** Mask for kind flags */
    public static final int KINDMASK = IS_LET | IS_CONST | IS_VAR;

    /** Is this a global binding */
    public static final int IS_GLOBAL = 1 << 3;
    /** Is this a parameter */
    public static final int IS_PARAM = 1 << 4;
    /** Is this a this symbol */
    public static final int IS_THIS = 1 << 5;
    /** Is this an internal symbol, never represented explicitly in source code */
    public static final int IS_INTERNAL = 1 << 6;
    /** Is this a function self-reference symbol */
    public static final int IS_FUNCTION_SELF = 1 << 7;
    /** Is this a hoistable var declaration? */
    public static final int IS_HOISTABLE_DECLARATION = 1 << 8;
    /** Is this a program level symbol? */
    public static final int IS_PROGRAM_LEVEL = 1 << 9;
    /** Is this symbol seen a declaration? Used for block scoped LET and CONST symbols only. */
    public static final int HAS_BEEN_DECLARED = 1 << 10;
    /** Is this symbol a block function declaration hoisted into the body scope. */
    public static final int IS_HOISTED_BLOCK_FUNCTION = 1 << 11;
    /**
     * Is this symbol a var declaration binding that needs to be initialized with the value of the
     * parent's scope's binding with the same name? Used for parameter bindings that are replicated
     * in the body's VariableEnvironment.
     */
    public static final int IS_VAR_REDECLARED_HERE = 1 << 12;
    /** Is this symbol declared in an unprotected switch case context? */
    public static final int IS_DECLARED_IN_SWITCH_BLOCK = 1 << 13;
    /** Is this symbol an indirect import binding of a module environment? */
    public static final int IS_IMPORT_BINDING = 1 << 14;
    /** Is this symbol a catch parameter binding? */
    public static final int IS_CATCH_PARAMETER = 1 << 15;
    /** Is this symbol a block function declaration? */
    public static final int IS_BLOCK_FUNCTION_DECLARATION = 1 << 16;
    /** Is this symbol a private name? */
    public static final int IS_PRIVATE_NAME = 1 << 17;
    /** Is this symbol a private name associated with a static member? */
    public static final int IS_PRIVATE_NAME_STATIC = 1 << 18;
    /** Is this symbol a private name associated with a method? */
    public static final int IS_PRIVATE_NAME_METHOD = 1 << 19;
    /** Is this symbol a private name associated with an accessor? */
    public static final int IS_PRIVATE_NAME_ACCESSOR = 1 << 20;
    /** Is this symbol the function 'arguments' binding? */
    public static final int IS_ARGUMENTS = 1 << 21;

    /** Is this symbol used? */
    public static final int IS_USED = 1 << 22;
    /** Is this symbol closed over by an inner function closure? */
    public static final int IS_CLOSED_OVER = 1 << 23;
    /** Is this symbol used by an inner scope within the same function? */
    public static final int IS_USED_IN_INNER_SCOPE = 1 << 24;

    /** Is this the home object, used by super property accesses. */
    public static final int IS_SUPER = 1 << 25;
    /** Is this the {@code new.target}. */
    public static final int IS_NEW_TARGET = 1 << 26;

    /** Null or name identifying symbol. */
    private final String name;

    /** Symbol flags. */
    private int flags;

    /**
     * Constructor
     *
     * @param name name of symbol
     * @param flags symbol flags
     */
    public Symbol(final String name, final int flags) {
        this.name = name;
        this.flags = flags;
        assert (flags & KINDMASK) != 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(name);

        sb.append(' ');
        if (isLet()) {
            sb.append('L');
        } else if (isConst()) {
            sb.append('C');
        } else if (isVar()) {
            if (isGlobal()) {
                sb.append('G');
            } else if (isParam()) {
                sb.append('P');
            } else {
                sb.append('V');
            }
        }

        return sb.toString();
    }

    @Override
    public int compareTo(final Symbol other) {
        return name.compareTo(other.name);
    }

    /**
     * Check if this symbol is a hoistable var declaration.
     *
     * @return true if a hoistable var declaration
     * @see VarNode#isHoistableDeclaration()
     */
    public boolean isHoistableDeclaration() {
        return (flags & IS_HOISTABLE_DECLARATION) != 0;
    }

    /**
     * Check if this symbol is a variable
     *
     * @return true if variable
     */
    public boolean isVar() {
        return (flags & IS_VAR) != 0;
    }

    /**
     * Check if this symbol is a global (undeclared) variable
     *
     * @return true if global
     */
    public boolean isGlobal() {
        return (flags & IS_GLOBAL) != 0;
    }

    /**
     * Check if this symbol is a function parameter
     *
     * @return true if parameter
     */
    public boolean isParam() {
        return (flags & IS_PARAM) != 0;
    }

    /**
     * Check if this is a program (script) level definition
     *
     * @return true if program level
     */
    public boolean isProgramLevel() {
        return (flags & IS_PROGRAM_LEVEL) != 0;
    }

    /**
     * Check if this symbol is a constant
     *
     * @return true if a constant
     */
    public boolean isConst() {
        return (flags & IS_CONST) != 0;
    }

    /**
     * Check if this is an internal symbol, without an explicit JavaScript source code equivalent
     *
     * @return true if internal
     */
    public boolean isInternal() {
        return (flags & IS_INTERNAL) != 0;
    }

    /**
     * Check if this symbol represents {@code this}
     *
     * @return true if this
     */
    public boolean isThis() {
        return (flags & IS_THIS) != 0;
    }

    /**
     * Check if this symbol represents {@code super}
     *
     * @return true if super
     */
    public boolean isSuper() {
        return (flags & IS_SUPER) != 0;
    }

    /**
     * Check if this symbol represents {@code new.target}
     *
     * @return true if {@code new.target}
     */
    public boolean isNewTarget() {
        return (flags & IS_NEW_TARGET) != 0;
    }

    /**
     * Check if this symbol is a let
     *
     * @return true if let
     */
    public boolean isLet() {
        return (flags & IS_LET) != 0;
    }

    /**
     * Flag this symbol as a function's self-referencing symbol.
     *
     * @return true if this symbol as a function's self-referencing symbol.
     */
    public boolean isFunctionSelf() {
        return (flags & IS_FUNCTION_SELF) != 0;
    }

    /**
     * Is this a block scoped symbol
     *
     * @return true if block scoped
     */
    public boolean isBlockScoped() {
        return isLet() || isConst();
    }

    /**
     * Has this symbol been declared
     *
     * @return true if declared
     */
    public boolean hasBeenDeclared() {
        return (flags & HAS_BEEN_DECLARED) != 0;
    }

    /**
     * Mark this symbol as declared
     */
    public void setHasBeenDeclared() {
        assert !isDeclaredInSwitchBlock();
        if (!hasBeenDeclared()) {
            flags |= HAS_BEEN_DECLARED;
        }
    }

    /**
     * Mark this symbol as declared/undeclared
     */
    public void setHasBeenDeclared(boolean declared) {
        if (declared) {
            flags |= HAS_BEEN_DECLARED;
        } else {
            flags &= ~HAS_BEEN_DECLARED;
        }
    }

    /**
     * Get the symbol flags
     *
     * @return flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Get the name of this symbol
     *
     * @return symbol name
     */
    public String getName() {
        return name;
    }

    /**
     * Has this symbol been declared
     *
     * @return true if declared
     */
    public boolean isDeclaredInSwitchBlock() {
        return (flags & IS_DECLARED_IN_SWITCH_BLOCK) != 0;
    }

    /**
     * @return true if this symbol is an indirect import binding
     * @see #IS_IMPORT_BINDING
     */
    public boolean isImportBinding() {
        return (flags & IS_IMPORT_BINDING) != 0;
    }

    /**
     * @return true if this symbol is a catch parameter binding
     * @see #IS_CATCH_PARAMETER
     */
    public boolean isCatchParameter() {
        return (flags & IS_CATCH_PARAMETER) != 0;
    }

    public boolean isVarRedeclaredHere() {
        return (flags & IS_VAR_REDECLARED_HERE) != 0;
    }

    /**
     * Is this symbol a hoisted block function declaration.
     */
    public boolean isHoistedBlockFunctionDeclaration() {
        return (flags & IS_HOISTED_BLOCK_FUNCTION) != 0;
    }

    /**
     * Mark this symbol as a hoisted block function declaration.
     */
    public void setHoistedBlockFunctionDeclaration() {
        assert isBlockScoped();
        flags |= IS_HOISTED_BLOCK_FUNCTION;
    }

    /**
     * Is this symbol a block function declaration.
     */
    public boolean isBlockFunctionDeclaration() {
        return (flags & IS_BLOCK_FUNCTION_DECLARATION) != 0;
    }

    /**
     * Is this symbol a private name.
     */
    public boolean isPrivateName() {
        return (flags & IS_PRIVATE_NAME) != 0;
    }

    /**
     * Is this symbol a private name associated with a static member.
     */
    public boolean isPrivateNameStatic() {
        return (flags & IS_PRIVATE_NAME_STATIC) != 0;
    }

    /**
     * Is this symbol a private name associated with a field.
     */
    public boolean isPrivateField() {
        return isPrivateName() && !isPrivateMethod() && !isPrivateAccessor();
    }

    /**
     * Is this symbol a private name associated with a method.
     */
    public boolean isPrivateMethod() {
        return (flags & IS_PRIVATE_NAME_METHOD) != 0;
    }

    /**
     * Is this symbol a private name associated with an accessor.
     */
    public boolean isPrivateAccessor() {
        return (flags & IS_PRIVATE_NAME_ACCESSOR) != 0;
    }

    /**
     * Is this symbol the function 'arguments' binding.
     */
    public boolean isArguments() {
        return (flags & IS_ARGUMENTS) != 0;
    }

    /**
     * Is this symbol used.
     */
    public boolean isUsed() {
        return (flags & IS_USED) != 0;
    }

    /**
     * Mark this symbol as used.
     */
    public void setUsed() {
        flags |= IS_USED;
    }

    /**
     * Is this symbol captured by a closure.
     */
    public boolean isClosedOver() {
        return (flags & IS_CLOSED_OVER) != 0;
    }

    /**
     * Mark this symbol as captured by a closure.
     */
    public void setClosedOver() {
        flags |= IS_CLOSED_OVER;
    }

    /**
     * Is this symbol captured by an inner scope.
     */
    public boolean isUsedInInnerScope() {
        return (flags & IS_USED_IN_INNER_SCOPE) != 0;
    }

    /**
     * Mark this symbol as captured by an inner scope.
     */
    public void setUsedInInnerScope() {
        flags |= IS_USED_IN_INNER_SCOPE;
    }
}
