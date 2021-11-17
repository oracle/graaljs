/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Predicate;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Test;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.LoadSourceListener;
import com.oracle.truffle.api.instrumentation.SourceFilter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;

public class GR22048 {
    static final ThreadLocal<Object> filterTL = new ThreadLocal<>();
    static final ThreadLocal<Context> contextTL = new ThreadLocal<>();

    @After
    public void cleanup() {
        filterTL.set(null);
        contextTL.set(null);
    }

    @Test
    public void testReentrant1() {
        try (Context context = TestUtil.newContextBuilder().build()) {
            contextTL.set(context);
            context.getBindings(JavaScriptLanguage.ID).putMember("instrument", new InstrumentInstaller(context.getEngine()));

            Source src = Source.newBuilder(JavaScriptLanguage.ID, "" +
                            "function rootNameIs(name){ return true; }\n" +
                            "instrument(rootNameIs);\n", "test.js").buildLiteral();
            context.eval(src);
        }
    }

    @Test
    public void testReentrant2() {
        try (Context context = TestUtil.newContextBuilder().build()) {
            contextTL.set(context);
            context.getBindings(JavaScriptLanguage.ID).putMember("instrument", new InstrumentInstaller(context.getEngine()));

            Source src = Source.newBuilder(JavaScriptLanguage.ID, "" +
                            "(function fib(n) {\n" +
                            "  function rootNameIs(name){ return true; }\n" +
                            "  if (n < 2) return 1;\n" +
                            "  return ((n == 5) ? instrument(rootNameIs) : 0) + fib(n - 1) + fib(n - 2);\n" +
                            "})\n", "test.js").buildLiteral();
            Value f = context.eval(src);

            f.execute(10);
        }
    }

    @TruffleInstrument.Registration(id = GR22048Instrument.ID, services = {GR22048Instrument.class})
    public static class GR22048Instrument extends TruffleInstrument {
        public static final String ID = "GR22048Instrument";

        private final Context context;

        public GR22048Instrument() {
            this.context = contextTL.get();
        }

        @Override
        protected void onCreate(Env env) {
            env.registerService(this);

            env.getInstrumenter().attachLoadSourceListener(SourceFilter.ANY, (LoadSourceListener) event -> {
            }, false);

            SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootTag.class).rootNameIs(new Predicate<String>() {
                private final ThreadLocal<Boolean> querying = new ThreadLocal<>();

                @Override
                public boolean test(String name) {
                    Object res = Boolean.FALSE;
                    Object filter = filterTL.get();
                    if (filter != null && name != null) {
                        Boolean prev = querying.get();
                        try {
                            if (Boolean.TRUE.equals(prev)) {
                                return false;
                            }
                            querying.set(Boolean.TRUE);

                            context.enter();
                            try {
                                res = InteropLibrary.getUncached().execute(filter, name);
                            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                                throw new AssertionError(e);
                            } finally {
                                context.leave();
                            }
                        } finally {
                            querying.set(prev);
                        }
                    }
                    return res == Boolean.TRUE;
                }
            }).build();
            env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, (ExecutionEventNodeFactory) ec -> null);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class InstrumentInstaller implements TruffleObject {
        private final Engine engine;

        public InstrumentInstaller(Engine engine) {
            this.engine = engine;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        final boolean isExecutable() {
            return true;
        }

        @TruffleBoundary
        @ExportMessage
        final Object execute(Object[] arguments) {
            filterTL.set(arguments[0]);
            engine.getInstruments().get(GR22048Instrument.ID).lookup(GR22048Instrument.class);
            return 0;
        }
    }
}
