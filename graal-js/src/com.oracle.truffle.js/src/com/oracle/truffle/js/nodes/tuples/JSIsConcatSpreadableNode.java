package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Represents the abstract operation IsConcatSpreadable.
 */
public abstract class JSIsConcatSpreadableNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getSpreadableNode;
    @Child private JSToBooleanNode toBooleanNode;
    @Child private JSIsTupleNode isTupleNode;
    @Child private JSIsArrayNode isArrayNode;

    protected final JSContext context;

    protected JSIsConcatSpreadableNode(JSContext context) {
        super();
        this.context = context;
    }

    public abstract boolean execute(Object operand);

    @Specialization
    protected boolean doObject(Object o) {
        if (!JSRuntime.isObject(o) && !JSRuntime.isTuple(o)) {
            return false;
        }
        if (JSDynamicObject.isJSDynamicObject(o)) {
            Object spreadable = getSpreadableProperty(o);
            if (spreadable != Undefined.instance) {
                return toBoolean(spreadable);
            }
        }
        return isTuple(o) || isArray(o);
    }

    private boolean isArray(Object object) {
        if (isArrayNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isArrayNode = insert(JSIsArrayNode.createIsArrayLike());
        }
        return isArrayNode.execute(object);
    }

    private boolean isTuple(Object object) {
        if (isTupleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isTupleNode = insert(JSIsTupleNode.create());
        }
        return isTupleNode.execute(object);
    }

    private Object getSpreadableProperty(Object obj) {
        if (getSpreadableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSpreadableNode = insert(PropertyGetNode.create(Symbol.SYMBOL_IS_CONCAT_SPREADABLE, context));
        }
        return getSpreadableNode.getValue(obj);
    }

    protected boolean toBoolean(Object target) {
        if (toBooleanNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toBooleanNode = insert(JSToBooleanNode.create());
        }
        return toBooleanNode.executeBoolean(target);
    }

    public static JSIsConcatSpreadableNode create(JSContext context) {
        return JSIsConcatSpreadableNodeGen.create(context);
    }
}
