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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.nodes.control.ContinueTargetNode;
import com.oracle.truffle.js.nodes.control.DirectContinueTargetNode;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;
import com.oracle.truffle.js.test.TestHelper.ParsedFunction;

public class ContinueTargetTest extends JSTest {

    @Test
    public void testDirectContinueTarget() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            helper.enterContext();
            try {
                ParsedFunction function = helper.parseFirstFunction("""
                                function test() {
                                    let i = 0;
                                    while (i < 10) {
                                        i++;
                                        continue;
                                    }
                                    return i;
                                }
                                """);
                RootNode root = function.getRootNode();
                assertNotNull(NodeUtil.findFirstNodeInstance(root, DirectContinueTargetNode.class));
                assertNull(NodeUtil.findFirstNodeInstance(root, ContinueTargetNode.class));
                assertEquals(10, function.call(new Object[0]));
            } finally {
                helper.leaveContext();
            }
        }
    }

    @Test
    public void testLabeledDirectContinueTarget() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            helper.enterContext();
            try {
                ParsedFunction function = helper.parseFirstFunction("""
                                function test() {
                                    let i = 0;
                                    loop: while (i < 10) {
                                        i++;
                                        continue loop;
                                    }
                                    return i;
                                }
                                """);
                RootNode root = function.getRootNode();
                assertNotNull(NodeUtil.findFirstNodeInstance(root, DirectContinueTargetNode.class));
                assertNull(NodeUtil.findFirstNodeInstance(root, ContinueTargetNode.class));
                assertEquals(10, function.call(new Object[0]));
            } finally {
                helper.leaveContext();
            }
        }
    }

    @Test
    public void testDirectAndTargetedContinue() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            helper.enterContext();
            try {
                ParsedFunction function = helper.parseFirstFunction("""
                                function test() {
                                    let i = 0, afterInner = 0;
                                    outer: while (i < 10) {
                                        i++;
                                        let j = 0;
                                        while (j < 2) {
                                            j++;
                                            if (j === 1) continue;
                                            continue outer;
                                        }
                                        afterInner++;
                                    }
                                    return i * 100 + afterInner;
                                }
                                """);
                RootNode root = function.getRootNode();
                assertNotNull(NodeUtil.findFirstNodeInstance(root, DirectContinueTargetNode.class));
                assertNotNull(NodeUtil.findFirstNodeInstance(root, ContinueTargetNode.class));
                assertEquals(1000, function.call(new Object[0]));
            } finally {
                helper.leaveContext();
            }
        }
    }

    @Test
    public void testContinueThroughSwitchAndFinally() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            assertEquals("3,3,0", helper.runValue("""
                            let i = 0, finalized = 0, afterSwitch = 0;
                            loop: while (i < 3) {
                                i++;
                                try {
                                    switch (i) {
                                        case 1: continue;
                                        default: continue loop;
                                    }
                                    afterSwitch++;
                                } finally {
                                    finalized++;
                                }
                            }
                            [i, finalized, afterSwitch].join(',');
                            """).asString());
        }
    }

    @Test
    public void testDirectContinueDoesNotCloseIterator() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            assertEquals("123,1,3", helper.runValue("""
                            let visited = '', closed = 0, finalized = 0;
                            function* values() {
                                try {
                                    yield 1;
                                    yield 2;
                                    yield 3;
                                } finally {
                                    closed++;
                                }
                            }
                            loop: for (const value of values()) {
                                try {
                                    visited += value;
                                    if (value === 1) continue;
                                    continue loop;
                                } finally {
                                    finalized++;
                                }
                            }
                            [visited, closed, finalized].join(',');
                            """).asString());
        }
    }

    @Test
    public void testTargetedContinueClosesInnerIterator() {
        try (TestHelper helper = new TestHelper(newContextBuilder())) {
            assertEquals("2,1,3,4", helper.runValue("""
                            let innerClosed = 0, outerClosed = 0;
                            let outerFinalized = 0, innerFinalized = 0;
                            function* values(inner) {
                                try {
                                    yield 0;
                                    yield 1;
                                    yield 2;
                                } finally {
                                    if (inner) innerClosed++;
                                    else outerClosed++;
                                }
                            }
                            outer: for (const value of values(false)) {
                                try {
                                    if (value === 1) continue;
                                    for (const inner of values(true)) {
                                        try {
                                            if (inner === 0) continue;
                                            continue outer;
                                        } finally {
                                            innerFinalized++;
                                        }
                                    }
                                } finally {
                                    outerFinalized++;
                                }
                            }
                            [innerClosed, outerClosed, outerFinalized, innerFinalized].join(',');
                            """).asString());
        }
    }
}
