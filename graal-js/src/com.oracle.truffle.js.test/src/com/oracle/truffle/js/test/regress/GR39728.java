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
package com.oracle.truffle.js.test.regress;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GR39728 {

    @Ignore
    @Test
    public void testRep() {
        int attempts = 1000;
        // default is '$262.agent.timeouts.long == 1000'
        int timeout = 1000;
        // default is '3'
        int workers = 3;
        while (attempts-- > 0) {
            try (Context context = JSTest.newContextBuilder().allowCreateThread(true).option(JSContextOptions.TEST262_MODE_NAME, "true").build()) {
                String code = "" +
                                "$262.agent.waitUntil = function(typedArray, index, expected) {" +
                                "  var agents = 0;" +
                                "  while ((agents = Atomics.load(typedArray, index)) !== expected) {" +
                                "    /* nothing */" +
                                "  }" +
                                "};" +

                                "$262.agent.timeouts = {" +
                                "  yield: 10," +
                                "  small: 200," +
                                "  long: 1000," +
                                "  huge: 10000," +
                                "};" +

                                "$262.agent.tryYield = function() {" +
                                "  $262.agent.sleep($262.agent.timeouts.yield);" +
                                "};" +

                                "const WAIT_INDEX = 0;" +
                                "const RUNNING = 1;" +
                                "const NOTIFYCOUNT = " + workers + ";" +
                                "const NUMAGENT = " + workers + ";" +
                                "const BUFFER_SIZE = 4;" +
                                "const TIMEOUT = " + timeout + ";" +

                                "for (var i = 0; i < NUMAGENT; i++ ) {" +
                                "  $262.agent.start(`" +
                                "    $262.agent.receiveBroadcast(function(sab) {" +
                                "      const i32a = new Int32Array(sab);" +
                                "      Atomics.add(i32a, ${RUNNING}, 1);" +
                                "      $262.agent.report(Atomics.wait(i32a, ${WAIT_INDEX}, 0, ${TIMEOUT}));" +
                                "      $262.agent.leaving();" +
                                "    });" +
                                "  `);" +
                                "};" +

                                "const i32a = new Int32Array(" +
                                "  new SharedArrayBuffer(Int32Array.BYTES_PER_ELEMENT * BUFFER_SIZE)" +
                                ");" +

                                "$262.agent.broadcast(i32a.buffer);" +

                                "$262.agent.waitUntil(i32a, RUNNING, NUMAGENT);" +
                                "$262.agent.tryYield();" +
                                "Atomics.notify(i32a, WAIT_INDEX, NOTIFYCOUNT);";

                Value result = context.eval(JavaScriptLanguage.ID, code);
                Assert.assertTrue(result.isNumber());
                Assert.assertEquals(workers, result.asInt());
            }
        }
    }
}
