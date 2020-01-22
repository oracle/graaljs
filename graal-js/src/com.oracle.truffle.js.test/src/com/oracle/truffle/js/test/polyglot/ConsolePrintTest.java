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
package com.oracle.truffle.js.test.polyglot;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;

/**
 * Test the toString conversion when printing to the console (or for a debugger, the inspector,
 * etc.). Especially for objects.
 *
 */
public class ConsolePrintTest extends JSTest {

    private String runInteractive(String sourceCode) {
        return runInteractive(sourceCode, Collections.emptyMap());
    }

    private String runInteractive(String sourceCode, Map<String, Object> bindings) {
        return testHelper.runToString(sourceCode, true, bindings).trim();
    }

    private static String empty(int count) {
        if (count == 0) {
            return "empty";
        }
        return "empty \u00d7 " + count;
    }

    @Test
    public void testObject() {
        String result = runInteractive("var obj = { a: \"foo\", b: true, c: { dummy: false }, d: 3.1415}; obj;");
        assertEquals("{a: \"foo\", b: true, c: {dummy: false}, d: 3.1415}", result);

        // "indexed" property after length
        result = runInteractive("var s = Object('abc'); s[5] = 'foo'; s;");
        assertEquals("String{5: \"foo\", [[PrimitiveValue]]: \"abc\"}", result);
    }

    @Test
    public void testStringObject() {
        String result = runInteractive("var obj = new String(\"abc\"); obj.x=1; obj;");
        // note: we omit the "array" indices and the length
        assertEquals("String{x: 1, [[PrimitiveValue]]: \"abc\"}", result);
    }

    @Test
    public void testNumberObject() {
        String result = runInteractive("var obj = new Number(1234.5); obj.x=1; obj;");
        assertEquals("Number{x: 1, [[PrimitiveValue]]: 1234.5}", result);
    }

    @Test
    public void testBooleanObject() {
        String result = runInteractive("var obj = new Boolean(true); obj.x=1; obj;");
        assertEquals("Boolean{x: 1, [[PrimitiveValue]]: true}", result);
    }

    @Test
    public void testArrayObject() {
        String result = runInteractive("var obj = [1,2,3]; obj.x=1; obj;");
        assertEquals("(3)[1, 2, 3, x: 1]", result);
    }

    @Test
    public void testArrayHoles() {
        String result = runInteractive("var a = new Array(10); a[3] = true; a[5] = false; a.x=1; a;");
        assertEquals("(10)[" + empty(3) + ", true, empty, false, " + empty(4) + ", x: 1]", result);

        result = runInteractive("new Array(3);");
        assertEquals("(3)[" + empty(3) + "]", result);

        result = runInteractive("var a = new Array(3); a[0] = 42; a;");
        assertEquals("(3)[42, " + empty(2) + "]", result);
    }

    @Test
    public void testMaxPrintProperties() {
        String result = runInteractive("var a = new Array(100); for (i=0;i<a.length;i+=2) { a[i] = true; } a.x=1; a;");
        assertEquals("(100)[true, empty, true, empty, true, empty, true, empty, true, empty, true, empty, true, empty, true, empty, true, empty, true, empty, ...]", result);
    }

    @Test
    public void testForeignArrayHoles() {
        String result = runInteractive("var a = new Array(10); a[3] = true; a[5] = false; a.x=1; a;");
        assertEquals("(10)[" + empty(3) + ", true, empty, false, " + empty(4) + ", x: 1]", result);
    }

    @Test
    public void testFunctionObject() {
        Object result = testHelper.runNoPolyglot("function f(a,b) { return a+b; }; f.x=1; f;");
        testHelper.enterContext();
        String converted = JSRuntime.safeToString(result);
        testHelper.leaveContext();
        assertEquals("function f(a,b) { return a+b; }", converted);
    }

    @Test
    public void testInternalFunction() {
        Object result = testHelper.runNoPolyglot("Promise;");
        testHelper.enterContext();
        String converted = JSRuntime.safeToString(result);
        testHelper.leaveContext();
        assertEquals("function Promise() { [native code] }", converted);
    }

    @Test
    public void testCallableProxyObject() {
        String result = runInteractive("var f = function (a) { return a; }; var p = new Proxy(f,{}); p.x=1; p;");
        assertEquals("Proxy(function (a) { return a; }, {})", result);
    }

    @Test
    public void testProxy() {
        String result = runInteractive("var obj = { a: \"foo\" }; var p = new Proxy(obj, {}); p.x=true; p;");
        assertEquals("Proxy({a: \"foo\", x: true}, {})", result);
    }

    @Test
    public void testDate() {
        String result = runInteractive("var date = new Date(0); date.x=1; date;");
        assertEquals("1970-01-01T00:00:00.000Z", result);
    }

    @Test
    public void testError() {
        String result = runInteractive("var e = new Error(\"fail\"); e.x=1; e;");
        assertEquals("Error: fail", result);

        result = runInteractive("var e = new TypeError(\"fail\"); e.x=1; e;");
        assertEquals("TypeError: fail", result);
    }

    @Test
    public void testRegExp() {
        String result = runInteractive("var r = new RegExp(\"a|b\",\"g\"); r.x=1; r;");
        assertEquals("/a|b/g", result);
    }

    @Test
    public void testArrayBuffer() {
        String result = runInteractive("var buffer = new ArrayBuffer(16); buffer.x=1; buffer;");
        assertEquals("ArrayBuffer{x: 1}", result);
    }

    @Test
    public void testTypedArray() {
        String result = runInteractive("var buffer = new ArrayBuffer(16); var int32View = new Int32Array(buffer); buffer.x=1; int32View.y=2; int32View;");
        assertEquals("Int32Array(4)[0, 0, 0, 0, y: 2]", result);
    }

    @Test
    public void testDataView() {
        String result = runInteractive("var view = new DataView(new ArrayBuffer(1)); view.x=1; view;");
        assertEquals("DataView{x: 1}", result);
    }

    @Test
    public void testMap() {
        String result = runInteractive("var m = new Map(); m.set(\"foo\",42); m.x=1; m;");
        assertEquals("Map(1){\"foo\" => 42}", result);
        assertEquals("Map(1){undefined => undefined}", runInteractive("new Map().set(undefined, undefined)"));
        assertEquals("Map(1){null => null}", runInteractive("new Map().set(null, null)"));
        assertEquals("Map(1){1n => 2n}", runInteractive("new Map().set(1n, 2n)"));
    }

    @Test
    public void testSet() {
        String result = runInteractive("var s = new Set(); s.add(\"foo\"); s.x=1; s;");
        assertEquals("Set(1){\"foo\"}", result);
    }

    @Test
    public void testWeakMap() {
        String result = runInteractive("var key = {}; var m = new WeakMap(); m.set(key,42); m.x=1; m;");
        assertEquals("WeakMap", result);
    }

    @Test
    public void testWeakSet() {
        String result = runInteractive("var key = {}; var s = new WeakSet(); s.add(key); s.x=1; s;");
        assertEquals("WeakSet", result);
    }

    @Test
    public void testPromisePending() {
        String result = runInteractive("var promise = new Promise(function(){}); promise.x = 1; promise;");
        assertEquals("Promise{x: 1, [[PromiseStatus]]: \"pending\", [[PromiseValue]]: undefined}", result);
    }

    @Test
    public void testPromiseResolved() {
        String result = runInteractive("Promise.resolve(42);");
        assertEquals("Promise{[[PromiseStatus]]: \"resolved\", [[PromiseValue]]: 42}", result);
    }

    @Test
    public void testPromiseRejected() {
        String result = runInteractive("Promise.reject('error');");
        assertEquals("Promise{[[PromiseStatus]]: \"rejected\", [[PromiseValue]]: \"error\"}", result);
    }

    @Test
    public void testZeros() {
        assertEquals("0", runInteractive("0"));
        assertEquals("-0", runInteractive("-0"));
        assertEquals("(2)[0, -0]", runInteractive("[0,-0]"));
        assertEquals("{positive: 0, negative: -0}", runInteractive("({ positive: 0, negative: -0 })"));
    }

    @Test
    public void testBigInt() {
        assertEquals("1n", runInteractive("1n"));
        assertEquals("[1n]", runInteractive("[1n]"));
        assertEquals("{bigOne: 1n}", runInteractive("({ bigOne: 1n })"));
    }

    @Test
    public void testRLikeNumber() {
        assertEquals("[42]", runInteractive("rLikeNumber", Collections.singletonMap("rLikeNumber", new RLikeNumber(42))));
    }

    @Test
    public void testPointer() {
        assertEquals("Pointer[0xcafebabe]", runInteractive("pointer", Collections.singletonMap("pointer", new Pointer(0xcafebabeL))));
    }

    /**
     * Resembles the behavior of numbers in R (they are numbers, pointers and arrays).
     */
    @ExportLibrary(InteropLibrary.class)
    static final class RLikeNumber implements TruffleObject {
        private final double value;

        RLikeNumber(double value) {
            this.value = value;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        long getArraySize() {
            return 1;
        }

        @ExportMessage
        Object readArrayElement(long index) throws UnsupportedMessageException {
            if (index == 0) {
                return value;
            } else {
                throw UnsupportedMessageException.create();
            }
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index == 0;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isPointer() {
            return true;
        }

        @ExportMessage
        long asPointer() {
            return Double.doubleToLongBits(value);
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isNumber() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInByte() {
            return false;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInShort() {
            return false;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInInt() {
            return false;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInLong() {
            return false;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInFloat() {
            return false;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean fitsInDouble() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        byte asByte() throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        short asShort() throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        int asInt() throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        long asLong() throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        float asFloat() throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @ExportMessage
        double asDouble() {
            return value;
        }

    }

    @ExportLibrary(InteropLibrary.class)
    static final class Pointer implements TruffleObject {
        private final long address;

        Pointer(long address) {
            this.address = address;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean isPointer() {
            return true;
        }

        @ExportMessage
        long asPointer() {
            return address;
        }

    }

}
