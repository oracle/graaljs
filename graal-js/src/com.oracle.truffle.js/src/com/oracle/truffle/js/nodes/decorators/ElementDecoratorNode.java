package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;

public class ElementDecoratorNode extends JavaScriptBaseNode {
    @Child
    JavaScriptNode expressionNode;
    @Child
    JSFunctionCallNode functionNode;

    public ElementDecoratorNode(JavaScriptNode expressionNode) {
        this.expressionNode = expressionNode;
        this.functionNode = JSFunctionCallNode.createCall();
    }

    //DecorateElement
    public ElementDescriptor[] executeDecorator(VirtualFrame frame, ElementDescriptor element, JSContext context) {
        if(element.isHook()) {
            //TODO: throw TypeError
        }
        Object elementObject = DescriptorUtil.fromElementDescriptor(element, context);
        JSFunctionObject function = (JSFunctionObject) expressionNode.execute(frame);
        Object elementExtrasObject = functionNode.executeCall(JSArguments.createOneArg(null, function, elementObject));
        if(JSRuntime.isNullOrUndefined(elementExtrasObject)) {
            return new ElementDescriptor[]{ element };
        }
        return new ElementDescriptor[] { DescriptorUtil.toElementDescriptor(elementExtrasObject) };
    }
}
