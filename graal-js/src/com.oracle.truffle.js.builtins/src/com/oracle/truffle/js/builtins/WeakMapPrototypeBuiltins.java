/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapGetNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapHasNodeGen;
import com.oracle.truffle.js.builtins.WeakMapPrototypeBuiltinsFactory.JSWeakMapSetNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSWeakMap;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain JSWeakMap}.prototype.
 */
public final class WeakMapPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WeakMapPrototypeBuiltins.WeakMapPrototype> {
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
    public abstract static class JSWeakMapGetNode extends JSBuiltinNode {

        public JSWeakMapGetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakMap(thisObj)", "isJSObject(key)"})
        protected static Object get(DynamicObject thisObj, DynamicObject key) {
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
    }

    /**
     * Implementation of the WeakMap.prototype.set().
     */
    public abstract static class JSWeakMapSetNode extends JSBuiltinNode {

        public JSWeakMapSetNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
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
    }

    /**
     * Implementation of the WeakMap.prototype.has().
     */
    public abstract static class JSWeakMapHasNode extends JSBuiltinNode {

        public JSWeakMapHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
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
