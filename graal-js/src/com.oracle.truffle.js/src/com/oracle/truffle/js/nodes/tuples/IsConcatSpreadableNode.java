package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class IsConcatSpreadableNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getSpreadableNode;
    @Child private JSIsArrayNode isArrayNode;
    @Child private JSToBooleanNode toBooleanNode;

    protected final JSContext context;

    protected IsConcatSpreadableNode(JSContext context) {
        super();
        this.context = context;
    }

    public abstract boolean execute(Object operand);

    @Specialization
    protected boolean doObject(Object o) {
        if (o == Undefined.instance || o == Null.instance) {
            return false;
        }
        if (JSDynamicObject.isJSDynamicObject(o)) {
            DynamicObject obj = (DynamicObject) o;
            Object spreadable = getSpreadableProperty(obj);
            if (spreadable != Undefined.instance) {
                return toBoolean(spreadable);
            }
        }
        return isArray(o);
    }

    private boolean isArray(Object object) {
        if (isArrayNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isArrayNode = insert(JSIsArrayNode.createIsArrayLike());
        }
        return isArrayNode.execute(object);
    }

    private Object getSpreadableProperty(Object obj) {
        if (getSpreadableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSpreadableNode = insert(PropertyGetNode.create(Symbol.SYMBOL_IS_CONCAT_SPREADABLE, false, context));
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

    public static IsConcatSpreadableNode create(JSContext context) {
        return IsConcatSpreadableNodeGen.create(context);
    }
}
