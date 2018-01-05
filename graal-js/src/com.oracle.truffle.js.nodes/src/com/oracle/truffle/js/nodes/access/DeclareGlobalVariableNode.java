/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

public class DeclareGlobalVariableNode extends StatementNode {
    private final String varName;
    private final JSContext context;
    private final boolean configurable;
    private final JSClassProfile classProfile = JSClassProfile.create();

    public DeclareGlobalVariableNode(JSContext context, String varName, boolean configurable) {
        this.varName = varName;
        this.context = context;
        this.configurable = configurable;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject globalObject = GlobalObjectNode.getGlobalObject(context);
        if (!JSObject.hasOwnProperty(globalObject, varName, classProfile)) {
            if (!JSObject.isExtensible(globalObject, classProfile)) {
                throw Errors.createTypeError("cannot define global variable");
            }
            if (JSGlobalObject.isJSGlobalObject(globalObject)) {
                JSObjectUtil.putDeclaredDataProperty(context, globalObject, varName, Undefined.instance,
                                configurable ? JSAttributes.configurableEnumerableWritable() : JSAttributes.notConfigurableEnumerableWritable());
            } else {
                PropertyDescriptor desc = configurable ? PropertyDescriptor.undefinedDataDesc : PropertyDescriptor.undefinedDataDescNotConfigurable;
                JSObject.defineOwnProperty(globalObject, varName, desc, true);
            }
        }
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new DeclareGlobalVariableNode(context, varName, configurable);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        assert EMPTY == Undefined.instance;
        return clazz == Undefined.class;
    }
}
