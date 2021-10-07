/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.instrumentation.LoadSourceEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceListener;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionEvent;
import com.oracle.truffle.api.instrumentation.LoadSourceSectionListener;
import com.oracle.truffle.api.instrumentation.SourceFilter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Test;

public class GR18957 {

    @Test
    public void testIt() throws InterruptedException {
        final Context context = TestUtil.newContextBuilder().build();
        context.eval(JavaScriptLanguage.ID, "typeof abc === 'undefined'");
        // Verify that the materialization of JSTypeofIdenticalNode does not
        // throw when it is triggered without an entered context.
        final boolean[] passed = new boolean[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                context.getEngine().getInstruments().get(GR18957Instrument.ID).lookup(GR18957Instrument.class);
                passed[0] = true; // Invoked when no exception is thrown only
            }
        });
        t.start();
        t.join();
        Assert.assertTrue(passed[0]);
    }

    @TruffleInstrument.Registration(id = GR18957Instrument.ID, services = {GR18957Instrument.class})
    public static class GR18957Instrument extends TruffleInstrument {
        public static final String ID = "GR18957Instrument";

        @Override
        protected void onCreate(Env env) {
            env.registerService(this);
            env.getInstrumenter().attachLoadSourceListener(SourceFilter.ANY, new LoadSourceListener() {
                @Override
                public void onLoad(LoadSourceEvent event) {
                    // no action needed
                }
            }, true);
            env.getInstrumenter().visitLoadedSourceSections(SourceSectionFilter.ANY, new LoadSourceSectionListener() {
                @Override
                public void onLoad(LoadSourceSectionEvent event) {
                    // no action needed
                }
            });
        }
    }
}
