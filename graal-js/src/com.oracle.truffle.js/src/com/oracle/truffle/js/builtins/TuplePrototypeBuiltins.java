package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTuple;

/**
 * Contains builtins for Tuple.prototype.
 */
public final class TuplePrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TuplePrototypeBuiltins.TuplePrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TuplePrototypeBuiltins();

    protected TuplePrototypeBuiltins() {
        super(JSTuple.PROTOTYPE_NAME, TuplePrototype.class);
    }

    public enum TuplePrototype implements BuiltinEnum<TuplePrototype> {
        toString(0),
        values(0);

        private final int length;

        TuplePrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TuplePrototype builtinEnum) {
        switch (builtinEnum) {
            case toString:
                return TuplePrototypeBuiltinsFactory.JSTupleToStringNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case values:
                return TuplePrototypeBuiltinsFactory.TupleIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_VALUE, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTupleToStringNode extends JSBuiltinNode {

        public JSTupleToStringNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String toString(Tuple thisObj) {
            return thisObj.toString();
        }

        @Specialization(guards = {"isJSTuple(thisObj)"})
        protected String toString(DynamicObject thisObj) {
            return JSTuple.valueOf(thisObj).toString();
        }

        @Fallback
        protected void toStringNoTuple(Object thisObj) {
            throw Errors.createTypeError("Tuple.prototype.toString requires that 'this' be a Tuple");
        }
    }

    public abstract static class TupleIteratorNode extends JSBuiltinNode {
        @Child private ArrayPrototypeBuiltins.CreateArrayIteratorNode createArrayIteratorNode;

        public TupleIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.createArrayIteratorNode = ArrayPrototypeBuiltins.CreateArrayIteratorNode.create(context, iterationKind);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        protected DynamicObject doJSObject(VirtualFrame frame, DynamicObject thisObj) {
            return createArrayIteratorNode.execute(frame, thisObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected DynamicObject doNotJSObject(
                VirtualFrame frame,
                Object thisObj,
                @Cached("createToObject(getContext())") JSToObjectNode toObjectNode
        ) {
            return createArrayIteratorNode.execute(frame, toObjectNode.execute(thisObj));
        }
    }
}
