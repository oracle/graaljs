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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.array.SparseArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantByteArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantEmptyArray;
import com.oracle.truffle.js.runtime.array.dyn.ConstantIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ContiguousDoubleArray;
import com.oracle.truffle.js.runtime.array.dyn.ContiguousIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ContiguousJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesIntArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.HolesObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedIntArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedJSObjectArray;
import com.oracle.truffle.js.runtime.array.dyn.ZeroBasedObjectArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper.ParsedFunction;

public class InternalArrayTest extends JSTest {

    public static final TruffleString START = Strings.constant("start");
    public static final TruffleString LOWER_BORDER_1 = Strings.constant("lowerBorder-1");
    public static final TruffleString LOWER_BORDER = Strings.constant("lowerBorder");
    public static final TruffleString MIDDLE = Strings.constant("middle");
    public static final TruffleString UPPER_BORDER = Strings.constant("upperBorder");
    public static final TruffleString UPPER_BORDER_1 = Strings.constant("upperBorder+1");
    public static final TruffleString END = Strings.constant("end");

    public static final TruffleString TEST = Strings.constant("test");
    public static final TruffleString FOO = Strings.constant("foo");

    @Test
    public void testSimpleArraySetTest2() {
        testHelper.enterContext();
        JSContext ctx = testHelper.getJSContext();
        ParsedFunction fn = testHelper.parseFirstFunction("function fn(a) { return a[1] = -2; }");
        var a = JSArray.createConstant(ctx, testHelper.getRealm(), new Object[]{1, 2, 3, 4, 5});
        Object result = fn.call(new Object[]{a});
        assertEquals(-2, result);
        assertEquals(-2, JSObject.get(a, 1));
        testHelper.leaveContext();
    }

    @Test
    public void testSimpleArraySetTest3() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; return a[1] = -2; } fn()");
        assertEquals(-2, result);
    }

    @Test
    public void testGetElementPrototypeHole() {
        assertEquals(-1, testHelper.run("Array.prototype[1] = -1; var x = [1,,1];  x[1]"));
    }

    @Test
    public void testGetElementPrototypeOutOfBounds1() {
        assertEquals(-1, testHelper.run("Array.prototype[3] = -1; var x = [1,1];  x[3]"));
    }

    @Test
    public void testGetElementPrototypeInBounds() {
        assertEquals(-1, testHelper.run("Array.prototype[1] = 1; var x = [,-1,];  x[1]"));
    }

    @Test
    public void testSimpleArraySetTest7() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; a[0] += 1; return a[0]; } fn()");
        assertEquals(2, result);
    }

    @Test
    public void testSimpleArraySetTest8() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; var i = 0; a[++i] += 1; return a[1]; } fn()");
        assertEquals(3, result);
    }

    @Test
    public void testSimpleArraySetTest9() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; var i = 0; a[i++] += 1; return a[0]; } fn()");
        assertEquals(2, result);
    }

    @Test
    public void testSimpleArraySetTest4() {
        var array = testHelper.runJSArray("function fn() { var a = [1,2,3,4,5]; a[1] = -2; return a; } fn()");
        assertArrayEquals(new Object[]{1, -2, 3, 4, 5}, JSArray.toArray(array));
        for (int i = 0; i < 5; i++) {
            assertEquals(i != 1 ? i + 1 : -2, JSObject.get(array, i));
        }
    }

    @Test
    public void testSimpleArraySetTest5() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; a[1] = -2; return a; } fn()[1]");
        assertEquals(-2, result);
    }

    @Test
    public void testSimpleArraySetTest6() {
        String source = "function fn() { var a = [1,2,3,4,5]; a[1]++; return -a[1]; } fn()";
        Object result = testHelper.run(source);
        assertEquals(-3, result);
    }

    @Test
    public void testArrayLength() {
        Object result = testHelper.run("function fn() { var a = [1,2,3,4,5]; return a.length; } fn()");
        assertEquals(5, result);
    }

    @Test
    public void testIntArrayDelete() {
        String script = "var a = [10,11,12,13,14,15]; delete a[1]; delete a[5]; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{10, Undefined.instance, 12, 13, 14, Undefined.instance}, arr);

        String script2 = "var s=''; for (var i in a) { s+= i.toString(); } s;";
        assertEquals("0234", testHelper.run(script2).toString());
    }

    @Test
    public void testArrayDeleteTransformSparse() {
        String script = "var a = [10,11,12]; delete a[1]; delete a[0]; delete a[2]; a.length==3 && a[0]===undefined && a[1]===undefined && a[2]===undefined;";
        assertTrue(testHelper.runBoolean(script));

        String script2 = "a[999] = true; a[777] = false; a[888] = undefined; var s=''; for (var i in a) { s+= i.toString(); } s;";
        assertEquals("777888999", testHelper.run(script2).toString());
    }

    @Test
    public void testObjectArrayDelete() {
        String script = "var a = ['test',11,12,13,14,15]; delete a[1]; delete a[5]; a; ";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{TEST, Undefined.instance, 12, 13, 14, Undefined.instance}, arr);

        String script2 = "var s=''; for (var i in a) { s+= i.toString(); } s;";
        assertEquals("0234", testHelper.run(script2).toString());
    }

    @Test
    public void testSparseArrayDelete() {
        String script = "var a = ['test',11,12,13,14,15]; a[10000] = true; delete a[10000]; delete a[5]; delete a[1]; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertEquals(10001, arr.length);
        assertEquals(TEST, arr[0]);
        assertEquals(Undefined.instance, arr[1]);
        assertEquals(12, arr[2]);
        assertEquals(13, arr[3]);
        assertEquals(14, arr[4]);
        assertEquals(Undefined.instance, arr[5]);
        assertEquals(Undefined.instance, arr[6]);
        assertEquals(Undefined.instance, arr[10000]);
    }

    @Test
    public void testObjectDeletedArray() {
        String script = "var a = ['test',11,12,13,14,15]; delete a[1]; for (var i=2;i<a.length;i++) { a[i] = i+20; } a[2]=true;a[5]='foo'; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{TEST, Undefined.instance, true, 23, 24, FOO}, arr);
    }

    @Test
    public void testIntArrayGrowing() {
        String script = "var a = []; a[3] = 1; a[2] = 2; a[4] = 3; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{Undefined.instance, Undefined.instance, 2, 1, 3}, arr);
    }

    @Test
    public void testIntArrayGrowingRightWithHoles() {
        String script = "var a = []; a[3] = 1; a[2] = 2; a[7] = 3; a;";
        var array = testHelper.runJSArray(script);
        assertEquals(HolesIntArray.class, array.getArrayType().getClass());
        Object[] arr = array.getArrayType().toArray(array);
        assertArrayEquals(new Object[]{Undefined.instance, Undefined.instance, 2, 1, Undefined.instance, Undefined.instance, Undefined.instance, 3}, arr);
    }

    @Test
    public void testIntArrayGrowingLeftWithHoles() {
        String script = "var a = []; a[7] = 1; a[8] = 2; a[3] = 3; a;";
        var array = testHelper.runJSArray(script);
        assertEquals(HolesIntArray.class, array.getArrayType().getClass());
        Object[] arr = array.getArrayType().toArray(array);
        assertArrayEquals(new Object[]{Undefined.instance, Undefined.instance, Undefined.instance, 3, Undefined.instance, Undefined.instance, Undefined.instance, 1, 2}, arr);
    }

    @Test
    public void testSparseArrayFromCrypto() {
        testHelper.enterContext();
        String script = "var BI_RC = new Array(); var rr,vv; rr = '0'.charCodeAt(0); for(vv = 0; vv <= 9; ++vv) BI_RC[rr++] = vv; rr = 'a'.charCodeAt(0); for(vv = 10; vv < 36; ++vv) BI_RC[rr++] = vv;" +
                        "rr = 'A'.charCodeAt(0); for(vv = 10; vv < 36; ++vv) BI_RC[rr++] = vv; BI_RC;";
        String result = String.valueOf(JSObject.toPrimitive(testHelper.runJSArray(script), JSToPrimitiveNode.Hint.String));
        assertEquals(",,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0,1,2,3,4,5,6,7,8,9,,,,,,,,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,,,,,,," +
                        "10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35", result);
        testHelper.leaveContext();
    }

    @Test
    public void testObjectArrayReduceLength() {
        String script = "var a = ['test',11]; a.length = 1; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{TEST}, arr);
    }

    @Test
    public void testObjectDeletedArrayReduceLength() {
        String script = "var a = ['test',11,12]; delete(a[1]); a.length = 2; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{TEST, Undefined.instance}, arr);
    }

    @Test
    public void testObjectArrayIncreaseLength() {
        String script = "var a = ['test',11]; a.length = 3; a.length===3 && a[0]==='test' && a[1]===11;";
        assertTrue(testHelper.runBoolean(script));
    }

    @Test
    public void testObjectDeletedArrayIncreaseLength() {
        String script = "var a = ['test',11,12]; delete(a[1]); a.length = 4; a.length===4 && a.toString()==='test,,12,';";
        assertTrue(testHelper.runBoolean(script));
    }

    @Test
    public void testArrayObjectShrinkAndGrow() {
        String script = "var o = {}; var a = [o,o,o]; a.length = 1; a.length = 5; a.length;";
        assertEquals(5, testHelper.run(script));
    }

    @Test
    public void testIntBaseOffset() {
        String script = "var a = new Array(5); var i; for (i=4;i>=0;i--) {a[i] = i; }; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{0, 1, 2, 3, 4}, arr);
    }

    @Test
    public void testObjectBaseOffset() {
        String script = "var o = {}; var a = new Array(5); var i; for (i=4;i>=0;i--) {a[i] = o; }; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertEquals(5, arr.length);
        assertTrue(arr[0] instanceof JSDynamicObject);
        assertTrue(arr[1] instanceof JSDynamicObject);
        assertTrue(arr[2] instanceof JSDynamicObject);
        assertTrue(arr[3] instanceof JSDynamicObject);
        assertTrue(arr[4] instanceof JSDynamicObject);
    }

    @Test
    public void testObjectConstructorWithSize() {
        // tests whether the ObjectArray(int capacity) sets the length correctly
        String script = "var o = {}; var a = [1,2]; a[0] = undefined; a[1] = undefined; a[0]=o; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertEquals(2, arr.length);
        assertTrue(arr[0] instanceof JSDynamicObject);
        assertEquals(Undefined.instance, arr[1]);
    }

    @Test
    public void testArrayTypeEmpty() {
        String script = "var a = new Array(10); Debug.arraytype(a);";
        assertEquals(ConstantEmptyArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeLazyByte() {
        String script = "var a = [127]; Debug.arraytype(a);";
        assertEquals(ConstantByteArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeInt() {
        String script = "var a = [128]; Debug.arraytype(a);";
        assertEquals(ConstantIntArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayIntBaseOffset() {
        String script = "var a = new Array(1000); var i; for (i=50;i>=20;i--) { a[i] = i; }; Debug.arraytype(a);";
        assertEquals(ContiguousIntArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeDouble() {
        String script = "var a = [1.1]; Debug.arraytype(a);";
        assertEquals(ConstantDoubleArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayDoubleBaseOffset() {
        String script = "var a = new Array(1000); var i; for (i=50;i>=20;i--) { a[i] = i+0.5; }; Debug.arraytype(a);";
        assertEquals(ContiguousDoubleArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeObject() {
        String script = "var o = {}; var a = [o]; Debug.arraytype(a);";
        assertEquals(ZeroBasedObjectArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeObjectFromInt() {
        String script = "var o = {}; var a = [1]; a[2] = o; Debug.arraytype(a);";
        assertEquals(HolesObjectArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeIntWithHoles() {
        String script = "var a = [1,2,3]; delete a[1]; Debug.arraytype(a);";
        assertEquals(HolesIntArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeSparse() {
        Assume.assumeTrue(JSConfig.MaxArrayHoleSize > 0);
        int secondIndex = JSConfig.MaxArrayHoleSize * 2;
        String script = "var o = {}; var a = new Array(10); a[0]=o; a[" + secondIndex + "]=" + secondIndex + "; Debug.arraytype(a);";
        assertEquals(SparseArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeObjectDeleted() {
        String script = "var o={}; var a = [o,o,o]; delete a[1]; Debug.arraytype(a);";
        assertEquals(HolesObjectArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeObjectDeletePost() {
        // test that deleting the last element of an ObjectArray does NOT transfer it to an
        // ObjectDeletedArray
        String script = "var o={}; var a = [o,o,o]; delete a[2]; Debug.arraytype(a);";
        assertEquals(HolesObjectArray.class.getSimpleName(), testHelper.run(script));
    }

    @Test
    public void testArrayTypeIntGrowsToHole() {
        // test that increasing the length of an IntArray does not transfer it to an
        // IntWithHolesArray
        String script = "var a = [0,1,2]; a.length=10; Debug.arraytype(a) + a.hasOwnProperty('2') + a.hasOwnProperty('3');";
        assertEquals(ZeroBasedIntArray.class.getSimpleName() + "true" + "false", testHelper.run(script));
    }

    @Test
    public void testArrayTypeIntStaysInt() {
        // test that increasing setting an int to array[array.length] (i.e. increasing length by 1)
        // does NOT transfer to HolesIntArray
        String script = "var a = [0,1,2]; a[3] = 3; Debug.arraytype(a) + a.hasOwnProperty('3') + a.hasOwnProperty('4');";
        assertEquals(ZeroBasedIntArray.class.getSimpleName() + "true" + "false", testHelper.run(script));
    }

    @Test
    public void testEvaluationOrder() {
        String script = "var a = [0,1,2,3,4]; var i=2; a[i] = a[++i]+10; a;";
        var result = testHelper.runJSArray(script);
        Object[] arr = result.getArrayType().toArray(result);
        assertArrayEquals(new Object[]{0, 1, 13, 3, 4}, arr);
    }

    @Test
    public void testNaN() {
        String script = "function testcase() { var _NaN = NaN; var a = new Array(\"NaN\",undefined,0,false,null,{toString:function (){return NaN}},\"false\",_NaN,NaN);" +
                        "if (a.indexOf(NaN) === -1)  /* NaN is equal to nothing, including itself. */ return true; else return false; } testcase()";
        assertEquals(Boolean.TRUE, testHelper.run(script));
    }

    @Test
    public void testStayContiguous() {
        Object result = testHelper.run("var a = Array(4); a[3] = 3; a[2] = 2; a[1] = 1; a[0] = 0; Debug.arraytype(a)");
        assertEquals(ZeroBasedIntArray.class.getSimpleName(), result);
    }

    @Test
    public void testStayZeroBased() {
        Object result = testHelper.run("var a = Array(4); a[0] = 0; a[1] = 1; a[2] = 2; a[3] = 3; Debug.arraytype(a)");
        assertEquals(ZeroBasedIntArray.class.getSimpleName(), result);
    }

    @Test
    public void testStayContiguos2() {
        Object result = testHelper.run("var a = Array(4); a[2] = 2; a[1] = 1; a[3] = 3; a[0] = 0; Debug.arraytype(a)");
        assertEquals(ZeroBasedIntArray.class.getSimpleName(), result);
    }

    @Test
    public void testTransformHolesToZeroBased() {
        Object result = testHelper.run("var a = []; a[2] = 42; a[0] = 42; a[1] = 42; a[1] = 42; Debug.arraytype(a)");
        assertEquals(ZeroBasedIntArray.class.getSimpleName(), result);
    }

    @Test
    public void testReduceLengthBelowIndexOffset() {
        var result = testHelper.runJSArray("var a = []; a[20] = 42; a.length = 10; a[15] = 666; a;");
        Object[] array = result.getArrayType().toArray(result);
        assertEquals(16, array.length);
        assertEquals(666, array[15]);
    }

    @Test
    public void testZeroBasedJSObjectArray() {
        Object result = testHelper.run("var o1 = {b:1}; var o2={b:2}; var a = []; a[0] = o1; a[1] = o2; Debug.arraytype(a)");
        assertEquals(ZeroBasedJSObjectArray.class.getSimpleName(), result);
    }

    @Test
    public void testContiguousJSObjectArray() {
        Object result = testHelper.run("var o1 = {b:1}; var o2={b:2}; var a = []; a[10] = o1; a[11] = o2; Debug.arraytype(a)");
        assertEquals(ContiguousJSObjectArray.class.getSimpleName(), result);
    }

    @Test
    public void testHolesJSObjectArray() {
        Object result = testHelper.run("var o1 = {b:1}; var o2={b:2}; var a = []; a[10] = o1; a[20] = o2; Debug.arraytype(a)");
        assertEquals(HolesJSObjectArray.class.getSimpleName(), result);
    }

    @Test
    public void testIntToHolesWithMinvalueArray() {
        // a non-holes int array can contain the hole-value as regular value.
        int hole = HolesIntArray.HOLE_VALUE + 1;
        assertTrue((Boolean) testHelper.run("var a = []; a[0] = " + hole + "-1; a[3] = 3; a[0] !== undefined;"));
        assertTrue((Boolean) testHelper.run("var b = [0,1,2," + hole + "-1,4,5]; b[3] !== undefined;"));
        assertTrue((Boolean) testHelper.run("var c = [1000]; for (var i=0;i<1000;i++) { c[i]=i; }; c[1000]=" + hole + "-1; c[1000] !== undefined;"));
    }

    @Test
    public void testLiteralWithVoid() {
        Object result = testHelper.run("var a = [,void 0]; a.length == 2 && a.hasOwnProperty(0) == false && a.hasOwnProperty(1) == true;");
        assertTrue((Boolean) result);
    }

    @Test
    public void testLengthMaxintCorner() {
        Object result = testHelper.run("var a = []; a[0x7fffffff]=1; a.length===0x80000000 && a[0x7fffffff]===1 && a[0] === undefined;");
        assertTrue((Boolean) result);
    }

    @Test
    public void testReadForInFromArguments() {
        Object result = testHelper.run("function func(a) { for (var i in arguments) { return arguments[i]; } }; func(\"a\",\"b\",\"c\") === \"a\";");
        assertTrue((Boolean) result);
    }

    @Test
    public void testReadForInFromArray() {
        Object result = testHelper.run("function func(a) { for (var i in a) { return a[i]; } }; func([\"a\",\"b\",\"c\"]) === \"a\";");
        assertTrue((Boolean) result);
    }

    @Test
    public void testStrictDelete() {
        testHelper.run("a = [];" +
                        "Object.defineProperty(a, 0, { value: 44, configurable: false });" +
                        "function sdelete() { 'use strict'; delete a[0]; }" +
                        "try { sdelete(); throw 'fail'; } catch (e) { if (!(e instanceof TypeError)) throw e; }");
    }

    @Test
    public void testWriteIndexTrue() {
        assertTrue((Boolean) testHelper.run("x = []; x[true] = 1; x.length === 0"));
    }

    @Test
    public void testWriteNegativeIndex() {
        assertTrue((Boolean) testHelper.run("x = []; x[-1] = 'a'; x[-1] === 'a'"));
    }

    @Test
    public void testStringIndexZero() {
        assertEquals(42, testHelper.run("x = [42,2,3]; x[0.0];"));
        assertEquals(42, testHelper.run("x = [42,2,3]; x[-0.0];"));
        assertEquals(42, testHelper.run("x = [42,2,3]; x[-0];"));
        assertEquals(42, testHelper.run("x = [42,2,3]; var minus0 = 0/-1.0; x[minus0];"));
    }

    @Test
    public void testStringIndexWithWhitespace() {
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x[' 1'] === undefined;"));
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x['1 '] === undefined;"));
    }

    @Test
    public void testStringIndexAlmostInteger() {
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x['1.0'] === undefined;"));
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x['+1'] === undefined;"));
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x['-0'] === undefined;"));
    }

    @Test
    public void testStringIndexLeadingZero() {
        assertTrue((Boolean) testHelper.run("x = [1,2,3]; x['01'] === undefined;"));
    }

    @Test
    public void testSparseArrayRemoveRange() {
        testHelper.enterContext();
        var object = JSArray.createSparseArray(testHelper.getJSContext(), testHelper.getRealm(), 1000);
        ScriptArray sparse = object.getArrayType();

        sparse.setElement(object, 100, START, false);
        sparse.setElement(object, 399, LOWER_BORDER_1, false);
        sparse.setElement(object, 400, LOWER_BORDER, false);
        sparse.setElement(object, 500, MIDDLE, false);
        sparse.setElement(object, 600, UPPER_BORDER, false);
        sparse.setElement(object, 601, UPPER_BORDER_1, false);
        sparse.setElement(object, 900, END, false);

        sparse = sparse.removeRange(object, 400, 601); // deleting 201 elements

        assertTrue(sparse instanceof SparseArray);

        assertTrue(sparse.hasElement(object, 100));
        assertEquals(START, sparse.getElement(object, 100));

        assertTrue(sparse.hasElement(object, 399));
        assertEquals(LOWER_BORDER_1, sparse.getElement(object, 399));

        assertTrue(sparse.hasElement(object, 400));
        assertEquals(UPPER_BORDER_1, sparse.getElement(object, 400));

        assertTrue(sparse.hasElement(object, 699));
        assertEquals(END, sparse.getElement(object, 699));
        testHelper.leaveContext();
    }

    @Test
    public void testSparseArrayAddRange() {
        testHelper.enterContext();
        var object = JSArray.createSparseArray(testHelper.getJSContext(), testHelper.getRealm(), 1000);
        ScriptArray sparse = object.getArrayType();

        sparse.setElement(object, 100, START, false);
        sparse.setElement(object, 399, LOWER_BORDER_1, false);
        sparse.setElement(object, 400, UPPER_BORDER_1, false);
        sparse.setElement(object, 699, END, false);

        ScriptArray result = sparse.addRange(object, 400, 201);

        assertTrue(result instanceof SparseArray);

        assertTrue(result.hasElement(object, 100));
        assertEquals(START, result.getElement(object, 100));

        assertTrue(result.hasElement(object, 399));
        assertEquals(LOWER_BORDER_1, result.getElement(object, 399));

        assertFalse(result.hasElement(object, 400));
        assertFalse(result.hasElement(object, 500));
        assertFalse(result.hasElement(object, 600));

        assertTrue(result.hasElement(object, 601));
        assertEquals(UPPER_BORDER_1, result.getElement(object, 601));

        assertTrue(result.hasElement(object, 900));
        assertEquals(END, result.getElement(object, 900));
        testHelper.leaveContext();
    }

    @Test
    public void testTypedArrayNegativeIndex() {
        assertEquals(Boolean.TRUE, testHelper.run("var a = new Int32Array(100); a[-10] = 42; a[-10] === undefined;"));
    }

    /**
     * Make sure typed array length isn't truncated to int32 range.
     */
    @Test
    public void testTypedArrayConstructorToIntegerConversion() {
        // This assertion only serves to document an invariant expected by this test.
        assert testHelper.getJSContext().getLanguageOptions().maxTypedArrayLength() <= Integer.MAX_VALUE;
        // We throw a RangeError if length is too big (> MaxTypedArrayLength).
        // If length were incorrectly cast to int32, we'd allocate an array with 4 elements.
        long longLongLength = 0x1_0000_0000L + 4; // (int) (bigIndex & 0xFFFFFFFFL) == 4
        Consumer<String> expectRangeError = inner -> assertEquals(Boolean.TRUE,
                        testHelper.run("var success = false; try { " + inner + " } catch (e) { success = e instanceof RangeError; } success;"));
        expectRangeError.accept("new Int8Array(" + longLongLength + ");");
        expectRangeError.accept("new Int8Array(new ArrayBuffer(16), " + longLongLength + ");");
        expectRangeError.accept("new Int8Array(new ArrayBuffer(16), 0, " + longLongLength + ");");

        expectRangeError.accept("new Int8Array(Infinity);");
        expectRangeError.accept("new Int8Array(-Infinity);");
        assertEquals(0, testHelper.run("var a = new Int32Array(NaN); a.length"));

        // ArrayBuffer
        expectRangeError.accept("new ArrayBuffer(" + longLongLength + ");");
    }
}
