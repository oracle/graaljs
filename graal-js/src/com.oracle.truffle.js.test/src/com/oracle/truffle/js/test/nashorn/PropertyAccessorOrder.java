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
package com.oracle.truffle.js.test.nashorn;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

//see e.g. GR-26443
public class PropertyAccessorOrder {

    private static void testIntl(String sourceText, Object obj) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME, "true").allowAllAccess(true).build()) {
            context.getBindings("js").putMember("obj", obj);
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "property-accessor-test").buildLiteral());
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testGetter() {
        // expected order in nashorn-compat mode is: `getId`>`isId`>`id`

        // check property access
        testIntl("obj.id === 'field'", new TestField());
        testIntl("obj.id === 'getId'", new TestGetter());
        testIntl("obj.id === 'isId'", new TestChecker());
        testIntl("obj.id === 'isId'", new TestFieldAndChecker());
        testIntl("obj.id === 'getId'", new TestAll());

        // check calls
        testIntl("obj.id === 'field';", new TestFieldAndFunction());
        testIntl("obj.id() === 'function';", new TestFieldAndFunction());
        testIntl("obj.id() === 'function'", new TestAll());
    }

    @Test
    public void testSetter() {
        // expected order in nashorn-compat mode is: `setId`>`id`
        testIntl("obj.id = 'test'; obj.countSet===0 && obj.id === 'test';", new TestField());
        testIntl("obj.id = 'test'; obj.countSet===1 && obj.id === undefined;", new TestSetter());
        testIntl("obj.id = 'test'; obj.countSet===1 && obj.id === 'getId';", new TestAll());
    }

    public static class TestGetSet {
        public int countSet = 0;
    }

    public static class TestField extends TestGetSet {
        public String id = "field";
    }

    public static class TestFieldAndFunction extends TestGetSet {
        public String id = "field";

        public String id() {
            return "function";
        }
    }

    public static class TestGetter extends TestGetSet {
        public String getId() {
            return "getId";
        }
    }

    public static class TestSetter extends TestGetSet {
        public void setId(@SuppressWarnings("unused") Object value) {
            countSet++;
        }
    }

    public static class TestChecker extends TestGetSet {
        public String isId() {
            return "isId";
        }
    }

    public static class TestFieldAndChecker extends TestGetSet {
        public String id = "field";

        public String isId() {
            return "isId";
        }
    }

    public static class TestAll extends TestGetSet {
        public String id = "field";

        public String isId() {
            return "isId";
        }

        public String getId() {
            return "getId";
        }

        public void setId(@SuppressWarnings("unused") Object value) {
            countSet++;
        }

        public String id() {
            return "function";
        }
    }
}
