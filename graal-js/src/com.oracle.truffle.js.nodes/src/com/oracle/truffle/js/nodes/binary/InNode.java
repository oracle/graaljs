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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.JSProxyHasPropertyNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.LargeInteger;
import com.oracle.truffle.js.runtime.Symbol;

public abstract class InNode extends JSBinaryNode {

    protected final JSContext context;
    @Child private JSHasPropertyNode hasPropertyNode;

    protected InNode(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        super(left, right);
        this.context = context;
    }

    public static InNode create(JSContext context, JavaScriptNode left, JavaScriptNode right) {
        return InNodeGen.create(context, left, right);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Specialization(guards = {"isJSObject(haystack)", "!isJSProxy(haystack)"})
    protected boolean doObject(Object needle, DynamicObject haystack) {
        return getHasPropertyNode().executeBoolean(haystack, needle);
    }

    @Specialization(guards = {"isJSProxy(haystack)"})
    protected boolean doProxy(Object needle, DynamicObject haystack,
                    @Cached("create(context)") JSProxyHasPropertyNode proxyHasPropertyNode) {
        return proxyHasPropertyNode.executeWithTargetAndKeyBoolean(haystack, needle);
    }

    @Specialization(guards = "isForeignObject(haystack)")
    protected boolean doForeign(Object needle, TruffleObject haystack) {
        return getHasPropertyNode().executeBoolean(haystack, needle);
    }

    @Specialization(guards = "isNullOrUndefined(haystack)")
    protected static Object doNullOrUndefined(@SuppressWarnings("unused") Object needle, Object haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    @Specialization
    protected static Object doSymbol(@SuppressWarnings("unused") Object needle, Symbol haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    @Specialization
    protected static Object doString(@SuppressWarnings("unused") Object needle, String haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    @Specialization
    protected static Object doLargeInteger(@SuppressWarnings("unused") Object needle, LargeInteger haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    @Specialization
    protected static Object doBigInt(@SuppressWarnings("unused") Object needle, BigInt haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    @Specialization(guards = "!isTruffleObject(haystack)")
    protected static Object doNotTruffleObject(@SuppressWarnings("unused") Object needle, Object haystack) {
        throw Errors.createTypeErrorNotAnObject(haystack);
    }

    private JSHasPropertyNode getHasPropertyNode() {
        if (hasPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            hasPropertyNode = insert(JSHasPropertyNode.create());
        }
        return hasPropertyNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return InNodeGen.create(context, cloneUninitialized(getLeft()), cloneUninitialized(getRight()));
    }
}
