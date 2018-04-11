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
