/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

public class JSONParseTest {

    @Test
    public void testJSONParseNumber() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value result = context.eval(ID, "JSON.parse('9007199254740992')");
            assertTrue(result.fitsInDouble());
            assertEquals(9007199254740992d, result.asDouble(), 0.0);
        }
    }

    @Test
    public void testJSONParseFail() {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(ID, "JSON.parse('- 43')");
            Assert.fail("failure expected");
        } catch (PolyglotException ex) {
            assertTrue(ex.isSyntaxError());
        }
    }

    @Test
    public void testJSONParseErrorPosition() {
        parseErrorPosition(1, "- 43");
        parseErrorPosition(4, "[1,2;");

    }

    private static void parseErrorPosition(int expectedPosition, String failingCode) {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(ID, "JSON.parse('" + failingCode + "')");
            Assert.fail("failure expected");
        } catch (PolyglotException ex) {
            assertTrue(ex.isSyntaxError());
            String message = ex.getMessage();
            String posStrPattern = "position ";
            int idx = message.indexOf(posStrPattern);

            Assert.assertTrue(idx >= 0);

            String posStr = message.substring(idx + posStrPattern.length());
            int pos = Integer.parseInt(posStr);

            Assert.assertEquals(expectedPosition, pos);
        }
    }

}
