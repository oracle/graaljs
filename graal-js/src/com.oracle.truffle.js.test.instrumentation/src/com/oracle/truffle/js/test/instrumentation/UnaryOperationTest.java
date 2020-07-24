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
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyTag;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class UnaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void typeof() {
        evalAllTags("var b = typeof Uint8Array;");

        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryOperationTag.class, (e2, unary) -> {
                assertAttribute(e2, OPERATOR, "typeof");
                enter(ReadPropertyTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "Uint8Array");
                    prop.input((e4) -> {
                        assertTrue(JSObject.isJSDynamicObject(e4.val));
                    });
                }).exit();
                unary.input(assertJSFunctionInput);
            }).exit();
            write.input("function");
        }).exit();
    }

    @Test
    public void voidMethod() {
        evalAllTags("void function foo() {}();");

        enter(UnaryOperationTag.class, (e2, unary) -> {
            assertAttribute(e2, OPERATOR, "void");
            enter(FunctionCallTag.class, (e3, call) -> {
                enter(LiteralTag.class).exit();
                call.input(assertUndefinedInput);
                enter(LiteralTag.class).exit();
                call.input(assertJSFunctionInput);
            }).exit(assertReturnValue(Undefined.instance));
            unary.input(Undefined.instance);
        }).exit();
    }

    @Test
    public void bitwiseNot() {
        assertBasicUnaryOperation("var x = true; var b = ~x;", true, true, -2, "~");
    }

    @Test
    public void not() {
        assertBasicUnaryOperation("var x = true; var b = !x;", true, true, false, "!");
    }

    @Test
    public void minus() {
        assertBasicUnaryOperation("var x = true; var b = -x;", true, true, -1, "-");
    }

    @Test
    public void plus() {
        assertBasicUnaryOperation("var x = true; var b = +x;", true, true, 1, "+");
    }

    private void assertBasicUnaryOperation(String src, Object expectedLiteralValue, Object expectedPreUnaryOpValue, Object expectedPostUnaryOpValue, String operator) {
        evalAllTags(src);

        assertGlobalVarDeclaration("x", true);

        enter(WritePropertyTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryOperationTag.class, (e2, unary) -> {
                assertAttribute(e2, OPERATOR, operator);
                enter(ReadPropertyTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "x");
                    prop.input(assertGlobalObjectInput);
                }).exit(assertReturnValue(expectedLiteralValue));
                unary.input(expectedPreUnaryOpValue);
            }).exit();
            write.input(expectedPostUnaryOpValue);
        }).exit();
    }

    @Test
    public void complement() {
        evalAllTags("0 & (~-1073741824);");

        enter(BinaryOperationTag.class, (e, b) -> {
            enter(LiteralTag.class).exit(assertReturnValue(0));
            b.input(0);

            enter(UnaryOperationTag.class, (e2, u) -> {
                enter(LiteralTag.class).exit(assertReturnValue(-1073741824));
                u.input(-1073741824);
            }).exit(assertReturnValue(1073741823));

            b.input(1073741823);
        }).exit();
    }

}
