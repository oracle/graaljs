/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WithVarWrapperNode extends JSTargetableNode implements ReadNode {

    @Child protected JSTargetableNode withAccessNode;
    @Child protected JavaScriptNode globalDelegate;
    @Child protected JavaScriptNode withTarget;

    protected WithVarWrapperNode(JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        this.withAccessNode = withAccessNode;
        this.globalDelegate = globalDelegate;
        this.withTarget = withTarget;
    }

    public static JavaScriptNode create(JSContext context, String varName, JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        if (withAccessNode instanceof WritePropertyNode) {
            return new WriteWithVarWrapperNode(context, varName, withTarget, withAccessNode, globalDelegate);
        } else {
            return new WithVarWrapperNode(withTarget, withAccessNode, globalDelegate);
        }
    }

    @Override
    public JavaScriptNode getTarget() {
        return withTarget;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object target = evaluateTarget(frame);
        return executeWithTarget(frame, target);
    }

    @Override
    public Object evaluateTarget(VirtualFrame frame) {
        return withTarget.execute(frame);
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        if (target != Undefined.instance) {
            // the property was found in the with object
            return withAccessNode.executeWithTarget(frame, target);
        } else {
            // property not found or blocked
            return globalDelegate.execute(frame);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new WithVarWrapperNode(cloneUninitialized(withTarget, materializedTags), cloneUninitialized(withAccessNode, materializedTags), cloneUninitialized(globalDelegate, materializedTags));
    }
}

class WriteWithVarWrapperNode extends WithVarWrapperNode implements WriteNode {

    @Child private HasPropertyCacheNode withObjectHasProperty;

    protected WriteWithVarWrapperNode(JSContext context, String varName, JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        super(withTarget, withAccessNode, globalDelegate);
        this.withObjectHasProperty = HasPropertyCacheNode.create(varName, context);
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        if (target != Undefined.instance) {
            // the property was found in the with object
            WritePropertyNode writePropertyNode = (WritePropertyNode) withAccessNode;
            Object rhsValue = ((WriteNode) globalDelegate).getRhs().execute(frame);

            boolean stillExists = withObjectHasProperty.hasProperty(target);
            if (!stillExists && writePropertyNode.isStrict()) {
                throw Errors.createReferenceErrorNotDefined(withObjectHasProperty.getContext(), withObjectHasProperty.getKey(), this);
            }

            return writePropertyNode.executeWithValue(target, rhsValue);
        } else {
            // property not found or blocked
            return globalDelegate.execute(frame);
        }
    }

    @Override
    public Object executeWrite(VirtualFrame frame, Object value) {
        throw Errors.shouldNotReachHere();
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) globalDelegate).getRhs();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return new WriteWithVarWrapperNode(withObjectHasProperty.getContext(), (String) withObjectHasProperty.getKey(),
                        cloneUninitialized(withTarget, materializedTags),
                        cloneUninitialized(withAccessNode, materializedTags),
                        cloneUninitialized(globalDelegate, materializedTags));
    }
}
