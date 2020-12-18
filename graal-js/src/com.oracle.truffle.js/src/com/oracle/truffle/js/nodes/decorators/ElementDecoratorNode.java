package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.ObjectFunctionBuiltins;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNodeGen;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNode;
import com.oracle.truffle.js.nodes.access.IteratorGetNextValueNodeGen;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNodeGen;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.util.IteratorUtil;
import com.sun.tools.javac.util.Iterators;

import java.util.ArrayList;
import java.util.List;

public class ElementDecoratorNode extends JavaScriptBaseNode {
    @Child
    JavaScriptNode expressionNode;
    @Child
    JSFunctionCallNode functionNode;
    @Child
    GetIteratorNode getIteratorNode;
    @Child
    IteratorStepNode stepNode;
    @Child
    IteratorValueNode valueNode;

    public ElementDecoratorNode(JavaScriptNode expressionNode, JSContext context) {
        this.expressionNode = expressionNode;
        this.functionNode = JSFunctionCallNode.createCall();
        this.getIteratorNode = GetIteratorNode.create(context);
        this.stepNode = IteratorStepNode.create(context);
        this.valueNode = IteratorValueNode.create(context);
    }

    //DecorateElement
    public List<ElementDescriptor> executeDecorator(VirtualFrame frame, ElementDescriptor element, JSContext context) {
        if(element.isHook()) {
            //TODO: throw TypeError
        }
        List<ElementDescriptor> elements = new ArrayList<>();
        Object elementObject = DescriptorUtil.fromElementDescriptor(element, context);
        JSFunctionObject function = (JSFunctionObject) expressionNode.execute(frame);
        Object elementExtrasObject = functionNode.executeCall(JSArguments.createOneArg(null, function, elementObject));
        elements.add(DescriptorUtil.toElementDescriptor(elementExtrasObject));
        Object extras = JSOrdinaryObject.get((DynamicObject) elementExtrasObject, "extras");
        if(!JSRuntime.isNullOrUndefined(extras)) {
            //ToElementDescriptors
            IteratorRecord record = getIteratorNode.execute(extras);
            while(!record.isDone()) {
                Object next = stepNode.execute(record);
                if(next == Boolean.FALSE) {

                    return elements;
                }
                Object elementsObject = valueNode.execute((DynamicObject) next);
                ElementDescriptor e = DescriptorUtil.toElementDescriptor(elementsObject);
                if(!JSRuntime.isNullOrUndefined(JSOrdinaryObject.get((DynamicObject) elementsObject, "extras"))) {
                    throw Errors.createTypeError("Extras");
                }
                elements.add(e);
                //TODO: abrupt completion
            }
        }
        return elements;
    }
}
