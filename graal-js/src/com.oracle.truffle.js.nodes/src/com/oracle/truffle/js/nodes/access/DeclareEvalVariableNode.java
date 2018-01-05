/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class DeclareEvalVariableNode extends StatementNode {
    @Child private JavaScriptNode dynamicScopeNode;
    @Child private WriteNode initScopeNode;
    @Child private HasPropertyCacheNode hasProperty;
    @Child private PropertySetNode defineProperty;
    private final JSContext context;
    private final String varName;

    public DeclareEvalVariableNode(JSContext context, String varName, JavaScriptNode dynamicScopeNode, WriteNode writeDynamicScopeNode) {
        this.context = context;
        this.varName = varName;
        this.dynamicScopeNode = dynamicScopeNode;
        this.initScopeNode = writeDynamicScopeNode;
        this.hasProperty = HasPropertyCacheNode.create(varName, context);
        this.defineProperty = PropertySetNode.create(varName, false, context, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject dynamicScope = (DynamicObject) dynamicScopeNode.execute(frame);
        if (dynamicScope == Undefined.instance) {
            // NB: dynamic scope object must not have a prototype (visible to user code)
            Shape shape = context.getEmptyShapePrototypeInObject();
            dynamicScope = JSObject.create(context, shape);
            JSShape.getPrototypeProperty(shape).setSafe(dynamicScope, Null.instance, null);
            // (GR-2060) consider eager initialization of dynamic scope object when
            // the function/block owning it is entered instead of at use (here).
            initScopeNode.executeWrite(frame, dynamicScope);
        }
        assert isValidDynamicScopeObject(dynamicScope);
        if (!hasProperty.hasProperty(dynamicScope)) {
            // must not have the same name declared in frame, it's either there or here
            assert frame.getFrameDescriptor().findFrameSlot(varName) == null;
            defineProperty.setValue(dynamicScope, Undefined.instance);
        }
        return EMPTY;
    }

    private static boolean isValidDynamicScopeObject(DynamicObject dynamicScope) {
        return dynamicScope != Undefined.instance && dynamicScope != Null.instance && dynamicScope != null;
    }

    public String getName() {
        return varName;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new DeclareEvalVariableNode(context, varName, cloneUninitialized(dynamicScopeNode), (WriteNode) cloneUninitialized((JavaScriptNode) initScopeNode));
    }
}
