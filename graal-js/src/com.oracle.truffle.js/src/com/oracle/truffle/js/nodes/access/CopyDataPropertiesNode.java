/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

@ImportStatic({JSConfig.class})
public abstract class CopyDataPropertiesNode extends JavaScriptBaseNode {
    protected final JSContext context;

    protected CopyDataPropertiesNode(JSContext context) {
        this.context = context;
    }

    public static CopyDataPropertiesNode create(JSContext context) {
        return CopyDataPropertiesNodeGen.create(context);
    }

    public final Object execute(Object target, Object source) {
        return executeImpl(target, source, null, false);
    }

    public final Object execute(Object target, Object source, Object[] excludedItems) {
        return executeImpl(target, source, excludedItems, true);
    }

    protected abstract Object executeImpl(Object target, Object source, Object[] excludedItems, boolean withExcluded);

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullOrUndefined(value)")
    protected static DynamicObject doNullOrUndefined(DynamicObject target, Object value, Object[] excludedItems, boolean withExcluded) {
        return target;
    }

    @Specialization(guards = {"isJSObject(source)"})
    protected static DynamicObject copyDataProperties(DynamicObject target, DynamicObject source, Object[] excludedItems, boolean withExcluded,
                    @Cached("create(context)") ReadElementNode getNode,
                    @Cached("create(false)") JSGetOwnPropertyNode getOwnProperty,
                    @Cached ListSizeNode listSize,
                    @Cached ListGetNode listGet,
                    @Cached JSClassProfile classProfile) {
        List<Object> ownPropertyKeys = JSObject.ownPropertyKeys(source, classProfile);
        int size = listSize.execute(ownPropertyKeys);
        for (int i = 0; i < size; i++) {
            Object nextKey = listGet.execute(ownPropertyKeys, i);
            assert JSRuntime.isPropertyKey(nextKey);
            if (!isExcluded(withExcluded, excludedItems, nextKey)) {
                PropertyDescriptor desc = getOwnProperty.execute(source, nextKey);
                if (desc != null && desc.getEnumerable()) {
                    Object propValue = getNode.executeWithTargetAndIndex(source, nextKey);
                    JSRuntime.createDataPropertyOrThrow(target, nextKey, propValue);
                }
            }
        }
        return target;
    }

    private static boolean isExcluded(boolean withExcluded, Object[] excludedKeys, Object key) {
        CompilerAsserts.partialEvaluationConstant(withExcluded);
        if (withExcluded) {
            for (Object e : excludedKeys) {
                assert JSRuntime.isPropertyKey(e);
                if (JSRuntime.propertyKeyEquals(e, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Specialization(guards = {"!isJSDynamicObject(from)"}, limit = "InteropLibraryLimit")
    protected final DynamicObject copyDataPropertiesForeign(DynamicObject target, Object from, Object[] excludedItems, boolean withExcluded,
                    @CachedLibrary("from") InteropLibrary objInterop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary iteratorInterop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary arrayInterop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary stringInterop,
                    @Cached ImportValueNode importValue,
                    @Cached JSToStringNode toString) {
        if (objInterop.isNull(from)) {
            return target;
        }
        try {
            if (context.getContextOptions().hasForeignHashProperties() && objInterop.hasHashEntries(from)) {
                Object entriesIterator = objInterop.getHashEntriesIterator(from);
                while (true) {
                    Object entry;
                    try {
                        entry = iteratorInterop.getIteratorNextElement(entriesIterator);
                    } catch (StopIterationException e) {
                        break;
                    }
                    Object key = arrayInterop.readArrayElement(entry, 0);
                    Object value = arrayInterop.readArrayElement(entry, 1);
                    String stringKey = toString.executeString(importValue.executeWithTarget(key));
                    if (!isExcluded(withExcluded, excludedItems, stringKey)) {
                        JSRuntime.createDataPropertyOrThrow(target, stringKey, importValue.executeWithTarget(value));
                    }
                }
            } else if (objInterop.hasMembers(from)) {
                Object members = objInterop.getMembers(from);
                long length = JSInteropUtil.getArraySize(members, arrayInterop, this);
                for (long i = 0; i < length; i++) {
                    Object key = arrayInterop.readArrayElement(members, i);
                    assert InteropLibrary.getUncached().isString(key);
                    String stringKey = stringInterop.asString(key);
                    if (!isExcluded(withExcluded, excludedItems, stringKey)) {
                        Object value = objInterop.readMember(from, stringKey);
                        JSRuntime.createDataPropertyOrThrow(target, stringKey, importValue.executeWithTarget(value));
                    }
                }
            }
        } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
            throw Errors.createTypeErrorInteropException(from, e, "CopyDataProperties", this);
        }
        return target;
    }
}
