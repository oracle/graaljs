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

import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class CallAccessTest extends FineGrainedAccessTest {

    @Test
    public void callOneArg() {
        evalAllTags("function foo(a) {}; foo(42);");

        // declaration
        assertGlobalFunctionExpressionDeclaration("foo");

        // foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // target (which is undefined in this case) and function
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // read 'foo' from the global object
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
            call.input(assertJSFunctionInput);
            // one argument
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);

            enterDeclareTag("a");

            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(42);
            }).exit();
        }).exit();
    }

    @Test
    public void callTwoArgs() {
        evalAllTags("function foo(a,b) {}; foo(42,24);");

        // declaration
        assertGlobalFunctionExpressionDeclaration("foo");

        // foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // tead the target for 'foo', which is undefined
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(ReadPropertyTag.class).input(assertGlobalObjectInput).exit();
            // target (which is undefined in this case) and function
            call.input(assertJSFunctionInput);
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
            enter(LiteralTag.class).exit(assertReturnValue(24));
            call.input(24);

            // locals declarations
            enterDeclareTag("a");
            enterDeclareTag("b");

            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(42);
            }).exit();
            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(24);
            }).exit();
        }).exit();
    }

    @Test
    public void methodCall() {
        evalAllTags("var foo = {x:function foo(a,b) {}}; foo.x(42,24);");

        // var foo = ...
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "foo");
            write.input(assertJSObjectInput);

            enter(LiteralTag.class, (e1, literal) -> {
                assertAttribute(e1, LITERAL_TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                }).exit();
                literal.input(assertJSFunctionInput);
            }).exit();

            write.input(assertJSObjectInput);
        }).exit();

        // x.foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // read 'foo' from global
            enter(ReadPropertyTag.class, (e1, prop) -> {
                assertAttribute(e1, KEY, "foo");
                prop.input(assertGlobalObjectInput);
            }).exit();
            // 1st argument to function is target
            call.input(assertJSObjectInput);
            // 2nd argument is the function itself
            enter(ReadPropertyTag.class, assertPropertyReadName("x")).input(assertJSObjectInput).exit();
            call.input(assertJSFunctionInput);
            // arguments

            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
            enter(LiteralTag.class).exit(assertReturnValue(24));
            call.input(24);

            // locals declarations
            enterDeclareTag("a");
            enterDeclareTag("b");

            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(42);
            }).exit();
            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(24);
            }).exit();
        }).exit();
    }

    @Test
    public void methodCallOneArg() {
        evalAllTags("var foo = {x:function foo(a,b) {}}; foo.x(42);");

        // var foo = ...
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "foo");
            write.input(assertJSObjectInput);

            enter(LiteralTag.class, (e1, literal) -> {
                assertAttribute(e1, LITERAL_TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                }).exit();
                literal.input(assertJSFunctionInput);
            }).exit();

            write.input(assertJSObjectInput);
        }).exit();

        // x.foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // read 'foo' from global
            enter(ReadPropertyTag.class, (e1, prop) -> {
                assertAttribute(e1, KEY, "foo");
                prop.input(assertGlobalObjectInput);
            }).exit();
            // 1st argument to function is target
            call.input(assertJSObjectInput);
            // 2nd argument is the function itself
            enter(ReadPropertyTag.class, assertPropertyReadName("x")).input().exit();
            call.input(assertJSFunctionInput);
            // arguments
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);

            // locals declarations
            enterDeclareTag("a");
            enterDeclareTag("b");

            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(42);
            }).exit();
            enter(WriteVariableTag.class, (e1, call1) -> {
                call1.input(Undefined.instance);
            }).exit();
        }).exit();
    }

    @Test
    public void methodCallElementArg() {
        evalAllTags("var a = {x:[function(){}]}; a.x[0](42);");

        // var a = ...
        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);

            enter(LiteralTag.class, (e1, oblit) -> {
                assertAttribute(e1, LITERAL_TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2, arrlit) -> {
                    assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.ArrayLiteral.name());
                    enter(LiteralTag.class, (e3) -> {
                        assertAttribute(e3, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                    }).exit();
                    arrlit.input(assertJSFunctionInput);
                }).exit();
                oblit.input(assertJSArrayInput);
            }).exit();
            write.input(assertJSObjectInput);
        }).exit();

        // a.x[0](42)
        enter(FunctionCallTag.class, (e, call) -> {
            // read 'a.x' from global
            enter(ReadPropertyTag.class, (e1, prop) -> {
                assertAttribute(e1, KEY, "x");
                enter(ReadPropertyTag.class, (e2, p2) -> {
                    assertAttribute(e2, KEY, "a");
                    p2.input(assertGlobalObjectInput);
                }).exit();
                prop.input(assertJSObjectInput);
            }).exit();
            // 1st argument is an array (i.e., target)
            call.input(assertJSArrayInput);
            // 2nd argument is the function itself

            enter(ReadElementTag.class, (e1, el) -> {
                el.input(assertJSArrayInput);
                enter(LiteralTag.class).exit(assertReturnValue(0));
                el.input(0);
            }).exit();

            call.input(assertJSFunctionInput);
            // arguments
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
        }).exit(assertReturnValue(Undefined.instance));

    }

    @Test
    public void newTest() {
        evalWithTags("function A() {}; var a = {x:function(){return 1;}}; new A(a.x(), a.x());", new Class<?>[]{ObjectAllocationTag.class, FunctionCallTag.class});

        enter(ObjectAllocationTag.class, (e, call) -> {
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class).input().input().exit();
            call.input(1);
            enter(FunctionCallTag.class).input().input().exit();
            call.input(1);

        }).exit((r) -> {
            Object[] vals = (Object[]) r.val;
            assertTrue(vals[2].equals(1));
            assertTrue(vals[3].equals(1));
            // should be the function instead of null
            assertTrue(JSFunction.isJSFunction(vals[1]));
        });
    }

    @Test
    public void changeFunc() {
        String src = "function foo(a){return a;}" +
                        "function bar(b){return b;}" +
                        "function run() {this.f();}" +
                        "function T() {this.f = foo;this.r = run;}" +
                        "for(var i = 0; i < 2; i++) {" +
                        " var t = new T();" +
                        " t.r();" +
                        " t.f = bar;" +
                        " t.r();" +
                        "}";
        evalWithTags(src, new Class<?>[]{ObjectAllocationTag.class, FunctionCallTag.class});

        // Invoke operations perform the two read operations independently.
        // 1. read the target object
        enter(ObjectAllocationTag.class, (e, call) -> {
            call.input(assertJSFunctionInput);
        }).exit();
        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput);
            }).exit();
        }).exit();
        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput);
            }).exit();
        }).exit();
        enter(ObjectAllocationTag.class, (e, call) -> {
            call.input(assertJSFunctionInput);
        }).exit();
        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput);
            }).exit();
        }).exit();
        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class, (e2, call2) -> {
                call2.input(assertJSObjectInput);
                call2.input(assertJSFunctionInput);
            }).exit();
        }).exit();
    }

    @Test
    public void castCrash() {
        String src = "function foo(){var fArr = [function (){}];for(i in fArr) {fArr[i]();}} foo();";
        evalWithTag(src, FunctionCallTag.class);

        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            enter(FunctionCallTag.class, (e2, call2) -> {
                call2.input(assertJSArrayInput);
                call2.input(assertJSFunctionInput);
            }).exit();
        }).exit();
    }

    @Test
    public void callForeignTest() {
        String src = "var r = Polyglot.import('run'); r.run();";
        declareInteropSymbol("run", new ForeignTestObject());
        evalWithTag(src, FunctionCallTag.class);

        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertJSObjectInput);
            call.input(assertJSFunctionInput);
            call.input("run");
        }).exit();
        enter(FunctionCallTag.class, (e, call) -> {
            call.input(assertTruffleObject);
            call.input(assertTruffleObject);
        }).exit();
    }

    @Test
    public void invokeGlobal() {
        String src = "arr=new Array();\n" +
                        "for(var a = 0; a < 100; a++){\n" +
                        "  arr.push(\"\");\n" +
                        "}";

        evalWithTags(src, new Class<?>[]{ObjectAllocationTag.class, FunctionCallTag.class});

        enter(ObjectAllocationTag.class, (e, call) -> {
            call.input(assertJSFunctionInput);
        }).exit();
        for (int i = 0; i < 100; i++) {
            enter(FunctionCallTag.class, (e, call) -> {
                call.input(assertJSArrayInput);
                call.input(assertJSFunctionInput);
                call.input("");
            }).exit();
        }
    }

    @Test
    public void restArgs() {
        evalWithTag("function foo(...args) {" +
                        "  return bar(...args);" +
                        "};" +
                        "function bar() {" +
                        "  return arguments[0];" +
                        "};" +
                        "foo(42);", FunctionCallTag.class);

        enter(FunctionCallTag.class, (e, fooCall) -> {
            fooCall.input(assertJSObjectInput);
            fooCall.input(assertJSFunctionInput);
            fooCall.input(42);
            enter(FunctionCallTag.class, (e2, barCall) -> {
                barCall.input(assertJSObjectInput);
                barCall.input(assertJSFunctionInput);
                barCall.input(assertJSArrayInput);
            }).exit();
        }).exit(assertReturnValue(42));
    }

    @Test
    public void restArgsMulti() {
        evalWithTag("function foo(x, y, ...args) {" +
                        "  return bar(x, y, ...args);" +
                        "};" +
                        "function bar() {" +
                        "  return arguments[4];" +
                        "};" +
                        "foo('a', 'b', 40, 41, 42);", FunctionCallTag.class);

        enter(FunctionCallTag.class, (e, fooCall) -> {
            fooCall.input(assertJSObjectInput);
            fooCall.input(assertJSFunctionInput);
            fooCall.input("a");
            fooCall.input("b");
            fooCall.input(40);
            fooCall.input(41);
            fooCall.input(42);
            enter(FunctionCallTag.class, (e2, barCall) -> {
                barCall.input(assertJSObjectInput);
                barCall.input(assertJSFunctionInput);
                barCall.input("a");
                barCall.input("b");
                barCall.input(assertJSArrayInput);
            }).exit(assertReturnValue(42));
        }).exit(assertReturnValue(42));
    }

    @Test
    public void supeCallTest() {
        evalWithTags("class Base {" +
                        "  constructor() {" +
                        "    this.someObj = {};" +
                        "  };" +
                        "  def() {" +
                        "    return this.someObj;" +
                        "  };" +
                        "};" +
                        "class Bar extends Base {" +
                        "  use() {" +
                        "    return super.def();" +
                        "  };" +
                        "};" +
                        "var bar = new Bar();" +
                        "bar.use();", new Class<?>[]{ObjectAllocationTag.class, FunctionCallTag.class});

        enter(ObjectAllocationTag.class, (e, newCall) -> {
            newCall.input(assertJSFunctionInputWithName("Bar"));
        }).exit(assertJSObjectReturn);

        enter(FunctionCallTag.class, (e2, useCall) -> {
            useCall.input(assertJSObjectInput);
            useCall.input(assertJSFunctionInputWithName("use"));
            enter(FunctionCallTag.class, (e3, defCall) -> {
                defCall.input(assertJSObjectInput);
                defCall.input(assertJSFunctionInputWithName("def"));
            }).exit(assertJSObjectReturn);
        }).exit(assertJSObjectReturn);
    }

    @Test
    public void splitMaterializedCallTest() {
        evalWithTags("function setKey(obj, keys) {" +
                        "  obj.a;" +
                        "  keys.slice(0, -1).forEach(function(key) {});" +
                        "};" +
                        "setKey({}, ['a']);" +
                        "for(var i =0; i<2; i++) {" +
                        "  setKey({a:1}, ['a']);" +
                        "};", new Class<?>[]{ObjectAllocationTag.class, FunctionCallTag.class});

        for (int i = 0; i < 3; i++) {
            enter(FunctionCallTag.class, (e, call) -> {
                call.input(assertUndefinedInput);
                call.input(assertJSFunctionInputWithName("setKey"));
                call.input(assertJSObjectInput);
                call.input(assertJSArrayInput);

                enter(FunctionCallTag.class, (e1, call1) -> {
                    enter(FunctionCallTag.class, (e2, call2) -> {
                        call2.input(assertJSArrayInput);
                        call2.input(assertJSFunctionInputWithName("slice"));
                        call2.input(0);
                        call2.input(-1);
                    }).exit();

                    call1.input(assertJSArrayInput);
                    call1.input(assertJSFunctionInputWithName("forEach"));
                    call1.input(assertJSFunctionInput);
                }).exit();
            }).exit();
        }
    }

    @Test
    public void splitMaterializedElementCallTest() {
        evalWithTag("function setKey(obj, keys) {" +
                        "  obj.a;" +
                        "  keys.slice[0][1][2](0, -1).forEach(function(key) {});" +
                        "};" +
                        "const callable = {" +
                        "  slice : [['',['','',function fakeslice() { return [1,2]; }]]]" +
                        "};" +
                        "setKey({}, callable);" +
                        "for (var i = 0; i < 2; i++) {" +
                        "  setKey({" +
                        "    a: 1" +
                        "  }, callable);" +
                        "};", FunctionCallTag.class);

        for (int i = 0; i < 3; i++) {
            enter(FunctionCallTag.class, (e, call) -> {
                call.input(assertUndefinedInput);
                call.input(assertJSFunctionInputWithName("setKey"));
                call.input(assertJSObjectInput);
                call.input(assertJSObjectInput);

                enter(FunctionCallTag.class, (e1, call1) -> {
                    enter(FunctionCallTag.class, (e2, call2) -> {
                        call2.input(assertJSArrayInput);
                        call2.input(assertJSFunctionInputWithName("fakeslice"));
                        call2.input(0);
                        call2.input(-1);
                    }).exit();

                    call1.input(assertJSArrayInput);
                    call1.input(assertJSFunctionInputWithName("forEach"));
                    call1.input(assertJSFunctionInput);
                }).exit();
            }).exit();
        }
    }

    @Test
    public void doWith() {
        evalWithTag("function bar() {" +
                        "  var obj = {" +
                        "    foo: function(){}" +
                        "  };" +
                        "  with(obj)" +
                        "    return foo('str', 42);" +
                        "}" +
                        "bar();", FunctionCallTag.class);

        enter(FunctionCallTag.class, (e, barCall) -> {
            barCall.input(assertUndefinedInput);
            barCall.input(assertJSFunctionInputWithName("bar"));
            enter(FunctionCallTag.class, (e2, fooCall) -> {
                fooCall.input(assertJSObjectInput);
                fooCall.input(assertJSFunctionInputWithName("foo"));
                fooCall.input("str");
                fooCall.input(42);
            }).exit();
        }).exit();
    }

    @Test
    public void github367() {
        evalWithTags("class C { #x = function() {}; m() { this.#x(42); } }; new C().m()", new Class[]{ObjectAllocationTag.class, FunctionCallTag.class});

        enter(FunctionCallTag.class, (e, mCall) -> {
            enter(ObjectAllocationTag.class, (n, newClassCall) -> {
                newClassCall.input(assertJSFunctionInputWithName("C"));
            }).exit(assertJSObjectReturn);
            mCall.input(assertJSObjectInput);
            mCall.input(assertJSFunctionInputWithName("m"));
            enter(FunctionCallTag.class, (e2, fieldCall) -> {
                fieldCall.input(assertJSObjectInput);
                fieldCall.input(assertJSFunctionInputWithName("#x"));
                fieldCall.input(42);
            }).exit();
        }).exit();
    }
}
