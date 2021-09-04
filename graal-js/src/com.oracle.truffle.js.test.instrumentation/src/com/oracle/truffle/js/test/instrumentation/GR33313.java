/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;

public class GR33313 {

    @Test
    public void testIt() {
        Context context = TestUtil.newContextBuilder().option("gr33313-instrument", "true").option("js.v8-realm-builtin", "true").build();
        context.eval("js", "Realm.create()");
    }

    @Option.Group(GR33313Instrument.ID)
    @TruffleInstrument.Registration(id = GR33313Instrument.ID)
    public static class GR33313Instrument extends TruffleInstrument implements ContextsListener {

        static final String ID = "gr33313-instrument";
        private TruffleInstrument.Env env;

        @Option(name = "", help = "Enable broken instrument.", category = OptionCategory.USER) //
        static final OptionKey<Boolean> ENABLED = new OptionKey<>(true);

        @Override
        protected OptionDescriptors getOptionDescriptors() {
            return new GR33313InstrumentOptionDescriptors();
        }

        @Override
        protected void onCreate(final TruffleInstrument.Env environment) {
            this.env = environment;
            environment.getInstrumenter().attachContextsListener(this, false);
        }

        @Override
        public void onLanguageContextInitialized(TruffleContext context, LanguageInfo language) {
            try {
                Source source = Source.newBuilder("js", "(function() {})", "fun").build();
                // Used to trigger AssertionError/NullPointerException for
                // a child realm because its agent was initialized too late
                env.parse(source).call();
            } catch (IOException ioex) {
                throw new RuntimeException(ioex);
            }
        }

        @Override
        protected void onDispose(final TruffleInstrument.Env environment) {
        }

        @Override
        public void onContextCreated(TruffleContext context) {
        }

        @Override
        public void onLanguageContextCreated(TruffleContext context, LanguageInfo language) {
        }

        @Override
        public void onLanguageContextFinalized(TruffleContext context, LanguageInfo language) {
        }

        @Override
        public void onLanguageContextDisposed(TruffleContext context, LanguageInfo language) {
        }

        @Override
        public void onContextClosed(TruffleContext context) {
        }

    }

}
