package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;

import java.util.ArrayList;
import java.util.List;

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

    public List<ElementDescriptor> decorateElement(VirtualFrame frame, ElementDescriptor element) {
        List<ElementDescriptor> extras = new ArrayList<>();
        for(Object decorator: element.getDecorators()) {
            if (element.isHook()) {
                throw Errors.createTypeError("Property kind of element descriptor must not have value 'hook'.");
            }
            Object elementObject = DescriptorUtil.fromElementDescriptor(element, context);
            Object elementExtrasObject = decoratorCallNode.executeCall(JSArguments.createOneArg(null, decorator, elementObject));
            if (JSRuntime.isNullOrUndefined(extras)) {
                elementExtrasObject = elementObject;
            } else {
                elementExtrasObject = JSRuntime.toObject(context, elementExtrasObject);
            }
            //ToElementExtras
            element = DescriptorUtil.toElementDescriptor(elementExtrasObject);
            Object extrasObject = JSOrdinaryObject.get((DynamicObject) elementExtrasObject, EXTRAS);
            List<ElementDescriptor> newExtras = toElementDescriptorsNode.toElementDescriptors(extrasObject);
            if(newExtras != null) {
                extras.addAll(newExtras);
            }
        }
        extras.add(element);
        return extras;
    }
}
