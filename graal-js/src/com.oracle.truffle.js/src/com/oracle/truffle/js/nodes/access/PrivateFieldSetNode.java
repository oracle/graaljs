/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

public abstract class PrivateFieldSetNode extends JSTargetableNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode keyNode;
    @Child @Executed protected JavaScriptNode valueNode;
    protected final JSContext context;

    public static PrivateFieldSetNode create(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context) {
        return PrivateFieldSetNodeGen.create(targetNode, indexNode, valueNode, context);
    }

    protected PrivateFieldSetNode(JavaScriptNode targetNode, JavaScriptNode indexNode, JavaScriptNode valueNode, JSContext context) {
        this.targetNode = targetNode;
        this.keyNode = indexNode;
        this.valueNode = valueNode;
        this.context = context;
    }

    public abstract Object execute(Object target, Object key, Object value);

    @Specialization(guards = {"key == cachedKey", "isJSObject(target)"}, limit = "1")
    Object doCachedKey(DynamicObject target, HiddenKey key, Object value,
                    @Cached("key") @SuppressWarnings("unused") HiddenKey cachedKey,
                    @Cached("create(key)") HasHiddenKeyCacheNode hasNode,
                    @Cached("createSetHidden(key, context)") PropertySetNode setNode) {
        if (hasNode.executeHasHiddenKey(target)) {
            setNode.setValue(target, value);
        } else {
            missing(key);
        }
        return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"isJSObject(target)"}, replaces = "doCachedKey")
    Object doUncachedKey(DynamicObject target, HiddenKey key, Object value) {
        if (!target.set(key, value)) {
            missing(key);
        }
        return value;
    }

    @TruffleBoundary
    private Object missing(HiddenKey key) {
        throw Errors.createTypeErrorCannotSetPrivateMember(key.getName(), this);
    }

    @TruffleBoundary
    @Fallback
    Object doFallback(@SuppressWarnings("unused") Object target, @SuppressWarnings("unused") Object key, @SuppressWarnings("unused") Object value) {
        throw Errors.createTypeErrorCannotSetProperty(keyAsString(), target, this);
    }

    @TruffleBoundary
    private String keyAsString() {
        return keyNode.expressionToString();
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(keyNode), cloneUninitialized(valueNode), context);
    }
}
