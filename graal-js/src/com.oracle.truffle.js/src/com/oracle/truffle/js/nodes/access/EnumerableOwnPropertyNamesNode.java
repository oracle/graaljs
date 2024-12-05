/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.util.JSClassProfile;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

/**
 * EnumerableOwnPropertyNames (O, kind).
 */
@ImportStatic({JSConfig.class})
public abstract class EnumerableOwnPropertyNamesNode extends JavaScriptBaseNode {

    private final boolean keys;
    private final boolean values;
    private final JSContext context;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;
    private final ConditionProfile hasFastShapesProfile = ConditionProfile.create();

    protected EnumerableOwnPropertyNamesNode(JSContext context, boolean keys, boolean values) {
        this.context = context;
        this.keys = keys;
        this.values = values;
    }

    @NeverDefault
    public static EnumerableOwnPropertyNamesNode createKeys(JSContext context) {
        return EnumerableOwnPropertyNamesNodeGen.create(context, true, false);
    }

    @NeverDefault
    public static EnumerableOwnPropertyNamesNode createValues(JSContext context) {
        return EnumerableOwnPropertyNamesNodeGen.create(context, false, true);
    }

    @NeverDefault
    public static EnumerableOwnPropertyNamesNode createKeysValues(JSContext context) {
        return EnumerableOwnPropertyNamesNodeGen.create(context, true, true);
    }

    public abstract UnmodifiableArrayList<? extends Object> execute(Object obj);

    @Specialization
    protected UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNames(JSDynamicObject thisObj,
                    @Cached JSClassProfile jsclassProfile,
                    @Cached ListSizeNode listSize,
                    @Cached ListGetNode listGet,
                    @Cached HasOnlyShapePropertiesNode hasOnlyShapeProperties,
                    @Cached @Exclusive InlinedBranchProfile growProfile) {
        JSClass jsclass = jsclassProfile.getJSClass(thisObj);
        if (hasFastShapesProfile.profile(keys && !values && JSConfig.FastOwnKeys && hasOnlyShapeProperties.execute(thisObj, jsclass))) {
            return JSShape.getEnumerablePropertyNames(thisObj.getShape());
        } else {
            boolean isProxy = JSProxy.isJSProxy(thisObj);
            List<Object> ownKeys = jsclass.ownPropertyKeys(thisObj);
            int ownKeysSize = listSize.execute(ownKeys);
            SimpleArrayList<Object> properties = new SimpleArrayList<>();
            for (int i = 0; i < ownKeysSize; i++) {
                Object key = listGet.execute(ownKeys, i);
                if (key instanceof TruffleString name) {
                    PropertyDescriptor desc = getOwnProperty(thisObj, name);
                    if (desc != null && desc.getEnumerable()) {
                        Object element;
                        if (keys && !values) {
                            element = name;
                        } else {
                            Object value = (desc.isAccessorDescriptor() || isProxy) ? jsclass.get(thisObj, name) : desc.getValue();
                            if (!keys && values) {
                                element = value;
                            } else {
                                assert keys && values;
                                element = createKeyValuePair(name, value);
                            }
                        }
                        properties.add(element, this, growProfile);
                    }
                }
            }
            return new UnmodifiableArrayList<>(properties.toArray());
        }
    }

    private Object createKeyValuePair(Object key, Object value) {
        return JSArray.createConstant(context, getRealm(), new Object[]{key, value});
    }

    protected PropertyDescriptor getOwnProperty(JSDynamicObject thisObj, Object key) {
        if (getOwnPropertyNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getOwnPropertyNode = insert(JSGetOwnPropertyNode.create(values, true, false, false, false));
        }
        return getOwnPropertyNode.execute(thisObj, key);
    }

    @SuppressWarnings("truffle-static-method")
    @InliningCutoff
    @Specialization(guards = "isForeignObject(obj)", limit = "InteropLibraryLimit")
    protected UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNamesForeign(Object obj,
                    @Bind Node node,
                    @CachedLibrary("obj") InteropLibrary interop,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary members,
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary asString,
                    @Cached ImportValueNode importValue,
                    @Cached @Exclusive InlinedBranchProfile errorBranch,
                    @Cached TruffleString.SwitchEncodingNode switchEncodingNode,
                    @Cached TruffleString.ToJavaStringNode toJavaStringNode) {
        try {
            long arraySize = 0;
            if (interop.hasArrayElements(obj)) {
                arraySize = interop.getArraySize(obj);
            }
            Object keysObj = null;
            long memberCount = 0;
            if (interop.hasMembers(obj)) {
                keysObj = interop.getMembers(obj);
                memberCount = members.getArraySize(keysObj);
            }
            long size = arraySize + memberCount;
            if (arraySize < 0 || memberCount < 0 || size < 0 || size >= Integer.MAX_VALUE) {
                errorBranch.enter(node);
                throw Errors.createRangeErrorInvalidArrayLength(this);
            }
            if (size > 0) {
                SimpleArrayList<Object> list = new SimpleArrayList<>((int) size);
                for (long i = 0; i < arraySize; i++) {
                    TruffleString key = Strings.fromLong(i);
                    Object element;
                    if (values) {
                        Object value = importValue.executeWithTarget(interop.readArrayElement(obj, i));
                        if (keys) {
                            element = createKeyValuePair(key, value);
                        } else {
                            element = value;
                        }
                    } else {
                        element = key;
                    }
                    list.addUnchecked(element);
                }
                for (int i = 0; i < memberCount; i++) {
                    Object objectKey = members.readArrayElement(keysObj, i);
                    assert InteropLibrary.getUncached().isString(objectKey);
                    TruffleString key = Strings.interopAsTruffleString(objectKey, asString, switchEncodingNode);
                    Object element;
                    if (values) {
                        String javaStringKey = Strings.toJavaString(toJavaStringNode, key);
                        Object value = importValue.executeWithTarget(interop.readMember(obj, javaStringKey));
                        if (keys) {
                            element = createKeyValuePair(key, value);
                        } else {
                            element = value;
                        }
                    } else {
                        element = key;
                    }
                    list.addUnchecked(element);
                }
                return new UnmodifiableArrayList<>(list.toArray());
            }
            // fall through
        } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
            // fall through
        }
        return new UnmodifiableArrayList<>(ScriptArray.EMPTY_OBJECT_ARRAY);
    }

}
