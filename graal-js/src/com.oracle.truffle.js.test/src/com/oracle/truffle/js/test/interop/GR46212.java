/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

/**
 * Tests of the modification of length property of foreign arrays.
 */
@RunWith(Parameterized.class)
public class GR46212 {
    private final String arrayLengthExpr;

    @Parameterized.Parameters
    public static List<String> data() {
        return List.of("array.length", "array['length']");
    }

    public GR46212(String arrayLengthExpr) {
        this.arrayLengthExpr = arrayLengthExpr;
    }

    @Test
    public void testDecrease() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = new MapBasedProxyArray();
            array.set(0, Value.asValue(42));
            array.set(1, Value.asValue(211));
            array.set(2, Value.asValue(3.14));
            context.getBindings(ID).putMember("array", array);
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 3);
            context.eval(ID, arrayLengthExpr + " = 1");
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 1);
            assertEquals(context.eval(ID, "array[0]").asLong(), 42);
        }
    }

    @Test
    public void testIncrease() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = new MapBasedProxyArray();
            array.set(0, Value.asValue(42));
            context.getBindings(ID).putMember("array", array);
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 1);
            context.eval(ID, arrayLengthExpr + " = 3");
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 3);
            assertEquals(context.eval(ID, "array[0]").asLong(), 42);
        }
    }

    @Test
    public void testNotInteger() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = new MapBasedProxyArray();
            context.getBindings(ID).putMember("array", array);
            assertTrue(context.eval(ID, "try { " + arrayLengthExpr + " = 3.14; false; } catch (e) { e instanceof RangeError }").asBoolean());
        }
    }

    @Test
    public void testNegative() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = new MapBasedProxyArray();
            context.getBindings(ID).putMember("array", array);
            assertTrue(context.eval(ID, "try { " + arrayLengthExpr + " = -1; false; } catch (e) { e instanceof RangeError }").asBoolean());
        }
    }

    @Test
    public void testUnsupportedSloppy() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyArray array = ProxyArray.fromArray(42, 211);
            context.getBindings(ID).putMember("array", array);
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
            context.eval(ID, arrayLengthExpr + " = 5");
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
            context.eval(ID, arrayLengthExpr + " = 1");
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
        }
    }

    @Test
    public void testUnsupportedStrict() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.STRICT_NAME, "true").build()) {
            ProxyArray array = ProxyArray.fromArray(42, 211);
            context.getBindings(ID).putMember("array", array);
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
            assertTrue(context.eval(ID, "try { " + arrayLengthExpr + " = 5; false; } catch (e) { e instanceof TypeError }").asBoolean());
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
            assertTrue(context.eval(ID, "try { " + arrayLengthExpr + " = 1; false; } catch (e) { e instanceof TypeError }").asBoolean());
            assertEquals(context.eval(ID, arrayLengthExpr).asLong(), 2);
        }
    }

    private static final class MapBasedProxyArray implements ProxyArray {
        private Map<Long, Object> map = new HashMap<>();

        @Override
        public Object get(long index) {
            return map.get(index);
        }

        @Override
        public void set(long index, Value value) {
            map.put(index, value);
        }

        @Override
        public boolean remove(long index) {
            return (map.remove(index) != null);
        }

        @Override
        public long getSize() {
            long maxIndex = -1;
            for (long index : map.keySet()) {
                maxIndex = Math.max(maxIndex, index);
            }
            return maxIndex + 1;
        }

    }

}
