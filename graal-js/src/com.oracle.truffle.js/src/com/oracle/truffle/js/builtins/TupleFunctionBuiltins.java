package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.tuples.JSIsTupleNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTuple;

/**
 * Contains builtins for Tuple function.
 */
public final class TupleFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TupleFunctionBuiltins.TupleFunction> {

    public static final JSBuiltinsContainer BUILTINS = new TupleFunctionBuiltins();

    protected TupleFunctionBuiltins() {
        super(JSTuple.CLASS_NAME, TupleFunction.class);
    }

    public enum TupleFunction implements BuiltinEnum<TupleFunction> {
        isTuple(1);
        // TODO: from(1),
        // TODO: of(1);

        private final int length;

        TupleFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TupleFunction builtinEnum) {
        switch (builtinEnum) {
            case isTuple:
                return TupleFunctionBuiltinsFactory.TupleIsTupleNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class TupleIsTupleNode extends JSBuiltinNode {

        @Child private JSIsTupleNode isTupleNode = JSIsTupleNode.create();

        public TupleIsTupleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isTuple(Object object) {
            return isTupleNode.execute(object);
        }
    }
}
