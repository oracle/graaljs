/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.parser.env;

import java.util.Map;
import java.util.StringJoiner;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JSFrameSlot;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;

public final class GlobalEnvironment extends DerivedEnvironment {

    private enum DeclarationKind {
        Var(false, false, true),
        Let(true, false, false),
        LetDeclared(true, false, true),
        Const(true, true, false),
        ConstDeclared(true, true, true);

        private final boolean isLexical;
        private final boolean isConst;
        private final boolean isDeclared;

        DeclarationKind(boolean isLexical, boolean isConst, boolean isDeclared) {
            this.isLexical = isLexical;
            this.isConst = isConst;
            this.isDeclared = isDeclared;
        }

        public final boolean isLexical() {
            return isLexical;
        }

        public final boolean isConst() {
            return isConst;
        }

        public final boolean isDeclared() {
            return isDeclared;
        }

        public final DeclarationKind withDeclared(boolean declared) {
            assert isLexical() || declared;
            if (!isLexical() || isDeclared() == declared) {
                return this;
            }
            if (declared) {
                return isConst() ? DeclarationKind.ConstDeclared : DeclarationKind.LetDeclared;
            } else {
                return isConst() ? DeclarationKind.Const : DeclarationKind.Let;
            }
        }
    }

    /**
     * Always-defined immutable constant value properties of the global object.
     */
    private static final UnmodifiableEconomicMap<TruffleString, DeclarationKind> PREDEFINED_IMMUTABLE_GLOBALS = initPredefinedImmutableGlobals();

    private final EconomicMap<TruffleString, DeclarationKind> declarations;

    public GlobalEnvironment(Environment parent, NodeFactory factory, JSContext context) {
        super(parent, factory, context);
        this.declarations = EconomicMap.create(PREDEFINED_IMMUTABLE_GLOBALS);
    }

    @Override
    public JSFrameSlot findBlockFrameSlot(Object name) {
        return null;
    }

    public void addLexicalDeclaration(TruffleString name, boolean isConst) {
        declarations.putIfAbsent(name, isConst ? DeclarationKind.Const : DeclarationKind.Let);
    }

    public boolean hasLexicalDeclaration(TruffleString name) {
        DeclarationKind decl = declarations.get(name);
        return decl != null && decl.isLexical();
    }

    public boolean hasConstDeclaration(TruffleString name) {
        DeclarationKind decl = declarations.get(name);
        return decl != null && decl.isConst();
    }

    public void addVarDeclaration(TruffleString name) {
        declarations.putIfAbsent(name, DeclarationKind.Var);
    }

    public boolean hasVarDeclaration(TruffleString name) {
        DeclarationKind decl = declarations.get(name);
        return decl != null && !decl.isLexical();
    }

    /**
     * Returns true for always-defined immutable value properties of the global object.
     */
    public static boolean isGlobalObjectConstant(TruffleString name) {
        return PREDEFINED_IMMUTABLE_GLOBALS.containsKey(name);
    }

    private static UnmodifiableEconomicMap<TruffleString, DeclarationKind> initPredefinedImmutableGlobals() {
        EconomicMap<TruffleString, DeclarationKind> map = EconomicMap.create();
        map.put(Strings.UNDEFINED, DeclarationKind.Var);
        map.put(Strings.NAN, DeclarationKind.Var);
        map.put(Strings.INFINITY, DeclarationKind.Var);
        return map;
    }

    public boolean hasBeenDeclared(TruffleString name) {
        DeclarationKind decl = declarations.get(name);
        if (decl != null) {
            return decl.isDeclared();
        } else {
            return false;
        }
    }

    public void setHasBeenDeclared(TruffleString name, boolean declared) {
        DeclarationKind decl = declarations.get(name);
        if (decl != null && decl.isLexical() && decl.isDeclared() != declared) {
            declarations.put(name, decl.withDeclared(declared));
        }
    }

    @Override
    protected String toStringImpl(Map<String, Integer> state) {
        return "Global" + new StringJoiner(", ", "{", "}").add(joinElements(declarations.getKeys())).toString();
    }
}
