/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop.specific;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Remaining target type mapping tests that do not fit in any of the specific classes
 * {@link NumberToIntTargetTypeMappingTest} and {@link StringToNumberTargetTypeMappingTest}.
 */
public class RemainingTargetTypeMappingsTest {
    private Context context;

    @Before
    public void setUp() {
        context = ContextSetup.CONTEXT_BUILDER.build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    /**
     * Test that JavaScript strings <code>'true'</code> and <code>'false'</code> can be accessed as
     * Java booleans.
     */
    @Test
    public void testStringToBooleanCoercion() {
        Value val = context.eval(ID, "'true'");
        Assert.assertEquals(true, val.as(Boolean.class));
        Assert.assertEquals(true, val.as(boolean.class));
        val = context.eval(ID, "'false'");
        Assert.assertEquals(false, val.as(Boolean.class));
        Assert.assertEquals(false, val.as(boolean.class));
    }

    /**
     * Test that JavaScript numbers and booleans can be accessed as Java String.
     */
    @Test
    public void testNumberAndBooleanToStringCoercion() {
        Value val = context.eval(ID, "1");
        Assert.assertEquals("1", val.as(String.class));
        val = context.eval(ID, "2147483647");
        Assert.assertEquals("2147483647", val.as(String.class));
        val = context.eval(ID, "2147483648");
        Assert.assertEquals(2147483648.0d, Double.parseDouble(val.as(String.class)), 0.0000001d);
        val = context.eval(ID, "true");
        Assert.assertEquals("true", val.as(String.class));
        val = context.eval(ID, "false");
        Assert.assertEquals("false", val.as(String.class));
    }
}
