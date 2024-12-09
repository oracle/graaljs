/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.junit.Test;

/**
 * Regression test of Object.assign(target, foreignArray).
 */
public class GR60380 {

    @Test
    public void testProxyArrayToArray() {
        try (Context context = Context.create("js")) {
            String value0 = "foo";
            String value1 = "bar";
            ProxyArray array = ProxyArray.fromArray(value0, value1);
            context.getBindings("js").putMember("array", array);
            Value result = context.eval("js", "Object.assign([], array)");
            assertTrue(result.getArraySize() == 2);
            assertEquals(value0, result.getArrayElement(0).asString());
            assertEquals(value1, result.getArrayElement(1).asString());
        }
    }

    @Test
    public void testProxyArrayToObject() {
        try (Context context = Context.create("js")) {
            String value0 = "foo";
            String value1 = "bar";
            ProxyArray array = ProxyArray.fromArray(value0, value1);
            context.getBindings("js").putMember("array", array);
            Value result = context.eval("js", "Object.assign({}, array)");
            Set<String> keys = result.getMemberKeys();
            assertTrue(keys.size() == 2);
            Iterator<String> iterator = keys.iterator();
            for (int i = 0; i < 2; i++) {
                String key = iterator.next();
                assertEquals(String.valueOf(i), key);
                assertEquals((i == 0) ? value0 : value1, result.getMember(key).asString());
            }
        }
    }

}
