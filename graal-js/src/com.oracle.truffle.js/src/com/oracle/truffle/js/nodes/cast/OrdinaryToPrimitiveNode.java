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
package com.oracle.truffle.js.nodes.cast;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
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
@GenerateUncached
@ImportStatic({JSConfig.class, Hint.class, Strings.class})
public abstract class OrdinaryToPrimitiveNode extends JavaScriptBaseNode {

    protected OrdinaryToPrimitiveNode() {
    }

    public abstract Object execute(Object object, Hint hint);

    @Specialization
    protected static Object doObject(JSObject object, Hint hint,
                    @Bind Node node,
                    @Shared @Cached(value = "createGetMethod(TO_STRING, getJSContext())", uncached = "getNullNode()") PropertyGetNode getToStringNode,
                    @Shared @Cached(value = "createGetMethod(VALUE_OF, getJSContext())", uncached = "getNullNode()") PropertyGetNode getValueOfNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Shared @Cached IsPrimitiveNode isPrimitiveNode,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callToStringNode,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callValueOfNode) {
        TruffleString propertyKey1;
        TruffleString propertyKey2;
        PropertyGetNode getMethod1;
        PropertyGetNode getMethod2;
        JSFunctionCallNode methodCall1;
        JSFunctionCallNode methodCall2;
        if (hint == Hint.String) {
            // toString(), valueOf()
            propertyKey1 = Strings.TO_STRING;
            propertyKey2 = Strings.VALUE_OF;
            getMethod1 = getToStringNode;
            getMethod2 = getValueOfNode;
            methodCall1 = callToStringNode;
            methodCall2 = callValueOfNode;
        } else {
            // valueOf(), toString()
            assert hint == Hint.Number;
            propertyKey1 = Strings.VALUE_OF;
            propertyKey2 = Strings.TO_STRING;
            getMethod1 = getValueOfNode;
            getMethod2 = getToStringNode;
            methodCall1 = callValueOfNode;
            methodCall2 = callToStringNode;
        }
        Object method = JSToPrimitiveNode.getMethod(object, propertyKey1, getMethod1);
        if (isCallableNode.executeBoolean(method)) {
            Object result = methodCall1.executeCall(JSArguments.createZeroArg(object, method));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }
        method = JSToPrimitiveNode.getMethod(object, propertyKey2, getMethod2);
        if (isCallableNode.executeBoolean(method)) {
            Object result = methodCall2.executeCall(JSArguments.createZeroArg(object, method));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(node);
    }

    @InliningCutoff
    @Specialization(guards = {"isForeignObject(object)"}, limit = "InteropLibraryLimit")
    protected static Object doForeign(Object object, Hint hint,
                    @Bind Node node,
                    @Shared @Cached(value = "createGetMethod(TO_STRING, getJSContext())", uncached = "getNullNode()") PropertyGetNode getToStringNode,
                    @Shared @Cached(value = "createGetMethod(VALUE_OF, getJSContext())", uncached = "getNullNode()") PropertyGetNode getValueOfNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Shared @Cached IsPrimitiveNode isPrimitiveNode,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callToStringNode,
                    @Shared @Cached(value = "createCall()", uncached = "getUncachedCall()") JSFunctionCallNode callValueOfNode,
                    @Shared @Cached ForeignObjectPrototypeNode foreignObjectPrototypeNode,
                    @CachedLibrary("object") InteropLibrary interop) {
        TruffleString propertyKey1;
        TruffleString propertyKey2;
        String stringKey1;
        String stringKey2;
        PropertyGetNode getMethod1;
        PropertyGetNode getMethod2;
        JSFunctionCallNode methodCall1;
        JSFunctionCallNode methodCall2;
        if (hint == Hint.String) {
            // toString(), valueOf()
            propertyKey1 = Strings.TO_STRING;
            propertyKey2 = Strings.VALUE_OF;
            stringKey1 = Strings.TO_STRING_JLS;
            stringKey2 = Strings.VALUE_OF_JLS;
            getMethod1 = getToStringNode;
            getMethod2 = getValueOfNode;
            methodCall1 = callToStringNode;
            methodCall2 = callValueOfNode;
        } else {
            // valueOf(), toString()
            assert hint == Hint.Number;
            propertyKey1 = Strings.VALUE_OF;
            propertyKey2 = Strings.TO_STRING;
            stringKey1 = Strings.VALUE_OF_JLS;
            stringKey2 = Strings.TO_STRING_JLS;
            getMethod1 = getValueOfNode;
            getMethod2 = getToStringNode;
            methodCall1 = callValueOfNode;
            methodCall2 = callToStringNode;
        }

        Object result = tryInvokeForeignMethod(object, stringKey1, interop, isPrimitiveNode);
        if (result != null) {
            return result;
        }
        JSDynamicObject proto = foreignObjectPrototypeNode.execute(object);
        Object method = JSToPrimitiveNode.getPrototypeMethod(proto, object, propertyKey1, getMethod1);
        if (isCallableNode.executeBoolean(method)) {
            result = methodCall1.executeCall(JSArguments.createZeroArg(object, method));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }

        result = tryInvokeForeignMethod(object, stringKey2, interop, isPrimitiveNode);
        if (result != null) {
            return result;
        }
        method = JSToPrimitiveNode.getPrototypeMethod(proto, object, propertyKey2, getMethod2);
        if (isCallableNode.executeBoolean(method)) {
            result = methodCall2.executeCall(JSArguments.createZeroArg(object, method));
            if (isPrimitiveNode.executeBoolean(result)) {
                return result;
            }
        }
        throw Errors.createTypeErrorCannotConvertToPrimitiveValue(node);
    }

    @NeverDefault
    public static OrdinaryToPrimitiveNode create() {
        return OrdinaryToPrimitiveNodeGen.create();
    }

    @InliningCutoff
    private static Object tryInvokeForeignMethod(Object object, String methodName, InteropLibrary interop, IsPrimitiveNode isPrimitiveNode) {
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
}
