/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetAddNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetClearNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetDeleteNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetForEachNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetHasNodeGen;
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
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

/**
 * Contains builtins for {@linkplain JSSet}.prototype.
 */
public final class SetPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SetPrototypeBuiltins.SetPrototype> {

    protected SetPrototypeBuiltins() {
        super(JSSet.PROTOTYPE_NAME, SetPrototype.class);
    }

    public enum SetPrototype implements BuiltinEnum<SetPrototype> {
        clear(0),
        delete(1),
        add(1),
        has(1),
        forEach(1);

        private final int length;

        SetPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SetPrototype builtinEnum) {
        switch (builtinEnum) {
            case clear:
                return JSSetClearNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case delete:
                return JSSetDeleteNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case add:
                return JSSetAddNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case has:
                return JSSetHasNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case forEach:
                return JSSetForEachNodeGen.create(context, builtin, args().withThis().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSSetOperation extends JSBuiltinNode {
        /** Dummy value to associate with a key in the backing map. */
        protected static final Object PRESENT = new Object();

        @Child private JSCollectionsNormalizeNode normalizeNode;

        public JSSetOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected static RuntimeException typeErrorSetExpected() {
            throw Errors.createTypeError("Set expected");
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
     * Implementation of the Set.prototype.clear().
     */
    public abstract static class JSSetClearNode extends JSSetOperation {

        public JSSetClearNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(thisObj)")
        protected static DynamicObject clear(DynamicObject thisObj) {
            JSSet.getInternalSet(thisObj).clear();
            return Undefined.instance;
        }

        @Specialization(guards = "!isJSSet(thisObj)")
        protected static DynamicObject notSet(@SuppressWarnings("unused") Object thisObj) {
            throw typeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.delete().
     */
    public abstract static class JSSetDeleteNode extends JSSetOperation {

        public JSSetDeleteNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(thisObj)")
        protected boolean delete(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSSet.getInternalSet(thisObj).remove(normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static boolean notSet(Object thisObj, Object key) {
            throw typeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.add().
     */
    public abstract static class JSSetAddNode extends JSSetOperation {

        public JSSetAddNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(thisObj)")
        protected DynamicObject add(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            JSSet.getInternalSet(thisObj).put(normalizedKey, PRESENT);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static DynamicObject notSet(Object thisObj, Object key) {
            throw typeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.has().
     */
    public abstract static class JSSetHasNode extends JSSetOperation {

        public JSSetHasNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(thisObj)")
        protected boolean has(DynamicObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            return JSSet.getInternalSet(thisObj).has(normalizedKey);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean hasNoObject(Object thisObj, Object key) {
            throw typeErrorSetExpected();
        }
    }

    public abstract static class JSSetForEachNode extends JSSetOperation {
        @Child private JSFunctionCallNode callNode;

        public JSSetForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private Object call(Object target, DynamicObject function, Object[] params) {
            if (callNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callNode = insert(JSFunctionCallNode.createCall());
            }
            return callNode.executeCall(JSArguments.create(target, function, params));
        }

        @Specialization(guards = {"isJSSet(thisObj)", "isCallable(callback)"})
        protected Object forEachFunction(DynamicObject thisObj, DynamicObject callback, Object thisArg) {
            forEachIntl(thisObj, thisArg, callback);
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSSet(thisObj)", "!isCallable(callback)"})
        protected static Object forEachFunctionNoFunction(Object thisObj, Object callback, Object thisArg) {
            throw Errors.createTypeError("Callable expected");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static Object forEachFunctionNoSet(Object thisObj, Object callback, Object thisArg) {
            throw typeErrorSetExpected();
        }

        private void forEachIntl(DynamicObject thisObj, Object thisArg, DynamicObject callbackObj) {
            assert JSRuntime.isCallable(callbackObj);
            JSHashMap map = JSSet.getInternalSet(thisObj);
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object key = cursor.getKey();
                call(thisArg, callbackObj, new Object[]{key, key, thisObj});
            }
        }
    }
}
