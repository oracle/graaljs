/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.nodes.access.*;
import com.oracle.truffle.js.nodes.function.DefineMethodNodeFactory.FunctionCreateNodeGen;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.builtins.*;

public class DefineMethodNode extends JavaScriptBaseNode {

    private final JSFunctionData functionData;
    @Child private FunctionCreateNode functionCreateNode;
    @Child private PropertySetNode makeMethodNode;

    protected DefineMethodNode(JSContext context, JSFunctionData functionData) {
        this.functionData = functionData;
        this.functionCreateNode = FunctionCreateNode.create(context, functionData);
        this.makeMethodNode = PropertySetNode.create(JSFunction.HOME_OBJECT_ID, false, context, false);
    }

    public static DefineMethodNode create(JSContext context, JSFunctionExpressionNode functionExpressionNode) {
        return new DefineMethodNode(context, functionExpressionNode.functionData);
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

    @ImportStatic(JSTruffleOptions.class)
    protected abstract static class FunctionCreateNode extends JavaScriptBaseNode {
        private final JSFunctionData functionData;
        @Child private RealmNode realmNode;

        protected FunctionCreateNode(JSContext context, JSFunctionData functionData) {
            this.functionData = functionData;
            this.realmNode = RealmNode.create(context);
        }

        public static FunctionCreateNode create(JSContext context, JSFunctionData functionData) {
            return FunctionCreateNodeGen.create(context, functionData);
        }

        public abstract DynamicObject executeWithPrototype(VirtualFrame frame, Object prototype);

        @SuppressWarnings("unused")
        @Specialization(guards = {"prototype == cachedPrototype", "isJSObject(cachedPrototype)"}, limit = "PropertyCacheLimit")
        protected final DynamicObject doCached(VirtualFrame frame, DynamicObject prototype,
                        @Cached("prototype") DynamicObject cachedPrototype,
                        @Cached("makeFactory(getRealm(frame), prototype)") DynamicObjectFactory factory) {
            return makeFunction(factory, frame);

        }

        @Specialization(guards = "isJSObject(prototype)", replaces = "doCached")
        protected final DynamicObject doUncached(VirtualFrame frame, DynamicObject prototype) {
            DynamicObjectFactory factory = makeFactory(getRealm(frame), prototype);
            return makeFunction(factory, frame);
        }

        @TruffleBoundary
        protected final DynamicObjectFactory makeFactory(JSRealm realm, DynamicObject prototype) {
            return JSFunction.makeConstructorShape(JSFunction.makeInitialFunctionShape(realm, prototype, true), functionData.isPrototypeNotWritable()).createFactory();
        }

        protected final DynamicObject makeFunction(DynamicObjectFactory factory, VirtualFrame frame) {
            return JSFunction.create(factory, getRealm(frame), functionData, functionData.needsParentFrame() ? frame.materialize() : JSFrameUtil.NULL_MATERIALIZED_FRAME);
        }

        protected final JSRealm getRealm(VirtualFrame frame) {
            return realmNode.execute(frame);
        }

        @Specialization(guards = "!isJSObject(prototype)")
        protected static DynamicObject doNonObject(@SuppressWarnings("unused") Object prototype) {
            throw Errors.createTypeError("functionPrototype not an object");
        }
    }
}
