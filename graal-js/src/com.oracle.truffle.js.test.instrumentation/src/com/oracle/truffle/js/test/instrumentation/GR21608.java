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
package com.oracle.truffle.js.test.instrumentation;

import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import org.graalvm.polyglot.Context;
import org.junit.Test;

public class GR21608 {

    @Test
    public void testIt() {
        final Context context = TestUtil.newContextBuilder().build();
        // Register first dummy instrument. Will wrap root nodes with JS wrapper nodes.
        context.getEngine().getInstruments().get(DummyInstrument1.ID).lookup(DummyInstrument1.class);
        context.eval(JavaScriptLanguage.ID, "(function() {})();");
        // Register second dummy instrument. Will materialize root nodes with wrapper parent nodes.
        context.getEngine().getInstruments().get(DummyInstrument2.ID).lookup(DummyInstrument2.class);
        context.eval(JavaScriptLanguage.ID, "(function() {})();");
    }

    @TruffleInstrument.Registration(id = DummyInstrument1.ID, services = {DummyInstrument1.class})
    public static class DummyInstrument1 extends TruffleInstrument {
        public static final String ID = "DummyInstrument1";

        @Override
        protected void onCreate(Env env) {
            env.registerService(this);
            SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootTag.class).build();
            env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, context -> null);
        }
    }

    @TruffleInstrument.Registration(id = DummyInstrument2.ID, services = {DummyInstrument2.class})
    public static class DummyInstrument2 extends TruffleInstrument {
        public static final String ID = "DummyInstrument2";

        @Override
        protected void onCreate(Env env) {
            env.registerService(this);
            SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(StandardTags.RootTag.class).build();
            SourceSectionFilter otherFilter = SourceSectionFilter.newBuilder().tagIs(JSTags.DeclareTag.class).build();
            env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, context -> null);
            env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, otherFilter, context -> null);
        }
    }
}
