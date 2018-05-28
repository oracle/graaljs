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
        @Child private PropertyGetNode getGeneratorContext;
        @Child private InternalCallNode callNode;

        public GeneratorResumeNode(JSContext context, JSBuiltin builtin, Completion.Type resumeType) {
            super(context, builtin);
            this.resumeType = resumeType;
            this.getGeneratorTarget = PropertyGetNode.createGetHidden(JSFunction.GENERATOR_TARGET_ID, context);
            this.getGeneratorContext = PropertyGetNode.createGetHidden(JSFunction.GENERATOR_CONTEXT_ID, context);
            this.callNode = InternalCallNode.create();
        }

        @Specialization(guards = "isJSObject(generator)")
        protected Object resume(DynamicObject generator, Object value) {
            Object generatorTarget = getGeneratorTarget.getValue(generator);
            if (generatorTarget != Undefined.instance) {
                Object generatorContext = getGeneratorContext.getValue(generator);
                return callNode.execute((CallTarget) generatorTarget, new Object[]{generatorContext, generator, value, resumeType});
            } else {
                throw Errors.createTypeErrorGeneratorObjectExpected();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object resume(Object thisObj, Object value) {
            throw Errors.createTypeErrorGeneratorObjectExpected();
        }
    }
}
