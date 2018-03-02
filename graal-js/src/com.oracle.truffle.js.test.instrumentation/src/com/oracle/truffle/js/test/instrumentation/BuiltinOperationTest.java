/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

        assertEngineInit();

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

        assertEngineInit();

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
