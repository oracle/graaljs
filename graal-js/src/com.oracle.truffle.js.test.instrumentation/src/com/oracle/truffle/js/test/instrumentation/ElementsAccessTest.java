/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteElementTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ElementsAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        evalAllTags("var a = [1]; a[0];");

        assertGlobalArrayLiteralDeclaration("a");

        enter(ReadElementTag.class, (e, elem) -> {
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralTag.class).exit();
            elem.input(0);
        }).exit();
    }

    @Test
    public void nestedRead() {
        evalAllTags("var a = [0]; a[a[0]];");

        assertGlobalArrayLiteralDeclaration("a");

        enter(ReadElementTag.class, (e, elem) -> {
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            // nested read a[0]
            enter(ReadElementTag.class, (e1, elem1) -> {
                enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
                elem1.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
                enter(LiteralTag.class).exit();
                elem1.input(0);
            }).exit();
            // outer read
            elem.input(0);
        }).exit();
    }

    @Test
    public void write() {
        evalAllTags("var a = []; a[1] = 'foo';");

        assertGlobalArrayLiteralDeclaration("a");
        // write element
        enter(WriteElementTag.class, (e, elem) -> {
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralTag.class).exit();
            elem.input(1);
            enter(LiteralTag.class).exit();
            elem.input("foo");
        }).exit();
    }

    @Test
    public void elementWriteIncDec() {
        evalWithTag("var u=[2,4,6]; var p = 1; u[p] -= 42", WriteElementTag.class);

        enter(WriteElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(1);
            b.input(-38);
        }).exit();
    }

    @Test
    public void elementReadInvoke() {
        evalWithTag("var u={x:[function(){}]}; u.x[0]()", ReadElementTag.class);

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(0);
        }).exit((res) -> {
            Object[] val = (Object[]) res.val;
            // # of input events + result
            assertEquals(3, val.length);
            // result
            assertTrue(JSFunction.isJSFunction(val[0]));
            // target
            assertTrue(JSArray.isJSArray(val[1]));
            // idx
            assertEquals(0, val[2]);
        });
    }

    @Test
    public void targetTest() {
        evalWithTag("var Box2d = {};Box2d.postDefs = [function(){}];function test(){var i = 0;Box2d.postDefs[i]();}; test();", ReadElementTag.class);

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(0);
        }).exit();
    }

    @Test
    public void exprBlockTestIncPost() {
        assertNestedIncDecElementRead("self.cursorState.cursorIndex++", 1, 1);
    }

    @Test
    public void exprBlockTestIncPre() {
        assertNestedIncDecElementRead("++self.cursorState.cursorIndex", 0, 1);
    }

    @Test
    public void exprBlockTestDecPost() {
        assertNestedIncDecElementRead("self.cursorState.cursorIndex--", 1, 1);
    }

    @Test
    public void exprBlockTestDecPre() {
        assertNestedIncDecElementRead("--self.cursorState.cursorIndex", 1, 0);
    }

    private void assertNestedIncDecElementRead(String op, int initial, int returned) {
        evalWithTag("(function() {" +
                        "  var self = {" +
                        "    cursorState : {" +
                        "      documents : ['foo', 'bar']," +
                        "      cursorIndex : " + initial +
                        "    }" +
                        "  };" +
                        "  var doc = self.cursorState.documents[" + op + "];" +
                        "})()", ReadElementTag.class);

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(returned);
        }).exit();
    }

    @Test
    public void nestedInvokeReads() {
        evalWithTag("function setKey(obj, keys) {" +
                        " obj.a;" +
                        " keys.slice[0][1][2](0, -1).forEach(function(key) {});" +
                        "};" +
                        "const callable = {" +
                        " slice : [['',['','',function fakeslice() { return [1,2]; }]]]" +
                        "};" +
                        "setKey({}, callable);" +
                        "for (var i = 0; i < 2; i++) {" +
                        " setKey({" +
                        " a: 1" +
                        " }, callable);" +
                        "}", ReadElementTag.class);

        for (int i = 0; i < 3; i++) {
            // First two reads are to retrieve the invoke "target"
            enter(ReadElementTag.class, (e, elem) -> {
                enter(ReadElementTag.class, (e1, elem1) -> {
                    elem1.input(assertJSArrayInput);
                    elem1.input(0);
                }).exit(assertJSObjectReturn);

                elem.input(assertJSArrayInput);
                elem.input(1);
            }).exit(assertJSObjectReturn);
            // Third read to retrieve the invoked function
            enter(ReadElementTag.class, (e, elem) -> {
                elem.input(assertJSArrayInput);
                elem.input(2);
            }).exit(assertJSFunctionReturn);
        }
    }

    @Test
    public void elementWriteIndexConvert() {
        evalWithTag("var a = []; a[true] = 0; a[undefined] = 0; a[{}] = 0;", WriteElementTag.class);

        enter(WriteElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(true);
            b.input(0);
        }).exit();

        enter(WriteElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(Undefined.instance);
            b.input(0);
        }).exit();

        enter(WriteElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(assertJSObjectInput);
            b.input(0);
        }).exit();

    }

    @Test
    public void elementReadIndexConvert() {
        evalWithTag("var a = []; a[true]; a[undefined]; a[{}];", ReadElementTag.class);

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(true);
        }).exit();

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(Undefined.instance);
        }).exit();

        enter(ReadElementTag.class, (e, b) -> {
            b.input(assertJSArrayInput);
            b.input(assertJSObjectInput);
        }).exit();

    }
}
