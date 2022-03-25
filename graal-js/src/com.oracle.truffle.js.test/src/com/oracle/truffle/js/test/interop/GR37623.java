/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.fail;

import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignNull;

/**
 * Tests of foreign objects in Object.hasOwn() and {@code in} operator.
 */
public class GR37623 {

    @Test
    public void testProxyHashMap() {
        try (Context context = JSTest.newContextBuilder(ID).build()) {
            ProxyHashMap map = ProxyHashMap.from(Map.of("foo", 42));
            context.getBindings(ID).putMember("map", map);
            Value hasOwnResult = context.eval(ID, "Object.hasOwn(map, 'foo')");
            assertFalse(hasOwnResult.asBoolean());
            Value inResult = context.eval(ID, "'foo' in map");
            assertFalse(inResult.asBoolean());
        }
    }

    private static void testForeignPrimitive(Object primitive) {
        try (Context context = JSTest.newContextBuilder(ID).build()) {
            context.getBindings(ID).putMember("o", primitive);
            Value hasOwnResult = context.eval(ID, "Object.hasOwn(o, 'foo')");
            assertFalse(hasOwnResult.asBoolean());
            verifyTypeError(context, "'foo' in o");
        }
    }

    private static void verifyTypeError(Context context, String code) {
        try {
            context.eval(ID, code);
            fail("TypeError expected");
        } catch (PolyglotException pex) {
            assertTrue(pex.isGuestException());
            String message = pex.getMessage();
            assertTrue(message, message.startsWith("TypeError"));
        }
    }

    @Test
    public void testForeignString() {
        testForeignPrimitive(ForeignBoxedObject.createNew("foreignString"));
    }

    @Test
    public void testForeignBoolean() {
        testForeignPrimitive(ForeignBoxedObject.createNew(true));
    }

    @Test
    public void testForeignNumber() {
        testForeignPrimitive(ForeignBoxedObject.createNew(42));
    }

    @Test
    public void testForeignNull() {
        try (Context context = JSTest.newContextBuilder(ID).build()) {
            ForeignNull o = new ForeignNull();
            context.getBindings(ID).putMember("o", o);
            verifyTypeError(context, "Object.hasOwn(o, 'foo')");
            verifyTypeError(context, "'foo' in o");
        }
    }

}
