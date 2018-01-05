/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implementation of the ToPropertyDescriptor function as defined in ECMA 8.10.5.
 */
public abstract class ToPropertyDescriptorNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private JSToBooleanNode toBooleanNode;

    @Child private PropertyGetNode getEnumerableNode;
    @Child private PropertyGetNode getConfigurableNode;
    @Child private PropertyGetNode getWritableNode;
    @Child private PropertyGetNode getValueNode;
    @Child private PropertyGetNode getSetNode;
    @Child private PropertyGetNode getGetNode;

    @Child private HasPropertyCacheNode hasEnumerableNode;
    @Child private HasPropertyCacheNode hasConfigurableNode;
    @Child private HasPropertyCacheNode hasWritableNode;
    @Child private HasPropertyCacheNode hasValueNode;
    @Child private HasPropertyCacheNode hasSetNode;
    @Child private HasPropertyCacheNode hasGetNode;

    public abstract Object execute(Object operand);

    public static ToPropertyDescriptorNode create(JSContext context) {
        return ToPropertyDescriptorNodeGen.create(context);
    }

    protected ToPropertyDescriptorNode(JSContext context) {
        this.context = context;
    }

    private void initialize() {
        if (toBooleanNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toBooleanNode = insert(JSToBooleanNode.create());
            hasEnumerableNode = insert(HasPropertyCacheNode.create(JSAttributes.ENUMERABLE, context));
            hasConfigurableNode = insert(HasPropertyCacheNode.create(JSAttributes.CONFIGURABLE, context));
            hasWritableNode = insert(HasPropertyCacheNode.create(JSAttributes.WRITABLE, context));
            hasValueNode = insert(HasPropertyCacheNode.create(JSAttributes.VALUE, context));
            hasGetNode = insert(HasPropertyCacheNode.create(JSAttributes.GET, context));
            hasSetNode = insert(HasPropertyCacheNode.create(JSAttributes.SET, context));
        }
    }

    private boolean toBoolean(Object target) {
        return toBooleanNode.executeBoolean(target);
    }

    @Specialization(guards = "isJSObject(obj)")
    protected Object doDefault(DynamicObject obj,
                    @Cached("create()") BranchProfile hasGetBranch,
                    @Cached("create()") BranchProfile hasSetBranch,
                    @Cached("create()") BranchProfile hasEnumerableBranch,
                    @Cached("create()") BranchProfile hasConfigurableBranch,
                    @Cached("create()") BranchProfile hasValueBranch,
                    @Cached("create()") BranchProfile hasWritableBranch) {
        initialize();
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();

        // 3.
        if (hasEnumerableNode.hasProperty(obj)) {
            hasEnumerableBranch.enter();
            desc.setEnumerable(getEnumerableValue(obj));
        }
        // 4.
        if (hasConfigurableNode.hasProperty(obj)) {
            hasConfigurableBranch.enter();
            desc.setConfigurable(getConfigurableValue(obj));
        }
        // 5.
        boolean hasValue = hasValueNode.hasProperty(obj);
        if (hasValue) {
            hasValueBranch.enter();
            desc.setValue(getValue(obj));
        }
        // 6.
        boolean hasWritable = hasWritableNode.hasProperty(obj);
        if (hasWritable) {
            hasWritableBranch.enter();
            desc.setWritable(getWritableValue(obj));
        }
        // 7.
        boolean hasGet = hasGetNode.hasProperty(obj);
        if (hasGet) {
            hasGetBranch.enter();
            Object getter = getGet(obj);
            if (!JSFunction.isJSFunction(getter) && getter != Undefined.instance) {
                throw Errors.createTypeError("Getter must be a function");
            }
            desc.setGet((DynamicObject) getter);
        }
        // 8.
        boolean hasSet = hasSetNode.hasProperty(obj);
        if (hasSet) {
            hasSetBranch.enter();
            Object setter = getSet(obj);
            if (!JSFunction.isJSFunction(setter) && setter != Undefined.instance) {
                throw Errors.createTypeError("Setter must be a function");
            }
            desc.setSet((DynamicObject) setter);
        }
        // 9.
        if ((hasGet || hasSet) && (hasValue || hasWritable)) {
            throw Errors.createTypeError("Invalid property. A property cannot both have accessors and be writable or have a value");
        }
        return desc;
    }

    private Object getSet(DynamicObject obj) {
        if (getSetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSetNode = insert(PropertyGetNode.create(JSAttributes.SET, false, context));
        }
        return getSetNode.getValue(obj);
    }

    private Object getGet(DynamicObject obj) {
        if (getGetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getGetNode = insert(PropertyGetNode.create(JSAttributes.GET, false, context));
        }
        return getGetNode.getValue(obj);
    }

    private Object getValue(DynamicObject obj) {
        if (getValueNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getValueNode = insert(PropertyGetNode.create(JSAttributes.VALUE, false, context));
        }
        return getValueNode.getValue(obj);
    }

    private boolean getWritableValue(DynamicObject obj) {
        if (getWritableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getWritableNode = insert(PropertyGetNode.create(JSAttributes.WRITABLE, false, context));
        }
        return toBoolean(getWritableNode.getValue(obj));
    }

    private boolean getConfigurableValue(DynamicObject obj) {
        if (getConfigurableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getConfigurableNode = insert(PropertyGetNode.create(JSAttributes.CONFIGURABLE, false, context));
        }
        return toBoolean(getConfigurableNode.getValue(obj));
    }

    private boolean getEnumerableValue(DynamicObject obj) {
        if (getEnumerableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getEnumerableNode = insert(PropertyGetNode.create(JSAttributes.ENUMERABLE, false, context));
        }
        return toBoolean(getEnumerableNode.getValue(obj));
    }

    @Specialization(guards = "!isJSObject(obj)")
    protected Object doNonObject(Object obj, @Cached("create()") JSToStringNode toStringNode) {
        final String message;
        if (context.isOptionV8CompatibilityMode()) {
            message = JSRuntime.stringConcat("Property description must be an object: ", toStringNode.executeString(obj));
        } else {
            message = "must be an object";
        }
        throw Errors.createTypeError(message);
    }
}
