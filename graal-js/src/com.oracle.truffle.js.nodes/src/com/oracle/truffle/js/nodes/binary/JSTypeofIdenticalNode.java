/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
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
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;

/**
 * This node optimizes the code patterns of typeof(a) === "typename" and "typename" == typeof (a).
 * It thus combines a TypeOfNode and an IdenticalNode or EqualsNode (both show the same behavior in
 * this case).
 *
 */
@ImportStatic(JSTypeofIdenticalNode.Type.class)
public abstract class JSTypeofIdenticalNode extends JSUnaryNode {
    protected static final int MAX_CLASSES = 3;
    private final BranchProfile proxyBranch = BranchProfile.create();
    private final BranchProfile foreignBranch = BranchProfile.create();
    @Child private Node isExecutable;
    @Child private Node isBoxed;
    @Child private Node unboxNode;
    @Child private JSForeignToJSTypeNode toJSTypeNode;

    public enum Type {
        Number,
        String,
        Boolean,
        Object,
        Undefined,
        Function,
        Symbol,
        False
    }

    private final Type type;

    protected JSTypeofIdenticalNode(Type type) {
        this.type = type;
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, JSConstantStringNode constStringNode) {
        return create(childNode, (String) constStringNode.execute(null));
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, String string) {
        return JSTypeofIdenticalNodeGen.create(typeStringToEnum(string), childNode);
    }

    private static Type typeStringToEnum(String string) {
        switch (string) {
            case JSNumber.TYPE_NAME:
                return Type.Number;
            case JSString.TYPE_NAME:
                return Type.String;
            case JSBoolean.TYPE_NAME:
                return Type.Boolean;
            case JSUserObject.TYPE_NAME:
                return Type.Object;
            case Undefined.TYPE_NAME:
                return Type.Undefined;
            case JSFunction.TYPE_NAME:
                return Type.Function;
            case JSSymbol.TYPE_NAME:
                return Type.Symbol;
            default:
                return Type.False;
        }
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

    @Override
    public abstract boolean executeBoolean(VirtualFrame frame);

    @Specialization(guards = {"value.getClass() == cachedClass"}, limit = "MAX_CLASSES")
    protected final boolean doCached(Object value,      //
                    @Cached("value.getClass()") Class<?> cachedClass) {
        return doDefault(cachedClass.cast(value));
    }

    @Specialization(replaces = "doCached")
    protected final boolean doDefault(Object value) {
        if (ambiguousForForeignObject() && JSRuntime.isForeignObject(value)) {
            foreignBranch.enter();
            return doForeignObject((TruffleObject) value);
        }
        return doDefaultImpl(value);
    }

    private boolean ambiguousForForeignObject() {
        // Type.False, Type.Symbol and Type.Undefined do not require special
        // handling of foreign objects - we always have foreignObject !== type
        return type == Type.Boolean || type == Type.Function || type == Type.Number || type == Type.Object || type == Type.String;
    }

    private boolean doForeignObject(TruffleObject object) {
        if (isBoxedForeignObject(object)) {
            Object unboxedValue = unboxForeignObject(object);
            return doDefaultImpl(unboxedValue);
        } else if (type == Type.Function) {
            return canExecuteForeignObject(object);
        } else if (type == Type.Object) {
            return !canExecuteForeignObject(object);
        } else {
            return false;
        }
    }

    private boolean doDefaultImpl(Object value) {
        if (type == Type.Number) {
            return JSTruffleOptions.NashornJavaInterop ? value instanceof Number : JSRuntime.isNumber(value);
        } else if (type == Type.String) {
            return JSRuntime.isString(value);
        } else if (type == Type.Boolean) {
            return value instanceof Boolean;
        } else if (type == Type.Object) {
            if (JSObject.isDynamicObject(value)) {
                DynamicObject obj = (DynamicObject) value;
                if (JSProxy.isProxy(obj)) {
                    proxyBranch.enter();
                    return checkProxy(obj, false);
                } else {
                    return !JSFunction.isJSFunction((DynamicObject) value) && value != Undefined.instance;
                }
            } else {
                return JSTruffleOptions.NashornJavaInterop && nonPrimitiveJavaObj(value);
            }
        } else if (type == Type.Undefined) {
            return value == Undefined.instance;
        } else if (type == Type.Symbol) {
            return value instanceof Symbol;
        } else if (type == Type.Function) {
            if (JSFunction.isJSFunction(value)) {
                return true;
            } else if (JSProxy.isProxy(value)) {
                proxyBranch.enter();
                return checkProxy((DynamicObject) value, true);
            } else if (JSTruffleOptions.NashornJavaInterop) {
                return value instanceof JavaClass || value instanceof JavaMethod;
            } else {
                return false;
            }
        } else if (type == Type.False) {
            return false;
        }
        throw Errors.shouldNotReachHere();
    }

    private boolean canExecuteForeignObject(TruffleObject object) {
        if (isExecutable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isExecutable = insert(Message.IS_EXECUTABLE.createNode());
        }
        return ForeignAccess.sendIsExecutable(isExecutable, object);
    }

    private boolean isBoxedForeignObject(TruffleObject object) {
        if (isBoxed == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isBoxed = insert(Message.IS_BOXED.createNode());
        }
        return ForeignAccess.sendIsExecutable(isBoxed, object);
    }

    private Object unboxForeignObject(TruffleObject object) {
        if (unboxNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unboxNode = insert(Message.UNBOX.createNode());
            toJSTypeNode = insert(JSForeignToJSTypeNode.create());
        }
        return toJSTypeNode.executeWithTarget(JSInteropNodeUtil.unbox(object, unboxNode));
    }

    private static boolean checkProxy(DynamicObject value, boolean isFunction) {
        TruffleObject obj = JSProxy.getTargetNonProxy(value);
        return isFunction ? JSFunction.isJSFunction(obj) : JSObject.isDynamicObject(obj);
    }

    private static boolean nonPrimitiveJavaObj(Object obj) {
        return !(JSObject.isDynamicObject(obj) || obj instanceof Number || obj instanceof Boolean || obj instanceof Symbol || JSRuntime.isString(obj) || obj instanceof JavaClass ||
                        obj instanceof JavaMethod);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSTypeofIdenticalNodeGen.create(type, cloneUninitialized(getOperand()));
    }
}
