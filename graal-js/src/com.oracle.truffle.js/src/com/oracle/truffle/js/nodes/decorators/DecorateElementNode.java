package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;

public class DecorateElementNode extends JavaScriptBaseNode {
    protected static final String EXTRAS = "extras";

    protected final JSContext context;

    @Child
    private JSFunctionCallNode decoratorCallNode;
    @Child private ToElementDescriptorsNode toElementDescriptorsNode;


    private DecorateElementNode(JSContext context) {
        this.decoratorCallNode = JSFunctionCallNode.createCall();
        this.toElementDescriptorsNode = ToElementDescriptorsNode.create(context);
        this.context = context;
    }

    public static DecorateElementNode create(JSContext context) {
        return new DecorateElementNode(context);
    }

    public void decorateElement(ElementDescriptor element, ClassElementList elements) {
        for(Object decorator: element.getDecorators()) {
            if (element.isHook()) {
                throw Errors.createTypeError("Property kind of element descriptor must not have value 'hook'.", this);
            }
            Object elementObject = ElementDescriptorUtil.fromElementDescriptor(element, context);
            Object decoratedObject = decoratorCallNode.executeCall(JSArguments.createOneArg(null, decorator, elementObject));
            if (!JSRuntime.isNullOrUndefined(decoratedObject)) {
                decoratedObject = elementObject;
            } else {
                decoratedObject = JSRuntime.toObject(context, decoratedObject);
            }
            //ToElementExtras
            assert JSRuntime.isObject(decoratedObject);
            elements.push(ElementDescriptorUtil.toElementDescriptor(decoratedObject, this));
            Object extrasObject = JSOrdinaryObject.get((DynamicObject) decoratedObject, EXTRAS);
            toElementDescriptorsNode.toElementDescriptors(extrasObject, elements);
        }
    }
}
