package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.Set;

public class DecorateClassNode extends JavaScriptBaseNode {
    private final JSContext context;

    @Child private DecorateElementNode decorateElementNode;
    @Child private DecorateConstructorNode decorateConstructorNode;
    @Children private final JavaScriptNode[] classDecorators;

    private DecorateClassNode(JSContext context, JavaScriptNode[] classDecorators) {
        this.context = context;
        this.decorateElementNode = DecorateElementNode.create(context);
        this.decorateConstructorNode = DecorateConstructorNode.create(context);
        this.classDecorators = classDecorators;
    }

    public static DecorateClassNode create(JSContext context, JavaScriptNode[] classDecorators) {
        return new DecorateClassNode(context, classDecorators);
    }

    //DecorateElements
    public ClassElementList executeElementDecoration(ElementDescriptor[] elements) {
        ClassElementList list = ClassElementList.create();
        for (ElementDescriptor element: elements) {
            if(element != null) {
                if (element.hasDecorators()) {
                    decorateElementNode.decorateElement(element, list);
                } else {
                    list.enqueue(element);
                }
            }
        }
        return list;
    }

    //DecorateClass
    @ExplodeLoop
    public ClassElementList executeClassDecoration(VirtualFrame frame, ClassElementList elements) {
        CompilerAsserts.partialEvaluationConstant(classDecorators);
        if(classDecorators.length > 0) {
            Object[] d = new Object[classDecorators.length];
            for(int i = 0; i < classDecorators.length; i++) {
                d[i] = classDecorators[i].execute(frame);
            }
            return decorateConstructorNode.decorateConstructor(elements, d);
        } else {
            return elements;
        }
    }

    private DecorateClassNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, JavaScriptNode.cloneUninitialized(classDecorators, materializedTags));
    }

    public static DecorateClassNode cloneUninitialized(DecorateClassNode decorateClassNode, Set<Class<? extends Tag>> materializedTags) {
        return decorateClassNode.copyUninitialized(materializedTags);
    }
}
