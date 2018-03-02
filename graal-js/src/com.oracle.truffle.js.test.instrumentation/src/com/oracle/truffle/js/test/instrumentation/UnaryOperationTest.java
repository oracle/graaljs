/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class UnaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void typeof() {
        evalAllTags("var b = typeof Uint8Array;");

        assertEngineInit();

        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryExpressionTag.class, (e2, unary) -> {
                enter(ReadPropertyExpressionTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "Uint8Array");
                    prop.input((e4) -> {
                        assertTrue(JSObject.isJSObject(e4.val));
                    });
                }).exit();
                unary.input((e4) -> {
                    assertTrue(JSObject.isJSObject(e4.val));
                });
            }).exit();
            write.input("function");
        }).exit();
    }

    @Test
    public void voidMethod() {
        evalAllTags("void function foo() {}();");

        assertEngineInit();

        enter(WriteVariableExpressionTag.class, (e, var) -> {
            assertAttribute(e, NAME, "<return>");
            enter(UnaryExpressionTag.class, (e2, unary) -> {
                enter(FunctionCallExpressionTag.class, (e3, call) -> {
                    enter(LiteralExpressionTag.class).exit();
                    call.input(assertUndefinedInput);
                    enter(LiteralExpressionTag.class).exit();
                    call.input(assertJSFunctionInput);
                    // lastly, read undefined to return it
                    enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
                }).exit();
                unary.input(Undefined.instance);
            }).exit();
            var.input(Undefined.instance);
        }).exit();
    }

    @Test
    public void toInt() {
        assertBasicUnaryOperation("var x = true; var b = ~x;", -2);
    }

    @Test
    public void not() {
        assertBasicUnaryOperation("var x = true; var b = !x;", false);
    }

    @Test
    public void minus() {
        assertBasicUnaryOperation("var x = true; var b = -x;", -1);
    }

    @Test
    public void plus() {
        assertBasicUnaryOperation("var x = true; var b = +x;", 1);
    }

    private void assertBasicUnaryOperation(String src, Object expectedPostUnaryOpValue) {
        evalAllTags(src);

        assertEngineInit();

        assertGlobalVarDeclaration("x", true);

        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryExpressionTag.class, (e2, unary) -> {
                enter(ReadPropertyExpressionTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "x");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                unary.input(true);
            }).exit();
            write.input(expectedPostUnaryOpValue);
        }).exit();
    }

}
