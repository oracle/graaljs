/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.parser;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.oracle.truffle.js.test.JSTest;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExtractorsParsingTest {
    private Source srcWithBasicCustomMatcher(String code) {
        final var src = Source.newBuilder("js", """                            
                class Foo {
                    static [Symbol.customMatcher](subject) {
                        return [subject.someRandomProp];
                    }
                
                    someRandomProp = 42;
                
                    constructor() {}
                }
                """ + code, "test").buildLiteral();

        return src;
    }

    @Before
    public void setup() {
        System.setProperty("truffle.js.parser.extractors", "true");
    }

    @After
    public void tearDown() {
        System.clearProperty("truffle.js.parser.extractors");
    }

    @Test
    public void testBasicExtractorBinding() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = srcWithBasicCustomMatcher("""
                    const foo = new Foo();
                    
                    let x;
                    Foo(x) = foo;
                    """);
            ctx.eval(src);
        }
    }

    @Test
    public void testBasicExtractorLetAssignment() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = srcWithBasicCustomMatcher("""
                    const subject = new Foo();
                    
                    let Foo(x) = subject;
                    """);
            ctx.eval(src);
        }
    }

    @Test
    public void testBasicExtractorConstAssignment() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = srcWithBasicCustomMatcher("""
                    const subject = new Foo();
                    
                    const Foo(x) = subject;
                    """);
            ctx.eval(src);
        }
    }

    @Test
    public void testNestedExtractor() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = Source.newBuilder("js", """
                    class C {
                        #f;
                        constructor(f) {
                            this.#f = f;
                        }
                        extractor = {
                            [Symbol.customMatcher](subject, _kind, receiver) {
                                return [receiver.#f(subject)];
                            }
                        };
                    }
                    
                    const obj = new C(data => data.toUpperCase());
                    const subject = "data";
                    
                    const obj.extractor(x) = subject;
                    """, "test").buildLiteral();
            ctx.eval(src);
        }
    }

    // todo-lw: failing
    @Test
    public void testNewlineAfterMemberExpression() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = Source.newBuilder("js", """                 
                    class C {
                        #data;
                        constructor(data) {
                            this.#data = data;
                        }
                        static [Symbol.customMatcher](subject) {
                            return #data in subject && [subject.#data];
                        }
                    }
                    
                    const subject = new C("data");
                    
                    let C
                    (y) = subject;
                    """, "test").buildLiteral();
            final var value = ctx.eval(src);
            assertTrue(value.isException());
        }
    }

    @Test
    public void testInvalidExtractorBindingsNew() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final var src = srcWithBasicCustomMatcher("""
                    const subject = new Foo();
                    
                    let new Foo(x, y) = subject;
                    """);
            ctx.eval(src);
            fail("Expected SyntaxError");
        } catch (PolyglotException e) {
            if (e.isInternalError()) {
                throw e;
            } else {
                Assert.assertTrue("SyntaxError", e.isSyntaxError());
            }
        }
    }
}
