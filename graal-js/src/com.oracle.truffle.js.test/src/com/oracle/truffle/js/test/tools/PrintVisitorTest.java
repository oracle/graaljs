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
package com.oracle.truffle.js.test.tools;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.parser.GraalJSParserHelper;
import com.oracle.truffle.js.parser.internal.ir.debug.PrintVisitor;
import com.oracle.truffle.js.runtime.JSParserOptions;
import com.oracle.truffle.js.test.JSTest;

public class PrintVisitorTest extends JSTest {

    @Test
    public void testPrintVisitor() {
        String code = "function fn(a,b) { for (var i=0;i<10;i++) { label: if (true) { break; } else {}; }; while(true) {};" +
                        "try { var o= {a:'foo'}; (o?o:o); o.a+o['a']; with(o) {a;} } catch (error) { throw false; }; " +
                        "switch(true) { case false: break; default: break; }; return -a+(++b); }";
        Source source = Source.newBuilder(JavaScriptLanguage.ID, code, "test").build();
        FunctionNode node = GraalJSParserHelper.parseScript(testHelper.getJSContext(), source, new JSParserOptions());

        PrintVisitor pv = new PrintVisitor(node);
        String result = pv.toString();
        assertTrue(result.length() > 100);
        assertTrue(result.contains("function fn"));
        assertTrue(result.contains("[TERMINAL]"));
    }
}
