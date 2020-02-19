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
package com.oracle.truffle.js.test.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;

import com.ibm.icu.impl.Assert;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.JavaScriptTranslator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.snapshot.Recording;
import com.oracle.truffle.js.snapshot.RecordingProxy;
import com.oracle.truffle.js.test.JSTest;

public class RecordingTest extends JSTest {

    @Test
    public void recordingEncodeDecodeTest() {
        testHelper.enterContext();

        JSContext context = testHelper.getJSContext();
        Source source = Source.newBuilder(JavaScriptLanguage.ID,
                        "const PI=3.14; const TRUE=true; const bi=new BigInt(123456); const theBiggestInt = 9007199254740990n;" +
                                        "switch (TRUE) { case true: break; default: 1; };" +
                                        "var sqr = (y)=>{return y*y;};" +
                                        "function test(n) { label: for (var i=0;i<sqr(n);i++) { " +
                                        "if (i%2 == 0) { print('hello' + i); continue; } else { break label; }; }; }; " +
                                        "test(5.5);",
                        "recordingTest").build();

        String prefix = "";
        String suffix = "";

        Recording rec = new Recording();
        ScriptNode program = JavaScriptTranslator.translateScript(RecordingProxy.createRecordingNodeFactory(rec, NodeFactory.getInstance(context)), context, source, false, prefix, suffix);
        rec.finish(program.getRootNode());

        try (OutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream("recordingTest", outs, true);
        } catch (IOException ex) {
            Assert.fail(ex);
        }

        testHelper.close();
    }
}
