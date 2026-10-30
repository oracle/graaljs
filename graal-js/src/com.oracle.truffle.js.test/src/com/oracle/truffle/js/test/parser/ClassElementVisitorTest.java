/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.ClassElement;
import com.oracle.js.parser.ir.ClassNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.js.parser.ir.IdentNode;
import com.oracle.js.parser.ir.LexicalContext;
import com.oracle.js.parser.ir.Node;
import com.oracle.js.parser.ir.visitor.NodeVisitor;

public class ClassElementVisitorTest {

    private static ClassElement parseElement() {
        ScriptEnvironment env = ScriptEnvironment.builder().ecmaScriptVersion(ScriptEnvironment.ES_STAGING).build();
        FunctionNode tree = new Parser(env, Source.sourceFor("test", "class C { @first @second method() {} }"), new ErrorManager.ThrowErrorManager()).parse();
        ClassNode[] result = new ClassNode[1];
        tree.accept(new NodeVisitor<>(new LexicalContext()) {
            @Override
            public boolean enterClassNode(ClassNode node) {
                result[0] = node;
                return false;
            }
        });
        Assert.assertNotNull(result[0]);
        return result[0].getClassElements().get(0);
    }

    /** An identity visitor must preserve class-element decorators and the unchanged element. */
    @Test
    public void testIdentityVisitorPreservesDecorators() {
        ClassElement original = parseElement();
        Assert.assertEquals(2, original.getDecorators().size());
        Node visited = original.accept(new NodeVisitor<>(new LexicalContext()) {
        });
        Assert.assertSame(original, visited);
    }

    /** Decorator expressions must be visited before the element's leave hook applies its rewrite. */
    @Test
    public void testDecoratorAndLeaveRewrites() {
        ClassElement original = parseElement();
        Expression first = original.getDecorators().get(0);
        Expression replacement = original.getDecorators().get(1);
        int[] leaveCount = {0};
        ClassElement visited = (ClassElement) original.accept(new NodeVisitor<>(new LexicalContext()) {
            @Override
            public Node leaveIdentNode(IdentNode node) {
                return node == first ? replacement : node;
            }

            @Override
            public Node leaveClassElement(ClassElement element) {
                leaveCount[0]++;
                Assert.assertEquals(2, element.getDecorators().size());
                Assert.assertSame(replacement, element.getDecorators().get(0));
                Assert.assertSame(replacement, element.getDecorators().get(1));
                return element.setDecorators(List.of(replacement));
            }
        });
        Assert.assertEquals(1, leaveCount[0]);
        Assert.assertEquals(List.of(replacement), visited.getDecorators());
        Assert.assertEquals(List.of(first, replacement), original.getDecorators());
    }

    /** Rejecting entry must skip both child traversal and the leave callback. */
    @Test
    public void testSkippedElement() {
        ClassElement original = parseElement();
        Node visited = original.accept(new NodeVisitor<>(new LexicalContext()) {
            @Override
            public boolean enterClassElement(ClassElement element) {
                return false;
            }

            @Override
            public boolean enterIdentNode(IdentNode node) {
                Assert.fail("Skipped class element must not visit children");
                return false;
            }

            @Override
            public Node leaveClassElement(ClassElement element) {
                Assert.fail("Skipped class element must not invoke leaveClassElement");
                return element;
            }
        });
        Assert.assertSame(original, visited);
    }
}
