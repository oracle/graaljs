/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper.ParsedFunction;

public class NodeLibraryTest extends JSTest {
    @Test
    public void testBlockScopeEnter() throws Exception {
        String src = "function testBlock(p) {\n" +
                        "    for (let i = 8; i <= 10; i++) {\n" +
                        "        let f = fac(i);\n" +
                        "        console.log(`fac(${i}) = ${f}`);\n" +
                        "    }\n" +
                        "}\n" +
                        "function fac(n) {\n" +
                        "  if (n <= 1) {\n" +
                        "    return 1;\n" +
                        "  } else {\n" +
                        "    return n * fac(n - 1);\n" +
                        "  }\n" +
                        "}" +
                        "\n" +
                        "testBlock(42);\n";

        testHelper.enterContext();
        try {
            ParsedFunction parsedFunction = testHelper.parseFirstFunction(src);
            RootNode rootNode = parsedFunction.getRootNode();
            BlockScopeNode blockScopeNode = NodeUtil.findFirstNodeInstance(rootNode, BlockScopeNode.class);
            Object[] args = JSArguments.createZeroArg(Undefined.instance, parsedFunction.createFunctionObject());
            FrameDescriptor frameDesc = rootNode.getFrameDescriptor();
            MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(args, frameDesc);
            int pSlot = IntStream.range(0, frameDesc.getNumberOfSlots()).filter(i -> String.valueOf(frameDesc.getSlotName(i)).equals("p")).findFirst().orElseThrow(
                            () -> new AssertionError("frame slot 'p' not found"));
            frame.setInt(pSlot, 42);
            Object scope = NodeLibrary.getUncached().getScope(blockScopeNode, frame, true);
            InteropLibrary interop = InteropLibrary.getUncached();
            Assert.assertTrue(interop.isMemberReadable(scope, "p"));
            Object pValue = interop.readMember(scope, "p");
            Assert.assertTrue(String.valueOf(pValue), interop.fitsInInt(pValue));
            Assert.assertEquals(42, interop.asInt(pValue));
        } finally {
            testHelper.leaveContext();
        }
    }
}
