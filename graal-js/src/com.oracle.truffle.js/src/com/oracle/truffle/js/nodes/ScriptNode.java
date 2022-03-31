/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public final class ScriptNode {

    private final JSContext context;
    private final JSFunctionData functionData;
    private final RootCallTarget callTarget;

    private ScriptNode(JSContext context, JSFunctionData functionData, RootCallTarget callTarget) {
        this.context = context;
        this.functionData = functionData;
        this.callTarget = callTarget;
    }

    public static ScriptNode fromFunctionRoot(JSContext context, FunctionRootNode root) {
        return fromFunctionData(context, root.getFunctionData());
    }

    public static ScriptNode fromFunctionData(JSContext context, JSFunctionData functionData) {
        return new ScriptNode(context, functionData, (RootCallTarget) functionData.getCallTarget());
    }

    public Object run(JSRealm realm) {
        return run(argumentsToRunWithThisObject(realm, realm.getGlobalObject()));
    }

    public Object[] argumentsToRun(JSRealm realm) {
        return argumentsToRunWithThisObject(realm, realm.getGlobalObject());
    }

    public Object[] argumentsToRunWithThisObject(JSRealm realm, Object thisObj) {
        JSDynamicObject functionObj = JSFunction.create(realm, functionData);
        return JSArguments.createZeroArg(thisObj, functionObj);
    }

    public Object[] argumentsToRunWithArguments(JSRealm realm, Object[] args) {
        return argumentsToRunWithThisObjectWithArguments(realm, realm.getGlobalObject(), args);
    }

    public Object[] argumentsToRunWithThisObjectWithArguments(JSRealm realm, Object thisObj, Object[] args) {
        JSDynamicObject functionObj = JSFunction.create(realm, functionData);
        return JSArguments.create(thisObj, functionObj, args);
    }

    public Object runEval(IndirectCallNode callNode, JSRealm realm, Object thisObj, MaterializedFrame materializedFrame) {
        JSDynamicObject functionObj = JSFunction.create(realm, getFunctionData(), materializedFrame);
        return callNode.call(callTarget, JSArguments.createZeroArg(thisObj, functionObj));
    }

    public Object runEval(IndirectCallNode callNode, JSRealm realm) {
        return runEval(callNode, realm, realm.getGlobalObject(), JSFrameUtil.NULL_MATERIALIZED_FRAME);
    }

    public Object run(Object[] args) {
        return callTarget.call(args);
    }

    public JSContext getContext() {
        return context;
    }

    public RootNode getRootNode() {
        return callTarget.getRootNode();
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public JSFunctionData getFunctionData() {
        return functionData;
    }
}
