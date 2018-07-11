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
package com.oracle.truffle.js.nodes.binary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * This node optimizes the code patterns of typeof(a) === "typename" and "typename" == typeof (a).
 * It thus combines a TypeOfNode and an IdenticalNode or EqualsNode (both show the same behavior in
 * this case).
 *
 * @see TypeOfNode
 * @see JSRuntime#typeof(Object)
 */
@ImportStatic({JSTypeofIdenticalNode.Type.class, JSInteropUtil.class})
public abstract class JSTypeofIdenticalNode extends JSUnaryNode {
    protected static final int MAX_CLASSES = 3;
    private final BranchProfile proxyBranch = BranchProfile.create();

    public enum Type {
        Number,
        BigInt,
        String,
        Boolean,
        Object,
        Undefined,
        Function,
        Symbol,
        False
    }

    private final Type type;

    protected JSTypeofIdenticalNode(JavaScriptNode childNode, Type type) {
        super(childNode);
        this.type = type;
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, JSConstantStringNode constStringNode) {
        return create(childNode, (String) constStringNode.execute(null));
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, String string) {
        return JSTypeofIdenticalNodeGen.create(childNode, typeStringToEnum(string));
    }

    private static Type typeStringToEnum(String string) {
        switch (string) {
            case JSNumber.TYPE_NAME:
                return Type.Number;
            case JSBigInt.TYPE_NAME:
                return Type.BigInt;
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

    @Specialization(guards = {"value.getClass() == cachedClass", "!isTruffleObject(value)"}, limit = "MAX_CLASSES")
    protected final boolean doCached(Object value,
                    @Cached("value.getClass()") Class<?> cachedClass) {
        return doUncached(cachedClass.cast(value));
    }

    @Specialization
    protected final boolean doSymbol(@SuppressWarnings("unused") Symbol value) {
        return (type == Type.Symbol);
    }

    @Specialization
    protected final boolean doLargeInteger(@SuppressWarnings("unused") LargeInteger value) {
        return (type == Type.Number);
    }

    @Specialization
    protected final boolean doBigInt(@SuppressWarnings("unused") BigInt value) {
        return (type == Type.BigInt);
    }

    @Specialization
    protected final boolean doLazyString(@SuppressWarnings("unused") JSLazyString value) {
        return (type == Type.String);
    }

    @Specialization(guards = {"isJSType(value)"})
    protected final boolean doJSType(DynamicObject value) {
        if (type == Type.Number || type == Type.BigInt || type == Type.String || type == Type.Boolean || type == Type.Symbol || type == Type.False) {
            return false;
        } else if (type == Type.Object) {
            if (JSProxy.isProxy(value)) {
                proxyBranch.enter();
                return checkProxy(value, false);
            } else {
                return !JSFunction.isJSFunction(value) && value != Undefined.instance;
            }
        } else if (type == Type.Undefined) {
            return value == Undefined.instance;
        } else if (type == Type.Function) {
            if (JSFunction.isJSFunction(value)) {
                return true;
            } else if (JSProxy.isProxy(value)) {
                proxyBranch.enter();
                return checkProxy(value, true);
            } else {
                return false;
            }
        }
        throw Errors.shouldNotReachHere();
    }

    @Specialization(guards = {"isForeignObject(value)"})
    protected final boolean doForeignObject(TruffleObject value,
                    @Cached("createIsExecutable()") Node isExecutable,
                    @Cached("createIsBoxed()") Node isBoxed,
                    @Cached("createUnbox()") Node unboxNode,
                    @Cached("createIsInstantiable()") Node isInstantiable,
                    @Cached("create()") JSForeignToJSTypeNode toJSTypeNode) {
        if (type == Type.Undefined || type == Type.Symbol || type == Type.False) {
            return false;
        } else {
            if (ForeignAccess.sendIsBoxed(isBoxed, value)) {
                Object unboxedValue = toJSTypeNode.executeWithTarget(JSInteropNodeUtil.unbox(value, unboxNode));
                return doUncached(unboxedValue);
            } else if (type == Type.Function) {
                return ForeignAccess.sendIsExecutable(isExecutable, value) || ForeignAccess.sendIsInstantiable(isInstantiable, value);
            } else if (type == Type.Object) {
                return !ForeignAccess.sendIsExecutable(isExecutable, value) && !ForeignAccess.sendIsInstantiable(isInstantiable, value);
            } else {
                return false;
            }
        }
    }

    @Specialization(guards = {"!isTruffleObject(value)"}, replaces = "doCached")
    protected boolean doUncached(Object value) {
        if (type == Type.Number) {
            return JSTruffleOptions.NashornJavaInterop ? value instanceof Number : JSRuntime.isNumber(value);
        } else if (type == Type.BigInt) {
            return JSRuntime.isBigInt(value);
        } else if (type == Type.String) {
            return JSRuntime.isString(value);
        } else if (type == Type.Boolean) {
            return value instanceof Boolean;
        } else if (type == Type.Object) {
            return JSTruffleOptions.NashornJavaInterop && nonPrimitiveJavaObj(value);
        } else if (type == Type.Undefined) {
            return false;
        } else if (type == Type.Symbol) {
            return false;
        } else if (type == Type.Function) {
            if (JSTruffleOptions.NashornJavaInterop) {
                return value instanceof JavaClass || value instanceof JavaMethod;
            } else {
                return false;
            }
        } else if (type == Type.False) {
            return false;
        }
        throw Errors.shouldNotReachHere();
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
        return JSTypeofIdenticalNodeGen.create(cloneUninitialized(getOperand()), type);
    }
}
