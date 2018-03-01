/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.GeneratorPrototypeBuiltinsFactory.GeneratorResumeNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.InternalCallNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Completion;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains built-in methods of Generator.prototype.
 */
public final class GeneratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<GeneratorPrototypeBuiltins.GeneratorPrototype> {

    protected GeneratorPrototypeBuiltins() {
        super(JSFunction.GENERATOR_PROTOTYPE_NAME, GeneratorPrototype.class);
    }

    public enum GeneratorPrototype implements BuiltinEnum<GeneratorPrototype> {
        next(1),
        return_(1),
        throw_(1);

        private final int length;

        GeneratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, GeneratorPrototype builtinEnum) {
        assert context.getEcmaScriptVersion() >= 6;
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
        return GeneratorResumeNodeGen.create(context, builtin, resumeMethod, args().withThis().fixedArgs(1).createArgumentNodes(context));
    }

    public abstract static class GeneratorResumeNode extends JSBuiltinNode {
        private final Completion.Type resumeType;
        @Child private PropertyGetNode getGeneratorTarget;
        @Child private InternalCallNode callNode;

        public GeneratorResumeNode(JSContext context, JSBuiltin builtin, Completion.Type resumeType) {
            super(context, builtin);
            this.resumeType = resumeType;
            this.getGeneratorTarget = PropertyGetNode.create(JSFunction.GENERATOR_TARGET_ID, false, context);
            this.callNode = InternalCallNode.create();
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected Object resume(DynamicObject thisObj, Object value) {
            Object generatorTarget = getGeneratorTarget.getValue(thisObj);
            if (generatorTarget != Undefined.instance) {
                CallTarget callTarget = (CallTarget) generatorTarget;
                return callNode.execute(callTarget, new Object[]{thisObj, value, resumeType});
            } else {
                throw Errors.createTypeError("not a generator function");
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object resume(Object thisObj, Object value) {
            throw Errors.createTypeErrorObjectExpected();
        }
    }
}
