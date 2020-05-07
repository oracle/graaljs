/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class TestMetaObject {

    @Test
    public void metaObjectTest() throws Exception {
        try (Context context = JSTest.newContextBuilder().build()) {
            // @formatter:off
            Value allJSDataTypes = context.eval("js",
                            "function Test() { };\n" +
                            "class TestClass { };\n" +
                            "var resultArr = [];\n" +
                            "resultArr.push([]);\n" +
                            "resultArr.push([1,2,[3,4]]);\n" +
                            "resultArr.push(true);\n" +
                            "resultArr.push(false);\n" +
                            "resultArr.push(0);\n" +
                            "resultArr.push(42);\n" +
                            "resultArr.push(42.42);\n" +
                            "resultArr.push(1000000000000000);\n" +
                            "resultArr.push('MyString');\n" +
                            "resultArr.push(function pow2(x) { return x*x; });\n" +
                            "resultArr.push(null);\n" +
                            "resultArr.push(Symbol());\n" +
                            "resultArr.push(Symbol('symbolic'));\n" +
                            "resultArr.push({});\n" +
                            "resultArr.push(new Date(0));\n" +
                            "resultArr.push(new Test());\n" +
                            "resultArr.push(new TestClass());\n" +
                            "resultArr.push(undefined);\n" +
                            "resultArr.push(new Proxy({a:2}, {}));\n" +
                            "resultArr.push(new Proxy(function(x) { return x*x; }, {}));\n" +
                            "resultArr;");
            // @formatter:on

            Value typeof = context.eval("js", "value => typeof value;");

            final String[] valueToStrings = {
                            "[]",
                            "(3)[1, 2, [3, 4]]",
                            "true",
                            "false",
                            "0",
                            "42",
                            "42.42",
                            "1000000000000000",
                            "MyString",
                            "function pow2(x) { return x*x; }",
                            "null",
                            "Symbol()",
                            "Symbol(symbolic)",
                            "{}",
                            "1970-01-01T00:00:00.000Z",
                            "{}",
                            "{}",
                            "undefined",
                            "Proxy({a: 2}, {})",
                            "Proxy(function(x) { return x*x; }, {})"
            };
            final String[] types = {
                            "object",
                            "object",
                            "boolean",
                            "boolean",
                            "number",
                            "number",
                            "number",
                            "number",
                            "string",
                            "function",
                            "object",
                            "symbol",
                            "symbol",
                            "object",
                            "object",
                            "object",
                            "object",
                            "undefined",
                            "object",
                            "function"
            };
            final String[] classNames = {
                            "Array",
                            "Array",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Function",
                            null,
                            null,
                            null,
                            "Object",
                            "Date",
                            "Test",
                            "TestClass",
                            null,
                            "Proxy",
                            "Proxy"
            };
            final String[] toStrings = {
                            "Array",
                            "Array",
                            "boolean",
                            "boolean",
                            "number",
                            "number",
                            "number",
                            "number",
                            "string",
                            "Function",
                            "null",
                            "symbol",
                            "symbol",
                            "Object",
                            "Date",
                            "Test",
                            "TestClass",
                            "undefined",
                            "Proxy",
                            "Proxy"
            };

            int n = toStrings.length;
            assertEquals(n, allJSDataTypes.getArraySize());
            for (int i = 0; i < n; i++) {
                Value value = allJSDataTypes.getArrayElement(i);
                String valueToString = value.toString();
                assertEquals(valueToStrings[i], valueToString);

                Value metaObject = value.getMetaObject();
                if (toStrings[i] != null) {
                    assertNotNull("meta object", metaObject);
                    assertEquals(toStrings[i], metaObject.getMetaSimpleName());
                    assertEquals(toStrings[i], metaObject.getMetaQualifiedName());
                } else {
                    assertNull("meta object", metaObject);
                }
                if (classNames[i] != null) {
                    assertNotNull("meta object", metaObject);
                    assertEquals(classNames[i], metaObject.getMetaSimpleName());
                    assertEquals(classNames[i], metaObject.getMetaQualifiedName());
                }

                String type = typeof.execute(value).asString();
                assertEquals("typeof " + valueToString, types[i], type);
            }
        }

    }
}
