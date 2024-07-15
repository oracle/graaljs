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
package com.oracle.truffle.js.test.regress;

import static com.oracle.truffle.js.runtime.JSContextOptions.UNHANDLED_REJECTIONS_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class GR55244 {

    /**
     * Unhandled promise rejection errors must not leak from one context to another.
     */
    @Test
    public void testPromiseRejectionTracker() {
        Object converters = ProxyObject.fromMap(Map.of(
                        "fromInt32ToNumber", (ProxyExecutable) (a) -> a[0].asInt(),
                        "toInt32", (ProxyExecutable) (a) -> a[0].asInt()));

        try (Engine eng = JSTest.newEngineBuilder().build()) {
            try (Context c1 = JSTest.newContextBuilder().engine(eng).option(UNHANDLED_REJECTIONS_NAME, "throw").build()) {
                c1.getBindings("js").putMember("converters", converters);
                Value func = c1.eval("js", """
                                (async function(args, result_setter) {
                                  let x = converters.fromInt32ToNumber(args.x);
                                  async function async_error_handling4() {
                                    async function foo() {
                                      throw "Error from foo()";
                                    }
                                    let y = foo(); // here it does not throw, only the Promise is created

                                    throw "Error from entry-point func";
                                    return 42;
                                  }
                                  result_setter(converters.toInt32(await async_error_handling4()));
                                })
                                """);
                try {
                    func.execute(ProxyObject.fromMap(Map.of("x", 41)), (ProxyExecutable) (a) -> {
                        assertEquals(1, a.length);
                        assertEquals(42, a[0].asInt());
                        return null;
                    });
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertThat(e.getMessage(), containsString("AggregateError: Unhandled promise rejections"));
                    Value errors = e.getGuestObject().getMember("errors");
                    assertNotNull("errors", errors);
                    assertTrue(errors.hasArrayElements());
                    assertEquals(2, errors.getArraySize());
                    assertTrue(errors.getArrayElement(0).isException());
                    assertTrue(errors.getArrayElement(1).isException());
                    try {
                        errors.getArrayElement(0).throwException();
                    } catch (PolyglotException e0) {
                        assertThat(e0.getMessage(), containsString("Error from foo()"));
                    }
                }
            }

            try (Context c2 = JSTest.newContextBuilder().engine(eng).option(UNHANDLED_REJECTIONS_NAME, "throw").build()) {
                c2.getBindings("js").putMember("converters", converters);
                Value func = c2.eval("js", """
                                (async function(args, result_setter) {
                                  let x = converters.fromInt32ToNumber(args.x);
                                  async function foo() { return x + 1; }
                                  result_setter(converters.toInt32(await foo()));
                                })
                                """);
                func.execute(ProxyObject.fromMap(Map.of("x", 41)), (ProxyExecutable) (a) -> {
                    assertEquals(1, a.length);
                    assertEquals(42, a[0].asInt());
                    return null;
                });
            }
        }
    }

    /**
     * Aggregate multiple unhandled promise rejection errors into one error.
     */
    @Test
    public void testMultipleRejections() {
        try (Engine eng = JSTest.newEngineBuilder().build()) {
            try (Context ctx = JSTest.newContextBuilder().engine(eng).option(UNHANDLED_REJECTIONS_NAME, "throw").build()) {
                try {
                    ctx.eval("js", "Promise.reject(42); Promise.reject(211);");
                    fail("should have thrown");
                } catch (PolyglotException e) {
                    assertThat(e.getMessage(), containsString("AggregateError: Unhandled promise rejections"));
                    Value errors = e.getGuestObject().getMember("errors");
                    assertNotNull("errors", errors);
                    assertTrue(errors.hasArrayElements());
                    assertEquals(2, errors.getArraySize());
                    assertTrue(errors.getArrayElement(0).isException());
                    assertTrue(errors.getArrayElement(1).isException());
                }
                ctx.eval("js", "1 + 1");
            }
        }
    }
}
