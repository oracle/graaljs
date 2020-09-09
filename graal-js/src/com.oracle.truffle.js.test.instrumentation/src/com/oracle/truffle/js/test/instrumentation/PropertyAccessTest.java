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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.DeclareTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag.Type;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public class PropertyAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        evalAllTags("var a = {x:42}; a.x;");

        // var a = {x:42}
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                assertAttribute(e2, LITERAL_TYPE, Type.ObjectLiteral.name());
                // num literal
                enter(LiteralTag.class).exit();
            }).input(42).exit();
        }).input((e) -> {
            assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
        }).exit();
        // a.x;
        enter(ReadPropertyTag.class, (e) -> {
            assertAttribute(e, KEY, "x");
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
        }).input((e) -> {
            assertTrue(JSDynamicObject.isJSDynamicObject(e.val));
        }).exit();
    }

    @Test
    public void nestedRead() {
        evalAllTags("var a = {x:{y:42}}; a.x.y;");

        // var a = {x:{y:42}}
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                enter(LiteralTag.class, (e3) -> {
                    assertAttribute(e3, LITERAL_TYPE, Type.ObjectLiteral.name());
                    // num literal
                    enter(LiteralTag.class).exit();
                }).input(42).exit();
            }).input().exit();
            write.input(assertJSObjectInput);
        }).exit();
        // a.x.y;
        enter(ReadPropertyTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "y");
            // a.x
            enter(ReadPropertyTag.class, (e1, read) -> {
                assertAttribute(e1, KEY, "x");
                enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
                read.input(assertJSObjectInput);
            }).exit();
            prop.input(assertJSObjectInput);
        }).exit();
    }

    @Test
    public void write() {
        evalAllTags("var a = {}; a.x = 42;");

        // var a = {}
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            // {}
            enter(LiteralTag.class).exit();
            write.input(assertJSObjectInput);
        }).exit();

        // a.x = 42
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "x");
            // global read
            enter(ReadPropertyTag.class, (e1, p) -> {
                assertAttribute(e1, KEY, "a");
                p.input(assertGlobalObjectInput);
            }).exit();
            write.input(assertJSObjectInput);
            enter(LiteralTag.class).exit();
            write.input(42);
        }).exit();
    }

    @Test
    public void read2() {
        String src = "var a = {log:function(){}}; a.log(42);";
        evalWithTag(src, ReadPropertyTag.class);

        // Invoke operations perform the two read operations independently.
        // 1. read the target object
        enter(ReadPropertyTag.class, (e1, pr1) -> {
            assertAttribute(e1, KEY, "a");
            pr1.input(assertGlobalObjectInput);
        }).exit();
        // 2. read the function to invoke
        enter(ReadPropertyTag.class, (e1, pr1) -> {
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
        evalWithTag(src, ReadPropertyTag.class);

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
        evalWithTag(src, ReadPropertyTag.class);

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

    @Test
    public void readPrototypeInCall() {
        String src = "var addProperty = function(color, func) {" +
                        "  String.prototype.__defineGetter__(color, func);" +
                        "};" +
                        "var colors = [1,2,3,4,5,6,7,8,9,10];" +
                        "addProperty(colors[0], function(){});" +
                        "colors.forEach(function(c) {" +
                        "    addProperty(c, function(){});" +
                        "  }" +
                        ");";
        evalWithTag(src, ReadPropertyTag.class);

        assertPropertyRead("addProperty");
        assertPropertyRead("colors");
        assertNestedPropertyRead("prototype", "String");
        assertPropertyRead("__defineGetter__");

        assertPropertyRead("colors");
        assertPropertyRead("forEach");

        for (int i = 0; i < 10; i++) {
            assertPropertyRead("addProperty");
            assertNestedPropertyRead("prototype", "String");
            assertPropertyRead("__defineGetter__");
        }
    }

    @Test
    public void globalPropertyRefError() {
        evalWithTags("var o = {foo: 1};" +
                        "with (o) {" +
                        "  foo = 42;" +
                        "}" +
                        "try {" +
                        "  foo;" +
                        // property read for "Error" not executed.
                        "  throw new Error();" +
                        "}" +
                        "catch (e) {" +
                        // exception must be reference error
                        "  e instanceof ReferenceError;" +
                        "}", new Class[]{ReadPropertyTag.class, BinaryOperationTag.class});

        enter(ReadPropertyTag.class, (e, p) -> {
            p.input(assertGlobalObjectInput);
            assertAttribute(e, KEY, "o");
        }).exit();

        enter(ReadPropertyTag.class, (e, p) -> {
            assertAttribute(e, KEY, "foo");
            p.input(assertGlobalObjectInput);
        }).exitExceptional();

        enter(BinaryOperationTag.class, (e, b) -> {

            b.input(assertJSObjectInput);
            enter(ReadPropertyTag.class, (e1, p) -> {
                p.input(assertGlobalObjectInput);
                assertAttribute(e1, KEY, "ReferenceError");
            }).exit();

            b.input(assertJSFunctionInput);

        }).exit(assertReturnValue(true));

    }

    @Test
    public void readNestedReadsInCalls() {
        String src = "var exports = {" +
                        "  bool: {" +
                        "    enforcing: {" +
                        "      trailingcomma: false" +
                        "    }," +
                        "    relaxing: {" +
                        "      elision: true," +
                        "    }" +
                        "  }," +
                        "  val: {" +
                        "    esversion: 5" +
                        "  }" +
                        "};" +
                        "Object.keys(exports.val)" +
                        "  .concat(Object.keys(exports.bool.relaxing))" +
                        "  .concat();";
        evalWithTags(src, new Class[]{FunctionCallTag.class, ReadPropertyTag.class});

        // .concat()
        enter(FunctionCallTag.class, (e0, call0) -> {
            // .concat(Object.keys((...))
            enter(FunctionCallTag.class, (e1, call1) -> {
                // Object.keys(exports.val)
                enter(FunctionCallTag.class, (e2, call2) -> {
                    assertPropertyRead("Object");
                    call2.input(assertJSFunctionInput);
                    assertPropertyRead("keys");
                    call2.input(assertJSFunctionInput);
                    assertNestedPropertyRead("val", "exports");
                    call2.input(assertTruffleObject);
                }).exit();
                call1.input(assertTruffleObject);
                assertPropertyRead("concat");
                call1.input(assertJSFunctionInput);
                // .concat(Object.keys(exports.bool.relaxing))
                enter(FunctionCallTag.class, (e2, call2) -> {
                    assertPropertyRead("Object");
                    call2.input(assertJSFunctionInput);
                    assertPropertyRead("keys");
                    call2.input(assertJSFunctionInput);
                    assertNestedPropertyRead("relaxing", "bool", "exports");
                    call2.input(assertTruffleObject);
                }).exit();
                call1.input(assertTruffleObject);
            }).exit();
            call0.input(assertTruffleObject);
            assertPropertyRead("concat");
            call0.input(assertJSFunctionInput);
        }).exit();
    }

    @Test
    public void nestedPropertyReadBuggy() {
        String src = "let foo = {bar: ()=>foo}; foo.bar(foo.bar).bar();";
        evalWithTag(src, ReadPropertyTag.class);
        assertPropertyRead("bar");
        assertPropertyRead("bar");
        assertPropertyRead("bar");
    }

    @Test
    public void nestedPropertyReadOK() {
        String src = "let foo = {bar: ()=>foo}; foo.bar(foo.bar).bar();";
        evalWithTags(src, new Class<?>[]{ReadPropertyTag.class, DeclareTag.class});
        assertPropertyRead("bar");
        assertPropertyRead("bar");
        assertPropertyRead("bar");
    }

    @Test
    public void nonMaterializedApply() {
        String src = "function foo(a) {\n" +
                        "    return a;\n" +
                        "}\n" +
                        "function bar() {\n" +
                        "    return foo.apply(undefined, arguments);\n" +
                        "}\n" +
                        "function baz(a) {\n" +
                        "    return a;\n" +
                        "}\n" +
                        "baz(bar(1));";
        // ReadPropertyTag instrumentation only; no input tags specified, thus should not trigger
        // materialize in CallApplyArgumentsNode
        evalWithTags(src, new Class[]{ReadPropertyTag.class}, new Class[]{});

        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, "baz");
        }).exit();
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, "bar");
        }).exit();
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, "foo");
        }).exit();
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, "apply");
        }).exit();
    }

    @Test
    public void materializedApply() {
        String src = "function foo(a) {\n" +
                        "    return a;\n" +
                        "}\n" +
                        "function bar() {\n" +
                        "    return foo.apply(undefined, arguments);\n" +
                        "}\n" +
                        "function baz(a) {\n" +
                        "    return a;\n" +
                        "}\n" +
                        "baz(bar(1));";
        // evalWithTag implicitly instruments expressions for inputs and thus should trigger
        // materialize in CallApplyArgumentsNode
        evalWithTag(src, ReadPropertyTag.class);

        assertPropertyRead("baz");
        assertPropertyRead("bar");
        assertPropertyRead("foo");
        assertPropertyRead("apply");
    }

    private void assertPropertyRead(String key) {
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, key);
            pr.input(assertTruffleObject);
        }).exit();
    }

    private void assertNestedPropertyRead(String key1, String key2) {
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, key1);
            enter(ReadPropertyTag.class, (e1, pr1) -> {
                assertAttribute(e1, KEY, key2);
                pr1.input(assertTruffleObject);
            }).exit();
            pr.input(assertTruffleObject);
        }).exit();
    }

    private void assertNestedPropertyRead(String key1, String key2, String key3) {
        enter(ReadPropertyTag.class, (e, pr) -> {
            assertAttribute(e, KEY, key1);
            enter(ReadPropertyTag.class, (e1, pr1) -> {
                assertAttribute(e1, KEY, key2);
                enter(ReadPropertyTag.class, (e2, pr2) -> {
                    assertAttribute(e2, KEY, key3);
                    pr2.input(assertTruffleObject);
                }).exit();
                pr1.input(assertTruffleObject);
            }).exit();
            pr.input(assertTruffleObject);
        }).exit();
    }
}
