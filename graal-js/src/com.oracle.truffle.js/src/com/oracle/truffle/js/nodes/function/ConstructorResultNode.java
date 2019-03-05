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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstructorResultNode extends JavaScriptNode {
    @Child private JavaScriptNode bodyNode;
    @Child private JavaScriptNode thisNode;
    private final boolean derived;

    @Child private IsPrimitiveNode isPrimitiveNode;
    private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNotUndefined = ConditionProfile.createBinaryProfile();

    private ConstructorResultNode(boolean derived, JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        this.bodyNode = bodyNode;
        this.derived = derived;
        this.thisNode = thisNode;
    }

    public static JavaScriptNode createBase(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return new ConstructorResultNode(false, bodyNode, thisNode);
    }

    public static JavaScriptNode createDerived(JavaScriptNode bodyNode, JavaScriptNode thisNode) {
        return new ConstructorResultNode(true, bodyNode, thisNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = bodyNode.execute(frame);

        // IsObject is replaced with a !IsPrimitive check for JavaInterop,
        // so that non-primitive Java types can be returned from the constructor, too.
        if (isObject.profile(!isPrimitive(result))) {
            return result;
        }

        // If [[ConstructorKind]] == "base" or result is undefined return this, otherwise throw
        if (derived && isNotUndefined.profile(result != Undefined.instance)) {
            // throw this error in the caller realm!
            throw Errors.createTypeError("constructor result not as expected").useCallerRealm();
        }

        Object thisObject = thisNode.execute(frame);
        assert thisObject != JSFunction.CONSTRUCT;
        assert !derived || JSRuntime.isObject(thisObject) || thisObject instanceof Symbol;
        return thisObject;
    }

    private boolean isPrimitive(Object result) {
        if (isPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isPrimitiveNode = insert(IsPrimitiveNode.create());
        }
        return isPrimitiveNode.executeBoolean(result);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new ConstructorResultNode(derived, cloneUninitialized(bodyNode), cloneUninitialized(thisNode));
    }
}
