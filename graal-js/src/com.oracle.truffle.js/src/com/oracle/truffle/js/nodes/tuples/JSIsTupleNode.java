package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Tuple;

/**
 * Represents the abstract operation IsTuple.
 */
public abstract class JSIsTupleNode extends JavaScriptBaseNode {

    protected JSIsTupleNode() {
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
    protected static boolean doOther(@SuppressWarnings("unused") Object object) {
        return false;
    }

    public static JSIsTupleNode create() {
        return JSIsTupleNodeGen.create();
    }
}
