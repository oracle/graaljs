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
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Various tests for accessing JavaScript strings as Java numbers.
 */
@RunWith(Parameterized.class)
public class StringToNumberTargetTypeMappingTest {

    @Parameters(name = "{index}: {0}, {1}, {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                        {Byte.class, (byte) 1, "256"},
                        {byte.class, (byte) 1, "256"},
                        {Short.class, (short) 1, "32768"},
                        {short.class, (short) 1, "32768"},
                        {Short.class, (short) 1, "-32769"},
                        {short.class, (short) 1, "-32769"},
                        {Integer.class, 1, "2147483648"},
                        {int.class, 1, "2147483648"},
                        {Integer.class, 1, "-2147483649"},
                        {int.class, 1, "-2147483649"},
                        {Long.class, 1L, "9223372036854775808"},
                        {long.class, 1L, "9223372036854775808"},
                        {Long.class, 1L, "-9223372036854775809"},
                        {long.class, 1L, "-9223372036854775809"},
                        {Float.class, 1.0f, "3.4028236e+38"},
                        {float.class, 1.0f, "3.4028236e+38"},
                        {Float.class, 1.0f, "1e-46"},
                        {float.class, 1.0f, "1e-46"},
                        {Double.class, 1.0d, "1.7976931348623159e+308"},
                        {double.class, 1.0d, "1.7976931348623159e+308"},
                        {Double.class, 1.0d, "1e-325"},
                        {double.class, 1.0d, "1e-325"},
        });
    }

    @Rule public ExpectedException expectedException = ExpectedException.none();

    private Context context;

    @Parameter public Class<? extends Number> numberClass;

    @Parameter(1) public Number numberOneOfProperType;

    @Parameter(2) public String outOfRangeValue;

    @Before
    public void setUp() {
        context = ContextSetup.CONTEXT_BUILDER.build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    public static class ToBePassedToJs {
        Number number;

        public void byteMethod(byte b) {
            number = b;
        }

        public void boxedByteMethod(Byte b) {
            number = b;
        }

        public void shortMethod(short s) {
            number = s;
        }

        public void boxedShortMethod(Short s) {
            number = s;
        }

        public void intMethod(int i) {
            number = i;
        }

        public void boxedIntegerMethod(Integer i) {
            number = i;
        }

        public void longMethod(long ll) {
            number = ll;
        }

        public void boxedLongMethod(Long ll) {
            number = ll;
        }

        public void floatMethod(float f) {
            number = f;
        }

        public void boxedFloatMethod(Float f) {
            number = f;
        }

        public void doubleMethod(double d) {
            number = d;
        }

        public void boxedDoubleMethod(Double d) {
            number = d;
        }
    }

    /**
     * Test that the test data are consistent.
     */
    @Test
    public void testCorrectNumberOneClass() {
        assertEquals(numberClass.isPrimitive() ? ContextSetup.PRIMITIVE_TO_BOXED.get(numberClass) : numberClass, numberOneOfProperType.getClass());
    }

    /**
     * Test that the JavaScript string <code>'1'</code> can be accessed as any Java Number.
     */
    @Test
    public void testSuccessfulCoercion() {
        Value bindings = context.getBindings(ID);
        ToBePassedToJs obj = new ToBePassedToJs();
        bindings.putMember("obj", obj);

        String methodPrefix = numberClass.getSimpleName();
        if (Character.isUpperCase(methodPrefix.charAt(0))) {
            methodPrefix = "boxed" + methodPrefix;
        }

        Value val = context.eval(ID, "var str = '1'; obj." + methodPrefix + "Method(str); str;");

        if (numberOneOfProperType instanceof Float) {
            assertEquals((Float) numberOneOfProperType, (Float) val.as(numberClass), 0.000001f);
            assertEquals((Float) numberOneOfProperType, (Float) obj.number, 0.000001f);
        } else if (numberOneOfProperType instanceof Double) {
            assertEquals((Double) numberOneOfProperType, (Double) val.as(numberClass), 0.000001d);
            assertEquals((Double) numberOneOfProperType, (Double) obj.number, 0.000001d);
        } else {
            assertEquals(numberOneOfProperType, val.as(numberClass));
            assertEquals(numberOneOfProperType, obj.number);
        }
    }

    /**
     * Test that the conversion from JavaScript string to a Java number never truncates the value.
     */
    @Test
    public void testLossyCoercion() {
        expectedException.expect(ClassCastException.class);
        expectedException.expectMessage("'" + numberClass.getName() + "': Invalid or lossy primitive coercion");
        Value val = context.eval(ID, "'" + outOfRangeValue + "'");
        Number num = val.as(numberClass);
        if (num instanceof Double && (((Double) num).isInfinite() || ((Double) num) == 0.0d)) {
            throw new ClassCastException("'" + numberClass.getName() + "': Invalid or lossy primitive coercion");
        }
    }
}
