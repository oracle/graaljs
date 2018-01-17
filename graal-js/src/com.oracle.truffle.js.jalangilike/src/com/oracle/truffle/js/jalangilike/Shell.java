package com.oracle.truffle.js.jalangilike;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.js.jalangilike.tests.FineGrainedAccessTest;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.shell.JSLauncher;

public class Shell extends JSLauncher {

    private static long X = 0;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (X != 0) {
                    System.out.println("______ " + X);
                }
            }
        });
    }

    @Override
    protected void preEval(Context context) {

        MyBasicExecutionTracer instrument = context.getEngine().getInstruments().get(MyBasicExecutionTracer.ID).lookup(MyBasicExecutionTracer.class);

        Instrumenter instrumenter = instrument.environment.getInstrumenter();

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
                        String format = String.format("%-7s|tag: %-20s @ %-20s|val: %-25s|from: %-20s", "INPUT", FineGrainedAccessTest.getTagNames((JavaScriptNode) c.getInstrumentedNode()),
                                        c.getInstrumentedNode().getClass().getSimpleName(), inputValue.toString(), i.getInstrumentedNode().getClass().getSimpleName());
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
                        if (n.hasTag(PropertyReadTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(VariableReadTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(PropertyWriteTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "key") + "' ";
                            extra += tagAttribute;
                        }
                        if (n.hasTag(VariableWriteTag.class)) {
                            String tagAttribute = "'" + getAttributeFrom(cx, "name") + "' ";
                            extra += tagAttribute;
                        }
                        return extra;
                    }
                };
            }
        };

        SourceSectionFilter expressionFilter = SourceSectionFilter.newBuilder().tagIs(FineGrainedAccessTest.allJSSpecificTags).build();

        SourceSectionFilter inputFilter = SourceSectionFilter.newBuilder().tagIs(
                        StandardTags.ExpressionTag.class
        //
        ).build();

        instrumenter.attachExecutionEventFactory(expressionFilter, inputFilter, factory);
        System.out.println("attached");
    }

    public static void main(String[] args) {
        new Shell().launch(args);
    }

}
