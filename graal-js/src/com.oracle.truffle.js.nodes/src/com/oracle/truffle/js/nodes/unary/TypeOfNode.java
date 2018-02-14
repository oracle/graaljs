/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * @see JSRuntime#typeof(Object)
 * @see JSTypeofIdenticalNode
 */
@SuppressWarnings("unused")
@NodeInfo(shortName = "typeof")
@ImportStatic({JSInteropUtil.class, JSRuntime.class})
public abstract class TypeOfNode extends JSUnaryNode {
    protected static final int MAX_CLASSES = 3;

    public static TypeOfNode create(JavaScriptNode operand) {
        return TypeOfNodeGen.create(operand);
    }

    public static TypeOfNode create() {
        return create(null);
    }

    public abstract String executeString(Object operand);

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == String.class;
    }

    @Specialization
    protected String doString(CharSequence operand) {
        return JSString.TYPE_NAME;
    }

    @Specialization
    protected String doInt(int operand) {
        return JSNumber.TYPE_NAME;
    }

    @Specialization
    protected String doDouble(double operand) {
        return JSNumber.TYPE_NAME;
    }

    @Specialization
    protected String doBoolean(boolean operand) {
        return JSBoolean.TYPE_NAME;
    }

    @Specialization(guards = "isJSNull(operand)")
    protected String doNull(Object operand) {
        return Null.TYPE_NAME;
    }

    @Specialization(guards = "isUndefined(operand)")
    protected String doUndefined(Object operand) {
        return Undefined.TYPE_NAME;
    }

    @Specialization(guards = "isJSFunction(operand)")
    protected String doJSFunction(DynamicObject operand) {
        return JSFunction.TYPE_NAME;
    }

    @Specialization(guards = {"isJSType(operand)", "!isJSFunction(operand)", "!isUndefined(operand)", "!isJSProxy(operand)"})
    protected String doJSObjectOnly(DynamicObject operand) {
        return JSUserObject.TYPE_NAME;
    }

    @Specialization(guards = {"isJSProxy(operand)"})
    protected String doJSProxy(DynamicObject operand,
                    @Cached("create()") TypeOfNode typeofNode) {
        return typeofNode.executeString(JSProxy.getTarget(operand));
    }

    @Specialization
    protected String doJavaClass(JavaClass operand) {
        return JavaClass.TYPE_NAME;
    }

    @Specialization
    protected String doJavaMethod(JavaMethod operand) {
        return JavaMethod.TYPE_NAME;
    }

    @Specialization
    protected String doSymbol(Symbol operand) {
        return JSSymbol.TYPE_NAME;
    }

    @TruffleBoundary
    @Specialization(guards = "isForeignObject(operand)")
    protected String doTruffleObject(TruffleObject operand,
                    @Cached("createIsExecutable()") Node isExecutable,
                    @Cached("createIsBoxed()") Node isBoxedNode,
                    @Cached("createUnbox()") Node unboxNode,
                    @Cached("create()") TypeOfNode recTypeOf,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        if (ForeignAccess.sendIsBoxed(isBoxedNode, operand)) {
            Object obj = foreignConvertNode.executeWithTarget(JSInteropNodeUtil.unbox(operand, unboxNode));
            return recTypeOf.executeString(obj);
        } else if (ForeignAccess.sendIsExecutable(isExecutable, operand)) {
            return JSFunction.TYPE_NAME;
        } else {
            return JSUserObject.TYPE_NAME;
        }
    }

    @Specialization(guards = {"cachedClass != null", "operand.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected String doOtherCached(Object operand,
                    @Cached("getJavaObjectClass(operand)") Class<?> cachedClass) {
        return JSUserObject.TYPE_NAME;
    }

    @Specialization(guards = "isJavaObject(operand)", replaces = "doOtherCached")
    protected String doOther(Object operand) {
        return JSUserObject.TYPE_NAME;
    }

    @Fallback
    protected String doJavaObject(Object operand) {
        assert operand != null;
        return operand instanceof Number ? JSNumber.TYPE_NAME : JSUserObject.TYPE_NAME;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return TypeOfNodeGen.create(cloneUninitialized(getOperand()));
    }

}
