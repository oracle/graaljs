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
package com.oracle.truffle.js.test.debug;

import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;

public class ParseInlineTest {
    @Registration(name = "", version = "", id = "testParseInline", services = ParseInlineInstrument.Tester.class)
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
                            final String code = eventContext.getInstrumentedSourceSection().getCharacters().toString();
                            assertTrue(code, code.contains("this"));
                            final Source source = Source.newBuilder(info.getId(), "this", "eval in context").internal(true).build();
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
            SourceSectionFilter filter = SourceSectionFilter.newBuilder().rootNameIs("Filter"::equals).lineIs(3).build();
            env.getInstrumenter().attachExecutionEventFactory(filter, eenf);
        }

        static class Tester {
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
            ParseInlineInstrument.Tester tester = context.getEngine().getInstruments().get("testParseInline").lookup(ParseInlineInstrument.Tester.class);

            context.eval(JavaScriptLanguage.ID, src);

            assertTrue(JSOrdinary.isJSUserObject(tester.result));
            assertTrue(JSObject.hasOwnProperty((DynamicObject) tester.result, "number"));
        }
    }
}
