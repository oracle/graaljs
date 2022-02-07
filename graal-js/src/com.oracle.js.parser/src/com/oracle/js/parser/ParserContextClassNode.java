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
package com.oracle.js.parser;

import java.util.Iterator;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.Scope;

/**
 * A ParserContextNode that represents a class that is currently being parsed.
 */
class ParserContextClassNode extends ParserContextBaseNode implements ParserContextScopableNode {

    private Scope scope;
    protected EconomicMap<String, IdentNode> unresolvedPrivateIdentifiers;

    /**
     * Constructs a ParserContextClassNode.
     *
     * @param scope The class's scope.
     */
    ParserContextClassNode(Scope scope) {
        this.scope = scope;
        assert scope.isClassHeadScope() || scope.isClassBodyScope();
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        assert scope.isClassHeadScope() || scope.isClassBodyScope();
        this.scope = scope;
    }

    /**
     * Register a private name usage for resolving.
     */
    void usePrivateName(IdentNode ident) {
        String name = ident.getName();
        if (scope.findPrivateName(name)) {
            // Private name has already been declared in this class.
            return;
        } else {
            // Register an unresolved private name.
            if (unresolvedPrivateIdentifiers == null) {
                unresolvedPrivateIdentifiers = EconomicMap.create();
            }
            if (!unresolvedPrivateIdentifiers.containsKey(name)) {
                unresolvedPrivateIdentifiers.put(name, ident);
            }
        }
    }

    IdentNode verifyAllPrivateIdentifiersValid(ParserContext lc) {
        if (unresolvedPrivateIdentifiers != null) {
            MapCursor<String, IdentNode> entries = unresolvedPrivateIdentifiers.getEntries();
            next: while (entries.advance()) {
                IdentNode unresolved = entries.getValue();
                String name = entries.getKey();
                if (scope.findPrivateName(name)) {
                    // found the private name in this or an outer class scope
                    continue next;
                } else {
                    // push unresolved private names to the outer class, if any
                    Iterator<ParserContextClassNode> it = lc.getClasses();
                    boolean seenThis = false;
                    while (it.hasNext()) {
                        ParserContextClassNode outer = it.next();
                        if (!seenThis) {
                            if (outer == this) {
                                seenThis = true;
                            }
                            continue;
                        }
                        outer.usePrivateName(unresolved);
                        continue next;
                    }
                    // this is already the outermost class, i.e. private name is not valid
                    return unresolved;
                }
            }
            unresolvedPrivateIdentifiers = null;
        }
        return null;
    }
}
