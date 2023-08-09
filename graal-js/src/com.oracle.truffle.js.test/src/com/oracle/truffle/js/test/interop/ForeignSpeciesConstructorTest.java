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

import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class ForeignSpeciesConstructorTest {

    @Test
    public void testRegExpMatchAllSpeciesConstructorReturningForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyInstantiable speciesConstructor = (args) -> ProxyObject.fromMap(new HashMap<>());
            context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
            Value result = context.eval(ID, """
                            let re = new RegExp("ab");
                            re.constructor = {
                                [Symbol.species]: foreignSpeciesConstructor
                            };
                            try {
                                for (let m of re[Symbol.matchAll]("ababababab")) {
                                    break;
                                }
                                "should have thrown a TypeError";
                            } catch (e) {
                                if (!(e instanceof TypeError)) {
                                    throw e;
                                }
                                e.message;
                            }
                            """);
            Assert.assertEquals("RegExp expected", result.asString());
        }
    }

    @Test
    public void testRegExpSplitSpeciesConstructorReturningForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyInstantiable speciesConstructor = (args) -> ProxyObject.fromMap(new HashMap<>());
            context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
            Value result = context.eval(ID, """
                            let re = new RegExp("ab");
                            re.constructor = {
                                [Symbol.species]: foreignSpeciesConstructor
                            };
                            try {
                                re[Symbol.split]("ab");
                                "should have thrown a TypeError";
                            } catch (e) {
                                if (!(e instanceof TypeError)) {
                                    throw e;
                                }
                                e.message;
                            }
                            """);
            Assert.assertEquals("RegExp expected", result.asString());
        }
    }

    @Test
    public void testTypedArraySpeciesConstructorReturningForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyInstantiable speciesConstructor = (args) -> ProxyObject.fromMap(Map.of());
            context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
            Value result = context.eval(ID, """
                            let a = new Int8Array(8);
                            a.constructor = {
                                [Symbol.species]: foreignSpeciesConstructor
                            };
                            try {
                                a.slice(4, 8);
                                "should have thrown a TypeError";
                            } catch (e) {
                                if (!(e instanceof TypeError)) {
                                    throw e;
                                }
                                e.message;
                            }
                            """);
            Assert.assertEquals("TypedArray expected", result.asString());
        }
    }

    @Test
    public void testArrayBufferSpeciesConstructorReturningForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyInstantiable speciesConstructor = (args) -> ProxyObject.fromMap(Map.of());
            context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
            Value result = context.eval(ID, """
                            let a = new ArrayBuffer(8);
                            a.constructor = {
                                [Symbol.species]: foreignSpeciesConstructor
                            };
                            try {
                                a.slice(4, 8);
                                "should have thrown a TypeError";
                            } catch (e) {
                                if (!(e instanceof TypeError)) {
                                    throw e;
                                }
                                e.message;
                            }
                            """);
            Assert.assertEquals("ArrayBuffer expected", result.asString());
        }
    }

    @Test
    public void testArraySpeciesConstructorReturningForeignObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object[] foreignObjs = {
                            ProxyArray.fromArray(42, 43),
                            ProxyObject.fromMap(new HashMap<>(Map.of("length", 2, "0", 42, "1", 43))),
            };
            for (var foreignObj : foreignObjs) {
                ProxyInstantiable speciesConstructor = (args) -> foreignObj;
                context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
                Value result = context.eval(ID, """
                                {
                                    let a = new Array(2);
                                    a.constructor = {
                                        [Symbol.species]: foreignSpeciesConstructor
                                    };
                                    a.slice(0, 1);
                                    a.splice(0, 1);
                                    a.filter(() => true);
                                    a.map((x) => x + 1);
                                    a.concat([44]);
                                    a.flat();
                                    a.flatMap((x) => []);
                                    "ok";
                                }
                                """);
                Assert.assertEquals("ok", result.asString());
            }
        }
    }

    @Test
    public void testPromiseThenForeignSpeciesConstructor() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw").build()) {
            ProxyInstantiable speciesConstructor = (args) -> ProxyObject.fromMap(Map.of());
            context.getBindings(ID).putMember("foreignSpeciesConstructor", speciesConstructor);
            Value result = context.eval(ID, """
                            let pr = Promise.resolve();
                            pr.constructor = {
                                [Symbol.species]: foreignSpeciesConstructor
                            };
                            try {
                                pr.then();
                                "should have thrown a TypeError";
                            } catch (e) {
                                if (!(e instanceof TypeError)) {
                                    throw e;
                                }
                                e.message;
                            }
                            """);
            Assert.assertEquals("Promise cannot be a foreign object", result.asString());
        }
    }
}
