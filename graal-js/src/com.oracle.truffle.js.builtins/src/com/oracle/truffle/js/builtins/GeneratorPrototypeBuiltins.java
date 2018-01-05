/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.GeneratorPrototypeBuiltinsFactory.GeneratorResumeNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunction.GeneratorResumeMethod;
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
        GeneratorResumeMethod resumeMethod;
        switch (builtinEnum) {
            case next:
                resumeMethod = GeneratorResumeMethod.Next;
                break;
            case return_:
                resumeMethod = GeneratorResumeMethod.Return;
                break;
            case throw_:
                resumeMethod = GeneratorResumeMethod.Throw;
                break;
            default:
                return null;
        }
        return GeneratorResumeNodeGen.create(context, builtin, resumeMethod, args().withThis().fixedArgs(1).createArgumentNodes(context));
    }

    public abstract static class GeneratorResumeNode extends JSBuiltinNode {
        private final GeneratorResumeMethod method;
        @Child private PropertyGetNode getGeneratorTarget;
        @Child private IndirectCallNode callNode;

        public GeneratorResumeNode(JSContext context, JSBuiltin builtin, GeneratorResumeMethod method) {
            super(context, builtin);
            this.method = method;
            this.getGeneratorTarget = PropertyGetNode.create(JSFunction.GENERATOR_TARGET_ID, false, context);
            this.callNode = IndirectCallNode.create();
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected Object resume(DynamicObject thisObj, Object value) {
            Object generatorTarget = getGeneratorTarget.getValue(thisObj);
            if (generatorTarget != Undefined.instance) {
                CallTarget callTarget = (CallTarget) generatorTarget;
                return callNode.call(callTarget, new Object[]{thisObj, value, method});
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
