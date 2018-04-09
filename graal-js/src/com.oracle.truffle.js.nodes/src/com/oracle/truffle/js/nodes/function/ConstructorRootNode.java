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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.JSTargetableNode;
import com.oracle.truffle.js.nodes.function.JSNewNode.SpecializedNewObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class ConstructorRootNode extends JavaScriptRootNode {
    private final JSFunctionData functionData;
    private final CallTarget callTarget;

    @Child private DirectCallNode callNode;
    @Child private JSTargetableNode newObjectNode;
    @Child private IsPrimitiveNode isPrimitiveNode;
    private final ConditionProfile isObject = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNotUndefined = ConditionProfile.createBinaryProfile();
    private final boolean newTarget;

    protected ConstructorRootNode(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        super(functionData.getContext().getLanguage(), ((RootCallTarget) callTarget).getRootNode().getSourceSection(), null);
        this.functionData = functionData;
        this.callTarget = callTarget;
        this.newTarget = newTarget;
    }

    public static ConstructorRootNode create(JSFunctionData functionData, CallTarget callTarget, boolean newTarget) {
        return new ConstructorRootNode(functionData, callTarget, newTarget);
    }

    private Object allocateThisObject(VirtualFrame frame, Object[] arguments) {
        Object functionObject = newTarget ? arguments[2] : arguments[1];
        Object thisObject = newObjectNode.executeWithTarget(frame, functionObject);
        arguments[0] = thisObject;
        return thisObject;
    }

    private Object filterConstructorResult(Object thisObject, Object result) {
        // IsObject is replaced with a !IsPrimitive check for JavaInterop,
        // so that non-primitive Java types can be returned from the constructor, too.
        if (isObject.profile(!isPrimitiveNode.executeBoolean(result))) {
            return result;
        }
        // If [[ConstructorKind]] == "base" or result is undefined return this, otherwise throw
        if (getFunctionData().isDerived() && isNotUndefined.profile(result != Undefined.instance)) {
            throw Errors.createTypeError("constructor result not as expected").setRealm(functionData.getContext().getRealm());
        }
        assert thisObject != JSFunction.CONSTRUCT;
        return thisObject;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (callNode == null || newObjectNode == null || isPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            initialize();
        }
        Object[] arguments = frame.getArguments();
        Object thisObject = allocateThisObject(frame, arguments);
        Object result = callNode.call(arguments);
        return filterConstructorResult(thisObject, result);
    }

    private void initialize() {
        this.callNode = insert(Truffle.getRuntime().createDirectCallNode(callTarget));
        this.newObjectNode = insert(SpecializedNewObjectNode.create(functionData.getContext(), functionData.isBuiltin(), functionData.isConstructor(), functionData.isGenerator(), null));
        this.isPrimitiveNode = insert(IsPrimitiveNode.create());
    }

    private JSFunctionData getFunctionData() {
        return functionData;
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    protected JavaScriptRootNode cloneUninitialized() {
        return new ConstructorRootNode(functionData, callTarget, newTarget);
    }

    @Override
    @TruffleBoundary
    public String toString() {
        String callTargetName = ((RootCallTarget) callTarget).getRootNode().toString();
        return JSTruffleOptions.DetailedCallTargetNames ? JSRuntime.stringConcat("[Construct]", callTargetName) : callTargetName;
    }
}
