/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.builtins.DebugBuiltinsFactory.DebugTypedArrayDetachBufferNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentBroadcastNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentGetReportNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentLeavingNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentReceiveBroadcastNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentReportNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentSleepNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262AgentStartNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262CreateRealmNodeGen;
import com.oracle.truffle.js.builtins.Test262BuiltinsFactory.Test262EvalScriptNodeGen;
import com.oracle.truffle.js.nodes.access.RealmNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.Evaluator;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTest262;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DebugJSAgent;

/**
 * Contains builtins to support special behavior used by Test262.
 */
public final class Test262Builtins extends JSBuiltinsContainer.SwitchEnum<Test262Builtins.Test262> {

    protected Test262Builtins() {
        super(JSTest262.CLASS_NAME, Test262.class);
    }

    public enum Test262 implements BuiltinEnum<Test262> {
        createRealm(0),
        evalScript(1),
        typedArrayDetachBuffer(1),

        agentStart(1),
        agentBroadcast(1),
        agentGetReport(0),
        agentSleep(1),
        agentReceiveBroadcast(1),
        agentReport(1),
        agentLeaving(0);

        private final int length;

        Test262(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Test262 builtinEnum) {
        switch (builtinEnum) {
            case typedArrayDetachBuffer:
                return DebugTypedArrayDetachBufferNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case createRealm:
                return Test262CreateRealmNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case evalScript:
                return Test262EvalScriptNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));

            default:
                if (!JSTruffleOptions.SubstrateVM) {
                    switch (builtinEnum) {
                        case agentStart:
                            return Test262AgentStartNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                        case agentBroadcast:
                            return Test262AgentBroadcastNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                        case agentGetReport:
                            return Test262AgentGetReportNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                        case agentSleep:
                            return Test262AgentSleepNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                        case agentReceiveBroadcast:
                            return Test262AgentReceiveBroadcastNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                        case agentReport:
                            return Test262AgentReportNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                        case agentLeaving:
                            return Test262AgentLeavingNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context));
                    }
                }
        }
        return null;
    }

    /**
     * Used by test262mockup.js.
     */
    public abstract static class Test262EvalScriptNode extends JSBuiltinNode {
        @Child private RealmNode realmNode;

        public Test262EvalScriptNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.realmNode = RealmNode.create(context);
        }

        @Specialization
        protected Object evalScript(VirtualFrame frame, Object obj) {
            String sourceText = JSRuntime.toString(obj);
            JSRealm realm = realmNode.execute(frame);
            return evalScript(realm, sourceText);
        }

        @TruffleBoundary
        private Object evalScript(JSRealm realm, String sourceText) {
            Source source = Source.newBuilder(sourceText).name(Evaluator.EVAL_SOURCE_NAME).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).build();
            return realm.getContext().getEvaluator().evaluate(realm, this, source);
        }

    }

    /**
     * Used by test262mockup.js.
     */
    public abstract static class Test262CreateRealmNode extends JSBuiltinNode {

        public Test262CreateRealmNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object createRealm() {
            return createChildRealm().getGlobalObject();
        }

        @TruffleBoundary
        private JSRealm createChildRealm() {
            return getContext().createChildContext().getRealm();
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentStart extends JSBuiltinNode {

        public Test262AgentStart(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object start(Object obj) {
            String sourceText = JSRuntime.toString(obj);
            return ((DebugJSAgent) getContext().getJSAgent()).startNewAgent(sourceText);
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentBroadcast extends JSBuiltinNode {

        public Test262AgentBroadcast(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object broadcast(Object sab) {
            ((DebugJSAgent) getContext().getJSAgent()).broadcast(sab);
            return Undefined.instance;
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentGetReport extends JSBuiltinNode {

        public Test262AgentGetReport(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object getReport() {
            return ((DebugJSAgent) getContext().getJSAgent()).getReport();
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentSleep extends JSBuiltinNode {

        public Test262AgentSleep(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doSleep(int time) {
            ((DebugJSAgent) getContext().getJSAgent()).sleep(time);
            return Undefined.instance;
        }

        @Specialization
        protected Object doSleep(@SuppressWarnings("unused") Object time) {
            throw Errors.createTypeError("Integer expected");
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentReceiveBroadcast extends JSBuiltinNode {

        public Test262AgentReceiveBroadcast(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object receiveBroadcast(Object lambda) {
            ((DebugJSAgent) getContext().getJSAgent()).setDebugReceiveBroadcast(lambda);
            return Undefined.instance;
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentReport extends JSBuiltinNode {

        public Test262AgentReport(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object report(Object value) {
            ((DebugJSAgent) getContext().getJSAgent()).report(value);
            return Undefined.instance;
        }
    }

    /**
     * Used by test262mockup.js to test concurrent agents.
     */
    public abstract static class Test262AgentLeaving extends JSBuiltinNode {

        public Test262AgentLeaving(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object leaving() {
            ((DebugJSAgent) getContext().getJSAgent()).leaving();
            return Undefined.instance;
        }
    }
}
