/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class JSONWriterTest {
    private static String testIntl(String sourceText) {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME, "true").build()) {
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, "json-writer-test").buildLiteral());
            Assert.assertTrue(result.isString());
            return result.asString();
        }
    }

    @Test
    public void testParseToJSON() {
        String source = "var a=0;; const b={a:'foo'}; let c=[1,2,3]; var r=/a/g; label: function foo(){ var x=a?b:c; x+=3;x=1*2+(3-4)/5*Math.sqrt(5);" +
                        "while(true) { if (true) { break; } else { continue; }; }; for (var i=0;i<10;i++); for (var x in Object); return ++a+b.a; };" +
                        "foo(c[0],c.a); try{ switch(a) { case 0: { with (b) {}; break; } default: throw 'hallo'; }; } catch (e) { }";
        String result = testIntl("parseToJSON(\"" + source + "\",'test','test2')");
        Assert.assertTrue(result.startsWith("{\"loc\":{\"source\":\"test\",\"start\":"));
    }
}
