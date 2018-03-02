/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

        assertEngineInit();

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

        assertEngineInit();

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

        assertEngineInit();

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
}
