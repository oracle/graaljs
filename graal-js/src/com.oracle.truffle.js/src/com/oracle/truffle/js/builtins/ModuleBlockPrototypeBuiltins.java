package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.builtins.ArrayIteratorPrototypeBuiltinsFactory.ArrayIteratorNextNodeGen;
import com.oracle.truffle.js.builtins.ArrayPrototypeBuiltinsFactory.JSArrayToStringNodeGen;
import com.oracle.truffle.js.builtins.ModuleBlockPrototypeBuiltinsFactory.JSModuleBlockToStringNodeGen;
import com.oracle.truffle.js.builtins.DatePrototypeBuiltins.JSDateOperation;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.module.ModuleBlockNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSDate;
import com.oracle.truffle.js.runtime.builtins.JSModuleBlock;
import com.oracle.truffle.js.runtime.util.WeakMap;

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

        JSContext context_;
        DynamicObject prototype;

        public JSModuleBlockToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            prototype = context.getRealm().getModuleBlockPrototype();
        }

        // TODO
        @Specialization(guards = "isJSOrdinaryObject(thisModuleBlock)")
        protected String doOperation(Object thisModuleBlock) {
            // TODO Type check, then check for hidden property body and return hidden property
            // sourceText

            if (DynamicObjectLibrary.getUncached().containsKey((DynamicObject) thisModuleBlock, (Object) ModuleBlockNode.getModuleBodyKey())) {
                PropertyGetNode getSourceCode = PropertyGetNode.createGetHidden(ModuleBlockNode.getModuleSourceKey(), this.getContext());

                Object sourceCode = getSourceCode.getValue(thisModuleBlock);

                return "module {" + sourceCode.toString() + " }";
            } else if (thisModuleBlock.equals(this.getContext().getRealm().getModuleBlockPrototype())) {
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