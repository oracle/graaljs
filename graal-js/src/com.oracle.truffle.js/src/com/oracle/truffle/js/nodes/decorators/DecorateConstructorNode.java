package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.HashMap;

public class DecorateConstructorNode extends JavaScriptBaseNode {
    private static final String ELEMENTS = "elements";

    protected final JSContext context;

    @Child
    private JSFunctionCallNode decoratorCallNode;
    @Child private ToElementDescriptorsNode toElementDescriptorsNode;

    private DecorateConstructorNode(JSContext context) {
        this.decoratorCallNode = JSFunctionCallNode.createCall();
        this.toElementDescriptorsNode = ToElementDescriptorsNode.create(context);
        this.context = context;
    }

    public static DecorateConstructorNode create(JSContext context) {
        return new DecorateConstructorNode(context);
    }

    @ExplodeLoop
    public void decorateConstructor(ClassElementList elements, Object[] decorators) {
        for(Object decorator: decorators) {
            Object obj = ElementDescriptorUtil.fromClassDescriptor(elements, context);
            Object result = decoratorCallNode.executeCall(JSArguments.createOneArg(null, decorator, obj));
            if (result == Undefined.instance) {
                result = obj;
            } else {
                result = JSRuntime.toObject(context, result);
            }
            ElementDescriptorUtil.checkClassDescriptor(result,this);
            Object elementsObject = JSOrdinaryObject.get((DynamicObject) result, ELEMENTS);
            toElementDescriptorsNode.toElementDescriptors(elementsObject,elements);
            //duplicates are checked in ClassElementList.
        }
    }
}
