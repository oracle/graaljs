/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.java.JavaInterop;
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

    @Specialization(guards = "isMapJavaObject(iteratedObject)")
    protected DynamicObject doEnumerateMap(TruffleObject iteratedObject) {
        Map<?, ?> map = (Map<?, ?>) JavaInterop.asJavaObject(iteratedObject);
        Iterator<?> iterator = values ? map.values().iterator() : map.keySet().iterator();
        return JSObject.create(context, context.getEnumerateIteratorFactory(), iterator);
    }

    protected static boolean isMapJavaObject(TruffleObject object) {
        return JavaInterop.isJavaObject(object) && JavaInterop.asJavaObject(object) instanceof Map;
    }

    @Specialization(guards = {"isForeignObject(iteratedObject)", "!isMapJavaObject(iteratedObject)"})
    protected DynamicObject doEnumerateTruffleObject(TruffleObject iteratedObject,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("createGetSize()") Node getSizeNode,
                    @Cached("createRead()") Node readNode,
                    @Cached("createKeys()") Node keysNode,
                    @Cached("create()") JSToLengthNode toLengthNode,
                    @Cached("create()") JSForeignToJSTypeNode foreignConvertNode) {
        return doEnumerateTruffleObjectIntl(context, iteratedObject, hasSizeNode, getSizeNode, readNode, keysNode, toLengthNode, foreignConvertNode, values);
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
}
