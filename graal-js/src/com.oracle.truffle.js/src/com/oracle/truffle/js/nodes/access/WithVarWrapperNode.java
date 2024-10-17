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

import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WithVarWrapperNode extends JSTargetableNode implements ReadNode, WriteNode {

    @Child private JSTargetableNode withAccessNode;
    @Child private JavaScriptNode globalDelegate;
    @Child private JavaScriptNode withTarget;
    @Child private HasPropertyCacheNode hasProperty;
    private final BranchProfile errorBranch = BranchProfile.create();
    private final JSContext context;
    private final TruffleString varName;
    private final boolean isStrict;

    protected WithVarWrapperNode(JSContext context, TruffleString varName, boolean isStrict, JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        this.withAccessNode = withAccessNode;
        this.globalDelegate = globalDelegate;
        this.withTarget = withTarget;
        this.hasProperty = (withAccessNode instanceof PropertyNode) ? HasPropertyCacheNode.create(varName, context) : null;
        this.context = context;
        this.varName = varName;
        this.isStrict = isStrict;
    }

    public static JavaScriptNode create(JSContext context, TruffleString varName, boolean isStrict, JavaScriptNode withTarget, JSTargetableNode withAccessNode, JavaScriptNode globalDelegate) {
        return new WithVarWrapperNode(context, varName, isStrict, withTarget, withAccessNode, globalDelegate);
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
            if (withAccessNode instanceof WritePropertyNode) {
                return ((WritePropertyNode) withAccessNode).executeWithValue(target, ((WriteNode) globalDelegate).getRhs().execute(frame));
            }
            if (hasProperty == null || hasProperty.hasProperty(target)) {
                return withAccessNode.executeWithTarget(frame, target);
            } else {
                if (isStrict) {
                    errorBranch.enter();
                    throw Errors.createReferenceErrorNotDefined(varName, this);
                } else {
                    return Undefined.instance;
                }
            }
        } else {
            // property not found or blocked
            return globalDelegate.execute(frame);
        }
    }

    @Override
    public void executeWrite(VirtualFrame frame, Object value) {
        throw Errors.shouldNotReachHere();
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, varName, isStrict, cloneUninitialized(withTarget, materializedTags), cloneUninitialized(withAccessNode, materializedTags),
                        cloneUninitialized(globalDelegate, materializedTags));
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) globalDelegate).getRhs();
    }
}
