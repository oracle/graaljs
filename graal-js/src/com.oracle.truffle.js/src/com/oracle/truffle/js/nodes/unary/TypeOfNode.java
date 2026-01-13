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
package com.oracle.truffle.js.nodes.unary;

import java.util.Set;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.binary.JSTypeofIdenticalNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryOperationTag;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRuntime;
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
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * @see JSRuntime#typeof(Object)
 * @see JSTypeofIdenticalNode
 */
@SuppressWarnings("unused")
@NodeInfo(shortName = "typeof")
@ImportStatic({JSRuntime.class, JSConfig.class})
public abstract class TypeOfNode extends JSUnaryNode {

    protected TypeOfNode(JavaScriptNode operand) {
        super(operand);
    }

    public static TypeOfNode create(JavaScriptNode operand) {
        return TypeOfNodeGen.create(operand);
    }

    @NeverDefault
    public static TypeOfNode create() {
        return create(null);
    }

    public abstract TruffleString executeString(Object operand);

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == UnaryOperationTag.class ? true : super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == TruffleString.class;
    }

    @Specialization
    protected TruffleString doString(TruffleString operand) {
        return JSString.TYPE_NAME;
    }

    @Specialization
    protected TruffleString doInt(int operand) {
        return JSNumber.TYPE_NAME;
    }

    @Specialization
    protected TruffleString doDouble(double operand) {
        return JSNumber.TYPE_NAME;
    }

    @Specialization
    protected TruffleString doBoolean(boolean operand) {
        return JSBoolean.TYPE_NAME;
    }

    @Specialization
    protected TruffleString doBigInt(BigInt operand) {
        return JSBigInt.TYPE_NAME;
    }

    @Specialization(guards = "isJSNull(operand)")
    protected TruffleString doNull(Object operand) {
        return Null.TYPE_NAME;
    }

    @Specialization(guards = "isUndefined(operand)")
    protected TruffleString doUndefined(Object operand) {
        return Undefined.TYPE_NAME;
    }

    @Specialization(guards = "isJSFunction(operand)")
    protected TruffleString doJSFunction(Object operand) {
        return JSFunction.TYPE_NAME;
    }

    @Specialization(guards = {"isJSDynamicObject(operand)", "!isJSFunction(operand)", "!isUndefined(operand)", "!isJSProxy(operand)"})
    protected TruffleString doJSObjectOnly(Object operand) {
        return JSOrdinary.TYPE_NAME;
    }

    @Specialization
    protected TruffleString doJSProxy(JSProxyObject operand,
                    @Cached TypeOfNode typeofNode) {
        Object target = JSProxy.getTargetNonProxy(operand);
        return typeofNode.executeString(target);
    }

    @Specialization
    protected TruffleString doSymbol(Symbol operand) {
        return JSSymbol.TYPE_NAME;
    }

    @Specialization
    protected static TruffleString doLong(long operand) {
        return JSNumber.TYPE_NAME;
    }

    @InliningCutoff
    @Specialization(guards = "isForeignObject(operand)", limit = "InteropLibraryLimit")
    protected TruffleString doTruffleObject(Object operand,
                    @CachedLibrary("operand") InteropLibrary interop) {
        if (interop.isBoolean(operand)) {
            return JSBoolean.TYPE_NAME;
        } else if (interop.isString(operand)) {
            return JSString.TYPE_NAME;
        } else if (interop.isNumber(operand)) {
            return JSNumber.TYPE_NAME;
        }
        if (interop.isExecutable(operand) || interop.isInstantiable(operand) || isHostSymbolInNashornCompatMode(operand, interop)) {
            return JSFunction.TYPE_NAME;
        } else {
            return JSOrdinary.TYPE_NAME;
        }
    }

    private boolean isHostSymbolInNashornCompatMode(Object value, InteropLibrary interop) {
        if (getLanguage().getJSContext().isOptionNashornCompatibilityMode()) {
            return interop.isHostObject(value) && interop.isScope(value);
        }
        return false;
    }

    @Fallback
    protected TruffleString doJavaObject(Object operand) {
        return JSOrdinary.TYPE_NAME;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return TypeOfNodeGen.create(cloneUninitialized(getOperand(), materializedTags));
    }

}
