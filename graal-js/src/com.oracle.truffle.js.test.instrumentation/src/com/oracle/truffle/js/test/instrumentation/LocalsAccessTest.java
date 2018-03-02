/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LocalsAccessTest extends FineGrainedAccessTest {

    @Test
    public void write() {
        evalAllTags("(function() { var a = 42; })();");

        assertEngineInit();

        enter(WriteVariableExpressionTag.class, (e, write) -> {
            enter(FunctionCallExpressionTag.class, (e1, call) -> {
                // fetch the target for the call (which is undefined)
                enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                // fetch the function, i.e., read the literal
                enter(LiteralExpressionTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                enter(UnaryExpressionTag.class, (e2, unary) -> {
                    enter(WriteVariableExpressionTag.class, (e3, var) -> {
                        enter(LiteralExpressionTag.class).exit();
                        var.input(42);
                    }).exit();
                    unary.input(42);
                }).exit();
            }).exit();
            write.input(Undefined.instance);
        }).exit();
    }

    @Test
    public void writeScope() {
        evalAllTags("(function() { var level; (function() { (function() { level = 42; })(); })();})();");

        assertEngineInit();

        enter(WriteVariableExpressionTag.class, (e, write) -> {
            // first call
            enter(FunctionCallExpressionTag.class, (e1, call) -> {
                enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                enter(LiteralExpressionTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                // second call
                enter(UnaryExpressionTag.class, (e6, unary) -> {
                    enter(FunctionCallExpressionTag.class, (e2, call2) -> {
                        enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                        call2.input(assertUndefinedInput);
                        enter(LiteralExpressionTag.class).exit((e3) -> {
                            assertAttribute(e3, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
                        });
                        call2.input(assertJSFunctionInput);
                        // third call
                        enter(UnaryExpressionTag.class, (e7, unary2) -> {
                            enter(FunctionCallExpressionTag.class, (e3, call3) -> {
                                enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                                call3.input(assertUndefinedInput);
                                enter(LiteralExpressionTag.class).exit((e4) -> {
                                    assertAttribute(e4, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
                                });
                                call3.input(assertJSFunctionInput);
                                // TODO missing input event for arguments to FunctionCallTag
                                enter(UnaryExpressionTag.class, (e8, unary3) -> {
                                    enter(WriteVariableExpressionTag.class, (e4, var) -> {
                                        enter(LiteralExpressionTag.class).exit();
                                        var.input(42);
                                    }).exit();
                                    unary3.input();
                                }).exit();
                            }).exit();
                            unary2.input();
                        }).exit();
                    }).exit();
                    unary.input();
                }).exit();
            }).exit();
            write.input(Undefined.instance);
        }).exit();
    }

    @Test
    public void read() {
        evalAllTags("(function() { var a = 42; return a; })();");

        assertEngineInit();

        enter(WriteVariableExpressionTag.class, (e, write) -> {
            enter(FunctionCallExpressionTag.class, (e1, call) -> {
                // fetch the target for the call (which is undefined)
                enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                // get the function from the literal
                enter(LiteralExpressionTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralExpressionTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                // write 42
                enter(WriteVariableExpressionTag.class, (e2, var) -> {
                    enter(LiteralExpressionTag.class).exit();
                    var.input(42);
                }).exit();
                // return statement
                enter(ReadVariableExpressionTag.class).exit();
            }).exit();
            write.input(42);
        }).exit();
    }

}
