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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.function.ConstructorRootNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

/**
 * This node can be used to create a callable 'constructor' function for self-hosted internal
 * JavaScript builtins.
 */
@NodeChildren({@NodeChild(value = "function")})
public abstract class JSToConstructorFunctionNode extends JavaScriptNode {

    private final JSContext context;

    protected JSToConstructorFunctionNode(JSContext context) {
        this.context = context;
    }

    public static JSToConstructorFunctionNode create(JSContext context, JavaScriptNode argument) {
        return JSToConstructorFunctionNodeGen.create(context, argument);
    }

    /**
     * Create a new {@link JSFunction} that can be called as constructor using 'new'.
     *
     * @param function The function to be converted to constructor.
     * @return A new function instance.
     */
    @TruffleBoundary
    @Specialization(guards = {"isJSFunction(function)"})
    protected Object doOther(Object function) {
        DynamicObject toConvertFun = (DynamicObject) function;
        CallTarget callTarget = JSFunction.getCallTarget(toConvertFun);
        int length = JSFunction.getLength(toConvertFun);
        String name = JSFunction.getName(toConvertFun);
        CallTarget newCallTarget = Truffle.getRuntime().createCallTarget(
                        NodeFactory.getInstance(context).createConstructorRequiresNewRoot(context, ((RootCallTarget) callTarget).getRootNode().getSourceSection()));
        JSFunctionData newFunctionData = JSFunctionData.create(context, newCallTarget, null, null, length, name,
                        true, false, JSFunction.isStrict(toConvertFun), false, JSFunction.needsParentFrame(toConvertFun), false, false, false, true, false, false);
        CallTarget newConstructTarget = Truffle.getRuntime().createCallTarget(ConstructorRootNode.create(newFunctionData, callTarget, false));
        newFunctionData.setConstructTarget(newConstructTarget);
        // Make the CallTarget extensible from ECMA6 classes
        CallTarget newTargetCallTarget = Truffle.getRuntime().createCallTarget(
                        ConstructorRootNode.create(newFunctionData, Truffle.getRuntime().createCallTarget(NodeFactory.getDefaultInstance().createDropNewTarget(callTarget)), true));
        newFunctionData.setConstructNewTarget(newTargetCallTarget);
        return JSFunction.create(JSFunction.getRealm(toConvertFun), newFunctionData, JSFunction.getEnclosingFrame(toConvertFun));
    }

    abstract JavaScriptNode getFunction();

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(getFunction()));
    }
}
