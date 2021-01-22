package com.oracle.truffle.js.nodes.decorators;

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
            if (toElementDescriptorsNode.toElementDescriptors(elementsObject,elements)) {
                HashMap<Object, ElementDescriptor> elementsMap = new HashMap<>();
                for(int i = 0; i < elements.size(); i++) {
                    ElementDescriptor a = elements.dequeue();
                    if(!elementsMap.containsKey(a.getKey())) {
                        elementsMap.put(a.getKey(), a);
                    } else {
                        ElementDescriptor b = elementsMap.get(a.getKey());
                        if(!a.isHook() && !b.isHook() && a.getPlacement() == b.getPlacement()) {
                            throw Errors.createTypeError(String.format("Duplicate definition of class element %s.", a.getKey()), this);
                            //TODO: test
                        }
                    }
                    elements.enqueue(a);
                }
            }
        }
    }
}
