/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag.Type;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class PropertyAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        evalAllTags("var a = {x:42}; a.x;");

        // var a = {x:42}
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e2) -> {
                assertAttribute(e2, TYPE, Type.ObjectLiteral.name());
                // num literal
                enter(LiteralExpressionTag.class).exit();
            }).input(42).exit();
        }).input((e) -> {
            assertTrue(JSObject.isJSObject(e.val));
        }).exit();
        // a.x;
        enter(ReadPropertyExpressionTag.class, (e) -> {
            assertAttribute(e, KEY, "x");
            enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
        }).input((e) -> {
            assertTrue(JSObject.isJSObject(e.val));
        }).exit();
    }

    @Test
    public void nestedRead() {
        evalAllTags("var a = {x:{y:42}}; a.x.y;");

        // var a = {x:{y:42}}
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralExpressionTag.class, (e2) -> {
                enter(LiteralExpressionTag.class, (e3) -> {
                    assertAttribute(e3, TYPE, Type.ObjectLiteral.name());
                    // num literal
                    enter(LiteralExpressionTag.class).exit();
                }).input(42).exit();
            }).input().exit();
            write.input(assertJSObjectInput);
        }).exit();
        // a.x.y;
        enter(ReadPropertyExpressionTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "y");
            // a.x
            enter(ReadPropertyExpressionTag.class, (e1, read) -> {
                assertAttribute(e1, KEY, "x");
                enter(ReadPropertyExpressionTag.class).input(assertGlobalObjectInput).exit();
                read.input(assertJSObjectInput);
            }).exit();
            prop.input(assertJSObjectInput);
        }).exit();
    }

    @Test
    public void write() {
        evalAllTags("var a = {}; a.x = 42;");

        // var a = {}
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            // {}
            enter(LiteralExpressionTag.class).exit();
            write.input(assertJSObjectInput);
        }).exit();

        // a.x = 42
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "x");
            // global read
            enter(ReadPropertyExpressionTag.class, (e1, p) -> {
                assertAttribute(e1, KEY, "a");
                p.input(assertGlobalObjectInput);
            }).exit();
            write.input(assertJSObjectInput);
            enter(LiteralExpressionTag.class).exit();
            write.input(42);
        }).exit();
    }

    @Test
    public void read2() {
        String src = "var a = {log:function(){}}; a.log(42);";
        evalWithTag(src, ReadPropertyExpressionTag.class);

        // Invoke operations perform the two read operations independently.
        // 1. read the target object
        enter(ReadPropertyExpressionTag.class, (e1, pr1) -> {
            assertAttribute(e1, KEY, "a");
            pr1.input(assertGlobalObjectInput);
        }).exit();
        // 2. read the function to invoke
        enter(ReadPropertyExpressionTag.class, (e1, pr1) -> {
            assertAttribute(e1, KEY, "log");
            pr1.input(assertJSObjectInput);
        }).exit(assertJSFunctionReturn);
    }

    @Test
    public void readMulti() {
        String src = "function Bar() {};" +
                        "var bar = new Bar();" +
                        "bar.a = {x:function(){}};" +
                        "for(var i = 0; i < 10; i++) {" +
                        "    bar.a.x(bar);" +
                        "}";
        evalWithTag(src, ReadPropertyExpressionTag.class);

        assertPropertyRead("Bar");
        assertPropertyRead("bar");
        assertPropertyRead("i");
        assertNestedPropertyRead("a", "bar");
        assertPropertyRead("x");

        for (int i = 0; i < 9; i++) {
            assertPropertyRead("bar");
            assertPropertyRead("i");
            assertPropertyRead("i");
            assertNestedPropertyRead("a", "bar");
            assertPropertyRead("x");
        }
        assertPropertyRead("bar");
        assertPropertyRead("i");
        assertPropertyRead("i");
    }

    @Test
    public void readMissingSourceSection() {
        String src = "function bar(){};" +
                        "function foo(){" +
                        "  this.v = new bar();" +
                        "};" +
                        "foo.prototype.x = function(){" +
                        "  this.y()[0]=1;" +
                        "};" +
                        "foo.prototype.y = function(){" +
                        "  return this.v;" +
                        "};" +
                        "var cnt = 0;" +
                        "var a = new foo();" +
                        "while(cnt < 10) {" +
                        "  a.x();" +
                        "  cnt++;" +
                        "}";
        evalWithTag(src, ReadPropertyExpressionTag.class);

        assertNestedPropertyRead("prototype", "foo");
        assertNestedPropertyRead("prototype", "foo");
        assertPropertyRead("foo");
        assertPropertyRead("bar");

        for (int cnt = 0; cnt < 10; cnt++) {
            assertPropertyRead("cnt");
            assertPropertyRead("a");
            assertPropertyRead("x");
            assertPropertyRead("y");
            assertPropertyRead("v");
            assertPropertyRead("cnt");
        }
        assertPropertyRead("cnt");
    }

    private void assertPropertyRead(String key) {
        enter(ReadPropertyExpressionTag.class, (e, pr) -> {
            assertAttribute(e, KEY, key);
            pr.input(assertTruffleObject);
        }).exit();
    }

    private void assertNestedPropertyRead(String key1, String key2) {
        enter(ReadPropertyExpressionTag.class, (e, pr) -> {
            assertAttribute(e, KEY, key1);
            enter(ReadPropertyExpressionTag.class, (e1, pr1) -> {
                assertAttribute(e1, KEY, key2);
                pr1.input(assertTruffleObject);
            }).exit();
            pr.input(assertTruffleObject);
        }).exit();
    }
}
