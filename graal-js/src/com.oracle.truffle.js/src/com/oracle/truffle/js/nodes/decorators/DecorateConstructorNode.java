package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

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

    public ClassElementList decorateConstructor(ClassElementList elements, Object[] decorators) {
        ClassElementList newElements = ClassElementList.create();
        for(Object decorator: decorators) {
            Object obj = ElementDescriptorUtil.fromClassDescriptor(elements, context);
            Object result = decoratorCallNode.executeCall(JSArguments.createOneArg(null, decorator, obj));
            if (result == Undefined.instance) {
                result = obj;
            } else {
                result = JSRuntime.toObject(context, result);
            }
            ElementDescriptorUtil.checkClassDescriptor(result,this);
            Object elementsObject = getElementsObject((DynamicObject)result);
            toElementDescriptorsNode.toElementDescriptors(elementsObject, newElements);
            //duplicates are checked in ClassElementList.
        }
        return newElements;
    }

    @TruffleBoundary
    Object getElementsObject(DynamicObject result) {
        return JSOrdinaryObject.get(result, ELEMENTS);
    }
}
