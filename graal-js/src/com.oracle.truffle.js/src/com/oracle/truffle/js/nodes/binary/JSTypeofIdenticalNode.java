/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.TokenType;
import com.oracle.js.parser.ir.BinaryNode;
import com.oracle.js.parser.ir.Expression;
import com.oracle.js.parser.ir.LiteralNode;
import com.oracle.js.parser.ir.UnaryNode;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRecord;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.builtins.JSTuple;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This node optimizes the code patterns of typeof(a) === "typename" and "typename" == typeof (a).
 * It thus combines a TypeOfNode and an IdenticalNode or EqualsNode (both show the same behavior in
 * this case).
 *
 * @see TypeOfNode
 * @see JSRuntime#typeof(Object)
 */
@ImportStatic({JSTypeofIdenticalNode.Type.class, JSConfig.class})
public abstract class JSTypeofIdenticalNode extends JSUnaryNode {

    public enum Type {
        Number,
        BigInt,
        String,
        Boolean,
        Object,
        Undefined,
        Function,
        Symbol,
        Record,
        Tuple,
        /** Unknown type string. Always false. */
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
            case JSOrdinary.TYPE_NAME:
                return Type.Object;
            case Undefined.TYPE_NAME:
                return Type.Undefined;
            case JSFunction.TYPE_NAME:
                return Type.Function;
            case JSSymbol.TYPE_NAME:
                return Type.Symbol;
            case JSRecord.TYPE_NAME:
                return Type.Record;
            case JSTuple.TYPE_NAME:
                return Type.Tuple;
            default:
                return Type.False;
        }
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == BinaryOperationTag.class || tag == UnaryOperationTag.class || tag == LiteralTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (materializedTags.contains(BinaryOperationTag.class) || materializedTags.contains(UnaryOperationTag.class) || materializedTags.contains(LiteralTag.class)) {
            Object[] info = parseMaterializationInfo();
            if (info == null) {
                info = new Object[]{type.name().toLowerCase(), true, true};
            }
            JavaScriptNode lhs = JSConstantNode.create(info[0]);
            JavaScriptNode rhs = TypeOfNode.create(cloneUninitialized(getOperand(), materializedTags));
            if ((Boolean) info[2]) {
                JavaScriptNode tmp = lhs;
                lhs = rhs;
                rhs = tmp;
            }
            JavaScriptNode materialized = ((Boolean) info[1]) ? JSIdenticalNode.createUnoptimized(lhs, rhs) : JSEqualNode.createUnoptimized(lhs, rhs);
            transferSourceSectionAddExpressionTag(this, lhs);
            transferSourceSectionAddExpressionTag(this, rhs);
            transferSourceSectionAndTags(this, materialized);
            return materialized;
        } else {
            return this;
        }
    }

    @SuppressWarnings("deprecation")
    private JavaScriptLanguage getLanguage() {
        return getRootNode().getLanguage(JavaScriptLanguage.class);
    }

    private Object[] parseMaterializationInfo() {
        String literal;
        boolean identity;
        boolean typeofAsLeftOperand;
        JSContext context = getLanguage().getJSContext();
        try {
            Expression expression = context.getEvaluator().parseExpression(context, getSourceSection().getCharacters().toString());
            if (expression instanceof BinaryNode) {
                BinaryNode binaryNode = (BinaryNode) expression;
                Expression lhs = binaryNode.getLhs();
                Expression rhs = binaryNode.getRhs();
                if (isTypeOf(lhs) && rhs instanceof LiteralNode) {
                    typeofAsLeftOperand = true;
                    literal = ((LiteralNode<?>) rhs).getString();
                } else if (isTypeOf(rhs) && lhs instanceof LiteralNode) {
                    typeofAsLeftOperand = false;
                    literal = ((LiteralNode<?>) lhs).getString();
                } else {
                    return null;
                }
                TokenType tokenType = binaryNode.tokenType();
                if (tokenType == TokenType.EQ) {
                    identity = false;
                } else if (tokenType == TokenType.EQ_STRICT) {
                    identity = true;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (ParserException ex) {
            return null;
        }
        return new Object[]{literal, identity, typeofAsLeftOperand};
    }

    private static boolean isTypeOf(Expression expression) {
        return (expression instanceof UnaryNode) && ((UnaryNode) expression).tokenType() == TokenType.TYPEOF;
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

    @Specialization
    protected final boolean doBoolean(@SuppressWarnings("unused") boolean value) {
        return (type == Type.Boolean);
    }

    @Specialization
    protected final boolean doNumber(@SuppressWarnings("unused") int value) {
        return (type == Type.Number);
    }

    @Specialization
    protected final boolean doNumber(@SuppressWarnings("unused") SafeInteger value) {
        return (type == Type.Number);
    }

    @Specialization
    protected final boolean doNumber(@SuppressWarnings("unused") long value) {
        return (type == Type.Number);
    }

    @Specialization
    protected final boolean doNumber(@SuppressWarnings("unused") double value) {
        return (type == Type.Number);
    }

    @Specialization
    protected final boolean doSymbol(@SuppressWarnings("unused") Symbol value) {
        return (type == Type.Symbol);
    }

    @Specialization
    protected final boolean doBigInt(@SuppressWarnings("unused") BigInt value) {
        return (type == Type.BigInt);
    }

    @Specialization
    protected final boolean doRecord(@SuppressWarnings("unused") Record value) {
        return (type == Type.Record);
    }

    @Specialization
    protected final boolean doTuple(@SuppressWarnings("unused") Tuple value) {
        return (type == Type.Tuple);
    }

    @Specialization
    protected final boolean doString(@SuppressWarnings("unused") CharSequence value) {
        return (type == Type.String);
    }

    @Specialization(guards = {"isJSDynamicObject(value)"})
    protected final boolean doJSType(DynamicObject value,
                    @Cached("create()") BranchProfile proxyBranch) {
        if (type == Type.Number || type == Type.BigInt || type == Type.String || type == Type.Boolean || type == Type.Symbol || type == Type.False) {
            return false;
        } else if (type == Type.Object) {
            if (JSProxy.isJSProxy(value)) {
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
            } else if (JSProxy.isJSProxy(value)) {
                proxyBranch.enter();
                return checkProxy(value, true);
            } else {
                return false;
            }
        }
        throw Errors.shouldNotReachHere();
    }

    @Specialization(guards = {"isForeignObject(value)"}, limit = "InteropLibraryLimit")
    protected final boolean doForeignObject(Object value,
                    @CachedLibrary("value") InteropLibrary interop) {
        if (type == Type.Undefined || type == Type.Symbol || type == Type.False) {
            return false;
        } else {
            if (type == Type.Boolean) {
                return interop.isBoolean(value);
            } else if (type == Type.String) {
                return interop.isString(value);
            } else if (type == Type.Number) {
                return interop.isNumber(value);
            } else if (type == Type.Function) {
                return interop.isExecutable(value) || interop.isInstantiable(value);
            } else if (type == Type.Object) {
                return !interop.isExecutable(value) && !interop.isInstantiable(value) && !interop.isBoolean(value) && !interop.isString(value) && !interop.isNumber(value);
            } else {
                return false;
            }
        }
    }

    private static boolean checkProxy(DynamicObject value, boolean isFunction) {
        // Find a non-proxy target (and the associated proxy)
        DynamicObject proxy;
        Object target = value;
        do {
            proxy = (DynamicObject) target;
            target = JSProxy.getTarget(proxy);
        } while (JSProxy.isJSProxy(target));

        if (target == Null.instance) { // revoked proxy
            return isFunction == JSRuntime.isRevokedCallableProxy(proxy);
        } else {
            if (isFunction) {
                return JSFunction.isJSFunction(target) || JSRuntime.isCallableForeign(target) || JSRuntime.isConstructorForeign(target);
            } else {
                return (JSDynamicObject.isJSDynamicObject(target) && !JSFunction.isJSFunction(target)) ||
                                (JSRuntime.isForeignObject(target) && !JSRuntime.isCallableForeign(target) && !JSRuntime.isConstructorForeign(target));
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSTypeofIdenticalNodeGen.create(cloneUninitialized(getOperand(), materializedTags), type);
    }
}
