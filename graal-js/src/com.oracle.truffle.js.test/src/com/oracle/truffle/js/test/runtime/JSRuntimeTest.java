/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignTestMap;

public class JSRuntimeTest extends JSTest {

    @Override
    public void setup() {
        super.setup();
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void testEqual() {
        JSContext context = testHelper.getJSContext();
        DynamicObject date = JSDate.create(context, 42);
        assertTrue(JSRuntime.equal(date, JSDate.toString(42, context)));
        assertFalse(JSRuntime.equal(Null.instance, false));
        assertFalse(JSRuntime.equal(0, Null.instance));
        assertFalse(JSRuntime.equal(true, Undefined.instance));
        assertFalse(JSRuntime.equal(Undefined.instance, 1));
        assertTrue(JSRuntime.equal(Float.MAX_VALUE, Float.MAX_VALUE));
    }

    @Test
    public void testIdentical() {
        assertTrue(JSRuntime.identical(new BigInteger("9876543210"), new BigInteger("9876543210")));
        TruffleLanguage.Env env = testHelper.getRealm().getEnv();
        assertTrue(JSRuntime.identical(env.asGuestValue(BigInteger.ONE), env.asGuestValue(BigInteger.ONE)));
    }

    @Test
    public void testNumberToStringWorksForLargeInteger() {
        assertEquals("42", JSRuntime.numberToString(LargeInteger.valueOf(42)));
    }

    @Test
    public void testQuote() {
        char char6 = 6;
        char char30 = 30;
        assertEquals("\"aA1_\\u0006\\u001e\\b\\f\\n\\r\\t\\\\\\\"\"", JSRuntime.quote("aA1_" + char6 + char30 + "\b\f\n\r\t\\\""));
    }

    @Test
    public void testImportValue() {
        assertEquals(Null.instance, JSRuntime.importValue(null));

        assertEquals(42, JSRuntime.importValue(42));
        assertEquals("42", JSRuntime.importValue("42"));
        assertEquals(true, JSRuntime.importValue(true));
        assertEquals("X", JSRuntime.importValue('X'));

        // same for now, might not hold eternally
        assertSame(42, JSRuntime.importValue((byte) 42));
        assertSame(42, JSRuntime.importValue((short) 42));
    }

    @Test
    public void testIsIntegerIndex() {
        assertTrue(JSRuntime.isIntegerIndex(0L));
        assertTrue(JSRuntime.isIntegerIndex(1L));
        assertTrue(JSRuntime.isIntegerIndex(9007199254740990L));
        assertTrue(JSRuntime.isIntegerIndex(9007199254740991L));

        assertFalse(JSRuntime.isIntegerIndex(9007199254740992L));
        assertFalse(JSRuntime.isIntegerIndex(9007199254740993L));
    }

    private static <T extends JavaScriptNode> T adopt(T node) {
        assert node.isAdoptable();
        Truffle.getRuntime().createCallTarget(new RootNode(null) {
            @Child JavaScriptNode child = node;

            @Override
            public Object execute(VirtualFrame frame) {
                return child.execute(frame);
            }
        });
        return node;
    }

    @Test
    public void testEqualRuntimeAndNode() {
        JSEqualNode node = adopt(JSEqualNode.create());
        Object[] values = createValues(testHelper.getJSContext());

        for (int i = 0; i < values.length; i++) {
            Object v1 = values[i];
            for (int j = 0; j < values.length; j++) {
                Object v2 = values[j];
                boolean r1 = JSRuntime.equal(v1, v2);
                boolean r2 = node.executeBoolean(v1, v2);
                assertTrue("wrong outcode of equals for i=" + i + ", j=" + j, r1 == r2);
            }
        }
    }

    private static Object[] createValues(JSContext ctx) {
        return new Object[]{0, 1, true, false, 0.5, "foo", Symbol.SYMBOL_MATCH, Null.instance, Undefined.instance, JSString.create(ctx, "hallo"), JSNumber.create(ctx, 4711),
                        JSBoolean.create(ctx, true), JSUserObject.create(ctx), JSProxy.create(ctx, JSUserObject.create(ctx), JSUserObject.create(ctx)), JSBigInt.create(ctx, BigInt.ZERO),
                        new ForeignTestMap()};
    }

    @Test
    public void testIdenticalRuntimeAndNode() {
        JSIdenticalNode node = adopt(JSIdenticalNode.createStrictEqualityComparison());
        Object[] values = createValues(testHelper.getJSContext());

        for (int i = 0; i < values.length; i++) {
            Object v1 = values[i];
            for (int j = 0; j < values.length; j++) {
                Object v2 = values[j];
                boolean r1 = JSRuntime.identical(v1, v2);
                boolean r2 = node.executeBoolean(v1, v2);
                assertTrue("wrong outcode of identical for i=" + i + ", j=" + j, r1 == r2);
            }
        }
    }

    @Test
    public void testTypeofRuntimeAndNode() {
        TypeOfNode node = adopt(TypeOfNode.create());
        Object[] values = createValues(testHelper.getJSContext());

        for (int i = 0; i < values.length; i++) {
            Object v1 = values[i];
            String r1 = JSRuntime.typeof(v1);
            String r2 = node.executeString(v1);
            assertTrue("wrong outcode of typeof for i=" + i, r1.equals(r2));
        }
    }
}
