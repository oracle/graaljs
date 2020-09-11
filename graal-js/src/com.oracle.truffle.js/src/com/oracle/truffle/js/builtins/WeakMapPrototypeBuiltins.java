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
package com.oracle.truffle.js.builtins;

import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapGetNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapHasNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapSetNodeGen;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.WeakMap;

/**
 * Contains builtins for {@linkplain JSWeakMap}.prototype.
 */
public final class WeakMapPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WeakMapPrototypeBuiltins.WeakMapPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new WeakMapPrototypeBuiltins();

    protected WeakMapPrototypeBuiltins() {
        super(JSWeakMap.PROTOTYPE_NAME, WeakMapPrototype.class);
    }

    public enum WeakMapPrototype implements BuiltinEnum<WeakMapPrototype> {
        delete(1),
        set(2),
        get(1),
        has(1);

        private final int length;

        WeakMapPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WeakMapPrototype builtinEnum) {
        switch (builtinEnum) {
            case delete:
                return JSWeakMapDeleteNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case set:
                return JSWeakMapSetNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case get:
                return JSWeakMapGetNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case has:
                return JSWeakMapHasNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    protected static RuntimeException typeErrorKeyIsNotObject() {
        throw Errors.createTypeError("WeakMap key must be an object");
    }

    protected static RuntimeException typeErrorWeakMapExpected() {
        throw Errors.createTypeError("WeakMap expected");
    }

    protected abstract static class JSWeakMapBaseNode extends JSBuiltinNode {

        protected JSWeakMapBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected PropertyGetNode createInvertedGet() {
            return WeakMap.createInvertedKeyMapGetNode(getContext());
        }

        protected HasHiddenKeyCacheNode createInvertedHas() {
            return WeakMap.createInvertedKeyMapHasNode();
        }
    }

    /**
     * Implementation of the WeakMap.prototype.delete().
     */
    public abstract static class JSWeakMapDeleteNode extends JSBuiltinNode {

        public JSWeakMapDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)"})
        protected static boolean delete(DynamicObject thisObj, DynamicObject key) {
            return Boundaries.mapRemove(JSWeakMap.getInternalWeakMap(thisObj), key) != null;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakMap(thisObj)", "!isJSObject(key)"})
        protected static Object deleteNonObjectKey(DynamicObject thisObj, Object key) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static boolean notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }
    }

    /**
     * Implementation of the WeakMap.prototype.get().
     */
    public abstract static class JSWeakMapGetNode extends JSWeakMapBaseNode {

        public JSWeakMapGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)", "!isJSProxy(key)"})
        protected Object getCached(DynamicObject thisObj, DynamicObject key,
                        @Cached("createInvertedGet()") PropertyGetNode invertedGetter,
                        @Cached("createInvertedHas()") HasHiddenKeyCacheNode invertedHas,
                        @Cached("createClassProfile()") ValueProfile invertedKlassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) JSWeakMap.getInternalWeakMap(thisObj);
            if (hasInvertedProfile.profile(invertedHas.executeHasHiddenKey(key))) {
                Object inverted = invertedKlassProfile.profile(invertedGetter.getValue(key));
                @SuppressWarnings("unchecked")
                WeakHashMap<WeakMap, Object> invertedMap = (WeakHashMap<WeakMap, Object>) inverted;
                Object value = mapGet(map, invertedMap);
                if (value != null) {
                    return value;
                }
            }
            return Undefined.instance;
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)"})
        protected Object getGeneric(DynamicObject thisObj, DynamicObject key) {
            Object value = Boundaries.mapGet(JSWeakMap.getInternalWeakMap(thisObj), key);
            if (value != null) {
                return value;
            } else {
                return Undefined.instance;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakMap(thisObj)", "!isJSObject(key)"})
        protected static Object getNonObjectKey(DynamicObject thisObj, Object key) {
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static boolean notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }

        @TruffleBoundary(allowInlining = true)
        private static Object mapGet(WeakMap map, WeakHashMap<WeakMap, Object> invertedMap) {
            return invertedMap.get(map);
        }
    }

    /**
     * Implementation of the WeakMap.prototype.set().
     */
    public abstract static class JSWeakMapSetNode extends JSWeakMapBaseNode {

        public JSWeakMapSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)", "!isJSProxy(key)"})
        protected Object setCached(DynamicObject thisObj, DynamicObject key, Object value,
                        @Cached("createInvertedGet()") PropertyGetNode invertedGetter,
                        @Cached("createInvertedHas()") HasHiddenKeyCacheNode invertedHas,
                        @Cached("createClassProfile()") ValueProfile invertedKlassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) JSWeakMap.getInternalWeakMap(thisObj);
            if (hasInvertedProfile.profile(invertedHas.executeHasHiddenKey(key))) {
                Object inverted = invertedKlassProfile.profile(invertedGetter.getValue(key));
                @SuppressWarnings("unchecked")
                WeakHashMap<WeakMap, Object> invertedMap = (WeakHashMap<WeakMap, Object>) inverted;
                mapPut(map, invertedMap, value);
            } else {
                Boundaries.mapPut(JSWeakMap.getInternalWeakMap(thisObj), key, value);
            }
            return thisObj;
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)"})
        protected static DynamicObject set(DynamicObject thisObj, DynamicObject key, Object value) {
            Boundaries.mapPut(JSWeakMap.getInternalWeakMap(thisObj), key, value);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakMap(thisObj)", "!isJSObject(key)"})
        protected static DynamicObject setNonObjectKey(DynamicObject thisObj, Object key, Object value) {
            throw typeErrorKeyIsNotObject();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static DynamicObject notWeakMap(Object thisObj, Object key, Object value) {
            throw typeErrorWeakMapExpected();
        }

        @TruffleBoundary(allowInlining = true)
        private static Object mapPut(WeakMap map, WeakHashMap<WeakMap, Object> invertedMap, Object value) {
            return invertedMap.put(map, value);
        }
    }

    /**
     * Implementation of the WeakMap.prototype.has().
     */
    public abstract static class JSWeakMapHasNode extends JSWeakMapBaseNode {

        public JSWeakMapHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)", "!isJSProxy(key)"})
        protected Object hasCached(DynamicObject thisObj, DynamicObject key,
                        @Cached("createInvertedGet()") PropertyGetNode invertedGetter,
                        @Cached("createInvertedHas()") HasHiddenKeyCacheNode invertedHas,
                        @Cached("createClassProfile()") ValueProfile invertedKlassProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) JSWeakMap.getInternalWeakMap(thisObj);
            if (hasInvertedProfile.profile(invertedHas.executeHasHiddenKey(key))) {
                Object inverted = invertedKlassProfile.profile(invertedGetter.getValue(key));
                @SuppressWarnings("unchecked")
                WeakHashMap<WeakMap, Object> invertedMap = (WeakHashMap<WeakMap, Object>) inverted;
                return mapHas(map, invertedMap);
            }
            return false;
        }

        @TruffleBoundary(allowInlining = true)
        private static boolean mapHas(WeakMap map, WeakHashMap<WeakMap, Object> invertedMap) {
            return invertedMap.containsKey(map);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)"})
        protected static boolean has(DynamicObject thisObj, DynamicObject key) {
            return Boundaries.mapContainsKey(JSWeakMap.getInternalWeakMap(thisObj), key);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakMap(thisObj)", "!isJSObject(key)"})
        protected static boolean hasNonObjectKey(DynamicObject thisObj, Object key) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static boolean notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }
    }
}
