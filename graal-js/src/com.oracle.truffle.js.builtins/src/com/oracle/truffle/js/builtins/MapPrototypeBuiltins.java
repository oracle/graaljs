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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.CreateMapIteratorNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapClearNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapDeleteNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapForEachNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapGetNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapHasNodeGen;
import com.oracle.truffle.js.builtins.MapPrototypeBuiltinsFactory.JSMapSetNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNodeGen;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
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
            throw Errors.createTypeErrorCallableExpected();
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

    public abstract static class CreateMapIteratorNode extends JSBuiltinNode {
        private final int iterationKind;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private PropertySetNode setIterationKindNode;

        public CreateMapIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
            this.createObjectNode = CreateObjectNode.createWithCachedPrototype(context, null);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSMap.MAP_ITERATION_KIND_ID, context);
        }

        @Specialization(guards = "isJSMap(map)")
        protected DynamicObject doMap(VirtualFrame frame, DynamicObject map) {
            DynamicObject iterator = createObjectNode.executeDynamicObject(frame, getContext().getRealm().getMapIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, map);
            setNextIndexNode.setValue(iterator, JSMap.getInternalMap(map).getEntries());
            setIterationKindNode.setValueInt(iterator, iterationKind);
            return iterator;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSMap(thisObj)")
        protected DynamicObject doIncompatibleReceiver(Object thisObj) {
            throw Errors.createTypeError("not a Map");
        }
    }
}
