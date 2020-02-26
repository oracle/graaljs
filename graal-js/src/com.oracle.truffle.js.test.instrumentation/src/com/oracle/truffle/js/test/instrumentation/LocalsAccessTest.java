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

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowBranchTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LocalsAccessTest extends FineGrainedAccessTest {

    @Test
    public void write() {
        evalAllTags("(function() { var a = 42; })();");

        enter(FunctionCallTag.class, (e1, call) -> {
            // fetch the target for the call (which is undefined)
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // fetch the function, i.e., read the literal
            enter(LiteralTag.class).exit((e2) -> {
                assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
            });
            call.input(assertJSFunctionInput);

            enterDeclareTag("a");

            enter(WriteVariableTag.class, (e3, var) -> {
                enter(LiteralTag.class).exit();
                var.input(42);
            }).exit();
        }).exit(assertReturnValue(Undefined.instance));
    }

    @Test
    public void writeScope() {
        evalAllTags("(function() { var level; (function() { (function() { level = 42; })(); })();})();");

        enter(FunctionCallTag.class, (e1, call) -> {
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(LiteralTag.class).exit((e2) -> {
                assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
            });
            call.input(assertJSFunctionInput);

            enterDeclareTag("level");

            // second call
            enter(FunctionCallTag.class, (e2, call2) -> {
                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                call2.input(assertUndefinedInput);
                enter(LiteralTag.class).exit((e3) -> {
                    assertAttribute(e3, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                });
                call2.input(assertJSFunctionInput);
                // third call
                enter(FunctionCallTag.class, (e3, call3) -> {
                    enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                    call3.input(assertUndefinedInput);
                    enter(LiteralTag.class).exit((e4) -> {
                        assertAttribute(e4, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
                    });
                    call3.input(assertJSFunctionInput);
                    // TODO missing input event for arguments to FunctionCallTag
                    enter(WriteVariableTag.class, (e4, var) -> {
                        enter(LiteralTag.class).exit();
                        var.input(42);
                    }).exit();
                }).exit(assertReturnValue(Undefined.instance));
            }).exit(assertReturnValue(Undefined.instance));
        }).exit(assertReturnValue(Undefined.instance));
    }

    @Test
    public void read() {
        evalAllTags("(function() { var a = 42; return a; })();");

        enter(FunctionCallTag.class, (e1, call) -> {
            // fetch the target for the call (which is undefined)
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // get the function from the literal
            enter(LiteralTag.class).exit((e2) -> {
                assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
            });
            call.input(assertJSFunctionInput);

            enterDeclareTag("a");

            // write 42
            enter(WriteVariableTag.class, (e2, var) -> {
                enter(LiteralTag.class).exit();
                var.input(42);
            }).exit();
            // return statement
            enter(ControlFlowBranchTag.class, (e4, v) -> {
                assertAttribute(e4, TYPE, ControlFlowBranchTag.Type.Return.name());
                enter(ReadVariableTag.class).exit();
                v.input(42);
            }).exitMaybeControlFlowException();
        }).exit();
    }

    @Test
    public void readConst() {
        evalAllTags("(function() { const a = 42; return a; })();");

        enter(FunctionCallTag.class, (e1, call) -> {

            // fetch the target for the call (which is undefined)
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // get the function from the literal
            enter(LiteralTag.class).exit((e2) -> {
                assertAttribute(e2, LITERAL_TYPE, LiteralTag.Type.FunctionLiteral.name());
            });
            call.input(assertJSFunctionInput);

            enterDeclareTag("a");
            // write 42
            enter(WriteVariableTag.class, (e2, var) -> {
                enter(LiteralTag.class).exit();
                var.input(42);
            }).exit();
            // return statement
            enter(ControlFlowBranchTag.class, (e4, v) -> {
                assertAttribute(e4, TYPE, ControlFlowBranchTag.Type.Return.name());
                enter(ReadVariableTag.class).exit();
                v.input(42);
            }).exitMaybeControlFlowException();

        }).exit(assertReturnValue(42));
    }

    @Test
    public void forOfConst() {
        evalWithTag("for(const a of [41,42]) {};", WriteVariableTag.class);

        enter(WriteVariableTag.class, (e, w) -> {
            w.input(41);
        }).exit();
        enter(WriteVariableTag.class, (e, w) -> {
            w.input(42);
        }).exit();
    }

    @Test
    public void forOfVar() {
        evalWithTag("for(var a of [41,42]) {};", WritePropertyTag.class);

        enter(WritePropertyTag.class, (e, w) -> {
            w.input(assertGlobalObjectInput);
            w.input(41);
        }).exit();
        enter(WritePropertyTag.class, (e, w) -> {
            w.input(assertGlobalObjectInput);
            w.input(42);
        }).exit();
    }

    @Test
    public void readThis() {
        evalWithTag("(()=>{return this;})()", ReadVariableTag.class);

        enter(ReadVariableTag.class, (e, r) -> {
            assertAttribute(e, NAME, "this");
        }).exit();
    }

    @Test
    public void classDeclaration() {
        evalWithTags("class Klass {}", new Class[]{ReadVariableTag.class,
                        ReadPropertyTag.class,
                        ReadElementTag.class});

        // No read events are generated for a class declaration.
        // Test asserts that event queue is empty.
    }

    @Test
    public void classDeclareExpression() {
        evalWithTag("foo = class Foo {};", WritePropertyTag.class);

        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "foo");
            write.input(assertGlobalObjectInput);
            write.input(assertJSFunctionInput);
        }).exit();
    }

}
