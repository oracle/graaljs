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

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class BuiltinOperationTest extends FineGrainedAccessTest {

    @Test
    public void mathRandom() {
        evalAllTags("var a = Math.random; a();");

        // var a = Math.random
        enter(WritePropertyExpressionTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(ReadPropertyExpressionTag.class, (e1, read) -> {
                assertAttribute(e1, KEY, "random");
                enter(ReadPropertyExpressionTag.class, (e2, prop) -> {
                    assertAttribute(e2, KEY, "Math");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                read.input(assertJSObjectInput);
            }).exit();
            write.input(assertJSFunctionInput);
        }).exit();

        // a()
        enter(FunctionCallExpressionTag.class, (e, call) -> {
            // read target for 'a' (which is undefined)
            enter(LiteralExpressionTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // read function 'a'
            enter(ReadPropertyExpressionTag.class, assertPropertyReadName("a")).input().exit();
            call.input(assertJSFunctionInput);
            enter(BuiltinRootTag.class, (e2) -> {
                assertAttribute(e2, NAME, "Math.random");
            }).exit();
        }).exit();
    }

    @Test
    public void objectDefineProp() {
        evalAllTags("const foo = {};" +
                        "Object.defineProperty(foo, 'bar', {" +
                        "  value: 42" +
                        "});");

        // const foo = {}
        enter(WriteVariableExpressionTag.class, (e, w) -> {
            assertAttribute(e, NAME, "foo");
            enter(LiteralExpressionTag.class).exit();
            w.input(assertJSObjectInput);
        }).exit();

        // call to 'defineProperty'
        enter(FunctionCallExpressionTag.class, (e, f) -> {
            // target
            enter(ReadPropertyExpressionTag.class, assertPropertyReadName("Object")).input(assertJSObjectInput).exit();
            f.input(assertJSFunctionInput);
            // function
            enter(ReadPropertyExpressionTag.class, assertPropertyReadName("defineProperty")).input().exit();
            f.input(assertJSFunctionInput);
            // 1st argument is the 'foo' variable
            enter(ReadVariableExpressionTag.class, assertVarReadName("foo")).exit();
            f.input(assertJSObjectInput);
            // 2nd argument is the 'bar' literal
            enter(LiteralExpressionTag.class).exit(assertReturnValue("bar"));
            f.input("bar");
            // create the object literal...
            enter(LiteralExpressionTag.class, (e1, l) -> {
                enter(LiteralExpressionTag.class).exit(assertReturnValue(42));
                l.input(42);
            }).exit();
            // ...use it as argument
            f.input(assertJSObjectInput);
            // detect this is a builtin call
            enter(BuiltinRootTag.class, (b) -> {
                assertAttribute(b, NAME, "Object.defineProperty");
            }).exit();

        }).exit();
    }

}
