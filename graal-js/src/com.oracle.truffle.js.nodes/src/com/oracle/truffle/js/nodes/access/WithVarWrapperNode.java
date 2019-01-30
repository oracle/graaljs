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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class WithVarWrapperNode extends JSTargetableNode implements ReadNode, WriteNode {

    @Child private JSTargetableNode defaultDelegate;
    @Child private JavaScriptNode globalDelegate;
    @Child private JavaScriptNode withTarget;
    private final String varName;

    protected WithVarWrapperNode(String varName, JavaScriptNode withTarget, JSTargetableNode defaultDelegate, JavaScriptNode globalDelegate) {
        this.defaultDelegate = defaultDelegate;
        this.globalDelegate = globalDelegate;
        this.withTarget = withTarget;
        this.varName = varName;

        if (defaultDelegate instanceof GlobalPropertyNode) {
            ((GlobalPropertyNode) defaultDelegate).setPropertyAssumptionCheckEnabled(false);
        }
    }

    public static JavaScriptNode create(String varName, JavaScriptNode withTarget, JSTargetableNode defaultDelegate, JavaScriptNode globalDelegate) {
        return new WithVarWrapperNode(varName, withTarget, defaultDelegate, globalDelegate);
    }

    @Override
    public JavaScriptNode getTarget() {
        return withTarget;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        TruffleObject target = evaluateTarget(frame);
        return executeWithTarget(frame, target);
    }

    @Override
    public TruffleObject evaluateTarget(VirtualFrame frame) {
        try {
            return withTarget.executeTruffleObject(frame);
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere();
        }
    }

    @Override
    public Object executeWithTarget(VirtualFrame frame, Object target) {
        if (target != Undefined.instance) {
            // the object was found in the with chain
            if (defaultDelegate instanceof WritePropertyNode && globalDelegate != null) {
                return ((WritePropertyNode) defaultDelegate).executeWithValue(target, ((WriteNode) globalDelegate).getRhs().execute(frame));
            }
            return defaultDelegate.executeWithTarget(frame, target);
        } else {
            // not found
            if (globalDelegate == null) {
                // the globalDelegate is the same as defaultDelegate
                // this can be configured by leaving globalDelegate null.
                return defaultDelegate.execute(frame);
            } else {
                return globalDelegate.execute(frame);
            }
        }
    }

    @Override
    public Object executeWrite(VirtualFrame frame, Object value) {
        throw Errors.shouldNotReachHere();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(varName, cloneUninitialized(withTarget), cloneUninitialized(defaultDelegate), cloneUninitialized(globalDelegate));
    }

    @Override
    public JavaScriptNode getRhs() {
        return ((WriteNode) globalDelegate).getRhs();
    }

    public JSTargetableNode getDefaultDelegate() {
        return defaultDelegate;
    }

    public JavaScriptNode getGlobalDelegate() {
        return globalDelegate;
    }
}
