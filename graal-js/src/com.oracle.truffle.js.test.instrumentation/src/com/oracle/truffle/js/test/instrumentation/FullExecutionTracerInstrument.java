/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

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
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadVariableExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WritePropertyExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.WriteVariableExpressionTag;

/**
 * Example instrument tracing all execution events in Graal.js.
 */
@Registration(id = FullExecutionTracerInstrument.ID, services = {FullExecutionTracerInstrument.class})
public class FullExecutionTracerInstrument extends TruffleInstrument {

    public static final String ID = "FullExecutionTracerInstrument";

    public Env environment;

    @Override
    protected void onCreate(Env env) {
        this.environment = env;
        env.registerService(this);
        // What source sections are we interested in?
        SourceSectionFilter sourceSectionFilter = SourceSectionFilter.newBuilder().tagIs(FineGrainedAccessTest.allJSSpecificTags).build();
        // What generates the input events to track?
        SourceSectionFilter inputGeneratingObjects = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class,
                        StandardTags.StatementTag.class).build();
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

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext i, int inputIndex, Object inputValue) {
                        Object val = inputValue != null ? inputValue.toString() : "null";
                        String format = String.format("%-7s|tag: %-20s @ %-20s|val: %-25s|from: %-20s", "IN " + (1 + inputIndex) + "/" + getInputCount(),
                                        FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), val, i.getInstrumentedNode().getClass().getSimpleName());
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
