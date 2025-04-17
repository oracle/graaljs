/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.builtins;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class ErrorMessageTest {

    /**
     * GR-63023: Provide a better error message by querying error name and message properties.
     */
    @Test
    public void testErrorNameAndMessage() {
        for (boolean as : new boolean[]{false, true}) {
            try (Context context = JSTest.newContextBuilder().build()) {
                Value customErrorClass = context.eval(JavaScriptLanguage.ID, """
                                class CustomError extends Error {
                                    constructor(initialMessage, name, message) {
                                        super(initialMessage ?? undefined);
                                        if (name != undefined) {
                                            this.name = name;
                                        }
                                        if (message != undefined) {
                                            this.message = message;
                                        }
                                    }
                                }
                                CustomError;
                                """);

                Value errorObject;
                PolyglotException polyglotException;
                // override name and message
                errorObject = customErrorClass.newInstance("initial message", "ExpectedError", "expected message");
                polyglotException = asPolyglotException(errorObject, as);
                Assert.assertEquals("ExpectedError: expected message", polyglotException.getMessage());

                // override only message
                errorObject = customErrorClass.newInstance("initial message", null, "expected message");
                polyglotException = asPolyglotException(errorObject, as);
                Assert.assertEquals("CustomError: expected message", polyglotException.getMessage());

                // override only name
                errorObject = customErrorClass.newInstance("initial message", "ExpectedError");
                polyglotException = asPolyglotException(errorObject, as);
                Assert.assertEquals("ExpectedError: initial message", polyglotException.getMessage());

                // override only name, no message
                errorObject = customErrorClass.newInstance(null, "ExpectedError");
                polyglotException = asPolyglotException(errorObject, as);
                Assert.assertEquals("ExpectedError", polyglotException.getMessage());

                // override none
                errorObject = customErrorClass.newInstance("initial message");
                polyglotException = asPolyglotException(errorObject, as);
                Assert.assertEquals("CustomError: initial message", polyglotException.getMessage());
            }
        }
    }

    private static PolyglotException asPolyglotException(Value exception, boolean as) {
        if (as) {
            return exception.as(PolyglotException.class);
        } else {
            try {
                exception.throwException();
                throw new AssertionError("Expected PolyglotException");
            } catch (PolyglotException polyglotException) {
                return polyglotException;
            }
        }
    }
}
