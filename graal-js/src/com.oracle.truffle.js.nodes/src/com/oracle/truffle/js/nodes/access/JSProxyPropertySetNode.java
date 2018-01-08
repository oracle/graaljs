/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.util.JSReflectUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class JSProxyPropertySetNode extends JavaScriptBaseNode {

    protected final boolean isDeep;
    private final boolean isStrict;

    @Child private JSFunctionCallNode call;
    @Child private JSToBooleanNode toBoolean;
    @Child protected GetMethodNode trapGet;
    @Child private JSToPropertyKeyNode toPropertyKeyNode;
    @Child private Node writeForeignNode;

    protected JSProxyPropertySetNode(JSContext context, boolean isDeep, boolean isStrict) {
        this.call = JSFunctionCallNode.createCall();
        this.trapGet = GetMethodNode.create(context, null, JSProxy.SET);
        this.toBoolean = JSToBooleanNode.create();
        this.isDeep = isDeep;
        this.isStrict = isStrict;
    }

    public abstract boolean executeWithReceiverAndValue(Object proxy, Object value, Object key, boolean floatingCondition);

    public abstract boolean executeWithReceiverAndValueInt(Object proxy, int value, Object key, boolean floatingCondition);

    public abstract boolean executeWithReceiverAndValueIntKey(Object proxy, Object value, int key, boolean floatingCondition);

    public static JSProxyPropertySetNode create(JSContext context, boolean isDeep, boolean isStrict) {
        return JSProxyPropertySetNodeGen.create(context, isDeep, isStrict);
    }

    @Specialization
    protected boolean doGeneric(DynamicObject obj, Object value, Object key, boolean floatingCondition,
                    @Cached("createBinaryProfile()") ConditionProfile walkProto,
                    @Cached("createBinaryProfile()") ConditionProfile hasTrap) {
        assert !(key instanceof HiddenKey);
        Object propertyKey = toPropertyKey(key);
        DynamicObject proxy = obj;
        if (!isDeep) {
            assert JSProxy.isProxy(proxy);
        } else if (walkProto.profile(!JSProxy.isProxy(proxy))) {
            // the proxy is only one of the prototypes
            while (!JSProxy.isProxy(proxy)) {
                // check that there is no defined property in the prototype chain
                if (!JSProxy.checkPropertyIsSettable(proxy, key)) {
                    return false;
                }
                proxy = JSObject.getPrototype(proxy);
            }
        }
        DynamicObject handler = JSProxy.getHandler(proxy, floatingCondition);
        TruffleObject target = JSProxy.getTarget(proxy, floatingCondition);
        Object trapFun = trapGet.executeWithTarget(handler);
        if (hasTrap.profile(trapFun == Undefined.instance)) {
            if (JSObject.isJSObject(target)) {
                return JSReflectUtils.performOrdinarySet((DynamicObject) target, propertyKey, value, obj);
            } else {
                truffleWrite(target, propertyKey, value);
                return true;
            }
        }
        Object trapResult = call.executeCall(JSArguments.create(handler, trapFun, target, propertyKey, value, obj));
        boolean booleanTrapResult = toBoolean.executeBoolean(trapResult);
        if (!booleanTrapResult) {
            if (isStrict) {
                throwTrapReturnedFalsishError(propertyKey);
            } else {
                return false;
            }
        }
        return JSProxy.checkProxySetTrapInvariants(proxy, propertyKey, value);
    }

    @TruffleBoundary
    private static void throwTrapReturnedFalsishError(Object propertyKey) {
        throw Errors.createTypeError("'set' on proxy: trap returned falsish for property '" + propertyKey + "'");
    }

    private Object truffleWrite(TruffleObject obj, Object key, Object value) {
        if (writeForeignNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            writeForeignNode = insert(Message.WRITE.createNode());
        }
        return JSInteropNodeUtil.write(obj, key, value, writeForeignNode);
    }

    private Object toPropertyKey(Object key) {
        if (toPropertyKeyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toPropertyKeyNode = insert(JSToPropertyKeyNode.create());
        }
        return toPropertyKeyNode.execute(key);
    }
}
