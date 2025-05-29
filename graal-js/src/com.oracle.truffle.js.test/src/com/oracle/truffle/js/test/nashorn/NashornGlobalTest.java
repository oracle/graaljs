/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.nashorn;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class NashornGlobalTest {
    private static boolean testIntl(String sourceText) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME,
                        "true").option(JSContextOptions.SCRIPTING_NAME, "true").allowAllAccess(true).build()) {
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "nashorn-global-test").buildLiteral());
            Assert.assertTrue(result.isBoolean());
            return result.asBoolean();
        }
    }

    @Test
    public void testEXEC() {
        String cmd = System.getProperty("os.name").startsWith("Windows") ? "help" : "ls";
        Assert.assertTrue(testIntl("var a = $EXEC('" + cmd + "'); a.length > 0;"));
    }

    @Test
    public void testLoadFileNonExistent() {
        String src = """
                        var ret = false;
                        var FILE = Java.type('java.io.File');
                        try {
                            load(new FILE('nonexistent.file'));
                        } catch (ex) {
                            ret = ex instanceof Error && ex.message.indexOf('nonexistent.file') >= 0;
                            if (!ret) {
                                throw ex;
                            }
                        }
                        ret;""";
        Assert.assertTrue(testIntl(src));
    }

    @Test
    public void testLoadURLNonExistent() {
        String src = """
                        var ret = false;
                        var URL = Java.type('java.net.URL');
                        try {
                            load(new URL('file:///nonexistent.file'));
                        } catch (ex) {
                            ret = ex instanceof Error && ex.message.indexOf('nonexistent.file') >= 0;
                            if (!ret) {
                                throw ex;
                            }
                        }
                        ret;""";
        Assert.assertTrue(testIntl(src));
    }
}
