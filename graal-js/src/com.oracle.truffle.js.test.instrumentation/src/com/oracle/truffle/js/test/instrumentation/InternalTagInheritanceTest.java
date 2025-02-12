/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceFilter;

public class InternalTagInheritanceTest {
    private static final String MAIN_SOURCE_NAME = "MainSource.js";
    private static final String EVAL_SOURCE_NAME = "<eval>";
    private static final String MAIN_SOURCE_TEMPLATE = "%d + %d  + eval('%s');%s";
    private static final String EVAL_SOURCE_TEMPLATE = "%d + %d + %d;%s";
    private static final String IN_TEXT_NONINTERNAL_SOURCE_NAME = "SourceNameOverride.js";
    private static final String IN_TEXT_INTERNAL_SOURCE_NAME = "internal:" + IN_TEXT_NONINTERNAL_SOURCE_NAME;
    private static final String IN_TEXT_SOURCE_NAME_TEMPLATE = "//# sourceURL=%s";
    private static final String IN_TEXT_INTERNAL_SOURCE_ANNOTATION = String.format(IN_TEXT_SOURCE_NAME_TEMPLATE, IN_TEXT_INTERNAL_SOURCE_NAME);
    private static final String IN_TEXT_NONINTERNAL_SOURCE_ANNOTATION = String.format(IN_TEXT_SOURCE_NAME_TEMPLATE, IN_TEXT_NONINTERNAL_SOURCE_NAME);

    @Test
    public void testEval() throws IOException {
        try (Context ctx = Context.create("js")) {
            TestingExecutionInstrument instrument = ctx.getEngine().getInstruments().get(TestingExecutionInstrument.ID).lookup(TestingExecutionInstrument.class);
            Instrumenter instrumenter = instrument.getEnvironment().getInstrumenter();
            List<com.oracle.truffle.api.source.Source> sources = new ArrayList<>();

            instrumenter.attachExecuteSourceListener(SourceFilter.ANY, event -> {
                sources.add(event.getSource());
            }, true);

            boolean mainSourceInternal;
            String mainSourceInTextSourceName;
            for (int i = 0; i < 3; i++) {
                mainSourceInTextSourceName = "";
                if (i == 1) {
                    mainSourceInTextSourceName += IN_TEXT_NONINTERNAL_SOURCE_ANNOTATION;
                } else if (i == 2) {
                    mainSourceInTextSourceName += IN_TEXT_INTERNAL_SOURCE_ANNOTATION;
                }
                for (int j = 0; j < 2; j++) {
                    mainSourceInternal = j == 1;
                    String evalSourceInTextSourceName;
                    for (int k = 0; k < 3; k++) {
                        evalSourceInTextSourceName = "";
                        if (k == 1) {
                            evalSourceInTextSourceName += IN_TEXT_NONINTERNAL_SOURCE_ANNOTATION;
                        } else if (k == 2) {
                            evalSourceInTextSourceName += IN_TEXT_INTERNAL_SOURCE_ANNOTATION;
                        }
                        String evalSource = String.format(Locale.ROOT, EVAL_SOURCE_TEMPLATE, i, j, k, evalSourceInTextSourceName);
                        String mainSource = String.format(Locale.ROOT, MAIN_SOURCE_TEMPLATE, i, j, evalSource, mainSourceInTextSourceName);
                        Source source = Source.newBuilder("js", mainSource, MAIN_SOURCE_NAME).internal(mainSourceInternal).build();
                        sources.clear();
                        Value val = ctx.eval(source);
                        Assert.assertEquals(2, sources.size());
                        com.oracle.truffle.api.source.Source interceptedMainSource = sources.get(0);
                        com.oracle.truffle.api.source.Source interceptedEvalSource = sources.get(1);
                        Assert.assertEquals(mainSource, interceptedMainSource.getCharacters());
                        switch (i) {
                            case 0:
                                Assert.assertEquals(MAIN_SOURCE_NAME, interceptedMainSource.getName());
                                Assert.assertEquals(j == 1, interceptedMainSource.isInternal());
                                break;
                            case 1:
                                Assert.assertEquals(IN_TEXT_NONINTERNAL_SOURCE_NAME, interceptedMainSource.getName());
                                Assert.assertEquals(j == 1, interceptedMainSource.isInternal());
                                break;
                            case 2:
                                Assert.assertEquals(IN_TEXT_INTERNAL_SOURCE_NAME, interceptedMainSource.getName());
                                Assert.assertTrue(interceptedMainSource.isInternal());
                                break;
                            default:
                                Assert.fail();
                                break;
                        }
                        Assert.assertEquals(evalSource, interceptedEvalSource.getCharacters());
                        switch (k) {
                            case 0:
                                Assert.assertEquals(EVAL_SOURCE_NAME, interceptedEvalSource.getName());
                                Assert.assertFalse(interceptedEvalSource.isInternal());
                                break;
                            case 1:
                                Assert.assertEquals(IN_TEXT_NONINTERNAL_SOURCE_NAME, interceptedEvalSource.getName());
                                Assert.assertFalse(interceptedEvalSource.isInternal());
                                break;
                            case 2:
                                Assert.assertEquals(IN_TEXT_INTERNAL_SOURCE_NAME, interceptedEvalSource.getName());
                                Assert.assertTrue(interceptedEvalSource.isInternal());
                                break;
                            default:
                                Assert.fail();
                                break;
                        }
                        Assert.assertEquals(i + j + i + j + k, val.asInt());
                    }
                }
            }
        }
    }
}
