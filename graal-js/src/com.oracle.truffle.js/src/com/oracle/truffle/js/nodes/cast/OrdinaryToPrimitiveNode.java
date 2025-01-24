/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.IsPrimitiveNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ForeignObjectPrototypeNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

/**
 * Implements OrdinaryToPrimitive (O, hint).
 *
 * @see JSToPrimitiveNode
 */
@ImportStatic({JSConfig.class})
public abstract class OrdinaryToPrimitiveNode extends JavaScriptBaseNode {
    private final Hint hint;
    @Child private PropertyGetNode getToStringNode;
    @Child private PropertyGetNode getValueOfNode;
    @Child private IsCallableNode isCallableNode;
    @Child private JSFunctionCallNode callToStringNode;
    @Child private JSFunctionCallNode callValueOfNode;
    @Child private IsPrimitiveNode isPrimitiveNode;
    @Child private ForeignObjectPrototypeNode foreignObjectPrototypeNode;

    protected OrdinaryToPrimitiveNode(Hint hint) {
        assert hint == Hint.String || hint == Hint.Number;
        this.hint = hint;
        this.isCallableNode = IsCallableNode.create();
        this.isPrimitiveNode = IsPrimitiveNode.create();
    }

    public abstract Object execute(Object object);

    @Specialization
    protected Object doObject(JSObject object,
                    @Shared @Cached InlinedConditionProfile toStringIsFunction,
                    @Shared @Cached InlinedConditionProfile valueOfIsFunction) {
        if (hint == Hint.String) {
            return doHintString(object, this, toStringIsFunction, valueOfIsFunction);
        } else {
            assert hint == Hint.Number;
            return doHintNumber(object, this, toStringIsFunction, valueOfIsFunction);
        }
    }

    @InliningCutoff
    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"isForeignObject(object)"}, limit = "InteropLibraryLimit")
    protected final Object doForeign(Object object,
                    @Bind Node node,
                    @CachedLibrary("object") InteropLibrary interop,
                    @Shared @Cached InlinedConditionProfile toStringIsFunction,
                    @Shared @Cached InlinedConditionProfile valueOfIsFunction) {
        if (hint == Hint.String) {
            return doForeignHintString(object, node, interop, toStringIsFunction, valueOfIsFunction);
        } else {
            assert hint == Hint.Number;
            return doForeignHintNumber(object, node, interop, toStringIsFunction, valueOfIsFunction);
        }
    }

    @NeverDefault
    public static OrdinaryToPrimitiveNode createHintString() {
        return create(Hint.String);
    }

    @NeverDefault
    public static OrdinaryToPrimitiveNode createHintNumber() {
        return create(Hint.Number);
    }

    @NeverDefault
    public static OrdinaryToPrimitiveNode create(Hint hint) {
        return OrdinaryToPrimitiveNodeGen.create(hint);
    }

    private Object doHintString(JSObject object,
                    Node node, InlinedConditionProfile toStringIsFunction, InlinedConditionProfile valueOfIsFunction) {
        Object toString = getToString().getValue(object);
        if (toStringIsFunction.profile(node, isCallableNode.executeBoolean(toString))) {
            Object result = callToStringNode.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        Object valueOf = getValueOf().getValue(object);
        if (valueOfIsFunction.profile(node, isCallableNode.executeBoolean(valueOf))) {
            Object result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    private Object doHintNumber(JSObject object,
                    Node node, InlinedConditionProfile toStringIsFunction, InlinedConditionProfile valueOfIsFunction) {
        assert JSGuards.isJSObject(object);
        Object valueOf = getValueOf().getValue(object);
        if (valueOfIsFunction.profile(node, isCallableNode.executeBoolean(valueOf))) {
            Object result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, valueOf));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        Object toString = getToString().getValue(object);
        if (toStringIsFunction.profile(node, isCallableNode.executeBoolean(toString))) {
            Object result = callToStringNode.executeCall(JSArguments.createZeroArg(object, toString));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    private Object doForeignHintString(Object object,
                    Node node, InteropLibrary interop, InlinedConditionProfile toStringIsFunction, InlinedConditionProfile valueOfIsFunction) {
        Object result = tryInvokeForeignMethod(object, interop, Strings.TO_STRING_JLS);
        if (result != null) {
            return result;
        }
        JSDynamicObject proto = getForeignObjectPrototype(object);
        Object func = getToString().getValueOrUndefined(proto, object);
        if (toStringIsFunction.profile(node, isCallableNode.executeBoolean(func))) {
            result = callToStringNode.executeCall(JSArguments.createZeroArg(object, func));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        result = tryInvokeForeignMethod(object, interop, Strings.VALUE_OF_JLS);
        if (result != null) {
            return result;
        }
        func = getValueOf().getValue(proto);
        if (valueOfIsFunction.profile(node, isCallableNode.executeBoolean(func))) {
            result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, func));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    private Object doForeignHintNumber(Object object,
                    Node node, InteropLibrary interop, InlinedConditionProfile toStringIsFunction, InlinedConditionProfile valueOfIsFunction) {
        Object result = tryInvokeForeignMethod(object, interop, Strings.VALUE_OF_JLS);
        if (result != null) {
            return result;
        }
        JSDynamicObject proto = getForeignObjectPrototype(object);
        Object func = getValueOf().getValueOrUndefined(proto, object);
        if (valueOfIsFunction.profile(node, isCallableNode.executeBoolean(func))) {
            result = callValueOfNode.executeCall(JSArguments.createZeroArg(object, func));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        result = tryInvokeForeignMethod(object, interop, Strings.TO_STRING_JLS);
        if (result != null) {
            return result;
        }
        func = getToString().getValue(proto);
        if (toStringIsFunction.profile(node, isCallableNode.executeBoolean(func))) {
            result = callToStringNode.executeCall(JSArguments.createZeroArg(object, func));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(this);
    }

    @InliningCutoff
    private Object tryInvokeForeignMethod(Object object, InteropLibrary interop, String methodName) {
        if (interop.hasMembers(object) && interop.isMemberInvocable(object, methodName)) {
            // Avoid calling toString() on Java arrays; use Array.prototype.toString() instead.
            if (isJavaArray(object, interop)) {
                return null;
            }
            try {
                Object result = JSRuntime.importValue(interop.invokeMember(object, methodName));
                if (isPrimitiveNode.executeBoolean(result)) {
                    return result;
                }
            } catch (InteropException e) {
                // ignore
            }
        }
        return null;
    }

    public static boolean isJavaArray(Object object, InteropLibrary interop) {
        return JSRealm.get(interop).getEnv().isHostObject(object) && interop.hasArrayElements(object) && interop.isMemberReadable(object, "length");
    }

    private PropertyGetNode getToString() {
        if (getToStringNode == null || callToStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getToStringNode = insert(PropertyGetNode.createGetMethod(Strings.TO_STRING, getLanguage().getJSContext()));
            callToStringNode = insert(JSFunctionCallNode.createCall());
        }
        return getToStringNode;
    }

    private PropertyGetNode getValueOf() {
        if (getValueOfNode == null || callValueOfNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getValueOfNode = insert(PropertyGetNode.createGetMethod(Strings.VALUE_OF, getLanguage().getJSContext()));
            callValueOfNode = insert(JSFunctionCallNode.createCall());
        }
        return getValueOfNode;
    }

    private JSDynamicObject getForeignObjectPrototype(Object truffleObject) {
        assert JSRuntime.isForeignObject(truffleObject);
        if (foreignObjectPrototypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            foreignObjectPrototypeNode = insert(ForeignObjectPrototypeNode.create());
        }
        return foreignObjectPrototypeNode.execute(truffleObject);
    }
}
