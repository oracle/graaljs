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
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.CreateSetIteratorNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetAddNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetClearNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetDeleteNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetForEachNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetHasNodeGen;
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
        forEach(1),
        values(0),
        entries(0);

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
            case values:
                return CreateSetIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_VALUE, args().withThis().createArgumentNodes(context));
            case entries:
                return CreateSetIteratorNodeGen.create(context, builtin, JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE, args().withThis().createArgumentNodes(context));
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
            throw Errors.createTypeErrorCallableExpected();
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

    public abstract static class CreateSetIteratorNode extends JSBuiltinNode {
        private final int iterationKind;
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setNextIndexNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private PropertySetNode setIterationKindNode;

        public CreateSetIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
            this.createObjectNode = CreateObjectNode.createWithCachedPrototype(context, null);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSSet.SET_ITERATION_KIND_ID, context);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject doSet(VirtualFrame frame, DynamicObject set) {
            DynamicObject iterator = createObjectNode.executeDynamicObject(frame, getContext().getRealm().getSetIteratorPrototype());
            setIteratedObjectNode.setValue(iterator, set);
            setNextIndexNode.setValue(iterator, JSSet.getInternalSet(set).getEntries());
            setIterationKindNode.setValueInt(iterator, iterationKind);
            return iterator;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected DynamicObject doIncompatibleReceiver(Object thisObj) {
            throw Errors.createTypeError("not a Set");
        }
    }
}
