package com.oracle.truffle.js.nodes.decorators;

import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;

public class AssignPrivateNamesNode extends JavaScriptBaseNode {
    private AssignPrivateNamesNode() {}

    public static AssignPrivateNamesNode create() {
        return new AssignPrivateNamesNode();
    }

    public void execute(ClassElementList elements) {
        int size = elements.size();
        for (int i = 0; i < size; i++) {
            ElementDescriptor element = elements.pop();
            if(element.hasPrivateKey()) {
                PrivateName name = new PrivateName((HiddenKey) element.getKey());
                if(element.isField() || element.isMethod() || element.isAccessor()) {
                    name.setKind(element.getKind());
                }
                name.setDescriptor(element.getDescriptor());
                element.setKey(name);
            }
            elements.push(element);
        }
    }
}
