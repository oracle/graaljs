package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;

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
        for(ElementDecoratorNode decorator : decorators) {
            initial = decorator.executeDecorator(frame,initial, context)[0];
        }
        return new ElementDescriptor[] { initial };
    }

    @Override
    public boolean isStatic() {
        return value.isStatic();
    }
}