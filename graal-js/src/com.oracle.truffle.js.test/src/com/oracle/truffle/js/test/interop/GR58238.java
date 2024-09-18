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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * Regression test of the missing interop boundary calls.
 */
public class GR58238 {

    private Context context;
    private CallbackVerifier verifier;

    @Before
    public void setUp() {
        context = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).build();
        verifier = new CallbackVerifier();
        context.getBindings(ID).putMember("verifier", verifier);
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void testGetMember() {
        String code = """
                        ({
                            get foo() {
                                return Promise.resolve().then(_ => verifier.call());
                            }
                        })""";
        Value object = context.eval("js", code);
        object.getMember("foo");
        verifier.assertCalled();
    }

    @Test
    public void testPutMember() {
        String code = """
                        ({
                            set foo(value) {
                                Promise.resolve().then(_ => verifier.call());
                            }
                        })""";
        Value object = context.eval("js", code);
        object.putMember("foo", 42);
        verifier.assertCalled();
    }

    @Test
    public void testHasIterator() {
        String code = """
                        ({
                            get [Symbol.iterator]() {
                                Promise.resolve().then(_ => verifier.call());
                                return function() {};
                            }
                        })""";
        Value object = context.eval("js", code);
        object.hasIterator();
        verifier.assertCalled();
    }

    @Test
    public void testGetIterator() {
        String code = """
                        ({
                            get [Symbol.iterator]() {
                                Promise.resolve().then(_ => verifier.call());
                                return function() {
                                    return { next() {} };
                                };
                            }
                        })""";
        Value object = context.eval("js", code);
        object.getIterator();
        verifier.assertCalled();
    }

    public static class CallbackVerifier {
        private boolean called;

        public void call() {
            called = true;
        }

        void assertCalled() {
            assert called;
        }
    }

}
