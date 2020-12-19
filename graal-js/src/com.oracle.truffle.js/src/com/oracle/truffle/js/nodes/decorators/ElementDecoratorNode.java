package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
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
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;

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
    @Child
    IteratorCloseNode closeNode;
    @Child
    JSGetLengthNode lengthNode;

    private int instanceFields = 0;
    private int staticFields = 0;

    public ElementDecoratorNode(JavaScriptNode expressionNode, JSContext context) {
        this.expressionNode = expressionNode;
        this.functionNode = JSFunctionCallNode.createCall();
        this.getIteratorNode = GetIteratorNode.create(context);
        this.stepNode = IteratorStepNode.create(context);
        this.valueNode = IteratorValueNode.create(context);
        this.closeNode = IteratorCloseNode.create(context);
        this.lengthNode = JSGetLengthNode.create(context);
    }

    //DecorateElement
    public ElementDescriptor[] executeDecorator(VirtualFrame frame, ElementDescriptor element, JSContext context) {
        if(element.isHook()) {
            throw Errors.createTypeError("Property kind of element descriptor must not have value 'hook'.");
        }
        Object elementObject = DescriptorUtil.fromElementDescriptor(element, context);
        JSFunctionObject function = (JSFunctionObject) expressionNode.execute(frame);
        Object elementExtrasObject = functionNode.executeCall(JSArguments.createOneArg(null, function, elementObject));
        if(JSRuntime.isNullOrUndefined(elementExtrasObject))
        {
            return new ElementDescriptor[] { element };
        }
        //ToElementExtras
        ElementDescriptor elementExtra = DescriptorUtil.toElementDescriptor(elementExtrasObject);
        Object extras = JSOrdinaryObject.get((DynamicObject) elementExtrasObject, "extras");
        if(JSRuntime.isNullOrUndefined(extras)) {
            return new ElementDescriptor[] { DescriptorUtil.toElementDescriptor(elementExtrasObject) };
        } else {
            //ToElementDescriptors
            //TODO: cast
            int length = (int)lengthNode.executeLong(extras);
            ElementDescriptor[] elements = new ElementDescriptor[length + 1];
            elements[0] = elementExtra;
            int extrasIndex = 1;
            IteratorRecord record = getIteratorNode.execute(extras);
            try {
                while (true) {
                    Object next = stepNode.execute(record);
                    if (next == Boolean.FALSE) {

                        return elements;
                    }
                    Object elementsObject = valueNode.execute((DynamicObject) next);
                    ElementDescriptor e = DescriptorUtil.toElementDescriptor(elementsObject);
                    if (!JSRuntime.isNullOrUndefined(JSOrdinaryObject.get((DynamicObject) elementsObject, "extras"))) {
                        throw Errors.createTypeError("Property extras of element descriptor must not have nested extras.");
                    }
                    elements[extrasIndex++] = e;
                }
            } catch (Exception ex) {
                closeNode.executeAbrupt(record.getIterator());
                throw ex;
            }
        }
    }
}
