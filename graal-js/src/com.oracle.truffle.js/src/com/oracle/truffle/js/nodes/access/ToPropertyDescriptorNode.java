/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
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
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implementation of the ToPropertyDescriptor function as defined in ECMA 8.10.5.
 */
public abstract class ToPropertyDescriptorNode extends JavaScriptBaseNode {
    private final JSContext context;
    @Child private JSToBooleanNode toBooleanNode;
    @CompilationFinal private boolean wasExecuted = false;

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

    private final BranchProfile errorBranch = BranchProfile.create();

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

    // DSL would not re-read the value if we queried the variable!
    protected boolean wasExecuted(@SuppressWarnings("unused") DynamicObject obj) {
        return wasExecuted;
    }

    /**
     * If this node is executed only once, there is no need to create all the specializing child
     * nodes.
     */
    @Specialization(guards = {"!wasExecuted(obj)", "isJSObject(obj)"})
    protected Object nonSpecialized(DynamicObject obj) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        wasExecuted = true;
        return JSRuntime.toPropertyDescriptor(obj);
    }

    @Specialization(guards = {"wasExecuted(obj)", "isJSObject(obj)"})
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
            if (!JSRuntime.isCallable(getter) && getter != Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("Getter must be a function");
            }
            desc.setGet((DynamicObject) getter);
        }
        // 8.
        boolean hasSet = hasSetNode.hasProperty(obj);
        if (hasSet) {
            hasSetBranch.enter();
            Object setter = getSet(obj);
            if (!JSRuntime.isCallable(setter) && setter != Undefined.instance) {
                errorBranch.enter();
                throw Errors.createTypeError("Setter must be a function");
            }
            desc.setSet((DynamicObject) setter);
        }
        // 9.
        if ((hasGet || hasSet) && (hasValue || hasWritable)) {
            errorBranch.enter();
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
