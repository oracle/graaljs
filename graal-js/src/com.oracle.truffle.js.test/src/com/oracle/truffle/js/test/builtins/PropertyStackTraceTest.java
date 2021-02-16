/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

@RunWith(Parameterized.class)
public class PropertyStackTraceTest {

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameter(value = 0) public boolean propertyCaches;

    private Context context;

    @Before
    public void before() {
        Context.Builder contextBuilder = JSTest.newContextBuilder();
        contextBuilder.allowHostAccess(HostAccess.ALL);
        contextBuilder.option(JSContextOptions.DEBUG_BUILTIN_NAME, Boolean.toString(true));
        if (!propertyCaches) {
            contextBuilder.option(JSContextOptions.PROPERTY_CACHE_LIMIT_NAME, String.valueOf(0));
        }
        context = contextBuilder.build();
    }

    @After
    public void dispose() {
        context.close();
    }

    @Test
    public void testGetterErrorLocation() {
        try {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "var O = {get P(){ throw new Error('expected'); }};\nO.P;", "SOURCE").buildLiteral());
            fail();
        } catch (PolyglotException ex) {
            verifyStackTrace(ex,
                            "<js> get P\\(SOURCE:1:.*\\)",
                            "<js> :program\\(SOURCE:2:.*\\)");
        }
    }

    @Test
    public void testSetterErrorLocation() {
        try {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "var O = {set P(V){ throw new Error('expected'); }};\nO.P = 42;", "SOURCE").buildLiteral());
            fail();
        } catch (PolyglotException ex) {
            verifyStackTrace(ex,
                            "<js> set P\\(SOURCE:1:.*\\)",
                            "<js> :program\\(SOURCE:2:.*\\)");
        }
    }

    @Test
    public void testProxyGetErrorLocation() {
        try {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "var O = new Proxy({}, {get(){ throw new Error('expected'); }});\nO.P;", "SOURCE").buildLiteral());
            fail();
        } catch (PolyglotException ex) {
            verifyStackTrace(ex,
                            "<js> get\\(SOURCE:1:.*\\)",
                            "<js> :program\\(SOURCE:2:.*\\)");
        }
    }

    @Test
    public void testProxySetErrorLocation() {
        try {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "var O = new Proxy({}, {set(){ throw new Error('expected'); }});\nO.P = 42;", "SOURCE").buildLiteral());
            fail();
        } catch (PolyglotException ex) {
            verifyStackTrace(ex,
                            "<js> set\\(SOURCE:1:.*\\)",
                            "<js> :program\\(SOURCE:2:.*\\)");
        }
    }

    @Test
    public void testProxyHasErrorLocation() {
        try {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, "var O = new Proxy({}, {has(){ throw new Error('expected'); }});\n'P' in O;", "SOURCE").buildLiteral());
            fail();
        } catch (PolyglotException ex) {
            verifyStackTrace(ex,
                            "<js> has\\(SOURCE:1:.*\\)",
                            "<js> :program\\(SOURCE:2:.*\\)");
        }
    }

    static void verifyStackTrace(PolyglotException ex, String... patterns) {
        StringWriter buffer = new StringWriter();
        try (PrintWriter out = new PrintWriter(buffer)) {
            ex.printStackTrace(out);
        }
        String[] lines = Arrays.stream(buffer.toString().split(System.lineSeparator())).map(String::trim).filter(l -> l.startsWith("at ")).map(l -> l.substring(3)).toArray(String[]::new);
        Assert.assertTrue("Not enough lines " + Arrays.toString(lines), patterns.length <= lines.length);
        for (int i = 0; i < patterns.length; i++) {
            String line = lines[i];
            Pattern pattern = Pattern.compile(patterns[i]);
            Assert.assertTrue("Expected " + patterns[i] + " but got " + line, pattern.matcher(line).matches());
        }
    }
}
