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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.TruffleSafepoint;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.CreateMapIteratorNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapClearNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapForEachNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapGetNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapHasNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapSetNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
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
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
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
        entries(0);

        private final int length;

        MapPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
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

        @Specialization(guards = "isJSMap(thisObj)")
        protected static DynamicObject doMap(DynamicObject thisObj) {
            JSMap.getInternalMap(thisObj).clear();
            return Undefined.instance;
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected DynamicObject doForeignMap(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary iteratorLib,
                        @Cached BranchProfile growProfile) {
            try {
                Object hashEntriesIterator = mapLib.getHashKeysIterator(thisObj);
                // Save keys to temporary array to avoid concurrent modification exceptions.
                SimpleArrayList<Object> keys = SimpleArrayList.create(mapLib.getHashSize(thisObj));
                while (true) {
                    try {
                        Object nextKey = iteratorLib.getIteratorNextElement(hashEntriesIterator);
                        keys.add(nextKey, growProfile);
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
                throw Errors.createTypeErrorInteropException(thisObj, e, "clear", null);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static DynamicObject notMap(@SuppressWarnings("unused") Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
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

        @Specialization(guards = "isJSMap(thisObj)")
        protected boolean doMap(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).remove(normalizedKey);
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected boolean doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            Object normalizedKey = normalize(key);
            try {
                mapLib.removeHashEntry(thisObj, normalizedKey);
                return true;
            } catch (UnknownKeyException e) {
                return false;
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "removeHashEntry", null);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static boolean notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
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

        @Specialization(guards = "isJSMap(thisObj)")
        protected Object doMap(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            Object value = JSMap.getInternalMap(thisObj).get(normalizedKey);
            return JSRuntime.nullToUndefined(value);
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib,
                        @Cached ImportValueNode importValue) {
            Object normalizedKey = normalize(key);
            try {
                return importValue.executeWithTarget(mapLib.readHashValueOrDefault(thisObj, normalizedKey, Undefined.instance));
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "readHashValue", null);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
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

        @Specialization(guards = "isJSMap(thisObj)")
        protected DynamicObject doMap(DynamicObject thisObj, Object key, Object value) {
            Object normalizedKey = normalize(key);
            JSMap.getInternalMap(thisObj).put(normalizedKey, value);
            return thisObj;
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib,
                        @Cached ExportValueNode exportValueNode) {
            Object normalizedKey = normalize(key);
            Object exportedValue = exportValueNode.execute(value);
            try {
                mapLib.writeHashEntry(thisObj, normalizedKey, exportedValue);
            } catch (UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "writeHashEntry", null);
            }
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static DynamicObject notMap(Object thisObj, Object key, Object value,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
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

        @Specialization(guards = "isJSMap(thisObj)")
        protected boolean doMap(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).has(normalizedKey);
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)"})
        protected Object doForeignMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            Object normalizedKey = normalize(key);
            return mapLib.isHashEntryReadable(thisObj, normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static boolean notMap(Object thisObj, Object key,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    @ImportStatic({JSConfig.class, JSMapOperation.class})
    public abstract static class JSMapForEachNode extends JSBuiltinNode {

        public JSMapForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSMap(thisObj)", "isCallable.executeBoolean(callback)"}, limit = "1")
        protected Object doMap(DynamicObject thisObj, Object callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("createCall()") @Shared("callNode") JSFunctionCallNode callNode) {
            JSHashMap map = JSMap.getInternalMap(thisObj);
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object value = cursor.getValue();
                Object key = cursor.getKey();
                callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{value, key, thisObj}));
            }
            return Undefined.instance;
        }

        @Specialization(guards = {"!isJSMap(thisObj)", "isForeignHash(thisObj, mapLib)", "isCallable.executeBoolean(callback)"}, limit = "1")
        protected Object doForeignMap(Object thisObj, Object callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("createCall()") @Shared("callNode") JSFunctionCallNode callNode,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary iteratorLib,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary entryLib) {
            try {
                Object hashEntriesIterator = mapLib.getHashEntriesIterator(thisObj);
                while (true) {
                    try {
                        Object nextEntry = iteratorLib.getIteratorNextElement(hashEntriesIterator);
                        Object key = entryLib.readArrayElement(nextEntry, 0);
                        Object value = entryLib.readArrayElement(nextEntry, 1);
                        callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{value, key, thisObj}));
                    } catch (StopIterationException e) {
                        break;
                    }
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw Errors.createTypeErrorInteropException(thisObj, e, "forEach", null);
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSMap(thisObj) || isForeignHash(thisObj, mapLib)", "!isCallable.executeBoolean(callback)"}, limit = "1")
        protected static Object invalidCallback(Object thisObj, Object callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            throw Errors.createTypeErrorCallableExpected();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected static Object notMap(Object thisObj, Object callback, Object thisArg,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }

    @ImportStatic({JSConfig.class, JSRuntime.class, JSMapOperation.class})
    public abstract static class CreateMapIteratorNode extends JSBuiltinNode {
        private final int iterationKind;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private PropertySetNode setIterationKindNode;

        public CreateMapIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSMap.MAP_ITERATION_KIND_ID, context);
        }

        @Specialization(guards = "isJSMap(map)")
        protected DynamicObject doMap(VirtualFrame frame, DynamicObject map) {
            DynamicObject iterator = createObjectNode.execute(frame, getRealm().getMapIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, map);
            setNextIndexNode.setValue(iterator, JSMap.getInternalMap(map).getEntries());
            setIterationKindNode.setValueInt(iterator, iterationKind);
            return iterator;
        }

        @Specialization(guards = {"!isJSMap(map)", "isForeignHash(map, mapLib)"})
        protected DynamicObject doForeignMap(Object map,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib,
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
                throw Errors.createTypeErrorInteropException(map, e, "get hash iterator", null);
            }

            DynamicObject iteratorObj = JSOrdinary.create(getContext(), getContext().getEnumerateIteratorFactory(), getRealm());
            setEnumerateIteratorNode.setValue(iteratorObj, iterator);
            return iteratorObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)", "!isForeignHash(thisObj, mapLib)"})
        protected DynamicObject doIncompatibleReceiver(Object thisObj,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("mapLib") InteropLibrary mapLib) {
            throw Errors.createTypeErrorMapExpected();
        }
    }
}
