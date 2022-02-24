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
package com.oracle.truffle.js.test.debug;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Instrument;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;

public class ParseInlineTest {

    static final String PARSE_INLINE_INSTRUMENT_ID = "testParseInline";

    @Registration(name = "", version = "", id = PARSE_INLINE_INSTRUMENT_ID, services = ParseInlineInstrument.Tester.class)
    public static class ParseInlineInstrument extends TruffleInstrument {

        @Override
        protected void onCreate(Env env) {
            Tester tester = new Tester();
            env.registerService(tester);

            ExecutionEventNodeFactory eenf = new ExecutionEventNodeFactory() {
                @Override
                public ExecutionEventNode create(final EventContext eventContext) {
                    return new ExecutionEventNode() {
                        @Override
                        protected void onEnter(VirtualFrame frame) {
                            final LanguageInfo info = eventContext.getInstrumentedNode().getRootNode().getLanguageInfo();
                            final String line = eventContext.getInstrumentedSourceSection().getCharacters().toString();
                            assertThat(line, containsString(tester.expectedContext));
                            final Source source = Source.newBuilder(info.getId(), tester.evalInContext, "eval in context").internal(true).build();
                            ExecutableNode fragment = env.parseInline(source, eventContext.getInstrumentedNode(), frame.materialize());
                            if (fragment != null) {
                                insert(fragment);
                                Object result = null;
                                try {
                                    result = fragment.execute(frame);
                                } finally {
                                    tester.result = result;
                                }
                            }
                        }
                    };
                }
            };
            SourceSectionFilter filter = SourceSectionFilter.newBuilder().tagIs(StandardTags.StatementTag.class).lineIs(3).build();
            env.getInstrumenter().attachExecutionEventFactory(filter, eenf);
        }

        static class Tester {
            String expectedContext = "";
            String evalInContext;
            Object result;
        }
    }

    @Test
    public void testThis() throws Exception {
        String src = "function Filter(number) {\n" +
                        "    this.number = number;\n" +
                        "    this; // <--\n" +
                        "}\n" +
                        "\n" +
                        "new Filter(123);\n";

        try (Context context = JSTest.newContextBuilder().build()) {
            Instrument instrument = context.getEngine().getInstruments().get(PARSE_INLINE_INSTRUMENT_ID);
            ParseInlineInstrument.Tester tester = instrument.lookup(ParseInlineInstrument.Tester.class);
            tester.expectedContext = "this";
            tester.evalInContext = "this";

            context.eval(JavaScriptLanguage.ID, src);

            assertTrue(JSOrdinary.isJSOrdinaryObject(tester.result));
            assertTrue(JSObject.hasOwnProperty((DynamicObject) tester.result, "number"));
        }
    }

    private static final String TEST_CLOSURE_SOURCE = "" +
                    "function Filter(first, second) {\n" +
                    "    this.first = (function closure() {\n" +
                    "        return first; // <--\n" +
                    "    })();\n" +
                    "    this.second = second;\n" +
                    "}\n" +
                    "\n" +
                    "new Filter(123, 456);\n";

    @Test
    public void testClosure() throws Exception {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.SCOPE_OPTIMIZATION_NAME, "true").build()) {
            Instrument instrument = context.getEngine().getInstruments().get(PARSE_INLINE_INSTRUMENT_ID);
            ParseInlineInstrument.Tester tester = instrument.lookup(ParseInlineInstrument.Tester.class);
            tester.expectedContext = "return first";
            tester.evalInContext = "`${typeof first} ${typeof second} ${first}`";

            context.eval(JavaScriptLanguage.ID, TEST_CLOSURE_SOURCE);

            assertNotNull(tester.result);
            assertTrue(String.valueOf(tester.result), InteropLibrary.getUncached().isString(tester.result));
            assertEquals("number undefined 123", InteropLibrary.getUncached().asString(tester.result));
        }
    }

    @Test
    public void testClosureWithoutOptimization() throws Exception {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.SCOPE_OPTIMIZATION_NAME, "false").build()) {
            Instrument instrument = context.getEngine().getInstruments().get(PARSE_INLINE_INSTRUMENT_ID);
            ParseInlineInstrument.Tester tester = instrument.lookup(ParseInlineInstrument.Tester.class);
            tester.expectedContext = "return first";
            tester.evalInContext = "`${typeof first} ${typeof second} ${first}`";

            context.eval(JavaScriptLanguage.ID, TEST_CLOSURE_SOURCE);

            assertNotNull(tester.result);
            assertTrue(String.valueOf(tester.result), InteropLibrary.getUncached().isString(tester.result));
            assertEquals("number number 123", InteropLibrary.getUncached().asString(tester.result));
        }
    }
}
