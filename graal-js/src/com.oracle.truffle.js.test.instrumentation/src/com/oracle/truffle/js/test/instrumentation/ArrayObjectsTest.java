/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.FunctionCallExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ArrayObjectsTest extends FineGrainedAccessTest {

    @Test
    public void arrayTest1() {
        String src = "var a = new Array(42);";

        evalWithTag(src, ObjectAllocationExpressionTag.class);

        enter(ObjectAllocationExpressionTag.class, (e, call) -> {
            assertAttribute(e, "isNew", true);
            assertAttribute(e, "isInvoke", false);
            // Allocations have only one input: constructor
            call.input(assertJSFunctionInput);
            call.input(42);
        }).exit((e) -> {
            Object[] vals = (Object[]) e.val;
            assertTrue(JSFunction.isJSFunction(vals[1]));
            assertEquals(42, vals[2]);
        });
    }

    @Test
    public void arrayTest2() {
        String src = "var a = Array(42);";

        evalWithTag(src, FunctionCallExpressionTag.class);

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            assertAttribute(e, "isNew", false);
            assertAttribute(e, "isInvoke", false);
            call.input(assertUndefinedInput);
            call.input(assertJSFunctionInput);
            call.input(42);
        }).exit((e) -> {
            Object[] vals = (Object[]) e.val;
            assertEquals(vals[1], Undefined.instance);
            assertEquals(vals[3], 42);
        });
    }

    @Test
    public void arrayTest3() {
        String src = "var a = [1,2,3]; a.push(42);";

        evalWithTag(src, FunctionCallExpressionTag.class);

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            assertAttribute(e, "isNew", false);
            assertAttribute(e, "isInvoke", true);
            call.input(assertJSArrayInput);
            call.input(assertJSFunctionInput);
            call.input(42);
        }).exit((e) -> {
            Object[] vals = (Object[]) e.val;
            assertTrue(vals.length == 4);
            assertTrue(JSObject.isJSObject(vals[1]));
            assertTrue(!JSFunction.isJSFunction(vals[1]));
            assertTrue(JSFunction.isJSFunction(vals[2]));
            assertEquals(vals[3], 42);
        });
    }

    @Test
    public void arrayTest4() {
        String src = "var a = [1,2,3]; a.push(42,41,40);";

        evalWithTag(src, FunctionCallExpressionTag.class);

        enter(FunctionCallExpressionTag.class, (e, call) -> {
            assertAttribute(e, "isNew", false);
            assertAttribute(e, "isInvoke", true);
            call.input(assertJSArrayInput);
            call.input(assertJSFunctionInput);
            call.input(42);
            call.input(41);
            call.input(40);
        }).exit((e) -> {
            Object[] vals = (Object[]) e.val;
            assertTrue(vals.length == 6);
            assertTrue(JSObject.isJSObject(vals[1]));
            assertTrue(!JSFunction.isJSFunction(vals[1]));
            assertTrue(JSFunction.isJSFunction(vals[2]));
            assertEquals(vals[3], 42);
            assertEquals(vals[4], 41);
            assertEquals(vals[5], 40);
        });
    }

}
