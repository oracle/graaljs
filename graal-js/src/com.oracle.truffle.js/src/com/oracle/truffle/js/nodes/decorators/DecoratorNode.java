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
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;

public abstract class DecoratorNode extends JavaScriptBaseNode {
    protected static final String EXTRAS = "extras";

    protected final JSContext context;

    @Child private JavaScriptNode decoratorExpressionNode;
    @Child private JSFunctionCallNode decoratorCallNode;
    @Child private GetIteratorNode getExtrasIteratorNode;
    @Child private IteratorStepNode extrasIteratorStepNode;
    @Child private IteratorValueNode extrasIteratorValueNode;
    @Child private IteratorCloseNode extrasIteratorCloseNode;
    @Child private JSGetLengthNode extrasLengthNode;

    protected DecoratorNode(JavaScriptNode expressionNode, JSContext context) {
        this.decoratorExpressionNode = expressionNode;
        this.decoratorCallNode = JSFunctionCallNode.createCall();
        this.getExtrasIteratorNode = GetIteratorNode.create(context);
        this.extrasIteratorStepNode = IteratorStepNode.create(context);
        this.extrasIteratorValueNode = IteratorValueNode.create(context);
        this.extrasIteratorCloseNode = IteratorCloseNode.create(context);
        this.extrasLengthNode = JSGetLengthNode.create(context);
        this.context = context;
    }

    public static DecoratorNode createElementDecorator(JavaScriptNode expressionNode, JSContext context) {
        return new ElementDecoratorNode(expressionNode, context);
    }

    public static DecoratorNode createClassDecorator(JavaScriptNode expressionNode, JSContext context) {
        return new ClassDecoratorNode(expressionNode, context);
    }

    protected Object callDecorator(VirtualFrame frame,Object elementObject) {
        Object decoratorFunction = decoratorExpressionNode.execute(frame);
        return decoratorCallNode.executeCall(JSArguments.createOneArg(null, decoratorFunction, elementObject));
    }

    protected int getObjectLength(Object extras) {
        return (int)extrasLengthNode.execute(extras);
    }

    protected IteratorRecord getIterator(Object extras) {
        return getExtrasIteratorNode.execute(extras);
    }

    protected Object getNext(IteratorRecord iteratorRecord) {
        try {
            Object next = extrasIteratorStepNode.execute(iteratorRecord);
            if (next == Boolean.FALSE) {
                return null;
            }
            return extrasIteratorValueNode.execute((DynamicObject) next);
        } catch (Exception ex) {
            extrasIteratorCloseNode.executeAbrupt(iteratorRecord.getIterator());
            throw ex;
        }
    }

    protected void checkEmptyExtras(Object extra) {
        if(!JSRuntime.isNullOrUndefined(JSOrdinaryObject.get((DynamicObject) extra, EXTRAS))) {
            throw Errors.createTypeError("Property extras of element descriptor must not have nested extras.");
        }
    }

    public abstract ElementDescriptor[] executeElementDecorator(VirtualFrame frame, ElementDescriptor descriptor);
    public abstract ElementDescriptor[] executeClassDecorator(VirtualFrame frame, ElementDescriptor[] descriptors);

    private static class ElementDecoratorNode extends DecoratorNode {
        protected  ElementDecoratorNode(JavaScriptNode expressionNode, JSContext context) {
            super(expressionNode, context);
        }

        @Override
        public ElementDescriptor[] executeElementDecorator(VirtualFrame frame, ElementDescriptor descriptor) {
            if(descriptor.isHook()) {
                throw Errors.createTypeError("Property kind of element descriptor must not have value 'hook'.");
            }
            Object elementObject = DescriptorUtil.fromElementDescriptor(descriptor, context);
            Object element = callDecorator(frame,elementObject);
            if(JSRuntime.isNullOrUndefined(element)) {
                return new ElementDescriptor[] { descriptor };
            }
            Object elementExtrasObject = JSOrdinaryObject.get((DynamicObject) element,EXTRAS);
            if(JSRuntime.isNullOrUndefined(elementExtrasObject)) {
                return new ElementDescriptor[] {DescriptorUtil.toElementDescriptor(element)};
            } else {
                int elementsIndex = 0;
                IteratorRecord iterator = getIterator(elementExtrasObject);
                ElementDescriptor[] elements = new ElementDescriptor[getObjectLength(elementExtrasObject) + 1];
                elements[elementsIndex++] = DescriptorUtil.toElementDescriptor(element);
                Object extra;
                while((extra = getNext(iterator)) != null) {
                    checkEmptyExtras(extra);
                    elements[elementsIndex++] = DescriptorUtil.toElementDescriptor(extra);
                }
                return elements;
            }
        }

        @Override
        public ElementDescriptor[] executeClassDecorator(VirtualFrame frame, ElementDescriptor[] descriptors) {
            throw Errors.shouldNotReachHere();
        }
    }

    private static class ClassDecoratorNode extends DecoratorNode {
        private static final String ELEMENTS = "elements";

        protected ClassDecoratorNode(JavaScriptNode expressionNode, JSContext context) {
            super(expressionNode, context);
        }

        @Override
        public ElementDescriptor[] executeElementDecorator(VirtualFrame frame, ElementDescriptor descriptor) {
            throw Errors.shouldNotReachHere();
        }

        @Override
        public ElementDescriptor[] executeClassDecorator(VirtualFrame frame, ElementDescriptor[] descriptors) {
            Object obj = DescriptorUtil.fromClassDescriptor(descriptors,context);
            Object result = callDecorator(frame,obj);
            if(JSRuntime.isNullOrUndefined(result)) {
                result = obj;
            }
            DescriptorUtil.checkClassDescriptor(result);
            Object elementsObject = JSOrdinaryObject.get((DynamicObject) result, ELEMENTS);
            if(JSRuntime.isNullOrUndefined(elementsObject)) {
                return descriptors;
            } else {
                //TODO:check for duplicates
                int elementsIndex = 0;
                IteratorRecord iterator = getIterator(elementsObject);
                ElementDescriptor[] elements = new ElementDescriptor[getObjectLength(elementsObject)];
                Object element;
                while((element = getNext(iterator)) != null) {
                    checkEmptyExtras(element);
                    elements[elementsIndex++] = DescriptorUtil.toElementDescriptor(element);
                }
                return elements;
            }
        }
    }
}
