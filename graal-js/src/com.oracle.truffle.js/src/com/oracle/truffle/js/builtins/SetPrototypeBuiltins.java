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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.CreateSetIteratorNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetAddNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetClearNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetDeleteNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetDifferenceNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetForEachNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetHasNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetIntersectionNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetIsDisjointFromNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetIsSubsetOfNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetIsSupersetOfNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetSymmetricDifferenceNodeGen;
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.JSSetUnionNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNodeGen;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

/**
 * Contains builtins for {@linkplain JSSet}.prototype.
 */
public final class SetPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<SetPrototypeBuiltins.SetPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new SetPrototypeBuiltins();
    public static final JSBuiltinsContainer NEW_SET_BUILTINS = new NewSetPrototypeBuiltins();

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

    /**
     * Built-ins from the NewSetMethods proposal (https://github.com/tc39/proposal-set-methods).
     */
    public static final class NewSetPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<NewSetPrototypeBuiltins.NewSetPrototype> {
        protected NewSetPrototypeBuiltins() {
            super(NewSetPrototype.class);
        }

        public enum NewSetPrototype implements BuiltinEnum<NewSetPrototype> {
            union(1),
            intersection(1),
            difference(1),
            symmetricDifference(1),
            isSubsetOf(1),
            isSupersetOf(1),
            isDisjointFrom(1);

            private final int length;

            NewSetPrototype(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, NewSetPrototype builtinEnum) {
            switch (builtinEnum) {
                case union:
                    return JSSetUnionNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case intersection:
                    return JSSetIntersectionNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case difference:
                    return JSSetDifferenceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case symmetricDifference:
                    return JSSetSymmetricDifferenceNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case isSubsetOf:
                    return JSSetIsSubsetOfNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case isSupersetOf:
                    return JSSetIsSupersetOfNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
                case isDisjointFrom:
                    return JSSetIsDisjointFromNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            }
            return null;
        }
    }

    public abstract static class JSSetOperation extends JSBuiltinNode {
        /** Dummy value to associate with a key in the backing map. */
        protected static final Object PRESENT = new Object();

        @Child private JSCollectionsNormalizeNode normalizeNode;

        public JSSetOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
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
    public abstract static class JSSetClearNode extends JSBuiltinNode {

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
            throw Errors.createTypeErrorSetExpected();
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
            throw Errors.createTypeErrorSetExpected();
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
            throw Errors.createTypeErrorSetExpected();
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
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Base class for the New Set Methods.
     */
    protected abstract static class JSSetNewOperation extends JSSetOperation {

        @Child protected GetIteratorNode getIteratorNode;
        @Child protected IteratorStepNode iteratorStepNode;
        @Child protected IteratorValueNode iteratorValueNode;
        @Child protected IteratorCloseNode iteratorCloseNode;
        @Child protected JSFunctionCallNode callFunctionNode;
        @Child protected PropertyGetNode getAddNode;
        @Child protected PropertyGetNode getRemoveNode;
        @Child protected PropertyGetNode getHasNode;
        @Child protected IsCallableNode isCallableNode;
        protected final BranchProfile iteratorError = BranchProfile.create();
        protected final BranchProfile adderError = BranchProfile.create();

        protected JSSetNewOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            getIteratorNode = GetIteratorNode.create(context);
            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);
        }

        protected Object addEntryFromIterable(Object target, Object iterable, Object adder) {
            if (!isCallable(adder)) {
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return target;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    call(adder, target, nextValue);
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        protected final void iteratorCloseAbrupt(DynamicObject iterator) {
            if (iteratorCloseNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                iteratorCloseNode = insert(IteratorCloseNode.create(getContext()));
            }
            iteratorCloseNode.executeAbrupt(iterator);
        }

        protected Object call(Object function, Object target, Object... userArguments) {
            if (callFunctionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callFunctionNode = insert(JSFunctionCallNode.createCall());
            }
            return callFunctionNode.executeCall(JSArguments.create(target, function, userArguments));
        }

        protected final Object getAddFunction(Object object) {
            if (getAddNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAddNode = insert(PropertyGetNode.create("add", false, getContext()));
            }
            return getAddNode.getValue(object);
        }

        protected final Object getRemoveFunction(Object object) {
            if (getRemoveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getRemoveNode = insert(PropertyGetNode.create("delete", false, getContext()));
            }
            return getRemoveNode.getValue(object);
        }

        protected final Object getHasFunction(Object object) {
            if (getHasNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getHasNode = insert(PropertyGetNode.create("has", false, getContext()));
            }
            return getHasNode.getValue(object);
        }

        protected final Object constructSet(Object... arguments) {
            Object ctr = getContext().getRealm().getSetConstructor();
            return JSRuntime.construct(ctr, arguments);
        }

        protected final boolean isCallable(Object object) {
            if (isCallableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isCallableNode = insert(IsCallableNode.create());
            }
            return isCallableNode.executeBoolean(object);
        }
    }

    /**
     * Implementation of the Set.prototype.union().
     */
    public abstract static class JSSetUnionNode extends JSSetNewOperation {

        public JSSetUnionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject union(DynamicObject set, Object iterable) {
            DynamicObject newSet = (DynamicObject) constructSet(set);
            Object adder = getAddFunction(newSet);
            addEntryFromIterable(newSet, iterable, adder);
            return newSet;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.intersection().
     */
    public abstract static class JSSetIntersectionNode extends JSSetNewOperation {

        private final BranchProfile hasError = BranchProfile.create();

        public JSSetIntersectionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject intersection(DynamicObject set, Object iterable) {
            DynamicObject newSet = (DynamicObject) constructSet();
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                hasError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            Object adder = getAddFunction(newSet);
            if (!isCallable(adder)) {
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return newSet;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    Object has = call(hasCheck, set, nextValue);
                    if (has == Boolean.TRUE) {
                        call(adder, newSet, nextValue);
                    }
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.difference().
     */
    public abstract static class JSSetDifferenceNode extends JSSetNewOperation {

        private final BranchProfile removerError = BranchProfile.create();

        public JSSetDifferenceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject difference(DynamicObject set, Object iterable) {
            DynamicObject newSet = (DynamicObject) constructSet(set);
            Object remover = getRemoveFunction(newSet);
            if (!isCallable(remover)) {
                removerError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return newSet;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    call(remover, newSet, nextValue);
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.symmetricDifference().
     */
    public abstract static class JSSetSymmetricDifferenceNode extends JSSetNewOperation {

        private final BranchProfile removerError = BranchProfile.create();

        public JSSetSymmetricDifferenceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject symmetricDifference(DynamicObject set, Object iterable) {
            DynamicObject newSet = (DynamicObject) constructSet(set);
            Object remover = getRemoveFunction(newSet);
            if (!isCallable(remover)) {
                removerError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            Object adder = getAddFunction(newSet);

            if (!isCallable(adder)) {
                // unreachable due to constructor add
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return newSet;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    Object removed = call(remover, newSet, nextValue);
                    if (removed == Boolean.FALSE) {
                        call(adder, newSet, nextValue);
                    }
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.isSubsetOf().
     */
    public abstract static class JSSetIsSubsetOfNode extends JSSetNewOperation {

        private final BranchProfile needCreateNewBranch = BranchProfile.create();
        private final BranchProfile isObjectError = BranchProfile.create();

        public JSSetIsSubsetOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected Boolean isSubsetOf(DynamicObject set, Object iterable) {
            IteratorRecord iteratorRecord = getIteratorNode.execute(set);
            if (!JSRuntime.isObject(iterable)) {
                isObjectError.enter();
                throw Errors.createTypeErrorNotIterable(iterable, this);
            }
            DynamicObject otherSet = (DynamicObject) iterable;
            Object hasCheck = getHasFunction(otherSet);
            if (!isCallable(hasCheck)) {
                needCreateNewBranch.enter();
                otherSet = (DynamicObject) constructSet();
                addEntryFromIterable(otherSet, iterable, getAddFunction(otherSet));
                hasCheck = getHasFunction(otherSet);
            }
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return Boolean.TRUE;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    Object has = call(hasCheck, otherSet, nextValue);
                    if (has == Boolean.FALSE) {
                        return Boolean.FALSE;
                    }
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.isSupersetOf().
     */
    public abstract static class JSSetIsSupersetOfNode extends JSSetNewOperation {

        private final BranchProfile hasError = BranchProfile.create();

        public JSSetIsSupersetOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected Boolean isSupersetOf(DynamicObject set, Object iterable) {
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                hasError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return Boolean.TRUE;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    Object has = call(hasCheck, set, nextValue);
                    if (has == Boolean.FALSE) {
                        return Boolean.FALSE;
                    }
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.isDisjointFrom().
     */
    public abstract static class JSSetIsDisjointFromNode extends JSSetNewOperation {

        private final BranchProfile hasError = BranchProfile.create();

        public JSSetIsDisjointFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJSSet(set)")
        protected Boolean isDisjointFrom(DynamicObject set, Object iterable) {
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                hasError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return Boolean.TRUE;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    Object has = call(hasCheck, set, nextValue);
                    if (has == Boolean.TRUE) {
                        return Boolean.FALSE;
                    }
                }
            } catch (Exception ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object key) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    public abstract static class JSSetForEachNode extends JSBuiltinNode {

        public JSSetForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSSet(thisObj)", "isCallable.executeBoolean(callback)"}, limit = "1")
        protected Object forEachFunction(DynamicObject thisObj, DynamicObject callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable,
                        @Cached("createCall()") JSFunctionCallNode callNode) {
            JSHashMap map = JSSet.getInternalSet(thisObj);
            JSHashMap.Cursor cursor = map.getEntries();
            while (cursor.advance()) {
                Object key = cursor.getKey();
                callNode.executeCall(JSArguments.create(thisArg, callback, new Object[]{key, key, thisObj}));
            }
            return Undefined.instance;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isJSSet(thisObj)", "!isCallable.executeBoolean(callback)"}, limit = "1")
        protected static Object forEachFunctionNoFunction(Object thisObj, Object callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            throw Errors.createTypeErrorCallableExpected();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static Object forEachFunctionNoSet(Object thisObj, Object callback, Object thisArg) {
            throw Errors.createTypeErrorSetExpected();
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
            this.createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.setNextIndexNode = PropertySetNode.createSetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.setIterationKindNode = PropertySetNode.createSetHidden(JSSet.SET_ITERATION_KIND_ID, context);
        }

        @Specialization(guards = "isJSSet(set)")
        protected DynamicObject doSet(VirtualFrame frame, DynamicObject set) {
            DynamicObject iterator = createObjectNode.execute(frame, getContext().getRealm().getSetIteratorPrototype());
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
