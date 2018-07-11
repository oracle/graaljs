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
package com.oracle.truffle.js.nodes.access;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToLengthNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSAdapter;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;
import com.oracle.truffle.js.runtime.util.EnumerateIterator;
import com.oracle.truffle.js.runtime.util.IteratorUtil;

/**
 * ES6 [[Enumerate]]().
 */
@ImportStatic(JSInteropUtil.class)
public abstract class EnumerateNode extends JavaScriptNode {
    /** Enumerate values instead of keys (used by for-each-in loop). */
    private final boolean values;
    protected final JSContext context;
    @Child @Executed protected JavaScriptNode targetNode;

    protected EnumerateNode(JSContext context, boolean values, JavaScriptNode targetNode) {
        this.context = context;
        this.values = values;
        this.targetNode = targetNode;
    }

    public static EnumerateNode create(JSContext context, JavaScriptNode target, boolean values) {
        return EnumerateNodeGen.create(context, values, target);
    }

    public static EnumerateNode create(JSContext context) {
        return create(context, null, false);
    }

    public EnumerateNode copyRecursive() {
        return create(context, null, values);
    }

    @Override
    public abstract DynamicObject execute(VirtualFrame frame);

    public abstract DynamicObject execute(Object iteratedObject);

    @Override
    protected JavaScriptNode copyUninitialized() {
        return EnumerateNodeGen.create(context, values, cloneUninitialized(targetNode));
    }

    @Specialization(guards = {"isJSType(iteratedObject)", "!isJSAdapter(iteratedObject)", "!isJSJavaWrapper(iteratedObject)"})
    protected DynamicObject doEnumerateObject(DynamicObject iteratedObject,
                    @Cached("createBinaryProfile()") ConditionProfile isObject) {
        Iterator<?> iterator;
        if (isObject.profile(JSRuntime.isObject(iteratedObject))) {
            iterator = createEnumerateIterator(iteratedObject);
        } else {
            // null or undefined
            iterator = Collections.emptyIterator();
        }
        return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
    }

    @TruffleBoundary
    private Iterator<?> createEnumerateIterator(DynamicObject iteratedObject) {
        Iterator<?> iterator = new EnumerateIterator(iteratedObject);
        if (values) {
            iterator = IteratorUtil.convertIterator(iterator, key -> {
                Object value = JSObject.get(iteratedObject, Boundaries.javaToString(key));
                return value;
            });
        }
        return iterator;
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
        return JSObject.create(context, context.getEnumerateIteratorFactory(), Collections.emptyIterator());
    }

    EnumerateNode createValues() {
        return create(context, null, true);
    }

    @Specialization(guards = "isJSJavaWrapper(iteratedObject)")
    protected DynamicObject doEnumerateJava(DynamicObject iteratedObject) {
        Iterator<?> iterator = (values ? JSJavaWrapper.getValues(iteratedObject) : JSObject.ownPropertyKeys(iteratedObject)).iterator();
        return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
    }

    @Specialization(guards = {"isForeignObject(iteratedObject)"})
    protected DynamicObject doEnumerateTruffleObject(TruffleObject iteratedObject,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("createGetSize()") Node getSizeNode,
                    @Cached("createRead()") Node readNode,
                    @Cached("createKeys()") Node keysNode,
                    @Cached("create()") JSToLengthNode toLengthNode,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode,
                    @Cached("createBinaryProfile()") ConditionProfile isHostObject) {
        TruffleLanguage.Env env = context.getRealm().getEnv();
        if (isHostObject.profile(env.isHostObject(iteratedObject))) {
            Object hostObject = env.asHostObject(iteratedObject);
            Iterator<?> iterator = getHostObjectIterator(hostObject, values, env);
            if (iterator != null) {
                return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
            }
        }

        return doEnumerateTruffleObjectIntl(context, iteratedObject, hasSizeNode, getSizeNode, readNode, keysNode, toLengthNode, foreignConvertNode, values);
    }

    @TruffleBoundary
    private static Iterator<?> getHostObjectIterator(Object hostObject, boolean values, TruffleLanguage.Env env) {
        if (hostObject != null) {
            if (hostObject instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) hostObject;
                Iterator<?> iterator = values ? map.values().iterator() : map.keySet().iterator();
                return IteratorUtil.convertIterator(iterator, env::asGuestValue);
            } else if (hostObject.getClass().isArray()) {
                return values ? new ArrayIterator(hostObject) : IteratorUtil.rangeIterator(Array.getLength(hostObject));
            } else if (!values && hostObject instanceof List<?>) {
                return IteratorUtil.rangeIterator(((List<?>) hostObject).size());
            } else if (values && hostObject instanceof Iterable<?>) {
                Iterator<?> iterator = ((Iterable<?>) hostObject).iterator();
                return IteratorUtil.convertIterator(iterator, env::asGuestValue);
            }
        }
        return null;
    }

    public static DynamicObject doEnumerateTruffleObjectIntl(JSContext context, TruffleObject iteratedObject, Node hasSizeNode, Node getSizeNode, Node readNode, Node keysNode,
                    JSToLengthNode toLengthNode, JSForeignToJSTypeNode foreignConvertNode, boolean values) {
        try {
            boolean hasSize = ForeignAccess.sendHasSize(hasSizeNode, iteratedObject);
            if (hasSize) {
                return enumerateForeignArrayLike(context, iteratedObject, getSizeNode, readNode, toLengthNode, foreignConvertNode, values);
            } else {
                TruffleObject keysObj = ForeignAccess.sendKeys(keysNode, iteratedObject);
                hasSize = ForeignAccess.sendHasSize(hasSizeNode, keysObj);
                if (hasSize) {
                    return enumerateForeignNonArray(context, iteratedObject, keysObj, getSizeNode, readNode, toLengthNode, foreignConvertNode, values);
                } else {
                    return JSArray.createEmptyZeroLength(context);
                }
            }
        } catch (UnsupportedMessageException ex) {
            // swallow and default
        }
        // in case of any errors, return an empty iterator
        return JSObject.create(context, context.getEnumerateIteratorFactory(), Collections.emptyIterator());
    }

    private static DynamicObject enumerateForeignArrayLike(JSContext context, TruffleObject iteratedObject, Node getSizeNode, Node readNode, JSToLengthNode toLengthNode,
                    JSForeignToJSTypeNode foreignConvertNode, boolean values)
                    throws UnsupportedMessageException {
        Object size = ForeignAccess.sendGetSize(getSizeNode, iteratedObject);
        long longSize = toLengthNode.executeLong(size);
        Iterator<Object> iterator = new Iterator<Object>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < longSize;
            }

            @Override
            public Object next() {
                if (hasNext()) {
                    if (values) {
                        try {
                            return foreignConvertNode.executeWithTarget(ForeignAccess.sendRead(readNode, iteratedObject, cursor++));
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            // swallow and default
                        }
                    } else {
                        return cursor++;
                    }
                }
                throw new NoSuchElementException();
            }
        };
        return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
    }

    private static DynamicObject enumerateForeignNonArray(JSContext context, TruffleObject iteratedObject, TruffleObject keysObject, Node getSizeNode, Node readNode, JSToLengthNode toLengthNode,
                    JSForeignToJSTypeNode foreignConvertNode,
                    boolean values) throws UnsupportedMessageException {
        Object size = ForeignAccess.sendGetSize(getSizeNode, keysObject);
        long longSize = toLengthNode.executeLong(size);
        Iterator<Object> iterator = new Iterator<Object>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < longSize;
            }

            @Override
            public Object next() {
                if (hasNext()) {
                    if (values) {
                        try {
                            // no conversion on KEYS, always String
                            Object key = ForeignAccess.sendRead(readNode, keysObject, cursor++);
                            return foreignConvertNode.executeWithTarget(ForeignAccess.sendRead(readNode, iteratedObject, key));
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            // swallow and default
                        }
                    } else {
                        try {
                            // no conversion on KEYS, always String
                            return ForeignAccess.sendRead(readNode, keysObject, cursor++);
                        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                            // swallow and default
                        }
                    }
                }
                throw new NoSuchElementException();
            }
        };
        return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
    }

    @Specialization(guards = {"!isJSObject(iteratedObject)"})
    protected DynamicObject doNonObject(Object iteratedObject,
                    @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode,
                    @Cached("copyRecursive()") EnumerateNode enumerateNode) {
        return enumerateNode.execute(toObjectNode.executeTruffleObject(iteratedObject));
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
}
