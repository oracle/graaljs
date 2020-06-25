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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignNull;

public class ObjectIdentityTest {

    @Test
    public void proxyObjectIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyObject proxy1 = ProxyObject.fromMap(Collections.singletonMap("key", 42));
            ProxyObject proxy2 = ProxyObject.fromMap(Collections.singletonMap("key", 42));

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertTrue(equals.execute(proxy1, proxy1).asBoolean());
            assertTrue(identical.execute(proxy1, proxy1).asBoolean());
            assertFalse(equals.execute(proxy1, proxy2).asBoolean());
            assertFalse(identical.execute(proxy1, proxy2).asBoolean());
        }
    }

    @Test
    public void mixedObjectIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyObject proxy = ProxyObject.fromMap(Collections.singletonMap("key", 42));
            Value jsobj = context.eval(ID, "({})");

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertFalse(equals.execute(proxy, jsobj).asBoolean());
            assertFalse(identical.execute(proxy, jsobj).asBoolean());
            assertFalse(equals.execute(jsobj, proxy).asBoolean());
            assertFalse(identical.execute(jsobj, proxy).asBoolean());

            assertTrue(equals.execute(proxy, proxy).asBoolean());
            assertTrue(identical.execute(proxy, proxy).asBoolean());
            assertTrue(equals.execute(jsobj, jsobj).asBoolean());
            assertTrue(identical.execute(jsobj, jsobj).asBoolean());
        }
    }

    @Test
    public void foreignNullIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object null1 = new ForeignNull();
            Object null2 = new ForeignNull();
            Value jsnull = context.eval(ID, "null");

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertTrue(equals.execute(null1, null1).asBoolean());
            assertTrue(identical.execute(null1, null1).asBoolean());
            assertTrue(equals.execute(null1, null2).asBoolean());
            assertTrue(identical.execute(null1, null2).asBoolean());

            assertTrue(equals.execute(null1, jsnull).asBoolean());
            assertTrue(identical.execute(null1, jsnull).asBoolean());
            assertTrue(equals.execute(jsnull, null1).asBoolean());
            assertTrue(identical.execute(jsnull, null1).asBoolean());
        }
    }

    @Test
    public void testForeignObjectWithoutIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object value1 = ForeignBoxedObject.createNew(42);
            Object value2 = ForeignBoxedObject.createNew(42);

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            // Value equality comparison
            assertTrue(equals.execute(value1, value1).asBoolean());
            assertTrue(equals.execute(value1, value2).asBoolean());

            // Always false because these values have no identity!
            assertFalse(identical.execute(value1, value1).asBoolean());
            assertFalse(identical.execute(value1, value2).asBoolean());
        }
    }

}
