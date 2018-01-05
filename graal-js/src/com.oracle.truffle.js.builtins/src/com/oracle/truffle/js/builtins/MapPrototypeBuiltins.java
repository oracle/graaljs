/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapClearNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapForEachNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapGetNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapHasNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapSetNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

/**
 * Contains builtins for {@linkplain JSMap}.prototype.
 */
public final class MapPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<MapPrototypeBuiltins.MapPrototype> {
    protected MapPrototypeBuiltins() {
        super(JSMap.PROTOTYPE_NAME, MapPrototype.class);
    }

    public enum MapPrototype implements BuiltinEnum<MapPrototype> {
        clear(0),
        delete(1),
        set(2),
        get(1),
        has(1),
        forEach(1);

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
        }
        return null;
    }

    public abstract static class JSMapOperation extends JSBuiltinNode {
        @Child private JSCollectionsNormalizeNode normalizeNode;

        public JSMapOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static RuntimeException typeErrorMapExpected() {
            throw Errors.createTypeError("Map expected");
        }

        protected Object normalize(Object value) {
            if (normalizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                normalizeNode = insert(JSCollectionsNormalizeNodeGen.create());
            }
            return normalizeNode.execute(value);
        }
    }

    /**
     * Implementation of the Map.prototype.clear().
     */
    public abstract static class JSMapClearNode extends JSMapOperation {

        public JSMapClearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSMap(thisObj)")
        protected static DynamicObject clear(DynamicObject thisObj) {
            JSMap.getInternalMap(thisObj).clear();
            return Undefined.instance;
        }

        @Specialization(guards = "!isJSMap(thisObj)")
        protected static DynamicObject notMap(@SuppressWarnings("unused") Object thisObj) {
            throw typeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.delete().
     */
    public abstract static class JSMapDeleteNode extends JSMapOperation {

        public JSMapDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSMap(thisObj)")
        protected boolean delete(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).remove(normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSMap(thisObj)")
        protected static boolean notMap(Object thisObj, Object key) {
            throw typeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.get().
     */
    public abstract static class JSMapGetNode extends JSMapOperation {

        public JSMapGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSMap(thisObj)")
        protected Object get(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            Object value = JSMap.getInternalMap(thisObj).get(normalizedKey);
            if (value != null) {
                return value;
            } else {
                return Undefined.instance;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSMap(thisObj)")
        protected static Object notMap(Object thisObj, Object key) {
            throw typeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.set().
     */
    public abstract static class JSMapSetNode extends JSMapOperation {

        public JSMapSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSMap(thisObj)")
        protected DynamicObject set(DynamicObject thisObj, Object key, Object value) {
            Object normalizedKey = normalize(key);
            JSMap.getInternalMap(thisObj).put(normalizedKey, value);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSMap(thisObj)")
        protected static DynamicObject notMap(Object thisObj, Object key, Object value) {
            throw typeErrorMapExpected();
        }
    }

    /**
     * Implementation of the Map.prototype.has().
     */
    public abstract static class JSMapHasNode extends JSMapOperation {

        public JSMapHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSMap(thisObj)")
        protected boolean has(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSMap.getInternalMap(thisObj).has(normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSMap(thisObj)")
        protected static boolean notMap(Object thisObj, Object key) {
            throw typeErrorMapExpected();
        }
    }

    public abstract static class JSMapForEachNode extends JSMapOperation {
        @Child private JSFunctionCallNode callNode;

        public JSMapForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private Object call(Object target, DynamicObject function, Object[] params) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSFunctionCallNode.createCall());
            }
            return callNode.executeCall(JSArguments.create(target, function, params));
        }

        @Specialization(guards = {"isJSMap(thisObj)", "isCallable(callback)"})
        protected Object forEachFunction(DynamicObject thisObj, DynamicObject callback, Object thisArg) {
            forEachIntl(thisObj, thisArg, callback);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSMap(thisObj)", "!isCallable(callback)"})
        protected static Object forEachFunctionNoFunction(Object thisObj, Object callback, Object thisArg) {
            throw Errors.createTypeError("Callable expected");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSMap(thisObj)"})
        protected static Object notMap(Object thisObj, Object callback, Object thisArg) {
            throw typeErrorMapExpected();
        }

        private void forEachIntl(DynamicObject thisObj, Object thisArg, DynamicObject callbackObj) {
            assert JSRuntime.isCallable(callbackObj);
            JSHashMap map = JSMap.getInternalMap(thisObj);
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object value = cursor.getValue();
                Object key = cursor.getKey();
                call(thisArg, callbackObj, new Object[]{value, key, thisObj});
            }
        }
    }
}
