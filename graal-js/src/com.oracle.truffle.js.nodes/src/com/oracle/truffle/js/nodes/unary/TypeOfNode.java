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
package com.oracle.truffle.js.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.NodeObjectDescriptor;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.BinaryExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSRuntime;
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

    protected TypeOfNode(JavaScriptNode operand) {
        super(operand);
    }

    public static TypeOfNode create(JavaScriptNode operand) {
        return TypeOfNodeGen.create(operand);
    }

    public static TypeOfNode create() {
        return create(null);
    }

    public abstract String executeString(Object operand);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == UnaryExpressionTag.class ? true : super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

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

    @Specialization
    protected String doBigInt(BigInt operand) {
        return JSBigInt.TYPE_NAME;
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
                    @Cached("createIsInstantiable()") Node isInstantiable,
                    @Cached("create()") TypeOfNode recTypeOf,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        if (ForeignAccess.sendIsBoxed(isBoxedNode, operand)) {
            Object obj = foreignConvertNode.executeWithTarget(JSInteropNodeUtil.unbox(operand, unboxNode));
            return recTypeOf.executeString(obj);
        } else if (ForeignAccess.sendIsExecutable(isExecutable, operand) || ForeignAccess.sendIsInstantiable(isInstantiable, operand)) {
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

    @Specialization(guards = {"isJavaObject(operand)", "!isJavaClass(operand)", "!isJavaMethod(operand)"}, replaces = "doOtherCached")
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
