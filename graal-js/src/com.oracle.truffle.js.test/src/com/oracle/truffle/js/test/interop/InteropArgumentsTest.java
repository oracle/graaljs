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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.interop.ImportArgumentsNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.test.TestHelper;

public class InteropArgumentsTest extends ParameterizedInteropLibraryTest {

    private static final int[] ARITIES = {0, 1, 3, 8, 9, 15, 16, 17, 32};

    @Test
    public void testUnchangedArgumentsKeepArrayIdentity() {
        for (int arity : ARITIES) {
            ImportArgumentsNode node = adoptNode(run.isCached() ? ImportArgumentsNode.create() : ImportArgumentsNode.getUncached());
            Object[] arguments = new Object[arity];
            for (int i = 0; i < arity; i++) {
                arguments[i] = i;
            }
            assertSame(arguments, node.execute(arguments));
            if (run.isCached()) {
                assertEquals(arity <= 16 ? arity : 1, NodeUtil.findAllNodeInstances(node, ImportValueNode.class).size());
            }
        }
    }

    @Test
    public void testCopyOnFirstChangedArgument() {
        for (int arity : ARITIES) {
            for (int changedIndex = 0; changedIndex < arity; changedIndex++) {
                ImportArgumentsNode node = adoptNode(run.isCached() ? ImportArgumentsNode.create() : ImportArgumentsNode.getUncached());
                Object[] arguments = new Object[arity];
                for (int i = 0; i < arity; i++) {
                    arguments[i] = i == changedIndex ? "converted" : i;
                }
                Object[] original = arguments.clone();
                Object[] imported = node.execute(arguments);
                assertNotSame(arguments, imported);
                assertArrayEquals(original, arguments);
                for (int i = 0; i < arity; i++) {
                    if (i == changedIndex) {
                        assertEquals("converted", Strings.toJavaString((TruffleString) imported[i]));
                    } else {
                        assertSame(arguments[i], imported[i]);
                    }
                }
            }
        }
    }

    @Test
    public void testMultipleChangedArguments() {
        Object[] values = {true, "first", (byte) 7, (short) 8, 1.5f, 'x', "last", false, 42};
        Object[] expected = {true, Strings.fromJavaString("first"), 7, 8, 1.5d, Strings.fromJavaString("x"), Strings.fromJavaString("last"), false, 42};
        for (int arity : new int[]{3, 8, 9}) {
            ImportArgumentsNode node = adoptNode(run.isCached() ? ImportArgumentsNode.create() : ImportArgumentsNode.getUncached());
            Object[] arguments = Arrays.copyOf(values, arity);
            Object[] original = arguments.clone();
            Object[] imported = node.execute(arguments);
            assertNotSame(arguments, imported);
            assertArrayEquals(original, arguments);
            assertArrayEquals(Arrays.copyOf(expected, arity), imported);
        }
    }

    @Test
    public void testIndependentArgumentProfiles() {
        assumeTrue(run.isCached());
        ImportArgumentsNode node = adoptNode(ImportArgumentsNode.create());
        node.execute(new Object[]{42, "text", true});
        List<ImportValueNode> importNodes = NodeUtil.findAllNodeInstances(node, ImportValueNode.class);
        assertEquals(3, importNodes.size());
        for (int i = 0; i < importNodes.size(); i++) {
            for (int j = i + 1; j < importNodes.size(); j++) {
                assertNotSame(importNodes.get(i), importNodes.get(j));
            }
            assertEquals(i == 1 ? 1 : 0, NodeUtil.findAllNodeInstances(importNodes.get(i), TruffleString.FromJavaStringNode.class).size());
        }
        node.execute(new Object[]{"changed", "text", true});
        List<ImportValueNode> updatedImportNodes = NodeUtil.findAllNodeInstances(node, ImportValueNode.class);
        for (int i = 0; i < importNodes.size(); i++) {
            assertSame(importNodes.get(i), updatedImportNodes.get(i));
            assertEquals(i < 2 ? 1 : 0, NodeUtil.findAllNodeInstances(importNodes.get(i), TruffleString.FromJavaStringNode.class).size());
        }
    }

    @Test
    public void testArityChangesUseGenericImport() {
        ImportArgumentsNode node = adoptNode(run.isCached() ? ImportArgumentsNode.create() : ImportArgumentsNode.getUncached());
        Object[] empty = new Object[0];
        assertSame(empty, node.execute(empty));
        node.execute(new Object[]{42, "text", true});
        if (run.isCached()) {
            assertEquals(3, NodeUtil.findAllNodeInstances(node, ImportValueNode.class).size());
        }
        for (int arity : new int[]{1, 8, 9, 16, 17, 0, 3}) {
            Object[] arguments = new Object[arity];
            Arrays.fill(arguments, "converted");
            Object[] imported = node.execute(arguments);
            assertEquals(arity, imported.length);
            for (int i = 0; i < arity; i++) {
                assertEquals("converted", Strings.toJavaString((TruffleString) imported[i]));
                assertEquals("converted", arguments[i]);
            }
            if (run.isCached()) {
                assertEquals(1, NodeUtil.findAllNodeInstances(node, ImportValueNode.class).size());
            }
        }
    }

    @Test
    public void testArrayReuseAfterConversion() {
        ImportArgumentsNode node = adoptNode(run.isCached() ? ImportArgumentsNode.create() : ImportArgumentsNode.getUncached());
        Object[] converted = {1, "converted", true};
        assertNotSame(converted, node.execute(converted));
        Object[] unchanged = {1, 2, true};
        assertSame(unchanged, node.execute(unchanged));
        assertSame(unchanged, node.execute(unchanged));
    }

    @Test
    public void testExecuteInvokeAndConstruct() throws Exception {
        try (TestHelper helper = new TestHelper()) {
            helper.enterContext();
            try {
                Object function = helper.runNoPolyglot("(function() { this.args = Array.from(arguments); })");
                Object receiver = helper.runNoPolyglot("({method: function() { return Array.from(arguments); }})");
                Object executeFunction = helper.runNoPolyglot("(function() { return Array.from(arguments); })");
                InteropLibrary uncached = InteropLibrary.getUncached();
                for (int arity : ARITIES) {
                    InteropLibrary functionInterop = createInteropLibrary(function);
                    InteropLibrary executeInterop = createInteropLibrary(executeFunction);
                    InteropLibrary receiverInterop = createInteropLibrary(receiver);
                    Object[] arguments = new Object[arity];
                    for (int i = 0; i < arity; i++) {
                        arguments[i] = i % 2 == 0 ? i : "arg" + i;
                    }
                    Object[] original = arguments.clone();
                    Object executed = executeInterop.execute(executeFunction, arguments);
                    Object invoked = receiverInterop.invokeMember(receiver, "method", arguments);
                    Object constructed = functionInterop.instantiate(function, arguments);
                    Object constructedArguments = uncached.readMember(constructed, "args");
                    for (Object result : new Object[]{executed, invoked, constructedArguments}) {
                        assertEquals(arity, uncached.getArraySize(result));
                        for (int i = 0; i < arity; i++) {
                            Object value = uncached.readArrayElement(result, i);
                            if (i % 2 == 0) {
                                assertEquals(i, uncached.asInt(value));
                            } else {
                                assertEquals("arg" + i, uncached.asString(value));
                            }
                        }
                    }
                    assertArrayEquals(original, arguments);
                }
            } finally {
                helper.leaveContext();
            }
        }
    }
}
