package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;

public class DecorateClassNode extends JavaScriptBaseNode {
    @Child private DecorateElementNode decorateElementNode;
    @Child private DecorateConstructorNode decorateConstructorNode;
    @Children private final JavaScriptNode[] classDecorators;

    private DecorateClassNode(JSContext context, JavaScriptNode[] classDecorators) {
        this.decorateElementNode = DecorateElementNode.create(context);
        this.decorateConstructorNode = DecorateConstructorNode.create(context);
        this.classDecorators = classDecorators;
    }

    public static DecorateClassNode create(JSContext context, JavaScriptNode[] classDecorators) {
        return new DecorateClassNode(context, classDecorators);
    }

    @ExplodeLoop
    public void execute(VirtualFrame frame, ClassElementList elements) {
        //DecorateElements
        for (int i = 0; i < elements.size(); i++) {
            ElementDescriptor element = elements.pop();
            if(element.hasDecorators()) {
                decorateElementNode.decorateElement(element, elements);
            } else {
                elements.push(element);
            }
        }
        //DecorateClass
        if(classDecorators.length > 0) {
            Object[] d = new Object[classDecorators.length];
            for(int i = 0; i < classDecorators.length; i++) {
                d[i] = classDecorators[i].execute(frame);
            }
            decorateConstructorNode.decorateConstructor(elements, d);
        }
    }
}
