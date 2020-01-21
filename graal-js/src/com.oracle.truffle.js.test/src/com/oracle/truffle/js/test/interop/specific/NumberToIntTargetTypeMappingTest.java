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

import java.util.Arrays;
import java.util.Collection;

import org.graalvm.polyglot.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test that any number created in JavaScript can be accessed as Java int, the number is truncated
 * if necessary.
 */
@RunWith(Parameterized.class)
public class NumberToIntTargetTypeMappingTest {
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                        {"1", Integer.class, 1},
                        {"1", int.class, 1},
                        {"256", Integer.class, 256},
                        {"256", int.class, 256},
                        {"32768", Integer.class, 32768},
                        {"32768", int.class, 32768},
                        {"2147483648", Integer.class, 2147483647}, // Double#intValue is used to get
                                                                   // the result
                        {"2147483648", int.class, 2147483647}, // ''
                        {"9223372036854775808", Integer.class, 2147483647}, // ''
                        {"9223372036854775808", int.class, 2147483647}, // ''
                        {"3.4028236e+38", Integer.class, 2147483647}, // ''
                        {"3.4028236e+38", int.class, 2147483647}, // ''
                        {"1.7976931348623159E+308", Integer.class, 2147483647}, // ''
                        {"1.7976931348623159E+308", int.class, 2147483647}, // ''
                        {"-1", Integer.class, -1},
                        {"-1", int.class, -1},
                        {"-32769", Integer.class, -32769},
                        {"-32769", int.class, -32769},
                        {"-2147483649", Integer.class, -2147483648}, // ''
                        {"-2147483649", int.class, -2147483648}, // ''
                        {"-9223372036854775809", Integer.class, -2147483648}, // ''
                        {"-9223372036854775809", int.class, -2147483648}, // ''
                        {"-3.4028236e+38", Integer.class, -2147483648}, // ''
                        {"-3.4028236e+38", int.class, -2147483648}, // ''
                        {"-1.7976931348623159E+308", Integer.class, -2147483648}, // ''
                        {"-1.7976931348623159E+308", int.class, -2147483648}, // ''
        });
    }

    private Context context;

    @Parameterized.Parameter public String numberString;

    @Parameterized.Parameter(1) public Class<? extends Number> integerClass;

    @Parameterized.Parameter(2) public int intResult;

    @Before
    public void setUp() {
        context = ContextSetup.CONTEXT_BUILDER.build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void testSuccessfulCoercion() {
        Assert.assertEquals(intResult, context.eval(ID, numberString).as(integerClass));
    }
}
