/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implementation of the ToPropertyDescriptor function as defined in ECMA 8.10.5.
 */
public abstract class ToPropertyDescriptorNode extends JavaScriptBaseNode {
    private final JSContext context;

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

    public abstract PropertyDescriptor execute(Object operand);

    @NeverDefault
    public static ToPropertyDescriptorNode create(JSContext context) {
        return ToPropertyDescriptorNodeGen.create(context);
    }

    protected ToPropertyDescriptorNode(JSContext context) {
        this.context = context;
        hasEnumerableNode = HasPropertyCacheNode.create(JSAttributes.ENUMERABLE, context);
        hasConfigurableNode = HasPropertyCacheNode.create(JSAttributes.CONFIGURABLE, context);
        hasWritableNode = HasPropertyCacheNode.create(JSAttributes.WRITABLE, context);
        hasValueNode = HasPropertyCacheNode.create(JSAttributes.VALUE, context);
        hasGetNode = HasPropertyCacheNode.create(JSAttributes.GET, context);
        hasSetNode = HasPropertyCacheNode.create(JSAttributes.SET, context);
    }

    @Specialization(guards = {"isObjectNode.executeBoolean(obj)"})
    protected PropertyDescriptor doDefault(Object obj,
                    @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObjectNode,
                    @Cached(inline = true) JSToBooleanNode toBooleanNode,
                    @Cached InlinedBranchProfile hasGetBranch,
                    @Cached InlinedBranchProfile hasSetBranch,
                    @Cached InlinedBranchProfile hasEnumerableBranch,
                    @Cached InlinedBranchProfile hasConfigurableBranch,
                    @Cached InlinedBranchProfile hasValueBranch,
                    @Cached InlinedBranchProfile hasWritableBranch,
                    @Cached InlinedBranchProfile errorBranch,
                    @Cached IsCallableNode isCallable) {
        PropertyDescriptor desc = PropertyDescriptor.createEmpty();

        // 3.
        if (hasEnumerableNode.hasProperty(obj)) {
            hasEnumerableBranch.enter(this);
            desc.setEnumerable(toBooleanNode.executeBoolean(this, getEnumerableNode().getValue(obj)));
        }
        // 4.
        if (hasConfigurableNode.hasProperty(obj)) {
            hasConfigurableBranch.enter(this);
            desc.setConfigurable(toBooleanNode.executeBoolean(this, getConfigurableNode().getValue(obj)));
        }
        // 5.
        boolean hasValue = hasValueNode.hasProperty(obj);
        if (hasValue) {
            hasValueBranch.enter(this);
            desc.setValue(getValue(obj));
        }
        // 6.
        boolean hasWritable = hasWritableNode.hasProperty(obj);
        if (hasWritable) {
            hasWritableBranch.enter(this);
            desc.setWritable(toBooleanNode.executeBoolean(this, getWritableNode().getValue(obj)));
        }
        // 7.
        boolean hasGet = hasGetNode.hasProperty(obj);
        if (hasGet) {
            hasGetBranch.enter(this);
            Object getter = getGet(obj);
            if (!isCallable.executeBoolean(getter) && getter != Undefined.instance) {
                errorBranch.enter(this);
                throw Errors.createTypeError("Getter must be a function");
            }
            desc.setGet(getter);
        }
        // 8.
        boolean hasSet = hasSetNode.hasProperty(obj);
        if (hasSet) {
            hasSetBranch.enter(this);
            Object setter = getSet(obj);
            if (!isCallable.executeBoolean(setter) && setter != Undefined.instance) {
                errorBranch.enter(this);
                throw Errors.createTypeError("Setter must be a function");
            }
            desc.setSet(setter);
        }
        // 9.
        if ((hasGet || hasSet) && (hasValue || hasWritable)) {
            errorBranch.enter(this);
            throw Errors.createTypeError("Invalid property. A property cannot both have accessors and be writable or have a value");
        }
        return desc;
    }

    private Object getSet(Object obj) {
        if (getSetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSetNode = insert(PropertyGetNode.create(JSAttributes.SET, context));
        }
        return getSetNode.getValue(obj);
    }

    private Object getGet(Object obj) {
        if (getGetNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getGetNode = insert(PropertyGetNode.create(JSAttributes.GET, context));
        }
        return getGetNode.getValue(obj);
    }

    private Object getValue(Object obj) {
        if (getValueNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getValueNode = insert(PropertyGetNode.create(JSAttributes.VALUE, context));
        }
        return getValueNode.getValue(obj);
    }

    private PropertyGetNode getWritableNode() {
        if (getWritableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getWritableNode = insert(PropertyGetNode.create(JSAttributes.WRITABLE, context));
        }
        return getWritableNode;
    }

    private PropertyGetNode getConfigurableNode() {
        if (getConfigurableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getConfigurableNode = insert(PropertyGetNode.create(JSAttributes.CONFIGURABLE, context));
        }
        return getConfigurableNode;
    }

    private PropertyGetNode getEnumerableNode() {
        if (getEnumerableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getEnumerableNode = insert(PropertyGetNode.create(JSAttributes.ENUMERABLE, context));
        }
        return getEnumerableNode;
    }

    @Specialization(guards = "!isObjectNode.executeBoolean(obj)")
    protected PropertyDescriptor doNonObject(Object obj,
                    @Cached @Shared @SuppressWarnings("unused") IsObjectNode isObjectNode) {
        throw Errors.createTypeErrorPropertyDescriptorNotAnObject(obj, this);
    }
}
