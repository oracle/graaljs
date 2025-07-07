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
import org.junit.Assert;
import org.junit.Test;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.js.parser.Source;
import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.truffle.js.test.JSTest;

public class ParserTest {

    private static FunctionNode testHelper(String code) {
        ScriptEnvironment env = ScriptEnvironment.builder().build();
        return new Parser(env, Source.sourceFor("", code), new ErrorManager.ThrowErrorManager()).parse();
    }

    @Test(expected = ParserException.class) // should not throw AssertionError
    public void testUnfinishedTemplate() {
        testHelper("`${");
    }

    @Test(expected = ParserException.class) // should not throw AssertionError
    public void testMissingCatchBlock() {
        testHelper("try {} catch (e) ");
    }

    @Test
    public void testIncompleteFunction() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source incompleteSource = org.graalvm.polyglot.Source.newBuilder("js", "function incompleteFunction(arg) { ", "test").buildLiteral();
            ctx.eval(incompleteSource);
        } catch (PolyglotException e) {
            Assert.assertTrue(e.isIncompleteSource());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testNotIncomplete() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source incompleteSource = org.graalvm.polyglot.Source.newBuilder("js", ");", "test").buildLiteral();
            ctx.eval(incompleteSource);
        } catch (PolyglotException e) {
            Assert.assertFalse(e.isIncompleteSource());
            return;
        }
        Assert.fail();
    }

    /**
     * Regression test for parsing switch statement with missing LBRACE. Should result in
     * SyntaxError but not trigger any assertion or internal errors.
     */
    @Test
    public void testIncompleteSwitch1() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source incompleteSource = org.graalvm.polyglot.Source.newBuilder("js", """
                            function bad() {
                                switch (42) ;
                            }
                            bad();
                            """, "test").buildLiteral();
            ctx.eval(incompleteSource);
            Assert.fail("should have thrown SyntaxError");
        } catch (PolyglotException e) {
            if (e.isInternalError()) {
                throw e;
            } else {
                Assert.assertTrue("SyntaxError", e.isSyntaxError());
            }
        }
    }

    @Test
    public void testIncompleteSwitch2() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source incompleteSource = org.graalvm.polyglot.Source.newBuilder("js", """
                            function bad() {
                                switch (42)
                            """, "test").buildLiteral();
            ctx.eval(incompleteSource);
            Assert.fail("should have thrown SyntaxError");
        } catch (PolyglotException e) {
            if (e.isInternalError()) {
                throw e;
            } else {
                Assert.assertTrue("SyntaxError", e.isSyntaxError());
                Assert.assertTrue("incomplete source", e.isIncompleteSource());
            }
        }
    }

    // todo-lw: move these tests to a separate extractor test file

    @Test
    public void testBasicExtractorBinding() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("js", """                            
                            class Foo {
                                static [Symbol.customMatcher](subject) {
                                    return [subject.someRandomProp];
                                }
                            
                                someRandomProp = 42;
                            
                                constructor() {}
                            }
                            
                            const foo = new Foo();
                            
                            let x;
                            Foo(x) = foo;
                            """, "test").buildLiteral();
            ctx.eval(src);
        }
    }

    @Test
    public void testBasicExtractorLetAssignment() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("js", """                        
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
                        
                        let C(x) = subject;
                        """, "test").buildLiteral();
            ctx.eval(src);
        }
    }

    @Test
    public void testBasicExtractorConstAssignment() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("js", """
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
                        
                        const C(x) = subject;
                        """, "test").buildLiteral();
            ctx.eval(src);
        }
    }

    @Test
    public void testNestedExtractor() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            org.graalvm.polyglot.Source src = org.graalvm.polyglot.Source.newBuilder("js", """
                        class C {
                            #f;
                            constructor(f) {
                                this.#f = f;
                            }
                            extractor = {
                                [Symbol.customMatcher](subject, _kind, receiver) {
                                    return receiver.#f(subject);
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
}
