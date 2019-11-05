/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

public abstract class CopyDataPropertiesNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    @Child protected JavaScriptNode excludedNode;
    protected final JSContext context;

    protected CopyDataPropertiesNode(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        this.context = context;
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
        this.excludedNode = excludedNode;
    }

    public static CopyDataPropertiesNode create(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        return CopyDataPropertiesNodeGen.create(context, targetNode, sourceNode, excludedNode);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullOrUndefined(value)")
    protected static DynamicObject doNullOrUndefined(DynamicObject restObj, Object value) {
        return restObj;
    }

    @Specialization(guards = {"isJSObject(value)", "excludedNode == null"})
    protected static DynamicObject doObject(DynamicObject restObj, DynamicObject value) {
        copyDataProperties(restObj, value, null);
        return restObj;
    }

    @Specialization(guards = {"isJSObject(value)", "excludedNode != null"})
    protected final DynamicObject doObjectWithExcluded(VirtualFrame frame, DynamicObject restObj, DynamicObject value) {
        Object[] excludedItems = excludedItems(frame);
        copyDataProperties(restObj, value, excludedItems);
        return restObj;
    }

    @Specialization(guards = {"!isJSType(value)", "excludedNode == null"})
    protected final Object doOther(DynamicObject restObj, Object value,
                    @Shared("toObject") @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode,
                    @Shared("isJSObject") @Cached("createBinaryProfile()") ConditionProfile isJSObjectProfile) {
        Object from = toObjectNode.executeTruffleObject(value);
        if (isJSObjectProfile.profile(JSGuards.isJSType(from))) {
            doObject(restObj, (DynamicObject) from);
        } else {
            copyDataPropertiesForeign(restObj, from, null);
        }
        return restObj;
    }

    @Specialization(guards = {"!isJSType(value)", "excludedNode != null"})
    protected final Object doOtherWithExcluded(VirtualFrame frame, DynamicObject restObj, Object value,
                    @Shared("toObject") @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode,
                    @Shared("isJSObject") @Cached("createBinaryProfile()") ConditionProfile isJSObjectProfile) {
        Object from = toObjectNode.executeTruffleObject(value);
        if (isJSObjectProfile.profile(JSGuards.isJSType(from))) {
            doObjectWithExcluded(frame, restObj, (DynamicObject) from);
        } else {
            copyDataPropertiesForeign(restObj, from, excludedItems(frame));
        }
        return restObj;
    }

    private Object[] excludedItems(VirtualFrame frame) {
        try {
            return JSArray.toArray(excludedNode.executeDynamicObject(frame));
        } catch (UnexpectedResultException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    @TruffleBoundary
    private static void copyDataProperties(DynamicObject target, DynamicObject from, Object[] excludedItems) {
        JSClass fromClass = JSObject.getJSClass(from);
        Iterable<Object> ownPropertyKeys = fromClass.ownPropertyKeys(from);
        for (Object nextKey : ownPropertyKeys) {
            if (!isExcluded(excludedItems, nextKey)) {
                PropertyDescriptor desc = fromClass.getOwnProperty(from, nextKey);
                if (desc != null && desc.getEnumerable()) {
                    Object propValue = fromClass.get(from, nextKey);
                    JSRuntime.createDataProperty(target, nextKey, propValue);
                }
            }
        }
    }

    private static boolean isExcluded(Object[] excludedKeys, Object key) {
        if (excludedKeys != null) {
            for (Object e : excludedKeys) {
                assert JSRuntime.isPropertyKey(e);
                if (e.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @TruffleBoundary
    private void copyDataPropertiesForeign(DynamicObject target, Object from, Object[] excludedItems) {
        InteropLibrary objInterop = InteropLibrary.getFactory().getUncached(from);
        try {
            Object members = objInterop.getMembers(from);
            InteropLibrary keysInterop = InteropLibrary.getFactory().getUncached(members);
            long length = JSInteropUtil.getArraySize(members, keysInterop, this);
            for (long i = 0; i < length; i++) {
                Object key = keysInterop.readArrayElement(members, i);
                assert InteropLibrary.getFactory().getUncached().isString(key);
                String stringKey = key instanceof String ? (String) key : InteropLibrary.getFactory().getUncached().asString(key);
                if (!isExcluded(excludedItems, stringKey)) {
                    Object value = objInterop.readMember(from, stringKey);
                    JSRuntime.createDataProperty(target, stringKey, JSRuntime.importValue(value));
                }
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(from, e, "CopyDataProperties", this);
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(targetNode), cloneUninitialized(sourceNode), cloneUninitialized(excludedNode));
    }
}
