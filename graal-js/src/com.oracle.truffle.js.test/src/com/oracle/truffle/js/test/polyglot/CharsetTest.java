/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CharsetTest {
    private static final String UNICODE_TEXT = "Tu\u010d\u0148\u00e1\u010d\u010d\u00ed \ud83d\udca9!";

    @Parameters(name = "{0}")
    public static List<String> charsetNames() {
        return Arrays.asList("UTF-8", "UTF-16", "UTF-32");
    }

    @Parameter(value = 0) public String charsetName;

    @Test
    public void testInput() {
        Charset charset = Charset.forName(charsetName);
        ByteArrayInputStream input = new ByteArrayInputStream(UNICODE_TEXT.getBytes(charset));
        try (Context context = JSTest.newContextBuilder().in(input).//
                        option(JSContextOptions.CHARSET_NAME, charsetName).//
                        option(JSContextOptions.SCRIPTING_NAME, "true").build()) {
            context.getBindings("js").putMember("text", UNICODE_TEXT);
            Value result = context.eval("js", "text === readLine()");
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testOutput() {
        Charset charset = Charset.forName(charsetName);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Context context = JSTest.newContextBuilder().out(output).//
                        option(JSContextOptions.CHARSET_NAME, charsetName).//
                        option(JSContextOptions.PRINT_NAME, "true").build()) {
            context.getBindings("js").putMember("text", UNICODE_TEXT);
            context.eval("js", "print(text)");
        }
        String result = output.toString(charset).trim();
        Assert.assertEquals(UNICODE_TEXT, result);
    }

}
