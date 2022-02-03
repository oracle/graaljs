/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.JSParser;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.snapshot.Recording;
import com.oracle.truffle.js.test.JSTest;

public class RecordingTest extends JSTest {

    @Override
    public void setup() {
        super.setup();
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void recordingEncodeDecodeTest() throws IOException {
        JSContext context = testHelper.getJSContext();
        Source source = Source.newBuilder(JavaScriptLanguage.ID, "" +
                        "const PI = 3.14;" +
                        "const TRUE = true;" +
                        "const bi = BigInt(123456);" +
                        "const theBiggestInt = 9007199254740990n;" +
                        "switch (TRUE) { case true: break; default: 1; };" +
                        "var sqr = (y) => y * y;" +
                        "let print = function(){};" +
                        "function test(n) {" +
                        "  let me = 40;" +
                        "  if (!n) { return -1; }" +
                        "  const ant = 69;" +
                        "  label: for (let i = 0; i < sqr(n); i++) { " +
                        "    if (i % 2 == 0) {" +
                        "      --me;" +
                        "      print('hello' + ant);" +
                        "      continue;" +
                        "    } else {" +
                        "      break label;" +
                        "    }" +
                        "  }" +
                        "  function closure(a) { me += a; return ++me; }" +
                        "  return closure(2);" +
                        "}" +
                        "test(5.5);" +
                        "",
                        "recordingTest.js").build();

        String prefix = "";
        String suffix = "";
        Recording rec = Recording.recordSource(source, context, false, prefix, suffix);

        byte[] snapshot;
        try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream(source.getName(), outs, true);
            snapshot = outs.toByteArray();
        }
        try (OutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream(source.getName(), outs, false);
        }

        ScriptNode script = ((JSParser) context.getEvaluator()).parseScript(context, source, ByteBuffer.wrap(snapshot));
        Object result = script.run(testHelper.getRealm());
        assertEquals(42, result);
    }

    @Test
    public void testFunctionWrap() throws IOException {
        JSContext context = testHelper.getJSContext();
        Source source = Source.newBuilder(JavaScriptLanguage.ID, "" +
                        "const {" +
                        "  Array," +
                        "} = {Array: primordials?.Array ?? globalThis?.Array};" +
                        "let array = new Array();" +
                        "for (let i = 0; i < 3; ++i) {" +
                        "  array.push(...[1,3,3,7]);" +
                        "}" +
                        "let ok = [...'ok'];" +
                        "function test(str) {" +
                        "  const len = str.length;" +
                        "  if (len === 0) return '';" +
                        "  let res = '', i = 0;" +
                        "  do {" +
                        "    res += str[i].toUpperCase();" +
                        "  } while (++i < len);" +
                        "  return res;" +
                        "}" +
                        "return test(ok);" +
                        "",
                        "recordingTest.js").build();

        String prefix = "(function (exports, require, module, process, internalBinding, primordials) {'use strict';";
        String suffix = "\n})()";
        Recording rec = Recording.recordSource(source, context, false, prefix, suffix);

        byte[] snapshot;
        try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream(source.getName(), outs, true);
            snapshot = outs.toByteArray();
        }
        try (OutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream(source.getName(), outs, false);
        }

        ScriptNode script = ((JSParser) context.getEvaluator()).parseScript(context, source, ByteBuffer.wrap(snapshot));
        Object result = script.run(testHelper.getRealm());
        assertEquals("OK", result);
    }

    @Test
    public void testUnpairedSurrogate() throws IOException {
        JSContext context = testHelper.getJSContext();
        Source source = Source.newBuilder(JavaScriptLanguage.ID, "'\\ud834'", "test.js").build();
        Recording rec = Recording.recordSource(source, context, false, "", "");

        byte[] snapshot;
        try (ByteArrayOutputStream outs = new ByteArrayOutputStream()) {
            rec.saveToStream(source.getName(), outs, true);
            snapshot = outs.toByteArray();
        }

        ScriptNode script = ((JSParser) context.getEvaluator()).parseScript(context, source, ByteBuffer.wrap(snapshot));
        Object result = script.run(testHelper.getRealm());
        assertEquals("\ud834", result);
    }

}
