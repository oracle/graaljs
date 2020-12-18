package com.oracle.truffle.js.nodes.decorators;


import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class ClassElementNode extends JavaScriptBaseNode {
    public static final ClassElementNode[] EMPTY = {};

    public abstract ElementDescriptor[] executeElementDescriptor(VirtualFrame frame, DynamicObject homeObject, JSContext context);

    public abstract boolean isStatic();
}
