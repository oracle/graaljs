/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.EvalCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class EvalTest extends FineGrainedAccessTest {

    @Test
    public void eval() {
        evalAllTags("eval('var a = 42;')");

        enter(WriteVariableExpressionTag.class, (v, var) -> {
            enter(EvalCallTag.class, (e, eval) -> {
                enter(ReadPropertyExpressionTag.class, assertPropertyReadName("eval")).input(assertGlobalObjectInput).exit();
                eval.input(assertJSFunctionInput);
                enter(LiteralExpressionTag.class, assertLiteralType(LiteralExpressionTag.Type.StringLiteral)).exit();
                eval.input("var a = 42;");
                enter(WritePropertyExpressionTag.class, (e1, prop) -> {
                    prop.input(assertJSObjectInput);
                    enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
                    prop.input(42);
                }).exit();
            }).exit();
            var.input(Undefined.instance);
        }).exit();
    }

}
