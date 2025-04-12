/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantStringNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.LiteralTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSBigInt;
import com.oracle.truffle.js.runtime.builtins.JSBoolean;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNumber;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSProxyObject;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSSymbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
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
        /** Unknown type string. Always false. */
        False
    }

    protected final Type type;

    protected JSTypeofIdenticalNode(JavaScriptNode childNode, Type type) {
        super(childNode);
        this.type = type;
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, JSConstantStringNode constStringNode) {
        return create(childNode, (TruffleString) constStringNode.execute(null));
    }

    public static JSTypeofIdenticalNode create(JavaScriptNode childNode, TruffleString string) {
        return JSTypeofIdenticalNodeGen.create(childNode, typeStringToEnum(string));
    }

    private static Type typeStringToEnum(TruffleString string) {
        if (Strings.equals(JSNumber.TYPE_NAME, string)) {
            return Type.Number;
        } else if (Strings.equals(JSBigInt.TYPE_NAME, string)) {
            return Type.BigInt;
        } else if (Strings.equals(JSString.TYPE_NAME, string)) {
            return Type.String;
        } else if (Strings.equals(JSBoolean.TYPE_NAME, string)) {
            return Type.Boolean;
        } else if (Strings.equals(JSOrdinary.TYPE_NAME, string)) {
            return Type.Object;
        } else if (Strings.equals(Undefined.TYPE_NAME, string)) {
            return Type.Undefined;
        } else if (Strings.equals(JSFunction.TYPE_NAME, string)) {
            return Type.Function;
        } else if (Strings.equals(JSSymbol.TYPE_NAME, string)) {
            return Type.Symbol;
        } else {
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
                info = new Object[]{Strings.fromJavaString(type.name().toLowerCase()), true, true};
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

    private JavaScriptLanguage getLanguageSafe() {
        try {
            JavaScriptLanguage language = getRootNode().getLanguage(JavaScriptLanguage.class);
            if (language == null) {
                language = getLanguage();
            }
            return language;
        } catch (Throwable e) {
            return null;
        }
    }

    private Object[] parseMaterializationInfo() {
        TruffleString literal;
        boolean identity;
        boolean typeofAsLeftOperand;
        JavaScriptLanguage language = getLanguageSafe();
        if (language == null) {
            return null;
        }
        JSContext context = language.getJSContext();
        try {
            Expression expression = context.getEvaluator().parseExpression(context, getSourceSection().getCharacters().toString());
            if (expression instanceof BinaryNode) {
                BinaryNode binaryNode = (BinaryNode) expression;
                Expression lhs = binaryNode.getLhs();
                Expression rhs = binaryNode.getRhs();
                if (isTypeOf(lhs) && rhs instanceof LiteralNode) {
                    typeofAsLeftOperand = true;
                    literal = Strings.fromJavaString(((LiteralNode<?>) rhs).getString());
                } else if (isTypeOf(rhs) && lhs instanceof LiteralNode) {
                    typeofAsLeftOperand = false;
                    literal = Strings.fromJavaString(((LiteralNode<?>) lhs).getString());
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
        return (expression instanceof UnaryNode) && expression.tokenType() == TokenType.TYPEOF;
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
    protected final boolean doString(@SuppressWarnings("unused") TruffleString value) {
        return (type == Type.String);
    }

    @Specialization(guards = {"type == Object || type == Function", "isJSFunction(value)"})
    protected final boolean doTypeObjectOrFunctionJSFunction(@SuppressWarnings("unused") Object value) {
        assert type == Type.Object || type == Type.Function;
        return type == Type.Function;
    }

    @Specialization(guards = {"type == Object || type == Function"})
    protected final boolean doTypeObjectOrFunctionJSProxy(JSProxyObject value,
                    @Cached IsCallableNode isCallableNode) {
        Object proxyTarget = JSProxy.getTargetNonProxy(value);
        boolean callable = isCallableNode.executeBoolean(proxyTarget);
        if (type == Type.Object) {
            return !callable;
        } else {
            assert type == Type.Function;
            return callable;
        }
    }

    @Specialization(guards = {"type == Object || type == Function", "!isJSFunction(value)", "!isJSProxy(value)"})
    protected final boolean doTypeObjectOrFunctionOther(JSDynamicObject value) {
        assert !JSGuards.isJSFunction(value) && !JSGuards.isJSProxy(value);
        if (type == Type.Object) {
            return value != Undefined.instance;
        } else {
            assert type == Type.Function;
            return false;
        }
    }

    @Specialization(guards = {"type != Object", "type != Function"})
    protected final boolean doTypePrimitive(JSDynamicObject value) {
        if (type == Type.Undefined) {
            return value == Undefined.instance;
        } else {
            assert (type == Type.Number || type == Type.BigInt || type == Type.String || type == Type.Boolean || type == Type.Symbol || type == Type.False);
            return false;
        }
    }

    @InliningCutoff
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
                return isFunction(value, interop);
            } else if (type == Type.Object) {
                return (!interop.isBoolean(value) && !interop.isString(value) && !interop.isNumber(value)) && !isFunction(value, interop);
            } else {
                return false;
            }
        }
    }

    private boolean isFunction(Object value, InteropLibrary interop) {
        return interop.isExecutable(value) || interop.isInstantiable(value) || isHostSymbolInNashornCompatMode(value);
    }

    private boolean isHostSymbolInNashornCompatMode(Object value) {
        if (getLanguage().getJSContext().isOptionNashornCompatibilityMode()) {
            TruffleLanguage.Env env = getRealm().getEnv();
            if (env.isHostSymbol(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return JSTypeofIdenticalNodeGen.create(cloneUninitialized(getOperand(), materializedTags), type);
    }
}
