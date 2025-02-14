/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class GlobalDeclarationInstantiationNode extends StatementNode {
    private final JSContext context;
    @Children private final DeclareGlobalNode[] globalDeclarations;

    public static final DeclareGlobalNode[] EMPTY_DECLARATION_ARRAY = new DeclareGlobalNode[0];

    protected GlobalDeclarationInstantiationNode(JSContext context, DeclareGlobalNode[] globalDeclarations) {
        this.context = context;
        this.globalDeclarations = globalDeclarations;
    }

    public static JavaScriptNode create(JSContext context, DeclareGlobalNode[] globalDeclarations) {
        return new GlobalDeclarationInstantiationNode(context, globalDeclarations);
    }

    public static JavaScriptNode create(JSContext context, List<DeclareGlobalNode> globalDeclarations) {
        return create(context, globalDeclarations.toArray(EMPTY_DECLARATION_ARRAY));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        JSRealm realm = getRealm();
        verifyDeclarations(realm);
        instantiateDeclarations(realm);
        return EMPTY;
    }

    /**
     * Verify that all declarations can be instantiated.
     */
    @ExplodeLoop
    private void verifyDeclarations(JSRealm realm) {
        for (DeclareGlobalNode declaration : globalDeclarations) {
            declaration.verify(context, realm);
        }
    }

    @ExplodeLoop
    private void instantiateDeclarations(JSRealm realm) {
        for (DeclareGlobalNode declaration : globalDeclarations) {
            declaration.executeVoid(context, realm);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new GlobalDeclarationInstantiationNode(context, cloneUninitialized(globalDeclarations, materializedTags));
    }

    private static DeclareGlobalNode[] cloneUninitialized(DeclareGlobalNode[] members, Set<Class<? extends Tag>> materializedTags) {
        DeclareGlobalNode[] copy = members.clone();
        for (int i = 0; i < copy.length; i++) {
            copy[i] = copy[i].copyUninitialized(materializedTags);
        }
        return copy;
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}
