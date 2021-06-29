/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.EnumerableOwnPropertyNamesNode;
import com.oracle.truffle.js.nodes.access.ReadElementNode;
import com.oracle.truffle.js.nodes.array.JSGetLengthNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.UnmodifiableArrayList;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class represents the abstract operation BuildImmutableProperty.
 */
public abstract class JSONBuildImmutablePropertyNode extends JavaScriptBaseNode {

    private final JSContext context;
    private final ConditionProfile isCallableProfile = ConditionProfile.create();

    @Child private IsCallableNode isCallableNode;
    @Child private JSFunctionCallNode functionCallNode;
    @Child private JSIsArrayNode isArrayNode;
    @Child private JSGetLengthNode getLengthNode;
    @Child private ReadElementNode readElementNode;
    @Child private EnumerableOwnPropertyNamesNode enumerableOwnPropertyNamesNode;

    protected JSONBuildImmutablePropertyNode(JSContext context) {
        this.context = context;
    }

    public abstract Object execute(Object value, String name, Object reviver);

    public static JSONBuildImmutablePropertyNode create(JSContext context) {
        return JSONBuildImmutablePropertyNodeGen.create(context);
    }

    @Specialization(guards = {"isJSObject(value)", "isArray(value)"})
    public Object doJSArray(Object value, String name, Object reviver) {
        int len = (int) lengthOfArrayLike(value);
        Object[] items = new Object[len];
        for (int i = 0; i < len; i++) {
            String childName = Boundaries.stringValueOf(i);
            Object childValue = get(value, i);
            Object newElement = execute(childValue, childName, reviver);
            assert !JSRuntime.isObject(newElement);
            items[i] = newElement;
        }
        Object immutable = Tuple.create(items);
        return mapIfCallable(immutable, reviver, name);
    }

    @Specialization(guards = {"isJSObject(value)", "!isArray(value)"})
    public Object doJSObject(DynamicObject value, String name, Object reviver) {
        UnmodifiableArrayList<? extends Object> props = enumerableOwnPropertyNames(value);
        Map<String, Object> fields = new TreeMap<>();
        for (Object prop : props) {
            String childName = JSRuntime.toString(get(prop, 0));
            Object childValue = get(prop, 1);
            Object newElement = execute(childValue, childName, reviver);
            assert !JSRuntime.isObject(newElement);
            if (newElement != Undefined.instance) {
                fields.put(childName, newElement);
            }
        }
        Object immutable = Record.create(fields);
        return mapIfCallable(immutable, reviver, name);
    }

    @Specialization(guards = "!isJSObject(value)")
    public Object doNonJSObject(Object value, String name, Object reviver) {
        return mapIfCallable(value, reviver, name);
    }

    protected Object mapIfCallable(Object immutable, Object reviver, String name) {
        if (isCallableProfile.profile(isCallable(reviver))) {
            immutable = call(JSArguments.create(Undefined.instance, reviver, name, immutable));
            if (JSRuntime.isObject(immutable)) {
                throw Errors.createTypeError("objects are not allowed here");
            }
        }
        return immutable;
    }

    protected boolean isArray(Object obj) {
        if (isArrayNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isArrayNode = insert(JSIsArrayNode.createIsArray());
        }
        return isArrayNode.execute(obj);
    }

    protected long lengthOfArrayLike(Object obj) {
        if (getLengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getLengthNode = insert(JSGetLengthNode.create(context));
        }
        return getLengthNode.executeLong(obj);
    }

    protected Object get(Object obj, int index) {
        if (readElementNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readElementNode = insert(ReadElementNode.create(context));
        }
        return readElementNode.executeWithTargetAndIndex(obj, index);
    }

    protected UnmodifiableArrayList<? extends Object> enumerableOwnPropertyNames(DynamicObject obj) {
        if (enumerableOwnPropertyNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            enumerableOwnPropertyNamesNode = insert(EnumerableOwnPropertyNamesNode.createKeysValues(context));
        }
        return enumerableOwnPropertyNamesNode.execute(obj);
    }

    protected boolean isCallable(Object obj) {
        if (isCallableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isCallableNode = insert(IsCallableNode.create());
        }
        return isCallableNode.executeBoolean(obj);
    }

    protected Object call(Object[] arguments) {
        if (functionCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            functionCallNode = insert(JSFunctionCallNode.createCall());
        }
        return functionCallNode.executeCall(arguments);
    }
}
