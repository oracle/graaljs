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

import java.util.HashSet;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags.RootBodyTag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

public class GR35314 {

    protected Context context;
    protected Instrumenter instrumenter;
    protected TestingExecutionInstrument instrument;

    @Before
    public void initTest() {
        context = TestUtil.newContextBuilder().option("engine.WarnInterpreterOnly", "false").build();
        instrument = context.getEngine().getInstruments().get(TestingExecutionInstrument.ID).lookup(TestingExecutionInstrument.class);
        instrumenter = instrument.getEnvironment().getInstrumenter();
    }

    @Test
    public void basicScopeGet() {
        assertHasSymbols(evalScopes("(function(x) { return x; })(42);", RootBodyTag.class), "x", "this");
    }

    @Test
    public void testBlockSwitchRoot() {
        final String gr35314 = "'use strict';" +
                        "function crash(x) {" +
                        "  switch (x) {" +
                        "    case 'foo':" +
                        "      var local = 42;" +
                        "      break;" +
                        "  }" +
                        "};" +
                        "crash('foo');";
        assertHasSymbols(evalScopes(gr35314, RootBodyTag.class), "x", "local", "this");
    }

    private static void assertHasSymbols(Set<String> collected, String... expected) {
        for (String s : expected) {
            if (!collected.contains(s)) {
                throw new AssertionError("Symbols collected by instrument do not contain: " + s);
            }
        }
    }

    protected Set<String> evalScopes(String src, Class<?>... tags) {
        final Set<String> scopeSymbols = new HashSet<>();
        SourceSectionFilter expFilter = SourceSectionFilter.newBuilder().tagIs(tags).includeInternal(false).build();
        instrumenter.attachExecutionEventFactory(expFilter, eventContext -> new ExecutionEventNode() {

            @Child private InteropLibrary interopLib = InteropLibrary.getUncached();
            @Child private NodeLibrary nodeLib = NodeLibrary.getFactory().getUncached();

            @Override
            protected void onEnter(VirtualFrame frame) {
                try {
                    Object scope = nodeLib.getScope(eventContext.getInstrumentedNode(), frame.materialize(), true);
                    Object members = interopLib.getMembers(scope, true);
                    if (interopLib.hasArrayElements(members)) {
                        long arraySize = interopLib.getArraySize(members);
                        for (int i = 0; i < arraySize; i++) {
                            Object element = interopLib.readArrayElement(members, i);
                            scopeSymbols.add(element.toString());
                        }
                    }
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    throw new AssertionError(e);
                }
            }
        });
        context.eval("js", src);
        return scopeSymbols;
    }

}
