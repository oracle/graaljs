/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.graalvm.collections.Pair;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;

public class BigIntTest {

    @Test
    public void testByte() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-129n");
            assertFalse(value.fitsInByte());

            value = context.eval(JavaScriptLanguage.ID, "-128n");
            assertTrue(value.fitsInByte());
            assertEquals(-128, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInByte());
            assertEquals(0, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "127n");
            assertTrue(value.fitsInByte());
            assertEquals(127, value.asByte());

            value = context.eval(JavaScriptLanguage.ID, "128n");
            assertFalse(value.fitsInByte());
        }
    }

    @Test
    public void testShort() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-32769n");
            assertFalse(value.fitsInShort());

            value = context.eval(JavaScriptLanguage.ID, "-32768n");
            assertTrue(value.fitsInShort());
            assertEquals(-32768, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInShort());
            assertEquals(0, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "32767n");
            assertTrue(value.fitsInShort());
            assertEquals(32767, value.asShort());

            value = context.eval(JavaScriptLanguage.ID, "32768n");
            assertFalse(value.fitsInShort());
        }
    }

    @Test
    public void testInt() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-2147483649n");
            assertFalse(value.fitsInInt());

            value = context.eval(JavaScriptLanguage.ID, "-2147483648n");
            assertTrue(value.fitsInInt());
            assertEquals(-2147483648, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInInt());
            assertEquals(0, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "2147483647n");
            assertTrue(value.fitsInInt());
            assertEquals(2147483647, value.asInt());

            value = context.eval(JavaScriptLanguage.ID, "2147483648n");
            assertFalse(value.fitsInInt());
        }
    }

    @Test
    public void testLong() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "-9223372036854775809n");
            assertFalse(value.fitsInLong());

            value = context.eval(JavaScriptLanguage.ID, "-9223372036854775808n");
            assertTrue(value.fitsInLong());
            assertEquals(-9223372036854775808L, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInLong());
            assertEquals(0, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775807n");
            assertTrue(value.fitsInLong());
            assertEquals(9223372036854775807L, value.asLong());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775808n");
            assertFalse(value.fitsInLong());
        }
    }

    @Test
    public void testFloat() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInFloat());
            assertTrue(0 == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "2n**120n");
            assertTrue(value.fitsInFloat());
            assertTrue(Math.pow(2, 120) == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**120n)");
            assertTrue(value.fitsInFloat());
            assertTrue(-Math.pow(2, 120) == value.asFloat());

            value = context.eval(JavaScriptLanguage.ID, "2n**160n");
            assertFalse(value.fitsInFloat());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**160n)");
            assertFalse(value.fitsInFloat());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775807n");
            assertFalse(value.fitsInFloat());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775808n");
            assertTrue(value.fitsInFloat());

            value = context.eval(JavaScriptLanguage.ID, "9223372036854775809n");
            assertFalse(value.fitsInFloat());
        }
    }

    @Test
    public void testDouble() {
        Value value;
        try (Context context = JSTest.newContextBuilder().build()) {
            value = context.eval(JavaScriptLanguage.ID, "0n");
            assertTrue(value.fitsInDouble());
            assertTrue(0 == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "2n**1000n");
            assertTrue(value.fitsInDouble());
            assertTrue(Math.pow(2, 1000) == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**1000n)");
            assertTrue(value.fitsInDouble());
            assertTrue(-Math.pow(2, 1000) == value.asDouble());

            value = context.eval(JavaScriptLanguage.ID, "2n**1100n");
            assertFalse(value.fitsInDouble());

            value = context.eval(JavaScriptLanguage.ID, "-(2n**1100n)");
            assertFalse(value.fitsInDouble());
        }
    }

    @Test
    public void testBigIntegerEquality() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value strictlyEqual = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a === b; })");
            Value looselyEqual = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a == b; })");
            Value toBigInt = context.eval(JavaScriptLanguage.ID, "(function(value) { return BigInt(value); })");
            Value isOfBigIntType = context.eval(JavaScriptLanguage.ID, "(function(value) { return typeof value === 'bigint'; })");

            for (Pair<String, BigInteger> pair : List.of(
                            Pair.create("0n", BigInteger.valueOf(0)),
                            Pair.create("0", BigInteger.valueOf(0)),

                            Pair.create("2147483647", BigInteger.valueOf(Integer.MAX_VALUE)),
                            Pair.create("-2147483648", BigInteger.valueOf(Integer.MIN_VALUE)),

                            Pair.create("2147483647n", BigInteger.valueOf(Integer.MAX_VALUE)),
                            Pair.create("-2147483648n", BigInteger.valueOf(Integer.MIN_VALUE)),
                            Pair.create("2147483648n", BigInteger.valueOf(Integer.MAX_VALUE + 1L)),
                            Pair.create("-2147483649n", BigInteger.valueOf(Integer.MIN_VALUE - 1L)),

                            Pair.create("9007199254740991", BigInteger.valueOf(JSRuntime.MAX_SAFE_INTEGER_LONG)),
                            Pair.create("-9007199254740991", BigInteger.valueOf(JSRuntime.MIN_SAFE_INTEGER_LONG)),

                            Pair.create("9007199254740991n", BigInteger.valueOf(JSRuntime.MAX_SAFE_INTEGER_LONG)),
                            Pair.create("-9007199254740991n", BigInteger.valueOf(JSRuntime.MIN_SAFE_INTEGER_LONG)),
                            Pair.create("9007199254740992n", BigInteger.valueOf(JSRuntime.MAX_SAFE_INTEGER_LONG + 1L)),
                            Pair.create("-9007199254740992n", BigInteger.valueOf(JSRuntime.MIN_SAFE_INTEGER_LONG - 1L)),

                            Pair.create("9223372036854775807n", BigInteger.valueOf(Long.MAX_VALUE)),
                            Pair.create("-9223372036854775808n", BigInteger.valueOf(Long.MIN_VALUE)),
                            Pair.create("9223372036854775808n", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1))),
                            Pair.create("-9223372036854775809n", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1))),

                            Pair.create("2n**1000n", BigInteger.valueOf(2).pow(1000)),
                            Pair.create("-(2n**1000n)", BigInteger.valueOf(2).pow(1000).negate()),
                            Pair.create("2n**1100n", BigInteger.valueOf(2).pow(1100)),
                            Pair.create("-(2n**1100n)", BigInteger.valueOf(2).pow(1100).negate()))) {
                String numericLiteralString = pair.getLeft();
                BigInteger bigInteger = pair.getRight();
                BigInteger bigIntegerCopy = bigInteger.negate().negate();
                String message = pair.toString();

                Value jsValue = context.eval(JavaScriptLanguage.ID, numericLiteralString);

                assertTrue(message, jsValue.fitsInBigInteger());
                assertEquals(message, bigInteger, jsValue.asBigInteger());
                assertEquals(message, bigInteger, jsValue.as(BigInteger.class));

                Value bigIntegerAsValue = context.asValue(bigInteger);
                Value bigIntegerAsJSBigInt = toBigInt.execute(bigInteger);
                assertEquals(message, bigInteger, bigIntegerAsJSBigInt.asBigInteger());

                assertEquals(bigIntegerAsValue.fitsInDouble(), jsValue.fitsInDouble());
                assertEquals(bigIntegerAsValue.fitsInBigInteger(), jsValue.fitsInBigInteger());

                assertTrue(message, strictlyEqual.execute(jsValue, jsValue).asBoolean());
                assertTrue(message, looselyEqual.execute(jsValue, jsValue).asBoolean());

                assertTrue(message, strictlyEqual.execute(bigInteger, bigInteger).asBoolean());
                assertTrue(message, looselyEqual.execute(bigInteger, bigInteger).asBoolean());

                boolean isJSBigInt = isOfBigIntType.execute(jsValue).asBoolean();
                boolean sameNumType = isJSBigInt == !bigIntegerAsValue.fitsInDouble();
                /*
                 * '===' does not convert the value; a comparison between a numeric value and a host
                 * object always returns false.
                 */
                assertEquals(message, sameNumType, strictlyEqual.execute(jsValue, bigInteger).asBoolean());
                assertEquals(message, sameNumType, strictlyEqual.execute(bigInteger, jsValue).asBoolean());

                /*
                 * '==' applies ToPrimitive conversion to the operands; java.math.BigInteger may be
                 * treated as a JS Number (double) or BigInt value; either way, they will be equal
                 * and fit the same numeric types.
                 */
                assertTrue(message, looselyEqual.execute(jsValue, bigInteger).asBoolean());
                assertTrue(message, looselyEqual.execute(bigInteger, jsValue).asBoolean());

                /*
                 * '===' returns false because java.math.BigInteger objects currently are not
                 * treated as primitive values but as host objects (that are also Number and fit in
                 * BigInteger), and therefore compared using isIdentical semantics.
                 */
                assertTrue(strictlyEqual.execute(bigInteger, bigIntegerCopy).asBoolean());
                // Of course, when explicitly converted to JS BigInt, JS value semantics apply.
                assertTrue(strictlyEqual.execute(toBigInt.execute(bigInteger), toBigInt.execute(bigIntegerCopy)).asBoolean());

                // Two equal BigIntegers are converted to the same numeric value+type, and hence ==.
                assertTrue(looselyEqual.execute(bigInteger, bigIntegerCopy).asBoolean());

                // (bigint) === (numeric) will be true iff the numeric value is a BigInt, too.
                assertEquals(message, isJSBigInt, strictlyEqual.execute(jsValue, bigIntegerAsJSBigInt).asBoolean());
                assertEquals(message, isJSBigInt, strictlyEqual.execute(bigIntegerAsJSBigInt, jsValue).asBoolean());

                // (bigint) == (numeric) of the same value; must always be true.
                assertTrue(message, looselyEqual.execute(jsValue, bigIntegerAsJSBigInt).asBoolean());
                assertTrue(message, looselyEqual.execute(bigIntegerAsJSBigInt, jsValue).asBoolean());

                if (bigIntegerAsValue.fitsInLong()) {
                    assertTrue(message, looselyEqual.execute(bigInteger, bigInteger.longValue()).asBoolean());
                    assertTrue(message, looselyEqual.execute(bigInteger.longValue(), bigInteger).asBoolean());

                    assertTrue(message, looselyEqual.execute(bigIntegerAsJSBigInt, bigInteger.longValue()).asBoolean());
                    assertTrue(message, looselyEqual.execute(bigInteger.longValue(), bigIntegerAsJSBigInt).asBoolean());

                    assertTrue(message, strictlyEqual.execute(bigInteger, bigInteger.longValue()).asBoolean());
                    assertTrue(message, strictlyEqual.execute(bigInteger.longValue(), bigInteger).asBoolean());

                    boolean fitsOnlyInBigInt = bigIntegerAsValue.fitsInLong() && !bigIntegerAsValue.fitsInDouble();
                    assertEquals(message, fitsOnlyInBigInt, strictlyEqual.execute(bigIntegerAsJSBigInt, bigInteger.longValue()).asBoolean());
                    assertEquals(message, fitsOnlyInBigInt, strictlyEqual.execute(bigInteger.longValue(), bigIntegerAsJSBigInt).asBoolean());
                } else if (bigIntegerAsValue.fitsInDouble()) {
                    assertTrue(message, looselyEqual.execute(bigInteger, bigInteger.doubleValue()).asBoolean());
                    assertTrue(message, looselyEqual.execute(bigInteger.doubleValue(), bigInteger).asBoolean());

                    assertTrue(message, looselyEqual.execute(bigIntegerAsJSBigInt, bigInteger.doubleValue()).asBoolean());
                    assertTrue(message, looselyEqual.execute(bigInteger.doubleValue(), bigIntegerAsJSBigInt).asBoolean());

                    assertTrue(message, strictlyEqual.execute(bigInteger, bigInteger.doubleValue()).asBoolean());
                    assertTrue(message, strictlyEqual.execute(bigInteger.doubleValue(), bigInteger).asBoolean());

                    assertFalse(message, strictlyEqual.execute(bigIntegerAsJSBigInt, bigInteger.doubleValue()).asBoolean());
                    assertFalse(message, strictlyEqual.execute(bigInteger.doubleValue(), bigIntegerAsJSBigInt).asBoolean());
                }
            }
        }
    }

    private static List<BigInteger> bigIntegersForArithmetic() {
        return List.of(
                        BigInteger.valueOf(0),

                        BigInteger.valueOf(Integer.MAX_VALUE),
                        BigInteger.valueOf(Integer.MIN_VALUE),
                        BigInteger.valueOf(Integer.MAX_VALUE + 1L),
                        BigInteger.valueOf(Integer.MIN_VALUE - 1L),

                        BigInteger.valueOf(JSRuntime.MAX_SAFE_INTEGER_LONG),
                        BigInteger.valueOf(JSRuntime.MIN_SAFE_INTEGER_LONG),
                        BigInteger.valueOf(JSRuntime.MAX_SAFE_INTEGER_LONG + 1L),
                        BigInteger.valueOf(JSRuntime.MIN_SAFE_INTEGER_LONG - 1L),

                        BigInteger.valueOf(Long.MAX_VALUE),
                        BigInteger.valueOf(Long.MIN_VALUE),
                        BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1)),
                        BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1)),

                        BigInteger.valueOf(2).pow(1000),
                        BigInteger.valueOf(2).pow(1000).negate(),
                        BigInteger.valueOf(2).pow(1100),
                        BigInteger.valueOf(2).pow(1100).negate());
    }

    private static boolean treatForeignBigIntegerAsBigInt = false;

    @Test
    public void testBigIntegerUnaryPlus() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value toBigInt = context.eval(JavaScriptLanguage.ID, "(function(value) { return BigInt(value); })");

            Value plus = context.eval(JavaScriptLanguage.ID, "(function(value) { return +(value); })");

            List<BigInteger> bigIntegers = bigIntegersForArithmetic();

            for (int i = 0; i < bigIntegers.size(); i++) {
                BigInteger a = bigIntegers.get(i);
                Value aAsValue = context.asValue(a);

                if (aAsValue.fitsInDouble()) {
                    Value result = plus.execute(a);
                    assertEquals(plus.execute(a.doubleValue()).asDouble(), result.asDouble(), 0);
                    assertTrue(result.toString(), result.fitsInBigInteger() || Double.isInfinite(result.asDouble()));
                    if (aAsValue.fitsInLong()) {
                        assertEquals(plus.execute(a.doubleValue()).asDouble(), plus.execute(a.longValue()).asDouble(), 0);
                    }
                } else {
                    assert aAsValue.fitsInBigInteger();
                    // TypeError: Cannot convert a BigInt value to a number.
                    JSTest.assertThrows(() -> plus.execute(a), JSErrorType.TypeError);
                    if (aAsValue.fitsInLong()) {
                        JSTest.assertThrows(() -> plus.execute(a.longValue()), JSErrorType.TypeError);
                    }
                    JSTest.assertThrows(() -> plus.execute(toBigInt.execute(a)), JSErrorType.TypeError);
                }
            }
        }
    }

    @Test
    public void testBigIntegerUnaryMinus() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value toBigInt = context.eval(JavaScriptLanguage.ID, "(function(value) { return BigInt(value); })");

            Value minus = context.eval(JavaScriptLanguage.ID, "(function(value) { return -(value); })");

            List<BigInteger> bigIntegers = bigIntegersForArithmetic();

            for (int i = 0; i < bigIntegers.size(); i++) {
                BigInteger a = bigIntegers.get(i);
                Value aAsValue = context.asValue(a);

                assertTrue(aAsValue.fitsInBigInteger());

                if (aAsValue.fitsInDouble()) {
                    Value result = minus.execute(a);
                    assertEquals(minus.execute(a.doubleValue()).asDouble(), result.asDouble(), 0);
                    assertTrue(result.toString(), result.fitsInBigInteger() || Double.isInfinite(result.asDouble()));
                    if (aAsValue.fitsInLong()) {
                        assertEquals(minus.execute(a.doubleValue()).asDouble(), minus.execute(a.longValue()).asDouble(), 0);
                    }
                } else {
                    Value bigIntResult = minus.execute(toBigInt.execute(a));
                    assertTrue(bigIntResult.fitsInBigInteger());

                    if (treatForeignBigIntegerAsBigInt) {
                        Value result = minus.execute(a);
                        assertTrue(result.fitsInBigInteger());
                        assertEquals(bigIntResult.asBigInteger(), result.asBigInteger());
                        if (aAsValue.fitsInLong()) {
                            assertEquals(bigIntResult.asBigInteger(), minus.execute(a.longValue()).asBigInteger());
                        }
                    } else {
                        JSTest.assertThrows(() -> minus.execute(a), JSErrorType.TypeError);
                        if (aAsValue.fitsInLong()) {
                            JSTest.assertThrows(() -> minus.execute(a.longValue()), JSErrorType.TypeError);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testBigIntegerBinaryArithmetic() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value toBigInt = context.eval(JavaScriptLanguage.ID, "(function(value) { return BigInt(value); })");

            Value add = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a + b; })");
            Value sub = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a - b; })");
            Value mul = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a * b; })");
            Value div = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a / b; })");
            Value mod = context.eval(JavaScriptLanguage.ID, "(function(a, b) { return a % b; })");

            Value[] binaryOps = new Value[]{add, sub, mul, div, mod};

            List<BigInteger> bigIntegers = bigIntegersForArithmetic();

            for (int i = 0; i < bigIntegers.size(); i++) {
                BigInteger a = bigIntegers.get(i);
                Value aAsValue = context.asValue(a);
                Value result;

                for (int j = i; j < bigIntegers.size(); j++) {
                    BigInteger b = j == i ? a : bigIntegers.get(j);
                    Value bAsValue = j == i ? aAsValue : context.asValue(b);

                    assertTrue(aAsValue.fitsInBigInteger() && bAsValue.fitsInBigInteger());
                    boolean bothDouble = aAsValue.fitsInDouble() && bAsValue.fitsInDouble();
                    boolean bothBigInteger = !aAsValue.fitsInDouble() && !bAsValue.fitsInDouble();

                    for (Value binaryOp : binaryOps) {
                        if (bothDouble) {
                            result = binaryOp.execute(a, b);
                            assertEquals(binaryOp.execute(a.doubleValue(), b.doubleValue()).asDouble(), result.asDouble(), 0);
                            if (binaryOp != div && binaryOp != mod) {
                                assertTrue(result.toString(), result.fitsInBigInteger() || Double.isInfinite(result.asDouble()));
                            }
                            // TypeError: Cannot mix BigInt and other types
                            JSTest.assertThrows(() -> binaryOp.execute(toBigInt.execute(a), b), JSErrorType.TypeError);
                            JSTest.assertThrows(() -> binaryOp.execute(a, toBigInt.execute(b)), JSErrorType.TypeError);
                        } else if (bothBigInteger) {
                            Value bigIntResult = binaryOp.execute(toBigInt.execute(a), toBigInt.execute(b));
                            assertTrue(bigIntResult.fitsInBigInteger());

                            if (treatForeignBigIntegerAsBigInt) {
                                result = binaryOp.execute(a, b);
                                assertTrue(result.fitsInBigInteger());
                                assertEquals(bigIntResult.asBigInteger(), result.asBigInteger());

                                result = binaryOp.execute(toBigInt.execute(a), b);
                                assertTrue(result.fitsInBigInteger());
                                assertEquals(bigIntResult.asBigInteger(), result.asBigInteger());

                                result = binaryOp.execute(a, toBigInt.execute(b));
                                assertTrue(result.fitsInBigInteger());
                                assertEquals(bigIntResult.asBigInteger(), result.asBigInteger());
                            } else {
                                JSTest.assertThrows(() -> binaryOp.execute(a, b), JSErrorType.TypeError);
                                JSTest.assertThrows(() -> binaryOp.execute(toBigInt.execute(a), b), JSErrorType.TypeError);
                                JSTest.assertThrows(() -> binaryOp.execute(a, toBigInt.execute(b)), JSErrorType.TypeError);
                            }
                        } else {
                            assertTrue(aAsValue.fitsInDouble() != bAsValue.fitsInDouble());

                            // TypeError: Cannot mix BigInt and other types
                            JSTest.assertThrows(() -> binaryOp.execute(a, b), JSErrorType.TypeError);
                            if (!aAsValue.fitsInDouble() || !treatForeignBigIntegerAsBigInt) {
                                JSTest.assertThrows(() -> binaryOp.execute(toBigInt.execute(a), b), JSErrorType.TypeError);
                            }
                            if (!bAsValue.fitsInDouble() || !treatForeignBigIntegerAsBigInt) {
                                JSTest.assertThrows(() -> binaryOp.execute(a, toBigInt.execute(b)), JSErrorType.TypeError);
                            }
                        }
                    }
                }
            }
        }
    }
}
