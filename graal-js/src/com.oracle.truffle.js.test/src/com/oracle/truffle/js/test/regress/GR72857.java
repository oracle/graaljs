/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import org.junit.Test;

public class GR72857 {

    private static Source resolverInitSource() {
        return Source.newBuilder("js", """
                        var resolver = Promise.withResolvers();
                        globalThis.resolve = resolver.resolve;
                        globalThis.reject = resolver.reject;
                        await resolver.promise
                        """, "source.mjs").buildLiteral();
    }

    @Test
    public void testResolve() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value modulePromise = context.eval(resolverInitSource());
            boolean[] resolved = new boolean[1];
            modulePromise.invokeMember("then", (ProxyExecutable) (Value... arguments) -> {
                resolved[0] = true;
                return null;
            });
            Value resolve = context.getBindings("js").getMember("resolve");
            resolve.execute();
            assertTrue(resolved[0]);
        }
    }

    @Test
    public void testReject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value modulePromise = context.eval(resolverInitSource());
            boolean[] rejected = new boolean[1];
            modulePromise.invokeMember("catch", (ProxyExecutable) (Value... arguments) -> {
                rejected[0] = true;
                return null;
            });
            Value reject = context.getBindings("js").getMember("reject");
            String error = "myError";
            try {
                reject.execute(error);
                fail("should have thrown");
            } catch (PolyglotException ex) {
                assertTrue(ex.getMessage().contains(error));
            }
            assertTrue(rejected[0]);
        }
    }

    private static final String CODE = """
                    globalThis.log = '';

                    function appendToLog(s) {
                        globalThis.log += s;
                    };

                    Promise.resolve().then(x => x).then(x => x).then(x => appendToLog('D'));
                    Promise.resolve().then(x => x).then(x => appendToLog('C'));
                    Promise.resolve().then(x => appendToLog('B'));
                    appendToLog('A');

                    throw new Error("someError");
                    """;

    @Test
    public void testScript() {
        testScriptOrModule(true);
    }

    @Test
    public void testModule() {
        testScriptOrModule(false);
    }

    private static void testScriptOrModule(boolean script) {
        try (Context context = JSTest.newContextBuilder().build()) {
            String mimeType = script ? JavaScriptLanguage.APPLICATION_MIME_TYPE : JavaScriptLanguage.MODULE_MIME_TYPE;
            Source source = Source.newBuilder("js", CODE, "source.js").mimeType(mimeType).buildLiteral();
            try {
                context.eval(source);
                fail("should have thrown");
            } catch (PolyglotException ex) {
                assertTrue(ex.getMessage().contains("someError"));
            }
            String log = context.getBindings("js").getMember("log").asString();
            assertEquals("ABCD", log);
        }
    }

}
