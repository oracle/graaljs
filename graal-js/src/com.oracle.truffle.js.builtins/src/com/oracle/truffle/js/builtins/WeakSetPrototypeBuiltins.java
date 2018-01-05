/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.WeakSetPrototypeBuiltinsFactory.JSWeakSetAddNodeGen;
import com.oracle.truffle.js.builtins.WeakSetPrototypeBuiltinsFactory.JSWeakSetDeleteNodeGen;
import com.oracle.truffle.js.builtins.WeakSetPrototypeBuiltinsFactory.JSWeakSetHasNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSWeakSet;

/**
 * Contains builtins for {@linkplain JSWeakSet}.prototype.
 */
public final class WeakSetPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<WeakSetPrototypeBuiltins.WeakSetPrototype> {
    protected WeakSetPrototypeBuiltins() {
        super(JSWeakSet.PROTOTYPE_NAME, WeakSetPrototype.class);
    }

    public enum WeakSetPrototype implements BuiltinEnum<WeakSetPrototype> {
        delete(1),
        add(1),
        has(1);

        private final int length;

        WeakSetPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WeakSetPrototype builtinEnum) {
        switch (builtinEnum) {
            case delete:
                return JSWeakSetDeleteNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case add:
                return JSWeakSetAddNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case has:
                return JSWeakSetHasNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    /** Dummy value to associate with a key in the backing map. */
    protected static final Object PRESENT = new Object();

    protected static RuntimeException typeErrorKeyIsNotObject() {
        throw Errors.createTypeError("WeakSet key must be an object");
    }

    protected static RuntimeException typeErrorWeakSetExpected() {
        throw Errors.createTypeError("WeakSet expected");
    }

    /**
     * Implementation of the WeakSet.prototype.delete().
     */
    public abstract static class JSWeakSetDeleteNode extends JSBuiltinNode {

        public JSWeakSetDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakSet(thisObj)", "isJSObject(key)"})
        protected static boolean delete(DynamicObject thisObj, DynamicObject key) {
            return Boundaries.mapRemove(JSWeakSet.getInternalWeakMap(thisObj), key) != null;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakSet(thisObj)", "!isJSObject(key)"})
        protected static boolean deleteNonObjectKey(Object thisObj, Object key) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakSet(thisObj)")
        protected static boolean notWeakSet(Object thisObj, Object key) {
            throw typeErrorWeakSetExpected();
        }
    }

    /**
     * Implementation of the WeakSet.prototype.add().
     */
    public abstract static class JSWeakSetAddNode extends JSBuiltinNode {

        public JSWeakSetAddNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakSet(thisObj)", "isJSObject(key)"})
        protected static DynamicObject add(DynamicObject thisObj, DynamicObject key) {
            Boundaries.mapPut(JSWeakSet.getInternalWeakMap(thisObj), key, PRESENT);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakSet(thisObj)", "!isJSObject(key)"})
        protected static DynamicObject addNonObjectKey(Object thisObj, Object key) {
            throw typeErrorKeyIsNotObject();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakSet(thisObj)")
        protected static DynamicObject notWeakSet(Object thisObj, Object key) {
            throw typeErrorWeakSetExpected();
        }
    }

    /**
     * Implementation of the WeakSet.prototype.has().
     */
    public abstract static class JSWeakSetHasNode extends JSBuiltinNode {

        public JSWeakSetHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSWeakSet(thisObj)", "isJSObject(key)"})
        protected static boolean has(DynamicObject thisObj, DynamicObject key) {
            return Boundaries.mapContainsKey(JSWeakSet.getInternalWeakMap(thisObj), key);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSWeakSet(thisObj)", "!isJSObject(key)"})
        protected static boolean hasNonObjectKey(Object thisObj, Object key) {
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSWeakSet(thisObj)")
        protected static boolean notWeakSet(Object thisObj, Object key) {
            throw typeErrorWeakSetExpected();
        }
    }
}
