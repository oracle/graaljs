/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.builtins.AsyncGeneratorPrototypeBuiltinsFactory.AsyncGeneratorResumeNodeGen;
import com.oracle.truffle.js.nodes.control.AsyncGeneratorEnqueueNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Completion;

/**
 * Contains built-in methods of AsyncGenerator.prototype.
 */
public final class AsyncGeneratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<AsyncGeneratorPrototypeBuiltins.AsyncGeneratorPrototype> {

    protected AsyncGeneratorPrototypeBuiltins() {
        super(JSFunction.ASYNC_GENERATOR_PROTOTYPE_NAME, AsyncGeneratorPrototype.class);
    }

    public enum AsyncGeneratorPrototype implements BuiltinEnum<AsyncGeneratorPrototype> {
        next(1),
        return_(1),
        throw_(1);

        private final int length;

        AsyncGeneratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AsyncGeneratorPrototype builtinEnum) {
        assert context.getEcmaScriptVersion() >= 8;
        Completion.Type resumeMethod;
        switch (builtinEnum) {
            case next:
                resumeMethod = Completion.Type.Normal;
                break;
            case return_:
                resumeMethod = Completion.Type.Return;
                break;
            case throw_:
                resumeMethod = Completion.Type.Throw;
                break;
            default:
                return null;
        }
        return AsyncGeneratorResumeNodeGen.create(context, builtin, resumeMethod, args().withThis().fixedArgs(1).createArgumentNodes(context));
    }

    public abstract static class AsyncGeneratorResumeNode extends JSBuiltinNode {
        private final Completion.Type resumeType;
        @Child private AsyncGeneratorEnqueueNode enqueueNode;

        public AsyncGeneratorResumeNode(JSContext context, JSBuiltin builtin, Completion.Type resumeType) {
            super(context, builtin);
            this.resumeType = resumeType;
            this.enqueueNode = AsyncGeneratorEnqueueNode.create(context);
        }

        @Specialization
        protected Object resume(VirtualFrame frame, Object thisObj, Object value) {
            Completion completion = Completion.create(resumeType, value);
            return enqueueNode.execute(frame, thisObj, completion);
        }
    }
}
