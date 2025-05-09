/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.CreateMapIteratorNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapClearNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapForEachNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapGetNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapHasNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapSetNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.MapGetOrInsertNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.MapGetOrInsertComputedNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.MapGetSizeNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.builtins.JSMapIterator;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Contains builtins for {@linkplain JSMap}.prototype.
 */
public final class MapPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<MapPrototypeBuiltins.MapPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new MapPrototypeBuiltins();

    protected MapPrototypeBuiltins() {
        super(JSMap.PROTOTYPE_NAME, MapPrototype.class);
    }

    public enum MapPrototype implements BuiltinEnum<MapPrototype> {
        clear(0),
        delete(1),
        set(2),
        get(1),
        has(1),
        forEach(1),
        keys(0),
        values(0),
        entries(0),
        size(0),

        getOrInsert(2),
        getOrInsertComputed(2);

        private final int length;

        MapPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == size;
        }

        @Override
        public int getECMAScriptVersion() {
            return switch (this) {
                case getOrInsert, getOrInsertComputed -> JSConfig.StagingECMAScriptVersion;
                default -> BuiltinEnum.super.getECMAScriptVersion();
            };
        }

    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, MapPrototype builtinEnum) {
        switch (builtinEnum) {
            case clear:
                return JSMapClearNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case delete:
                return JSMapDeleteNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case set:
                return JSMapSetNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case get:
                return JSMapGetNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case has:
                return JSMapHasNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case forEach:
                return JSMapForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case keys:
                return CreateMapIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY, args().withThis().createArgumentNodes(context));
            case values:
                return CreateMapIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_VALUE, args().withThis().createArgumentNodes(context));
            case entries:
                return CreateMapIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE, args().withThis().createArgumentNodes(context));
            case size:
                return MapGetSizeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case getOrInsert:
                return MapGetOrInsertNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case getOrInsertComputed:
                return MapGetOrInsertComputedNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSMapOperation extends JSBuiltinNode {
        @Child private JSCollectionsNormalizeNode normalizeNode = JSCollectionsNormalizeNode.create();

        protected JSMapOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected final Object normalize(Object value) {
            return normalizeNode.execute(value);
        }

        protected static boolean isForeignHash(Object value, InteropLibrary interopLibrary) {
            return interopLibrary.hasHashEntries(value) && !(value instanceof JSDynamicObject);
        }
    }

    /**
     * Implementation of the Map.prototype.clear().
     */
    @ImportStatic({JSConfig.class, JSMapOperation.class})
    public abstract static class JSMapClearNode extends JSBuiltinNode {

        public JSMapClearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static JSDynamicObject doMap(JSMapObject thisObj) {
            JSMap.getInternalMap(thisObj).clear();
            return Undefined.instance;
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected JSDynamicObject doForeignMap(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Exclusive InteropLibrary iteratorLib,
                        @Cached InlinedBranchProfile growProfile) {
            try {
                Object hashEntriesIterator = mapLib.getHashKeysIterator(thisObj);
                // Save keys to temporary array to avoid concurrent modification exceptions.
                SimpleArrayList<Object> keys = SimpleArrayList.create(mapLib.getHashSize(thisObj));
                while (true) {
                    try {
                        Object nextKey = iteratorLib.getIteratorNextElement(hashEntriesIterator);
                        keys.add(nextKey, this, growProfile);
                    } catch (StopIterationException e) {
                        break;
                    }
                    TruffleSafepoint.poll(this);
                }
                for (Object key : keys.toArray()) {
                    try {
                        mapLib.removeHashEntry(thisObj, key);
                    } catch (UnknownKeyException e) {
                        continue;
                    }
                    TruffleSafepoint.poll(this);
                }
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "clear", this);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static JSDynamicObject notMap(@SuppressWarnings("unused") Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.delete().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSMapDeleteNode extends JSMapOperation {

        public JSMapDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean doMap(JSMapObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).remove(normalizedKey);
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected boolean doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            Object normalizedKey = normalize(key);
            try {
                mapLib.removeHashEntry(thisObj, normalizedKey);
                return true;
            } catch (UnknownKeyException e) {
                return false;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "removeHashEntry", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static boolean notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.get().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSMapGetNode extends JSMapOperation {

        public JSMapGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doMap(JSMapObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            Object value = JSMap.getInternalMap(thisObj).get(normalizedKey);
            return JSRuntime.nullToUndefined(value);
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached ImportValueNode importValue) {
            Object normalizedKey = normalize(key);
            try {
                return importValue.executeWithTarget(mapLib.readHashValueOrDefault(thisObj, normalizedKey, Undefined.instance));
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "readHashValue", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.set().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSMapSetNode extends JSMapOperation {

        public JSMapSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject doMap(JSMapObject thisObj, Object key, Object value) {
            Object normalizedKey = normalize(key);
            JSMap.getInternalMap(thisObj).put(normalizedKey, value);
            return thisObj;
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached ExportValueNode exportValueNode) {
            Object normalizedKey = normalize(key);
            Object exportedValue = exportValueNode.execute(value);
            try {
                mapLib.writeHashEntry(thisObj, normalizedKey, exportedValue);
            } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "writeHashEntry", this);
            }
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static JSDynamicObject notMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.has().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class JSMapHasNode extends JSMapOperation {

        public JSMapHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean doMap(JSMapObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).has(normalizedKey);
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            Object normalizedKey = normalize(key);
            return mapLib.isHashEntryReadable(thisObj, normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static boolean notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    @ImportStatic({JSConfig.class, JSMapOperation.class})
    public abstract static class JSMapForEachNode extends JSBuiltinNode {

        public JSMapForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isCallable.executeBoolean(callback)"})
        protected Object doMap(JSMapObject thisObj, Object callback, Object thisArg,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("createCall()") @Shared JSFunctionCallNode callNode) {
            JSHashMap map = JSMap.getInternalMap(thisObj);
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object value = cursor.getValue();
                Object key = cursor.getKey();
                callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{value, key, thisObj}));
            }
            return Undefined.instance;
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)", "isCallable.executeBoolean(callback)"})
        protected Object doForeignMap(Object thisObj, Object callback, Object thisArg,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("createCall()") @Shared JSFunctionCallNode callNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Exclusive InteropLibrary iteratorLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Exclusive InteropLibrary entryLib,
                        @Cached ImportValueNode importValue) {
            try {
                Object hashEntriesIterator = mapLib.getHashEntriesIterator(thisObj);
                while (true) {
                    Object nextEntry;
                    try {
                        nextEntry = iteratorLib.getIteratorNextElement(hashEntriesIterator);
                    } catch (StopIterationException e) {
                        break;
                    }
                    Object key = importValue.executeWithTarget(entryLib.readArrayElement(nextEntry, 0));
                    Object value = importValue.executeWithTarget(entryLib.readArrayElement(nextEntry, 1));
                    callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{value, key, thisObj}));
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "forEach", this);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSMap(thisObj) || isForeignHash(thisObj, mapLib)", "!isCallable.executeBoolean(callback)"})
        protected static Object invalidCallback(Object thisObj, Object callback, Object thisArg,
                        @Cached @Shared IsCallableNode isCallable,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorCallableExpected();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object callback, Object thisArg,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.size getter.
     */
    @ImportStatic({JSConfig.class, JSMapOperation.class})
    public abstract static class MapGetSizeNode extends JSBuiltinNode {

        public MapGetSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static int doMap(JSMapObject thisObj) {
            return JSMap.getMapSize(thisObj);
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected final Object doForeignMap(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached(inline = true) LongToIntOrDoubleNode sizeToJSNumber) {
            try {
                long hashSize = mapLib.getHashSize(thisObj);
                return sizeToJSNumber.execute(this, hashSize);
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "getHashSize", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static int notMap(@SuppressWarnings("unused") Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    @ImportStatic({JSConfig.class, JSRuntime.class, JSMapOperation.class})
    public abstract static class CreateMapIteratorNode extends JSBuiltinNode {
        private final int iterationKind;

        public CreateMapIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
        }

        @Specialization
        protected final JSObject doMap(JSMapObject map) {
            return JSMapIterator.create(getContext(), getRealm(), map, JSMap.getInternalMap(map).getEntries(), iterationKind);
        }

        @InliningCutoff
        @Specialization(guards = {"!isJSMap(map)", "isForeignHash(map, mapLib)"})
        protected final JSObject doForeignMap(Object map,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached("createSetHidden(ENUMERATE_ITERATOR_ID, getContext())") PropertySetNode setEnumerateIteratorNode) {
            Object iterator;
            try {
                if (iterationKind == JSRuntime.ITERATION_KIND_KEY) {
                    iterator = mapLib.getHashKeysIterator(map);
                } else if (iterationKind == JSRuntime.ITERATION_KIND_VALUE) {
                    iterator = mapLib.getHashValuesIterator(map);
                } else {
                    assert iterationKind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE;
                    iterator = mapLib.getHashEntriesIterator(map);
                }
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(map, e, "get hash iterator", this);
            }

            JSObject iteratorObj = JSOrdinary.create(getContext(), getContext().getEnumerateIteratorFactory(), getRealm());
            setEnumerateIteratorNode.setValue(iteratorObj, iterator);
            return iteratorObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static JSObject doIncompatibleReceiver(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    // Used for the defaultValue of readHashValueOrDefault. We need it to differ
    // from all other valid values (and truffle wants it to be a valid interop value).
    static final TruffleObject DEFAULT_VALUE = new TruffleObject() {
    };

    /**
     * Implementation of the Map.prototype.getOrInsert().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class MapGetOrInsertNode extends JSMapOperation {

        public MapGetOrInsertNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doMap(JSMapObject thisObj, Object key, Object value) {
            Object normalizedKey = normalize(key);
            Object result = JSMap.getInternalMap(thisObj).getOrInsert(normalizedKey, value);
            return JSRuntime.nullToUndefined(result);
        }

        @InliningCutoff
        @Specialization(guards = {"isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached ImportValueNode importValue,
                        @Cached ExportValueNode exportValueNode) {
            Object normalizedKey = normalize(key);
            try {
                Object result = mapLib.readHashValueOrDefault(thisObj, normalizedKey, DEFAULT_VALUE);
                if (result == DEFAULT_VALUE) {
                    result = exportValueNode.execute(value);
                    mapLib.writeHashEntry(thisObj, normalizedKey, result);
                }
                return importValue.executeWithTarget(result);
            } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException ex) {
                throw Errors.createTypeErrorInteropException(thisObj, ex, "getOrInsert", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.getOrInsertComputed().
     */
    @ImportStatic({JSConfig.class})
    public abstract static class MapGetOrInsertComputedNode extends JSMapOperation {

        public MapGetOrInsertComputedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doMap(JSMapObject thisObj, Object key, Object callbackfn,
                        @Shared @Cached IsCallableNode isCallable,
                        @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                        @Shared @Cached InlinedBranchProfile errorBranch) {
            if (!isCallable.executeBoolean(callbackfn)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            Object normalizedKey = normalize(key);
            JSHashMap internalMap = JSMap.getInternalMap(thisObj);
            Object value = internalMap.get(normalizedKey);
            if (value == null) {
                value = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, callbackfn, normalizedKey));
                internalMap.put(normalizedKey, value);
            }
            return JSRuntime.nullToUndefined(value);
        }

        @InliningCutoff
        @Specialization(guards = {"isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key, Object callbackfn,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib,
                        @Cached ImportValueNode importValue,
                        @Cached ExportValueNode exportValueNode,
                        @Shared @Cached IsCallableNode isCallable,
                        @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                        @Shared @Cached InlinedBranchProfile errorBranch) {
            if (!isCallable.executeBoolean(callbackfn)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            Object normalizedKey = normalize(key);
            try {
                Object result = mapLib.readHashValueOrDefault(thisObj, normalizedKey, DEFAULT_VALUE);
                if (result == DEFAULT_VALUE) {
                    Object value = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, callbackfn, normalizedKey));
                    result = exportValueNode.execute(value);
                    mapLib.writeHashEntry(thisObj, normalizedKey, result);
                }
                return importValue.executeWithTarget(result);
            } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException ex) {
                throw Errors.createTypeErrorInteropException(thisObj, ex, "getOrInsertComputed", this);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object key, Object callbackfn,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

}
