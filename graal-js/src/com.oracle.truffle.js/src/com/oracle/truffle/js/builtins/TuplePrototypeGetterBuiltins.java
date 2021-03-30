package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTuple;

/**
 * Contains accessor builtins for Tuple.prototype.
 */
public final class TuplePrototypeGetterBuiltins extends JSBuiltinsContainer.SwitchEnum<TuplePrototypeGetterBuiltins.TuplePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TuplePrototypeGetterBuiltins();

    protected TuplePrototypeGetterBuiltins() {
        super(JSTuple.PROTOTYPE_NAME, TuplePrototype.class);
    }

    public enum TuplePrototype implements BuiltinEnum<TuplePrototype> {
        length;

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public boolean isGetter() {
            return true;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TuplePrototype builtinEnum) {
        switch (builtinEnum) {
            case length:
                return TuplePrototypeGetterBuiltinsFactory.LengthAccessorNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class LengthAccessor extends JSBuiltinNode {

        public LengthAccessor(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected long doTuple(Tuple thisObj) {
            return thisObj.getArraySize();
        }

        @Specialization(guards = {"isJSTuple(thisObj)"})
        protected long doJSTuple(DynamicObject thisObj) {
            return JSTuple.valueOf(thisObj).getArraySize();
        }

        @Fallback
        protected void doObject(Object thisObj) {
            Errors.createTypeErrorIncompatibleReceiver(thisObj);
        }
    }
}
