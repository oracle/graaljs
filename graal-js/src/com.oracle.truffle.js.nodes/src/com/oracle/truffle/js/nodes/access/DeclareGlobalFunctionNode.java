/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public class DeclareGlobalFunctionNode extends StatementNode {
    private final String varName;
    private final JSContext context;
    private final boolean configurable;
    @Child private JavaScriptNode valueNode;
    private final JSClassProfile classProfile = JSClassProfile.create();

    public DeclareGlobalFunctionNode(JSContext context, String varName, boolean configurable, JavaScriptNode valueNode) {
        this.varName = varName;
        this.context = context;
        this.configurable = configurable;
        this.valueNode = valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = valueNode == null ? Undefined.instance : valueNode.execute(frame);
        DynamicObject globalObject = GlobalObjectNode.getGlobalObject(context);
        PropertyDescriptor desc = JSObject.getOwnProperty(globalObject, varName, classProfile);
        if (desc == null && JSGlobalObject.isJSGlobalObject(globalObject)) {
            if (!JSObject.isExtensible(globalObject, classProfile)) {
                throw Errors.createTypeError("cannot define global variable");
            }
            JSObjectUtil.putDeclaredDataProperty(context, globalObject, varName, value,
                            configurable ? JSAttributes.configurableEnumerableWritable() : JSAttributes.notConfigurableEnumerableWritable());
        } else {
            if (desc == null || desc.getConfigurable()) {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value, true, true, configurable), true);
            } else {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value), true);
            }
            if (valueNode != null) {
                JSObject.set(globalObject, varName, value, false, classProfile);
            }
        }
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new DeclareGlobalFunctionNode(context, varName, configurable, valueNode);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}
