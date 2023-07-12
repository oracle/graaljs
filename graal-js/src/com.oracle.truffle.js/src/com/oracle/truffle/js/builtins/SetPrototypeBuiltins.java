/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
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
import com.oracle.truffle.js.builtins.SetPrototypeBuiltinsFactory.SetGetSizeNodeGen;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNodeGen;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetIterator;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
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
        entries(0),
        size(0);

        private final int length;

        SetPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isGetter() {
            return this == size;
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
            case size:
                return SetGetSizeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
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

        @Specialization
        protected static JSDynamicObject clear(JSSetObject thisObj) {
            JSSet.getInternalSet(thisObj).clear();
            return Undefined.instance;
        }

        @Specialization(guards = "!isJSSet(thisObj)")
        protected static JSDynamicObject notSet(@SuppressWarnings("unused") Object thisObj) {
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

        @Specialization
        protected boolean delete(JSSetObject thisObj, Object key) {
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

        @Specialization
        protected JSDynamicObject add(JSSetObject thisObj, Object key) {
            Object normalizedKey = normalize(key);
            JSSet.getInternalSet(thisObj).put(normalizedKey, PRESENT);
            return thisObj;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static JSDynamicObject notSet(Object thisObj, Object key) {
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

        @Specialization
        protected boolean has(JSSetObject thisObj, Object key) {
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
            iteratorStepNode = IteratorStepNode.create();
            iteratorValueNode = IteratorValueNode.create();
            iteratorCloseNode = IteratorCloseNode.create(context);
        }

        protected Object addEntryFromIterable(Object target, Object iterable, Object adder,
                        GetIteratorNode getIteratorNode) {
            if (!isCallable(adder)) {
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return target;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    call(adder, target, nextValue);
                }
            } catch (AbstractTruffleException ex) {
                iteratorError.enter();
                iteratorCloseAbrupt(iteratorRecord.getIterator());
                throw ex;
            }
        }

        protected final void iteratorCloseAbrupt(Object iterator) {
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
                getAddNode = insert(PropertyGetNode.create(Strings.ADD, false, getContext()));
            }
            return getAddNode.getValue(object);
        }

        protected final Object getRemoveFunction(Object object) {
            if (getRemoveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getRemoveNode = insert(PropertyGetNode.create(Strings.DELETE, false, getContext()));
            }
            return getRemoveNode.getValue(object);
        }

        protected final Object getHasFunction(Object object) {
            if (getHasNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getHasNode = insert(PropertyGetNode.create(Strings.HAS, false, getContext()));
            }
            return getHasNode.getValue(object);
        }

        protected final Object constructSet(Object... arguments) {
            Object ctr = getRealm().getSetConstructor();
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

        @Specialization
        protected JSDynamicObject union(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode) {
            JSDynamicObject newSet = (JSDynamicObject) constructSet(set);
            Object adder = getAddFunction(newSet);
            addEntryFromIterable(newSet, iterable, adder, getIteratorNode);
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

        public JSSetIntersectionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject intersection(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile hasError) {
            JSDynamicObject newSet = (JSDynamicObject) constructSet();
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                hasError.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            Object adder = getAddFunction(newSet);
            if (!isCallable(adder)) {
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
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
            } catch (AbstractTruffleException ex) {
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

        public JSSetDifferenceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject difference(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile removerError) {
            JSDynamicObject newSet = (JSDynamicObject) constructSet(set);
            Object remover = getRemoveFunction(newSet);
            if (!isCallable(remover)) {
                removerError.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
            try {
                while (true) {
                    Object next = iteratorStepNode.execute(iteratorRecord);
                    if (next == Boolean.FALSE) {
                        return newSet;
                    }
                    Object nextValue = iteratorValueNode.execute(next);
                    call(remover, newSet, nextValue);
                }
            } catch (AbstractTruffleException ex) {
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

        public JSSetSymmetricDifferenceNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject symmetricDifference(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile removerError) {
            JSDynamicObject newSet = (JSDynamicObject) constructSet(set);
            Object remover = getRemoveFunction(newSet);
            if (!isCallable(remover)) {
                removerError.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            Object adder = getAddFunction(newSet);

            if (!isCallable(adder)) {
                // unreachable due to constructor add
                adderError.enter();
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
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
            } catch (AbstractTruffleException ex) {
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

        public JSSetIsSubsetOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Boolean isSubsetOf(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile needCreateNewBranch,
                        @Cached InlinedBranchProfile isObjectError) {
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, set);
            if (!JSRuntime.isObject(iterable)) {
                isObjectError.enter(this);
                throw Errors.createTypeErrorNotIterable(iterable, this);
            }
            JSDynamicObject otherSet = (JSDynamicObject) iterable;
            Object hasCheck = getHasFunction(otherSet);
            if (!isCallable(hasCheck)) {
                needCreateNewBranch.enter(this);
                otherSet = (JSDynamicObject) constructSet();
                addEntryFromIterable(otherSet, iterable, getAddFunction(otherSet), getIteratorNode);
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
            } catch (AbstractTruffleException ex) {
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

        public JSSetIsSupersetOfNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Boolean isSupersetOf(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile notCallableError) {
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                notCallableError.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
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
            } catch (AbstractTruffleException ex) {
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

        public JSSetIsDisjointFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Boolean isDisjointFrom(JSSetObject set, Object iterable,
                        @Cached(inline = true) GetIteratorNode getIteratorNode,
                        @Cached InlinedBranchProfile notCallableError) {
            Object hasCheck = getHasFunction(set);
            if (!isCallable(hasCheck)) {
                notCallableError.enter(this);
                throw Errors.createTypeErrorCallableExpected();
            }
            IteratorRecord iteratorRecord = getIteratorNode.execute(this, iterable);
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
            } catch (AbstractTruffleException ex) {
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

        @Specialization(guards = {"isCallable.executeBoolean(callback)"}, limit = "1")
        protected Object forEachFunction(JSSetObject thisObj, JSDynamicObject callback, Object thisArg,
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
        @Specialization(guards = {"!isCallable.executeBoolean(callback)"}, limit = "1")
        protected static Object forEachFunctionNoFunction(JSSetObject thisObj, Object callback, Object thisArg,
                        @Cached @Shared("isCallable") @SuppressWarnings("unused") IsCallableNode isCallable) {
            throw Errors.createTypeErrorCallableExpected();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static Object forEachFunctionNoSet(Object thisObj, Object callback, Object thisArg) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    /**
     * Implementation of the Set.prototype.size getter.
     */
    public abstract static class SetGetSizeNode extends JSBuiltinNode {

        public SetGetSizeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static int doSet(JSSetObject thisObj) {
            return JSSet.getSetSize(thisObj);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isJSSet(thisObj)"})
        protected static int notSet(@SuppressWarnings("unused") Object thisObj) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    public abstract static class CreateSetIteratorNode extends JSBuiltinNode {
        private final int iterationKind;

        protected CreateSetIteratorNode(JSContext context, JSBuiltin builtin, int iterationKind) {
            super(context, builtin);
            this.iterationKind = iterationKind;
        }

        @Specialization
        protected final JSObject doSet(JSSetObject set) {
            return JSSetIterator.create(getContext(), getRealm(), set, JSSet.getInternalSet(set).getEntries(), iterationKind);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected static JSObject doIncompatibleReceiver(Object thisObj) {
            throw Errors.createTypeError("not a Set");
        }
    }

}
