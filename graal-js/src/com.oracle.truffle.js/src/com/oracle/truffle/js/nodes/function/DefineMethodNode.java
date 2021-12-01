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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.DefineMethodNodeFactory.FunctionCreateNodeGen;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionFactory;

public class DefineMethodNode extends JavaScriptBaseNode {

    private final JSFunctionData functionData;
    @Child private FunctionCreateNode functionCreateNode;
    @Child private PropertySetNode makeMethodNode;

    protected DefineMethodNode(JSContext context, JSFunctionData functionData, int blockScopeSlot) {
        this.functionData = functionData;
        this.functionCreateNode = FunctionCreateNode.create(context, functionData, blockScopeSlot);
        this.makeMethodNode = PropertySetNode.createSetHidden(JSFunction.HOME_OBJECT_ID, context);
    }

    public static DefineMethodNode create(JSContext context, JSFunctionExpressionNode functionExpressionNode, int blockScopeSlot) {
        return new DefineMethodNode(context, functionExpressionNode.functionData, blockScopeSlot);
    }

    public JSFunctionData getFunctionData() {
        return functionData;
    }

    public DynamicObject execute(VirtualFrame frame, DynamicObject homeObject, DynamicObject functionPrototype) {
        assert JSRuntime.isObject(functionPrototype);
        assert JSRuntime.isObject(homeObject);
        DynamicObject closure = functionCreateNode.executeWithPrototype(frame, functionPrototype);
        makeMethodNode.setValue(closure, homeObject);
        return closure;
    }

    int getBlockScopeSlot() {
        return functionCreateNode.blockScopeSlot;
    }

    protected abstract static class FunctionCreateNode extends JavaScriptBaseNode {
        private final JSFunctionData functionData;
        @Child private InitFunctionNode initFunctionNode;
        final int blockScopeSlot;

        protected FunctionCreateNode(JSContext context, JSFunctionData functionData, int blockScopeSlot) {
            assert context == functionData.getContext();
            this.functionData = functionData;
            this.initFunctionNode = InitFunctionNode.create(functionData);
            this.blockScopeSlot = blockScopeSlot;
        }

        public static FunctionCreateNode create(JSContext context, JSFunctionData functionData, int blockScopeSlot) {
            return FunctionCreateNodeGen.create(context, functionData, blockScopeSlot);
        }

        public abstract DynamicObject executeWithPrototype(VirtualFrame frame, Object prototype);

        @SuppressWarnings("unused")
        @Specialization(guards = {"!getContext().isMultiContext()", "prototype == cachedPrototype", "isJSObject(cachedPrototype)"}, limit = "getContext().getPropertyCacheLimit()")
        protected final DynamicObject doCached(VirtualFrame frame, DynamicObject prototype,
                        @Cached("prototype") DynamicObject cachedPrototype,
                        @Cached("makeFactory(prototype)") JSFunctionFactory factory) {
            return makeFunction(frame, factory, cachedPrototype);

        }

        @Specialization(guards = {"!getContext().isMultiContext()", "isJSObject(prototype)"}, replaces = "doCached")
        protected final DynamicObject doUncached(VirtualFrame frame, DynamicObject prototype) {
            JSFunctionFactory factory = makeFactory(prototype);
            return makeFunction(frame, factory, prototype);
        }

        @Specialization(guards = {"getContext().isMultiContext()", "isJSObject(prototype)"})
        protected final DynamicObject doMultiContext(VirtualFrame frame, DynamicObject prototype,
                        @Cached("makeFactoryMultiContext()") JSFunctionFactory factory) {
            return makeFunction(frame, factory, prototype);
        }

        @TruffleBoundary
        protected final JSFunctionFactory makeFactory(DynamicObject prototype) {
            return JSFunctionFactory.create(getContext(), prototype);
        }

        protected final JSFunctionFactory makeFactoryMultiContext() {
            return makeFactory(null);
        }

        protected final DynamicObject makeFunction(VirtualFrame frame, JSFunctionFactory factory, DynamicObject prototype) {
            MaterializedFrame enclosingFrame;
            if (functionData.needsParentFrame()) {
                if (blockScopeSlot >= 0) {
                    Object blockScope = frame.getObject(blockScopeSlot);
                    enclosingFrame = JSFrameUtil.castMaterializedFrame(blockScope);
                } else {
                    enclosingFrame = frame.materialize();
                }
            } else {
                enclosingFrame = JSFrameUtil.NULL_MATERIALIZED_FRAME;
            }
            JSRealm realm = getRealm();
            DynamicObject function = factory.createWithPrototype(functionData, enclosingFrame, JSFunction.CLASS_PROTOTYPE_PLACEHOLDER, realm, prototype);
            initFunctionNode.execute(function);
            return function;
        }

        final JSContext getContext() {
            return functionData.getContext();
        }

        @Specialization(guards = "!isJSObject(prototype)")
        protected final DynamicObject doNonObject(@SuppressWarnings("unused") Object prototype) {
            throw Errors.createTypeError("functionPrototype not an object", this);
        }
    }
}
