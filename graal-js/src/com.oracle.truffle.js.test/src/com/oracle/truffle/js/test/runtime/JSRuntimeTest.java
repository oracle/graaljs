/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.binary.JSEqualNode;
import com.oracle.truffle.js.nodes.binary.JSIdenticalNode;
import com.oracle.truffle.js.nodes.function.FunctionBodyNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBigIntObject;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSBooleanObject;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSNumberObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSStringObject;
import com.oracle.truffle.js.runtime.builtins.JSSymbolObject;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignDynamicObject;
import com.oracle.truffle.js.test.polyglot.ForeignNull;
import com.oracle.truffle.js.test.polyglot.ForeignTestMap;

public class JSRuntimeTest extends JSTest {

    private static final double BIGDELTA = 0.00001;

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

    private DynamicObject createOrdinaryObject() {
        return JSOrdinary.create(testHelper.getJSContext(), testHelper.getRealm());
    }

    @Test
    public void testEqual() {
        DynamicObject date = JSDate.create(testHelper.getJSContext(), testHelper.getRealm(), 42);
        assertTrue(JSRuntime.equal(date, JSDate.toString(42, testHelper.getRealm())));
        assertFalse(JSRuntime.equal(Null.instance, false));
        assertFalse(JSRuntime.equal(0, Null.instance));
        assertFalse(JSRuntime.equal(true, Undefined.instance));
        assertFalse(JSRuntime.equal(Undefined.instance, 1));
        assertTrue(JSRuntime.equal(Float.MAX_VALUE, Float.MAX_VALUE));

        DynamicObject obj = createOrdinaryObject();
        assertFalse(JSRuntime.equal(obj, Null.instance));
        assertFalse(JSRuntime.equal(obj, Undefined.instance));
        assertFalse(JSRuntime.equal(Null.instance, obj));
        assertFalse(JSRuntime.equal(Undefined.instance, obj));
        assertTrue(JSRuntime.equal(obj, obj));
        assertFalse(JSRuntime.equal(obj, createOrdinaryObject()));

        BigInt bi1a = new BigInt(new BigInteger("0123456789"));
        BigInt bi1b = new BigInt(new BigInteger("0123456789"));
        BigInt bi2 = new BigInt(new BigInteger("9876543210"));
        assertTrue(JSRuntime.equal(bi1a, bi1b));
        assertFalse(JSRuntime.equal(bi1a, bi2));
    }

    @Test
    public void testIdentical() {
        assertTrue(JSRuntime.identical(new BigInteger("9876543210"), new BigInteger("9876543210")));
        TruffleLanguage.Env env = testHelper.getRealm().getEnv();
        assertTrue(JSRuntime.identical(env.asGuestValue(BigInteger.ONE), env.asGuestValue(BigInteger.ONE)));
    }

    @Test
    public void testNumberToStringWorksForSafeInteger() {
        assertEquals("42", JSRuntime.numberToString(SafeInteger.valueOf(42)));
    }

    @Test
    public void testQuote() {
        char char6 = 6;
        char char30 = 30;
        assertEquals("\"aA1_\\u0006\\u001e\\b\\f\\n\\r\\t\\\\\\\"\"", JSRuntime.quote("aA1_" + char6 + char30 + "\b\f\n\r\t\\\""));
    }

    @Test
    public void testImportValue() {
        testHelper.getJSContext(); // initialize JSContext

        assertEquals(42, JSRuntime.importValue(42));
        assertEquals("42", JSRuntime.importValue("42"));
        assertEquals(true, JSRuntime.importValue(true));
        assertEquals("X", JSRuntime.importValue('X'));

        // same for now, might not hold eternally
        assertSame(42, JSRuntime.importValue((byte) 42));
        assertSame(42, JSRuntime.importValue((short) 42));

        assertSame(42, JSRuntime.importValue(42L));
        assertEquals(Long.MAX_VALUE, JSRuntime.importValue(Long.MAX_VALUE));
        assertEquals(42.0, (double) JSRuntime.importValue((float) 42), BIGDELTA);

        try {
            JSRuntime.importValue(new Object());
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("not supported in JavaScript"));
        }
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
        Object[] values = createValues(testHelper.getRealm());

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

    private static Object[] createValues(JSRealm realm) {
        JSContext ctx = realm.getContext();
        return new Object[]{0, 1,
                        true, false,
                        0.5,
                        "foo",
                        Symbol.SYMBOL_MATCH,
                        Null.instance,
                        Undefined.instance,
                        JSString.create(ctx, realm, "hallo"),
                        JSNumber.create(ctx, realm, 4711),
                        JSBoolean.create(ctx, realm, true),
                        JSOrdinary.create(ctx, realm),
                        JSProxy.create(ctx, realm, JSOrdinary.create(ctx, realm), JSOrdinary.create(ctx, realm)),
                        JSBigInt.create(ctx, realm, BigInt.ZERO),
                        new ForeignTestMap()};
    }

    @Test
    public void testIdenticalRuntimeAndNode() {
        JSIdenticalNode node = adopt(JSIdenticalNode.createStrictEqualityComparison());
        Object[] values = createValues(testHelper.getRealm());

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
        Object[] values = createValues(testHelper.getRealm());

        for (int i = 0; i < values.length; i++) {
            Object v1 = values[i];
            String r1 = JSRuntime.typeof(v1);
            String r2 = node.executeString(v1);
            assertTrue("wrong outcode of typeof for i=" + i, r1.equals(r2));
        }
    }

    @Test
    public void testSafeToStringCollections() {
        DynamicObject map = JSMap.create(testHelper.getJSContext(), testHelper.getRealm());
        JSMap.getInternalMap(map).put("foo", "bar");
        assertEquals("Map(1){\"foo\" => \"bar\"}", JSRuntime.safeToString(map));

        DynamicObject set = JSSet.create(testHelper.getJSContext(), testHelper.getRealm());
        JSSet.getInternalSet(set).put("foo", "UNUSED");
        assertEquals("Set(1){\"foo\"}", JSRuntime.safeToString(set));
    }

    @Test
    public void testIsArrayIndex() {
        // Boxed Integer
        assertFalse(JSRuntime.isArrayIndex(Integer.valueOf(-1)));
        assertTrue(JSRuntime.isArrayIndex(Integer.valueOf(0)));
        assertTrue(JSRuntime.isArrayIndex(Integer.valueOf(Integer.MAX_VALUE)));
        // Boxed Double
        assertFalse(JSRuntime.isArrayIndex(Double.valueOf(-1)));
        assertTrue(JSRuntime.isArrayIndex(Double.valueOf(0)));
        assertTrue(JSRuntime.isArrayIndex(Double.valueOf(4294967294L)));
        assertFalse(JSRuntime.isArrayIndex(Double.valueOf(4294967295L)));
        // Boxed Long
        assertFalse(JSRuntime.isArrayIndex(Long.valueOf(-1)));
        assertTrue(JSRuntime.isArrayIndex(Long.valueOf(0)));
        assertTrue(JSRuntime.isArrayIndex(Long.valueOf(4294967294L)));
        assertFalse(JSRuntime.isArrayIndex(Long.valueOf(4294967295L)));
        // String
        assertFalse(JSRuntime.isArrayIndex("-1"));
        assertTrue(JSRuntime.isArrayIndex("0"));
        assertFalse(JSRuntime.isArrayIndex((Object) "-1"));
        assertTrue(JSRuntime.isArrayIndex((Object) "0"));
        assertTrue(JSRuntime.isArrayIndex((Object) "4294967294"));
        assertFalse(JSRuntime.isArrayIndex((Object) "4294967295"));
        assertFalse(JSRuntime.isArrayIndex((Object) "99999999999999999999999"));
        assertFalse(JSRuntime.isArrayIndex((Object) "NaN"));
        assertFalse(JSRuntime.isArrayIndex(null));
    }

    @Test
    public void testIsPrototypeOf() {
        JSContext ctx = testHelper.getJSContext();
        DynamicObject parent1 = createOrdinaryObject();
        DynamicObject parent2 = createOrdinaryObject();
        DynamicObject child1 = JSOrdinary.createWithPrototype(parent1, ctx);
        DynamicObject grandchild1 = JSOrdinary.createWithPrototype(child1, ctx);

        assertFalse(JSRuntime.isPrototypeOf(parent1, parent2));
        assertFalse(JSRuntime.isPrototypeOf(parent1, parent1));
        assertFalse(JSRuntime.isPrototypeOf(parent1, grandchild1));
        assertFalse(JSRuntime.isPrototypeOf(child1, grandchild1));
        assertFalse(JSRuntime.isPrototypeOf(grandchild1, grandchild1));

        assertTrue(JSRuntime.isPrototypeOf(child1, parent1));
        assertTrue(JSRuntime.isPrototypeOf(grandchild1, child1));
        assertTrue(JSRuntime.isPrototypeOf(grandchild1, parent1));
    }

    @Test
    public void testToPropertyKey() {
        // no conversion necessary
        assertTrue(JSRuntime.isPropertyKey(JSRuntime.toPropertyKey("test")));
        assertTrue(JSRuntime.isPropertyKey(JSRuntime.toPropertyKey(Symbol.SYMBOL_SEARCH)));

        // conversion necessary
        assertTrue(JSRuntime.isPropertyKey(JSRuntime.toPropertyKey(1)));
        assertTrue(JSRuntime.isPropertyKey(JSRuntime.toPropertyKey(true)));
        assertTrue(JSRuntime.isPropertyKey(JSRuntime.toPropertyKey(createOrdinaryObject())));
    }

    @Test
    public void testCall() {
        JSContext ctx = testHelper.getJSContext();
        DynamicObject thisObj = createOrdinaryObject();
        Object[] defaultArgs = new Object[]{"foo", 42, false};

        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        DynamicObject fnObj = JSFunction.create(realm, JSFunctionData.createCallOnly(ctx, Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(ctx.getLanguage(), null, null) {
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                return "" + JSArguments.getUserArgument(args, 0) + JSArguments.getUserArgument(args, 1) + JSArguments.getUserArgument(args, 2);
            }
        }), 0, "test"));

        assertEquals("foo42false", JSRuntime.call(fnObj, thisObj, defaultArgs));
        assertEquals("foo42false", JSRuntime.call(JSProxy.create(ctx, realm, fnObj, createOrdinaryObject()), thisObj, defaultArgs));
    }

    @Test
    public void testConstruct() {
        JSContext ctx = testHelper.getJSContext();
        DynamicObject arrayCtrFn = JavaScriptLanguage.getCurrentJSRealm().getArrayConstructor();
        Object result = JSRuntime.construct(arrayCtrFn, new Object[]{10});
        assertTrue(JSArray.isJSArray(result));
        assertEquals(10, JSArray.arrayGetLength((DynamicObject) result));

        result = JSRuntime.construct(JSProxy.create(ctx, testHelper.getRealm(), arrayCtrFn, createOrdinaryObject()), new Object[]{10});
        assertTrue(JSArray.isJSArray(result));
        assertEquals(10, JSArray.arrayGetLength((DynamicObject) result));

        try {
            JSRuntime.construct(createOrdinaryObject(), new Object[]{10});
            assertTrue(false);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("not a function"));
        }
    }

    @Test
    public void testNodeToString() {
        ScriptNode scriptNode = testHelper.parse("1+2");
        Node node = scriptNode.getRootNode();
        FunctionRootNode frn = (FunctionRootNode) node;
        FunctionBodyNode fbn = (FunctionBodyNode) frn.getBody();
        JavaScriptNode jsnode = fbn.getBody();
        String str = jsnode.toString();
        assertTrue(str.contains("DualNode"));
        assertTrue(str.contains(":program"));
    }

    @Test
    public void testToLength() {
        // toLength(double)
        assertTrue(JSRuntime.toLength(-3.14) == 0);
        assertTrue(JSRuntime.toLength(JSRuntime.MAX_SAFE_INTEGER * 2) == JSRuntime.MAX_SAFE_INTEGER);
        assertTrue(JSRuntime.toLength(Math.PI) == Math.PI);

        // toLength(int)
        assertTrue(JSRuntime.toLength(-3) == 0);
        assertTrue(JSRuntime.toLength(42) == 42);
    }

    @Test
    public void testLength() {
        testHelper.getJSContext(); // intializes Context

        // lengthIntl(CharSequence)
        assertEquals(3, JSRuntime.length("ABC"));
        assertEquals(42, JSRuntime.length(new TestCharSequence()));
        assertEquals(40, JSRuntime.length(createLazyString()));
    }

    private static CharSequence createLazyString() {
        return JSLazyString.create("01234567890123456789", "01234567890123456789");
    }

    @Test
    public void testCharAt() {
        testHelper.getJSContext(); // initializes Context

        // charAt(CharSequence, int)
        assertEquals('A', JSRuntime.charAt("ABC", 0));
        assertEquals('A', JSRuntime.charAt(new TestCharSequence(), 0));
        assertEquals('0', JSRuntime.charAt(createLazyString(), 0));
    }

    private static class TestCharSequence implements CharSequence {
        @Override
        public int length() {
            return 42;
        }

        @Override
        public char charAt(int index) {
            return 'A';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return null;
        }
    }

    @Test
    public void testToUInt8() {
        // toUInt8(Object)
        assertTrue(JSRuntime.toUInt8((Object) 3) == 3);
        assertTrue(JSRuntime.toUInt8((Object) 3.14) == 3);
        assertTrue(JSRuntime.toUInt8((Object) Double.POSITIVE_INFINITY) == 0);
    }

    @Test
    public void testToInt8() {
        // toInt8(Object)
        assertTrue(JSRuntime.toInt8((Object) 3) == 3);
        assertTrue(JSRuntime.toInt8((Object) 3.14) == 3);
        assertTrue(JSRuntime.toInt8((Object) Double.POSITIVE_INFINITY) == 0);
    }

    @Test
    public void testToUInt16() {
        // toUInt16(Object)
        assertTrue(JSRuntime.toUInt16((Object) 3) == 3);
        assertTrue(JSRuntime.toUInt16((Object) 3.14) == 3);
        assertTrue(JSRuntime.toUInt16((Object) Double.POSITIVE_INFINITY) == 0);
    }

    @Test
    public void testToInt16() {
        // toInt16(Object)
        assertTrue(JSRuntime.toInt16((Object) 3) == 3);
        assertTrue(JSRuntime.toInt16((Object) 3.14) == 3);
        assertTrue(JSRuntime.toInt16((Object) Double.POSITIVE_INFINITY) == 0);
    }

    @Test
    public void testToInt32() {
        // toInt32(Object)
        assertTrue(JSRuntime.toInt32((Object) 3) == 3);
        assertTrue(JSRuntime.toInt32((Object) 3.14) == 3);
        assertTrue(JSRuntime.toInt32((Object) 3L) == 3);
        assertTrue(JSRuntime.toInt32((Object) Double.POSITIVE_INFINITY) == 0);
    }

    @Test
    public void testToObject() {
        JSContext ctx = testHelper.getJSContext();

        assertThrowsTypeError(() -> JSRuntime.toObject(ctx, Null.instance));
        assertThrowsTypeError(() -> JSRuntime.toObject(ctx, Undefined.instance));

        assertTrue(JSRuntime.toObject(ctx, true) instanceof JSBooleanObject);
        assertTrue(JSRuntime.toObject(ctx, "String") instanceof JSStringObject);
        assertTrue(JSRuntime.toObject(ctx, Math.PI) instanceof JSNumberObject);
        assertTrue(JSRuntime.toObject(ctx, Symbol.create("sym")) instanceof JSSymbolObject);
        assertTrue(JSRuntime.toObject(ctx, BigInt.valueOf(1)) instanceof JSBigIntObject);

        Object object = createOrdinaryObject();
        assertSame(object, JSRuntime.toObject(ctx, object));

        object = new ForeignDynamicObject();
        assertSame(object, JSRuntime.toObject(ctx, object));

        assertThrowsTypeError(() -> JSRuntime.toObject(ctx, new ForeignNull()));

        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew(false)) instanceof JSBooleanObject);
        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew(42)) instanceof JSNumberObject);
        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew((byte) 42)) instanceof JSNumberObject);
        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew(Math.E)) instanceof JSNumberObject);
        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew((float) Math.E)) instanceof JSNumberObject);
        assertTrue(JSRuntime.toObject(ctx, ForeignBoxedObject.createNew("abc")) instanceof JSStringObject);
    }

    @Test
    public void testToStringIsString() {
        testHelper.getJSContext(); // initialize JSContext

        // toStringIsString(Object)
        assertEquals("ABC", JSRuntime.toStringIsString("ABC"));
        assertEquals(40, JSRuntime.toStringIsString(createLazyString()).length());
    }

    @Test
    public void testTrimJSWhiteSpace() {
        // trimJSWhiteSpace(String)
        assertEquals("A", JSRuntime.trimJSWhiteSpace(" A "));
        assertEquals("A", JSRuntime.trimJSWhiteSpace("A "));
        assertEquals("A", JSRuntime.trimJSWhiteSpace(" A"));
        assertEquals("A", JSRuntime.trimJSWhiteSpace("A"));
        assertEquals("A", JSRuntime.trimJSWhiteSpace(" A "));
        assertEquals("AB", JSRuntime.trimJSWhiteSpace("AB  "));
        assertEquals("AB", JSRuntime.trimJSWhiteSpace("  AB"));
        assertEquals("AB", JSRuntime.trimJSWhiteSpace("AB"));
        assertEquals("", JSRuntime.trimJSWhiteSpace("  "));
        assertEquals("", JSRuntime.trimJSWhiteSpace(""));
    }

    @Test
    public void testIntValue() {
        // intValue(Number)
        assertEquals(42, JSRuntime.intValue(42));
        assertEquals(42, JSRuntime.intValue(42.3));
        assertEquals(42, JSRuntime.intValue(42L));
    }

    @Test
    public void testFloatValue() {
        // floatValue(Number)
        assertEquals(42, JSRuntime.floatValue(42), BIGDELTA);
        assertEquals(42.3, JSRuntime.floatValue(42.3), BIGDELTA);
        assertEquals(42, JSRuntime.floatValue(42L), BIGDELTA);
    }

    @Test
    public void testIsNegativeZero() {
        assertFalse(JSRuntime.isNegativeZero(0.0));
        assertTrue(JSRuntime.isNegativeZero(-0.0));
    }

    @Test
    public void testExportValue() {
        testHelper.getJSContext(); // initialize JSContext

        // exportValue(Object)
        assertEquals(42.0, (double) JSRuntime.exportValue(SafeInteger.valueOf(42)), BIGDELTA);

        Object exportedLazyString = JSRuntime.exportValue(createLazyString());
        assertTrue(exportedLazyString instanceof String);
        assertEquals(createLazyString().toString(), exportedLazyString);
    }

    private static void assertThrowsTypeError(Runnable runnable) {
        try {
            runnable.run();
            fail("TypeError expected");
        } catch (JSException ex) {
            assertSame(JSErrorType.TypeError, ex.getErrorType());
        }
    }

    @Test
    public void testRequireObjectCoercible() {
        JSContext context = testHelper.getJSContext();

        assertThrowsTypeError(() -> JSRuntime.requireObjectCoercible(Null.instance, context));
        assertThrowsTypeError(() -> JSRuntime.requireObjectCoercible(Undefined.instance, context));

        JSRuntime.requireObjectCoercible(true, context);
        JSRuntime.requireObjectCoercible(42, context);
        JSRuntime.requireObjectCoercible(Math.PI, context);
        JSRuntime.requireObjectCoercible(SafeInteger.valueOf(9876543210L), context);
        JSRuntime.requireObjectCoercible("foo", context);
        JSRuntime.requireObjectCoercible(JSLazyString.create("long left part", "long right part"), context);
        JSRuntime.requireObjectCoercible(Symbol.create("private"), context);
        JSRuntime.requireObjectCoercible(BigInt.valueOf(0), context);
        JSRuntime.requireObjectCoercible(createOrdinaryObject(), context);

        assertThrowsTypeError(() -> JSRuntime.requireObjectCoercible(new ForeignNull(), context));

        JSRuntime.requireObjectCoercible(ForeignBoxedObject.createNew(43), context);
        JSRuntime.requireObjectCoercible(ForeignBoxedObject.createNew(Math.E), context);
        JSRuntime.requireObjectCoercible(ForeignBoxedObject.createNew(false), context);
        JSRuntime.requireObjectCoercible(ForeignBoxedObject.createNew("bar"), context);
    }

}
