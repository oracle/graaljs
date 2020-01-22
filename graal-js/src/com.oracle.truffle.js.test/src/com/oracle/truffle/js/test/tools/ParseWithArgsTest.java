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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class ParseWithArgsTest {
    @Test
    public void sourceRemainsHiddenAndProperlyNamed() {
        Context ctx = Context.newBuilder().build();
        ParsingInstrument inst = ctx.getEngine().getInstruments().get("parsingInstrument").lookup(ParsingInstrument.class);
        assertNotNull(inst);
        Value x = ctx.eval("js", "var x = 42; debugger; x.toString()");
        assertEquals("42", x.asString());

        assertEquals("Two breaks: " + inst.sections, 2, inst.sections.size());
        final Source snd = inst.sections.get(1).getSource();
        assertEquals("Right name", "stop.js", snd.getName());
        assertEquals("Is internal", true, snd.isInternal());
    }

    @TruffleInstrument.Registration(id = "parsingInstrument", name = "Parsing Instrument", services = ParsingInstrument.class, version = "1.0")
    public static final class ParsingInstrument extends TruffleInstrument {
        final List<SourceSection> sections = new ArrayList<>();

        @Override
        protected void onCreate(Env env) {
            env.registerService(this);
            SourceSectionFilter stop = SourceSectionFilter.newBuilder().tagIs(DebuggerTags.AlwaysHalt.class).includeInternal(true).build();
            env.getInstrumenter().attachExecutionEventListener(stop, new ExecutionEventListener() {
                @Override
                public void onEnter(EventContext context, VirtualFrame frame) {
                    CompilerDirectives.transferToInterpreter();

                    final SourceSection section = context.getInstrumentedSourceSection();
                    sections.add(section);

                    if (section.getSource().isInternal()) {
                        return;
                    }

                    if (sections.size() > 10) {
                        return;
                    }

                    Source src = Source.newBuilder("js", "debugger;", "stop.js").internal(true).build();

                    try {
                        CallTarget call = env.parse(src, "a", "b", "c");
                        call.call(1, 2, 3);
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                }

                @Override
                public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
                }

                @Override
                public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
                }
            });
        }
    }
}
