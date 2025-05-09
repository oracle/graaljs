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

import java.util.Map;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapGetNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapHasNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapSetNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.WeakMapGetOrInsertNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.WeakMapGetOrInsertComputedNodeGen;
import com.oracle.truffle.js.builtins.helper.CanBeHeldWeaklyNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.builtins.JSWeakMapObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
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
        has(1),

        getOrInsert(2),
        getOrInsertComputed(2);

        private final int length;

        WeakMapPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
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
            case getOrInsert:
                return WeakMapGetOrInsertNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
            case getOrInsertComputed:
                return WeakMapGetOrInsertComputedNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    protected static RuntimeException typeErrorKeyIsNotValid() {
        throw Errors.createTypeError("Invalid value used as weak map key");
    }

    protected static RuntimeException typeErrorWeakMapExpected() {
        throw Errors.createTypeError("WeakMap expected");
    }

    protected static Object getInvertedMap(JSObject key, DynamicObjectLibrary library) {
        return library.getOrDefault(key, WeakMap.INVERTED_WEAK_MAP_KEY, null);
    }

    @SuppressWarnings("unchecked")
    protected static WeakHashMap<WeakMap, Object> castWeakHashMap(Object map) {
        return CompilerDirectives.castExact(map, WeakHashMap.class);
    }

    /**
     * Implementation of the WeakMap.prototype.delete().
     */
    @ImportStatic(JSConfig.class)
    public abstract static class JSWeakMapDeleteNode extends JSBuiltinNode {

        public JSWeakMapDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean deleteJSObject(JSWeakMapObject thisObj, JSObject key,
                        @CachedLibrary(limit = "PropertyCacheLimit") DynamicObjectLibrary invertedGetter,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Object inverted = getInvertedMap(key, invertedGetter);
            if (hasInvertedProfile.profile(this, inverted != null)) {
                WeakHashMap<WeakMap, Object> invertedMap = castWeakHashMap(inverted);
                return Boundaries.mapRemove(invertedMap, map) != null;
            }
            return false;
        }

        @Specialization(guards = {"canBeHeldWeakly.execute(this, key)"})
        protected boolean deleteSymbol(JSWeakMapObject thisObj, Symbol key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Map<WeakMap, Object> invertedMap = key.getInvertedMap();
            if (hasInvertedProfile.profile(this, invertedMap != null)) {
                return Boundaries.mapRemove(invertedMap, map) != null;
            }
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!canBeHeldWeakly.execute(this, key)"})
        protected static boolean deleteInvalidKey(JSWeakMapObject thisObj, Object key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static boolean notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WeakMapGetHelperNode extends JavaScriptBaseNode {

        public abstract Object execute(JSWeakMapObject thisObject, Object key);

        @Specialization
        protected Object getJSObject(JSWeakMapObject thisObj, JSObject key,
                        @CachedLibrary(limit = "PropertyCacheLimit") DynamicObjectLibrary invertedGetter,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Object inverted = getInvertedMap(key, invertedGetter);
            if (hasInvertedProfile.profile(this, inverted != null)) {
                WeakHashMap<WeakMap, Object> invertedMap = castWeakHashMap(inverted);
                return mapGet(invertedMap, map);
            }
            return null;
        }

        @Specialization(guards = {"canBeHeldWeakly.execute(this, key)"})
        protected Object getSymbol(JSWeakMapObject thisObj, Symbol key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Map<WeakMap, Object> invertedMap = key.getInvertedMap();
            if (hasInvertedProfile.profile(this, invertedMap != null)) {
                return Boundaries.mapGet(invertedMap, map);
            }
            return null;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!canBeHeldWeakly.execute(this, key)"})
        protected static Object getInvalidKey(JSWeakMapObject thisObj, Object key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            return null;
        }

        @TruffleBoundary(allowInlining = true)
        private static Object mapGet(WeakHashMap<WeakMap, Object> invertedMap, WeakMap map) {
            return invertedMap.get(map);
        }
    }

    /**
     * Implementation of the WeakMap.prototype.get().
     */
    public abstract static class JSWeakMapGetNode extends JSBuiltinNode {

        public JSWeakMapGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object weakMap(JSWeakMapObject thisObj, Object key,
                        @Cached WeakMapGetHelperNode getNode) {
            return JSRuntime.nullToUndefined(getNode.execute(thisObj, key));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static Object notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }
    }

    @ImportStatic(JSConfig.class)
    public abstract static class WeakMapSetHelperNode extends JavaScriptBaseNode {

        public abstract Object execute(JSWeakMapObject thisObj, Object key, Object value);

        @Specialization
        protected Object setJSObject(JSWeakMapObject thisObj, JSObject key, Object value,
                        @CachedLibrary(limit = "PropertyCacheLimit") DynamicObjectLibrary invertedGetter,
                        @CachedLibrary(limit = "PropertyCacheLimit") DynamicObjectLibrary invertedSetter,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Object inverted = getInvertedMap(key, invertedGetter);
            if (hasInvertedProfile.profile(this, inverted != null)) {
                WeakHashMap<WeakMap, Object> invertedMap = castWeakHashMap(inverted);
                mapPut(invertedMap, map, value);
            } else {
                Map<WeakMap, Object> newInvertedMap = map.newInvertedMapWithEntry(key, value);
                invertedSetter.put(key, WeakMap.INVERTED_WEAK_MAP_KEY, newInvertedMap);
            }
            return thisObj;
        }

        @Specialization(guards = {"canBeHeldWeakly.execute(this, key)"})
        protected Object setSymbol(JSWeakMapObject thisObj, Symbol key, Object value,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Map<WeakMap, Object> invertedMap = key.getInvertedMap();
            if (hasInvertedProfile.profile(this, invertedMap != null)) {
                Boundaries.mapPut(invertedMap, map, value);
            } else {
                Map<WeakMap, Object> newInvertedMap = map.newInvertedMapWithEntry(key, value);
                key.setInvertedMap(newInvertedMap);
            }
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!canBeHeldWeakly.execute(this, key)"})
        protected static Object setInvalidKey(JSWeakMapObject thisObj, Object key, Object value,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            throw typeErrorKeyIsNotValid();
        }

        @TruffleBoundary(allowInlining = true)
        private static Object mapPut(WeakHashMap<WeakMap, Object> invertedMap, WeakMap map, Object value) {
            return invertedMap.put(map, value);
        }
    }

    /**
     * Implementation of the WeakMap.prototype.set().
     */
    public abstract static class JSWeakMapSetNode extends JSBuiltinNode {

        public JSWeakMapSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object weakMap(JSWeakMapObject thisObj, Object key, Object value,
                        @Cached WeakMapSetHelperNode setNode) {
            return setNode.execute(thisObj, key, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static Object notWeakMap(Object thisObj, Object key, Object value) {
            throw typeErrorWeakMapExpected();
        }

    }

    /**
     * Implementation of the WeakMap.prototype.has().
     */
    @ImportStatic(JSConfig.class)
    public abstract static class JSWeakMapHasNode extends JSBuiltinNode {

        public JSWeakMapHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean hasJSObject(JSWeakMapObject thisObj, JSObject key,
                        @CachedLibrary(limit = "PropertyCacheLimit") DynamicObjectLibrary invertedGetter,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Object inverted = getInvertedMap(key, invertedGetter);
            if (hasInvertedProfile.profile(this, inverted != null)) {
                WeakHashMap<WeakMap, Object> invertedMap = castWeakHashMap(inverted);
                return mapHas(invertedMap, map);
            }
            return false;
        }

        @Specialization(guards = {"canBeHeldWeakly.execute(this, key)"})
        protected boolean hasSymbol(JSWeakMapObject thisObj, Symbol key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly,
                        @Cached @Shared InlinedConditionProfile hasInvertedProfile) {
            WeakMap map = (WeakMap) thisObj.getWeakHashMap();
            Map<WeakMap, Object> invertedMap = key.getInvertedMap();
            if (hasInvertedProfile.profile(this, invertedMap != null)) {
                return Boundaries.mapContainsKey(invertedMap, map);
            }
            return false;
        }

        @TruffleBoundary(allowInlining = true)
        private static boolean mapHas(WeakHashMap<WeakMap, Object> invertedMap, WeakMap map) {
            return invertedMap.containsKey(map);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!canBeHeldWeakly.execute(this, key)"})
        protected static boolean hasInvalidKey(JSWeakMapObject thisObj, Object key,
                        @Cached @Shared @SuppressWarnings("unused") CanBeHeldWeaklyNode canBeHeldWeakly) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static boolean notWeakMap(Object thisObj, Object key) {
            throw typeErrorWeakMapExpected();
        }
    }

    /**
     * Implementation of the WeakMap.prototype.getOrInsert().
     */
    public abstract static class WeakMapGetOrInsertNode extends JSBuiltinNode {

        public WeakMapGetOrInsertNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object weakMap(JSWeakMapObject thisObj, Object key, Object value,
                        @Cached WeakMapGetHelperNode getNode,
                        @Cached WeakMapSetHelperNode setNode) {
            Object current = getNode.execute(thisObj, key);
            if (current == null) {
                setNode.execute(thisObj, key, value);
                return value;
            } else {
                return current;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static Object notWeakMap(Object thisObj, Object key, Object value) {
            throw typeErrorWeakMapExpected();
        }
    }

    /**
     * Implementation of the WeakMap.prototype.getOrInsertComputed().
     */
    public abstract static class WeakMapGetOrInsertComputedNode extends JSBuiltinNode {

        public WeakMapGetOrInsertComputedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object weakMap(JSWeakMapObject thisObj, Object key, Object callbackfn,
                        @Cached IsCallableNode isCallable,
                        @Cached WeakMapGetHelperNode getNode,
                        @Cached("createCall()") JSFunctionCallNode callNode,
                        @Cached WeakMapSetHelperNode setNode,
                        @Cached InlinedBranchProfile errorBranch) {
            if (!isCallable.executeBoolean(callbackfn)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            Object current = getNode.execute(thisObj, key);
            if (current == null) {
                Object value = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, callbackfn, key));
                setNode.execute(thisObj, key, value);
                return value;
            } else {
                return current;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakMap(thisObj)")
        protected static Object notWeakMap(Object thisObj, Object key, Object callbackfn) {
            throw typeErrorWeakMapExpected();
        }
    }

}
