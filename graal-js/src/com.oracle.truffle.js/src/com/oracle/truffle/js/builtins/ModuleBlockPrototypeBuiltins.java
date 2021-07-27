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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.ModuleBlockPrototypeBuiltinsFactory.JSModuleBlockToStringNodeGen;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.module.ModuleBlockNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSModuleBlock;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

/**
 * Contains builtins for %ModuleBlock%.prototype.
 */
public class ModuleBlockPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<ModuleBlockPrototypeBuiltins.ModuleBlockPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new ModuleBlockPrototypeBuiltins();

    protected ModuleBlockPrototypeBuiltins() {
        super(JSModuleBlock.PROTOTYPE_NAME, ModuleBlockPrototype.class);
    }

    public enum ModuleBlockPrototype implements BuiltinEnum<ModuleBlockPrototype> {
        toString(0);

        private final int length;

        ModuleBlockPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, ModuleBlockPrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
                return JSModuleBlockToStringNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSModuleBlockToStringNode extends JSBuiltinNode {
        DynamicObject prototype;

        public JSModuleBlockToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            prototype = context.getRealm().getModuleBlockPrototype();
        }

        @Specialization(guards = "isJSOrdinaryObject(thisModuleBlock)")
        protected String doOperation(Object thisModuleBlock) {
            // Type check, then check for hidden property body and return hidden property
            // sourceText

            if (JSObjectUtil.hasHiddenProperty((DynamicObject) thisModuleBlock,
                            ModuleBlockNode.getModuleBodyKey())) {
                PropertyGetNode getSourceCode = PropertyGetNode.createGetHidden(ModuleBlockNode.getModuleSourceKey(), this.getContext());

                Object sourceCode = getSourceCode.getValue(thisModuleBlock);

                return "module {" + sourceCode.toString() + " }";
            } else if (thisModuleBlock.equals(prototype)) {
                return JSModuleBlock.PROTOTYPE_NAME;
            }

            notJSModuleBlock(thisModuleBlock);

            return "";
        }

        @Specialization(guards = "!isJSOrdinaryObject(thisModuleBlock)")
        protected static boolean notJSModuleBlock(Object thisModuleBlock) {
            throw Errors.createTypeError("Not a module block");
        }

    }
}