/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSString;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.ForInIterator;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

/**
 * Returns an Iterator object iterating over the enumerable properties of an object.
 */
public abstract class EnumerateNode extends JavaScriptNode {
    /** Enumerate values instead of keys (used by for-each-in loop). */
    private final boolean values;
    /** If true, throw a TypeError for foreign objects that do not have elements or members. */
    private final boolean requireIterable;
    protected final JSContext context;
    @Child @Executed protected JavaScriptNode targetNode;
    @Child private PropertySetNode setEnumerateIteratorNode;
    @Child private PropertySetNode setForInIteratorNode;

    protected EnumerateNode(JSContext context, boolean values, boolean requireIterable, JavaScriptNode targetNode) {
        this.context = context;
        this.values = values;
        this.requireIterable = requireIterable;
        this.targetNode = targetNode;
    }

    public static EnumerateNode create(JSContext context, JavaScriptNode target, boolean values) {
        return EnumerateNodeGen.create(context, values, false, target);
    }

    public static EnumerateNode create(JSContext context, boolean values, boolean requireIterable) {
        return EnumerateNodeGen.create(context, values, requireIterable, null);
    }

    EnumerateNode copyRecursive() {
        return create(context, values, requireIterable);
    }

    @Override
    public abstract DynamicObject execute(VirtualFrame frame);

    public abstract DynamicObject execute(Object iteratedObject);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return EnumerateNodeGen.create(context, values, requireIterable, cloneUninitialized(targetNode, materializedTags));
    }

    @Specialization(guards = {"isJSDynamicObject(iteratedObject)", "!isJSAdapter(iteratedObject)"})
    protected DynamicObject doEnumerateObject(DynamicObject iteratedObject,
                    @Cached("createBinaryProfile()") ConditionProfile isObject) {
        if (isObject.profile(JSRuntime.isObject(iteratedObject))) {
            return newForInIterator(iteratedObject);
        } else {
            // null or undefined
            Iterator<?> iterator = Collections.emptyIterator();
            return newEnumerateIterator(iterator);
        }
    }

    @Specialization(guards = "isJSAdapter(iteratedObject)")
    protected DynamicObject doEnumerateJSAdapter(DynamicObject iteratedObject,
                    @Cached("createValues()") EnumerateNode enumerateCallbackResultNode) {
        DynamicObject adaptee = JSAdapter.getAdaptee(iteratedObject);
        assert JSRuntime.isObject(adaptee);

        Object getIds = JSObject.get(adaptee, values ? JSAdapter.GET_VALUES : JSAdapter.GET_IDS);
        if (JSFunction.isJSFunction(getIds)) {
            Object returnValue = JSFunction.call((DynamicObject) getIds, adaptee, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            if (JSRuntime.isObject(returnValue)) {
                return enumerateCallbackResultNode.execute(returnValue);
            }
        }
        return newEnumerateIterator(Collections.emptyIterator());
    }

    EnumerateNode createValues() {
        return create(context, true, false);
    }

    @Specialization(guards = {"isForeignObject(iteratedObject)"}, limit = "3")
    protected DynamicObject doEnumerateTruffleObject(Object iteratedObject,
                    @CachedLibrary("iteratedObject") InteropLibrary interop,
                    @CachedLibrary(limit = "3") InteropLibrary keysInterop,
                    @Cached("createBinaryProfile()") ConditionProfile isHostObject,
                    @Cached BranchProfile notIterable) {
        TruffleLanguage.Env env = context.getRealm().getEnv();
        if (isHostObject.profile(env.isHostObject(iteratedObject))) {
            Object hostObject = env.asHostObject(iteratedObject);
            Iterator<?> iterator = getHostObjectIterator(hostObject, values, env);
            if (iterator != null) {
                return newEnumerateIterator(iterator);
            }
        }

        try {
            if (!interop.isNull(iteratedObject)) {
                if (interop.hasArrayElements(iteratedObject)) {
                    return enumerateForeignArrayLike(iteratedObject, interop);
                } else if (interop.hasMembers(iteratedObject)) {
                    Object keysObj = interop.getMembers(iteratedObject);
                    assert InteropLibrary.getFactory().getUncached().hasArrayElements(keysObj);
                    long longSize = keysInterop.getArraySize(keysObj);
                    return enumerateForeignNonArray(iteratedObject, keysObj, longSize, interop, keysInterop);
                } else if (interop.isString(iteratedObject)) {
                    String string = interop.asString(iteratedObject);
                    return enumerateString(string);
                }
            }
        } catch (UnsupportedMessageException ex) {
            // swallow and default
        }
        notIterable.enter();
        if (requireIterable) {
            throw Errors.createTypeErrorNotIterable(iteratedObject, this);
        }
        // in case of any errors, return an empty iterator
        return newEnumerateIterator(Collections.emptyIterator());
    }

    @TruffleBoundary
    private static Iterator<?> getHostObjectIterator(Object hostObject, boolean values, TruffleLanguage.Env env) {
        if (hostObject != null) {
            Iterator<?> iterator;
            if (hostObject instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) hostObject;
                iterator = values ? map.values().iterator() : map.keySet().iterator();
            } else if (hostObject.getClass().isArray()) {
                if (values) {
                    iterator = new ArrayIterator(hostObject);
                } else {
                    return IteratorUtil.rangeIterator(Array.getLength(hostObject));
                }
            } else if (!values && hostObject instanceof List<?>) {
                return IteratorUtil.rangeIterator(((List<?>) hostObject).size());
            } else if (values && hostObject instanceof Iterable<?>) {
                iterator = ((Iterable<?>) hostObject).iterator();
            } else {
                return null;
            }
            // the value is imported in the iterator's next method node
            return IteratorUtil.convertIterator(iterator, env::asGuestValue);
        }
        return null;
    }

    private DynamicObject enumerateForeignArrayLike(Object iteratedObject, InteropLibrary interop) {
        return newEnumerateIterator(new ForeignArrayIterator(iteratedObject, values, interop));
    }

    private DynamicObject enumerateForeignNonArray(Object iteratedObject, Object keysObject, long keysSize, InteropLibrary objInterop, InteropLibrary keysInterop) {
        return newEnumerateIterator(new ForeignMembersIterator(iteratedObject, keysObject, keysSize, values, objInterop, keysInterop));
    }

    private DynamicObject enumerateString(String string) {
        return newForInIterator(JSString.create(context, string));
    }

    private DynamicObject newEnumerateIterator(Iterator<?> iterator) {
        if (setEnumerateIteratorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setEnumerateIteratorNode = insert(PropertySetNode.createSetHidden(JSRuntime.ENUMERATE_ITERATOR_ID, context));
        }
        DynamicObject obj = JSUserObject.create(context, context.getEnumerateIteratorFactory());
        setEnumerateIteratorNode.setValue(obj, iterator);
        return obj;
    }

    private DynamicObject newForInIterator(DynamicObject obj) {
        if (setForInIteratorNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setForInIteratorNode = insert(PropertySetNode.createSetHidden(JSRuntime.FOR_IN_ITERATOR_ID, context));
        }
        DynamicObject iteratorObj = JSUserObject.create(context, context.getForInIteratorFactory());
        setForInIteratorNode.setValue(iteratorObj, new ForInIterator(obj, values));
        return iteratorObj;
    }

    @Specialization(guards = {"!isJSObject(iteratedObject)"})
    protected DynamicObject doNonObject(Object iteratedObject,
                    @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode,
                    @Cached("copyRecursive()") EnumerateNode enumerateNode) {
        return enumerateNode.execute(toObjectNode.execute(iteratedObject));
    }

    private static final class ArrayIterator implements Iterator<Object> {
        private final Object array;
        private final int length;
        private int index;

        ArrayIterator(Object array) {
            this.array = array;
            this.length = Array.getLength(array);
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public Object next() {
            if (hasNext()) {
                return Array.get(array, index++);
            }
            throw new NoSuchElementException();
        }
    }

    private static final class ForeignArrayIterator implements Iterator<Object> {
        private final Object iteratedObject;
        private final boolean values;
        private final InteropLibrary interop;
        private long cursor;

        private ForeignArrayIterator(Object iteratedObject, boolean values, InteropLibrary interop) {
            this.iteratedObject = iteratedObject;
            this.values = values;
            this.interop = interop;
        }

        @Override
        public boolean hasNext() {
            try {
                long currentSize = interop.getArraySize(iteratedObject);
                return cursor < currentSize;
            } catch (UnsupportedMessageException ex) {
                return false;
            }
        }

        @Override
        public Object next() {
            if (hasNext()) {
                long index = cursor++;
                if (values) {
                    try {
                        // the value is imported in the iterator's next method node
                        return interop.readArrayElement(iteratedObject, index);
                    } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
                        // swallow and default
                    }
                } else {
                    return index;
                }
            }
            throw new NoSuchElementException();
        }
    }

    private static final class ForeignMembersIterator implements Iterator<Object> {
        private final Object iteratedObject;
        private final Object keysObject;
        private final long keysSize;
        private final boolean values;
        private final InteropLibrary objInterop;
        private final InteropLibrary keysInterop;
        private long cursor;

        private ForeignMembersIterator(Object iteratedObject, Object keysObject, long keysSize, boolean values, InteropLibrary objInterop, InteropLibrary keysInterop) {
            this.iteratedObject = iteratedObject;
            this.keysObject = keysObject;
            this.keysSize = keysSize;
            this.values = values;
            this.objInterop = objInterop;
            this.keysInterop = keysInterop;
        }

        @Override
        public boolean hasNext() {
            return cursor < keysSize;
        }

        @Override
        public Object next() {
            if (hasNext()) {
                long index = cursor++;
                try {
                    Object key = keysInterop.readArrayElement(keysObject, index);
                    if (values) {
                        try {
                            assert InteropLibrary.getFactory().getUncached().isString(key);
                            String stringKey = key instanceof String ? (String) key : InteropLibrary.getFactory().getUncached().asString(key);
                            // the value is imported in the iterator's next method node
                            return objInterop.readMember(iteratedObject, stringKey);
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            // swallow and default
                        }
                    } else {
                        return key;
                    }
                } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
                    // swallow and default
                }
            }
            throw new NoSuchElementException();
        }
    }
}
