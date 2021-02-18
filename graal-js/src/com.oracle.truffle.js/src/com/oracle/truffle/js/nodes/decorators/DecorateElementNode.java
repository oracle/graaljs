package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class DecorateElementNode extends JavaScriptBaseNode {
    protected static final String EXTRAS = "extras";

    protected final JSContext context;
    protected final BranchProfile errorBranch = BranchProfile.create();

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
                //Can not test
                errorBranch.enter();
                throw Errors.createTypeErrorElementDescriptorProperty("kind", "must not have value 'hook'.", this);
            }
            Object elementObject = ElementDescriptorUtil.fromElementDescriptor(element, context);
            Object decoratedObject = decoratorCallNode.executeCall(JSArguments.createOneArg(Undefined.instance, decorator, elementObject));
            if (decoratedObject == Undefined.instance) {
                decoratedObject = elementObject;
            } else {
                decoratedObject = JSRuntime.toObject(context, decoratedObject);
            }
            //ToElementExtras
            assert JSRuntime.isObject(decoratedObject);
            elements.enqueue(ElementDescriptorUtil.toElementDescriptor(decoratedObject, this), false, this, true);
            Object extrasObject = JSOrdinaryObject.get((DynamicObject) decoratedObject, EXTRAS);
            toElementDescriptorsNode.toElementDescriptors(extrasObject, elements);
        }
    }
}
