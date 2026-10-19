/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.Truncatable;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.binary.JSAddNode;
import com.oracle.truffle.js.nodes.binary.JSAddSubNumericUnitNode;
import com.oracle.truffle.js.nodes.binary.JSSubtractNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class TruncatableTest {

    @Test
    public void testTruncatePreservesSourceSectionAndTags() {
        Source source = Source.newBuilder(JavaScriptLanguage.ID, "a + b", "truncation.js").build();
        SourceSection section = source.createSection(0, 5);
        JavaScriptNode[] nodes = {
                        JSAddNode.create(argument(0), argument(1)),
                        JSSubtractNode.create(argument(0), argument(1)),
                        JSAddNode.create(JSConstantNode.createInt(3), argument(0)),
                        JSAddNode.create(argument(0), JSConstantNode.createInt(3)),
                        JSAddNode.create(JSConstantNode.createDouble(3.5), argument(0)),
                        JSAddNode.create(argument(0), JSConstantNode.createDouble(3.5)),
                        JSAddSubNumericUnitNode.create(argument(0), true, false),
                        JSAddSubNumericUnitNode.create(argument(0), false, false),
                        JSConstantNode.createInt(42)
        };
        for (JavaScriptNode node : nodes) {
            node.setSourceSection(section);
            node.addExpressionTag();
            node.addStatementTag();
            node.addRootBodyTag();
            node.addCallTag();
            JavaScriptNode truncated = Truncatable.truncate(node);
            assertEquals(node.getClass().getSimpleName(), section, truncated.getSourceSection());
            assertTrue(truncated.hasTag(StandardTags.ExpressionTag.class));
            assertTrue(truncated.hasTag(StandardTags.StatementTag.class));
            assertTrue(truncated.hasTag(StandardTags.RootBodyTag.class));
            assertTrue(truncated.hasTag(StandardTags.CallTag.class));
            assertSame(truncated, Truncatable.truncate(truncated));
        }
    }

    @Test
    public void testAddFactoryTruncateFlag() {
        int[][] operands = {{Integer.MAX_VALUE, 1}, {Integer.MIN_VALUE, -1}, {1, 2}};
        for (boolean truncate : new boolean[]{false, true}) {
            for (int[] pair : operands) {
                JavaScriptNode add = JSAddNode.create(argument(0), argument(1), truncate);
                VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(JSArguments.create(Undefined.instance, Undefined.instance, pair[0], pair[1]), FrameDescriptor.newBuilder().build());
                Object result = add.execute(frame);
                if (truncate) {
                    assertEquals(pair[0] + pair[1], result);
                } else {
                    assertEquals((long) pair[0] + pair[1], ((Number) result).longValue());
                }
            }
        }
    }

    private static JavaScriptNode argument(int index) {
        return AccessIndexedArgumentNode.create(index);
    }
}
