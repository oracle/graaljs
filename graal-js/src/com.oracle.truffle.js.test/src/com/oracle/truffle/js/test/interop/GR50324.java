/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class GR50324 {

    @Test
    public void testProxyIteratorNext() {
        try (Context context = JSTest.newContextBuilder().build()) {
            List<String> list = List.of("foo", "bar", "baz");
            ProxyIterator proxyIter = ProxyIterator.from(list.iterator());
            context.getBindings(ID).putMember("iter", proxyIter);

            String script = "iter.next()";
            checkIterResult(context.eval(ID, script), false, "foo");
            checkIterResult(context.eval(ID, script), false, "bar");
            checkIterResult(context.eval(ID, script), false, "baz");
            checkIterResult(context.eval(ID, script), true, null);
        }
    }

    @Test
    public void testProxyIteratorMap() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ITERATOR_HELPERS_NAME, "true").build()) {
            List<String> list = List.of("foo", "bar", "baz");
            ProxyIterator proxyIter = ProxyIterator.from(list.iterator());
            context.getBindings(ID).putMember("iter", proxyIter);

            context.eval(ID, "iter = iter.map(item => 'mapped ' + item)");

            String script = "iter.next()";
            checkIterResult(context.eval(ID, script), false, "mapped foo");
            checkIterResult(context.eval(ID, script), false, "mapped bar");
            checkIterResult(context.eval(ID, script), false, "mapped baz");
            checkIterResult(context.eval(ID, script), true, null);
        }
    }

    private static void checkIterResult(Value result, boolean expectedDone, String expectedValue) {
        assertEquals(expectedDone, result.getMember("done").asBoolean());
        if (!expectedDone) {
            assertEquals(expectedValue, result.getMember("value").asString());
        }
    }

}
