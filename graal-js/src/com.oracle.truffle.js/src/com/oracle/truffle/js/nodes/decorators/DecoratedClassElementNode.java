package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.ArrayList;
import java.util.List;

public class DecoratedClassElementNode extends ClassElementNode {
    @Child ClassElementNode value;
    @Children ElementDecoratorNode[] decorators;

    protected DecoratedClassElementNode(ClassElementNode value,ElementDecoratorNode[] decorators) {
        this.value = value;
        this.decorators = decorators;
    }

    public static ClassElementNode create(ClassElementNode value, ElementDecoratorNode[] decorators) {
        return new DecoratedClassElementNode(value, decorators);
    }

    @Override
    @ExplodeLoop
    public ElementDescriptor[] executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context) {
        ElementDescriptor initial = value.executeElementDescriptor(frame, homeObject, context)[0];
        List<ElementDescriptor> elements = new ArrayList<>();
        for(ElementDecoratorNode decorator : decorators) {
            elements.addAll(0, decorator.executeDecorator(frame, initial, context));
            initial = elements.get(0);
        }
        return elements.toArray(new ElementDescriptor[]{});
    }

    @Override
    public boolean isStatic() {
        return value.isStatic();
    }
}