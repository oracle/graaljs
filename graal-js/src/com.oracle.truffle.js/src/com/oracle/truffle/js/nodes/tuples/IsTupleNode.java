package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Tuple;

/**
 * Represents abstract operation IsTuple.
 */
public abstract class IsTupleNode extends JavaScriptBaseNode {

    protected IsTupleNode() {
    }

    public abstract boolean execute(Object operand);

    @Specialization
    protected static boolean doTuple(@SuppressWarnings("unused") Tuple tuple) {
        return true;
    }

    @Specialization(guards = "isJSTuple(object)")
    protected static boolean doJSTuple(@SuppressWarnings("unused") DynamicObject object) {
        return true;
    }

    @Fallback
    protected static boolean doNotJSTuple(@SuppressWarnings("unused") Object object) {
        return false;
    }

    public static IsTupleNode create() {
        return IsTupleNodeGen.create();
    }
}
