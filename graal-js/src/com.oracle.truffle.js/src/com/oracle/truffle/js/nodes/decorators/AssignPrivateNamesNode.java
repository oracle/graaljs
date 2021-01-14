package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.runtime.JSContext;

public class AssignPrivateNamesNode extends JavaScriptBaseNode {
    @Child private PrivateClassElementAddNode addNode;

    private AssignPrivateNamesNode(JSContext context) {
        this.addNode = PrivateClassElementAddNode.create(context);
    }

    public static AssignPrivateNamesNode create(JSContext context) {
        return new AssignPrivateNamesNode(context);
    }

    public void execute(DynamicObject proto, DynamicObject constructor, ClassElementList elements) {
        int size = elements.size();
        for (int i = 0; i < size; i++) {
            ElementDescriptor element = elements.pop();
            if(element.hasPrivateKey()) {
                if(element.isField() || element.isMethod() || element.isAccessor()) {
                    DynamicObject target = element.isStatic() ? constructor : proto;
                    addNode.execute(target, element.getKey(), element.getDescriptor());
                }
            }
            elements.push(element);
        }
    }
}
