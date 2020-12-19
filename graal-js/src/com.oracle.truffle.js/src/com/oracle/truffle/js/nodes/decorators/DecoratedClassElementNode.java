package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;

public class DecoratedClassElementNode extends ClassElementNode {
    @Child ClassElementNode value;
    @Children DecoratorNode[] decorators;

    protected DecoratedClassElementNode(ClassElementNode value, DecoratorNode[] decorators) {
        this.value = value;
        this.decorators = decorators;
    }

    public static ClassElementNode create(ClassElementNode value, DecoratorNode[] decorators) {
        return new DecoratedClassElementNode(value, decorators);
    }

    @Override
    @ExplodeLoop
    public ElementDescriptor[] executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
        ElementDescriptor[] elements = value.executeElementDescriptor(frame, homeObject, context);
        for(DecoratorNode decorator : decorators) {
            ElementDescriptor current = elements[0];
            ElementDescriptor[] e = decorator.executeElementDecorator(frame, current);
            ElementDescriptor[] concatenation = new ElementDescriptor[elements.length - 1 + e.length];
            System.arraycopy(e,0,concatenation,0,e.length);
            System.arraycopy(elements,1,concatenation,e.length,elements.length - 1);
            elements = concatenation;
        }
        return elements;
    }

    @Override
    public boolean isStatic() {
        return value.isStatic();
    }
}