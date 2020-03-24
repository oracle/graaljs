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

import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.ReadNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Gets the value of a private field with a private name in a JS object. Throws a TypeError if the
 * object does not have a field with this private name.
 */
public abstract class PrivateFieldGetNode extends JSTargetableNode implements ReadNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode keyNode;
    protected final JSContext context;

    public static PrivateFieldGetNode create(JavaScriptNode targetNode, JavaScriptNode keyNode, JSContext context) {
        return PrivateFieldGetNodeGen.create(targetNode, keyNode, context);
    }

    protected PrivateFieldGetNode(JavaScriptNode targetNode, JavaScriptNode keyNode, JSContext context) {
        this.targetNode = targetNode;
        this.keyNode = keyNode;
        this.context = context;
    }

    @Specialization(guards = {"isJSObject(target)", "key == cachedKey"}, limit = "1")
    Object doFieldCachedKey(DynamicObject target, @SuppressWarnings("unused") HiddenKey key,
                    @Cached("key") @SuppressWarnings("unused") HiddenKey cachedKey,
                    @Cached("create(key)") HasHiddenKeyCacheNode hasNode,
                    @Cached("createGetHidden(key, context)") PropertyGetNode getNode,
                    @Cached @Shared("errorBranch") BranchProfile errorBranch) {
        if (hasNode.executeHasHiddenKey(target)) {
            return getNode.getValue(target);
        } else {
            errorBranch.enter();
            return missing(target, key);
        }
    }

    @TruffleBoundary
    @Specialization(guards = {"isJSObject(target)"}, replaces = "doFieldCachedKey")
    Object doFieldUncachedKey(DynamicObject target, HiddenKey key,
                    @Cached @Shared("errorBranch") BranchProfile errorBranch) {
        if (target.containsKey(key)) {
            return target.get(key, Undefined.instance);
        } else {
            errorBranch.enter();
            return missing(target, key);
        }
    }

    @Specialization(guards = {"isJSObject(target)", "isJSFunction(method)"})
    Object doMethod(@SuppressWarnings("unused") DynamicObject target, DynamicObject method) {
        return method;
    }

    @Specialization(guards = {"isJSObject(target)"})
    Object doAccessor(DynamicObject target, Accessor accessor,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached @Shared("errorBranch") BranchProfile errorBranch) {
        DynamicObject getter = accessor.getGetter();
        if (getter == Undefined.instance) {
            errorBranch.enter();
            throw Errors.createTypeErrorCannotGetAccessorProperty(keyAsString(), target, this);
        }
        return callNode.executeCall(JSArguments.createZeroArg(target, getter));
    }

    @TruffleBoundary
    @Fallback
    Object missing(@SuppressWarnings("unused") Object target, @SuppressWarnings("unused") Object key) {
        throw Errors.createTypeErrorCannotGetPrivateMember(keyAsString(), this);
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
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(keyNode, materializedTags), context);
    }

    @Override
    public String expressionToString() {
        if (targetNode != null && keyNode != null) {
            return Objects.toString(targetNode.expressionToString(), INTERMEDIATE_VALUE) + "." + Objects.toString(keyAsString(), INTERMEDIATE_VALUE);
        }
        return null;
    }
}
