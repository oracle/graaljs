/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.InputNodeTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

/**
 * Example instrument tracing all execution events in Graal.js.
 */
@Registration(id = FullExecutionTracerInstrument.ID, services = {FullExecutionTracerInstrument.class})
public class FullExecutionTracerInstrument extends TruffleInstrument {

    public static final String ID = "FullExecutionTracerInstrument";

    public Env environment;

    public static void main(String[] args) throws IOException {
        Context c = Context.create("js");
        c.getEngine().getInstruments().get(ID).lookup(FullExecutionTracerInstrument.class);
        c.eval(Source.newBuilder("js", new File(args[0])).build());
    }

    public static void trace(String code) {
        Context c = Context.create("js");
        c.getEngine().getInstruments().get(ID).lookup(FullExecutionTracerInstrument.class);
        c.eval(Source.create("js", code));
    }

    @Override
    protected void onCreate(Env env) {
        this.environment = env;
        env.registerService(this);
        // What source sections are we interested in?
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(JSTags.ALL).build();
        // What generates the input events to track?
        SourceSectionFilter inputGeneratingObjects = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class,
                        StandardTags.StatementTag.class,
                        InputNodeTag.class).build();
        env.getInstrumenter().attachExecutionEventFactory(sourceSectionFilter, inputGeneratingObjects, getFactory());
    }

    private static ExecutionEventNodeFactory getFactory() {
        ExecutionEventNodeFactory factory = new ExecutionEventNodeFactory() {

            private int depth = 0;

            @Override
            public ExecutionEventNode create(EventContext c) {
                return new ExecutionEventNode() {

                    private void log(String s) {
                        String p = "";
                        int d = depth;
                        while (d-- > 0) {
                            p += "    ";
                        }
                        System.out.println(p + s);
                    }

                    private String getValueDescription(Object inputValue) {
                        if (JSFunction.isJSFunction(inputValue)) {
                            return "JSFunction:'" + JSFunction.getName((DynamicObject) inputValue) + "'";
                        }
                        return inputValue != null ? inputValue.toString() : "null";
                    }

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext i, int inputIndex, Object inputValue) {
                        String format = String.format("%-7s|tag: %-20s @ %-20s|val: %-25s|from: %-20s", "IN " + (1 + inputIndex) + "/" + getInputCount(),
                                        FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getValueDescription(inputValue), i.getInstrumentedNode().getClass().getSimpleName());
                        log(format);
                    }

                    @Override
                    public void onEnter(VirtualFrame frame) {
                        String format = String.format("%-7s|tag: %-20s @ %-20s |attr: %-20s", "ENTER", FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                        depth++;
                    }

                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RETURN", FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), result, getAttributesDescription(c));
                        log(format);
                    }

                    @Override
                    protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
                        depth--;
                        String format = String.format("%-7s|tag: %-20s @ %-20s |rval: %-20s |attr: %-20s", "RET-EXC", FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), exception.getClass().getSimpleName(), getAttributesDescription(c));
                        log(format);
                    }

                    private String getAttributeFrom(EventContext cx, String name) {
                        try {
                            return (String) ForeignAccess.sendRead(Message.READ.createNode(), (TruffleObject) ((InstrumentableNode) cx.getInstrumentedNode()).getNodeObject(), name);
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private String getAttributesDescription(EventContext cx) {
                        String extra = "";
                        JavaScriptNode n = (JavaScriptNode) cx.getInstrumentedNode();
                        if (n.hasTag(BuiltinRootTag.class)) {
                            String tagAttribute = getAttributeFrom(cx, "name");
                            extra += tagAttribute;
                        }
                        if (n.hasTag(ReadPropertyExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(ReadVariableExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(WritePropertyExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(WriteVariableExpressionTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
                        }
                        return extra;
                    }
                };
            }
        };
        return factory;
    }

}
