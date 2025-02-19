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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
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
import com.oracle.truffle.js.nodes.access.GetIteratorFromMethodNode;
import com.oracle.truffle.js.nodes.access.GetSetRecordNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSSet;
import com.oracle.truffle.js.runtime.builtins.JSSetIterator;
import com.oracle.truffle.js.runtime.builtins.JSSetObject;
import com.oracle.truffle.js.runtime.builtins.SetRecord;
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

        protected JSSetNewOperation(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            iteratorStepNode = IteratorStepNode.create();
            iteratorValueNode = IteratorValueNode.create();
            iteratorCloseNode = IteratorCloseNode.create(context);
        }

        protected Object call(Object function, Object target, Object... userArguments) {
            if (callFunctionNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callFunctionNode = insert(JSFunctionCallNode.createCall());
            }
            return callFunctionNode.executeCall(JSArguments.create(target, function, userArguments));
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
        protected JSDynamicObject union(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
            JSHashMap resultSetData = set.getMap().copy();
            while (true) {
                Object next = iteratorStepNode.execute(keysIter);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = normalize(iteratorValueNode.execute(next));
                if (!resultSetData.has(nextValue)) {
                    resultSetData.put(nextValue, PRESENT);
                }
            }
            JSSetObject result = JSSet.create(getContext(), JSRealm.get(this), resultSetData);
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected JSDynamicObject notSet(Object thisObj, Object other) {
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
        protected JSDynamicObject intersection(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached InlinedBranchProfile thisSetSmallerProfile) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            JSHashMap resultSetData = new JSHashMap();
            JSHashMap thisSetData = set.getMap();
            int thisSize = thisSetData.size();
            if (thisSize <= otherRec.size()) {
                thisSetSmallerProfile.enter(this);
                JSHashMap.Cursor cursor = thisSetData.getEntries();
                while (cursor.advance()) {
                    Object e = cursor.getKey();
                    Object inOtherObj = call(otherRec.has(), otherRec.set(), e);
                    boolean inOther = toBooleanNode.executeBoolean(this, inOtherObj);
                    if (inOther && !resultSetData.has(e)) {
                        resultSetData.put(e, PRESENT);
                    }
                }
            } else {
                IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
                while (true) {
                    Object next = iteratorStepNode.execute(keysIter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextValue = normalize(iteratorValueNode.execute(next));
                    boolean alreadyInResult = resultSetData.has(nextValue);
                    boolean inThis = thisSetData.has(nextValue);
                    if (!alreadyInResult && inThis) {
                        resultSetData.put(nextValue, PRESENT);
                    }
                }
            }
            JSSetObject result = JSSet.create(getContext(), JSRealm.get(this), resultSetData);
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected JSDynamicObject notSet(Object thisObj, Object other) {
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
        protected JSDynamicObject difference(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached InlinedBranchProfile thisSetSmallerProfile) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            JSHashMap thisSetData = set.getMap();
            JSHashMap resultSetData = thisSetData.copy();
            int thisSize = thisSetData.size();
            if (thisSize <= otherRec.size()) {
                thisSetSmallerProfile.enter(this);
                JSHashMap.Cursor cursor = thisSetData.getEntries();
                while (cursor.advance()) {
                    Object e = cursor.getKey();
                    Object inOtherObj = call(otherRec.has(), otherRec.set(), e);
                    boolean inOther = toBooleanNode.executeBoolean(this, inOtherObj);
                    if (inOther) {
                        resultSetData.remove(e);
                    }
                }
            } else {
                IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
                while (true) {
                    Object next = iteratorStepNode.execute(keysIter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextValue = normalize(iteratorValueNode.execute(next));
                    resultSetData.remove(nextValue);
                }
            }
            JSSetObject result = JSSet.create(getContext(), JSRealm.get(this), resultSetData);
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected JSDynamicObject notSet(Object thisObj, Object other) {
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
        protected JSDynamicObject symmetricDifference(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
            JSHashMap thisSetData = set.getMap();
            JSHashMap resultSetData = thisSetData.copy();
            while (true) {
                Object next = iteratorStepNode.execute(keysIter);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = normalize(iteratorValueNode.execute(next));
                boolean inResult = resultSetData.has(nextValue);
                if (thisSetData.has(nextValue)) {
                    if (inResult) {
                        resultSetData.remove(nextValue);
                    }
                } else {
                    if (!inResult) {
                        resultSetData.put(nextValue, PRESENT);
                    }
                }
            }
            JSSetObject result = JSSet.create(getContext(), JSRealm.get(this), resultSetData);
            return result;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected JSDynamicObject notSet(Object thisObj, Object other) {
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
        protected boolean isSubsetOf(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            JSHashMap thisSetData = set.getMap();
            int thisSize = thisSetData.size();
            if (thisSize > otherRec.size()) {
                return false;
            }
            JSHashMap.Cursor cursor = thisSetData.getEntries();
            while (cursor.advance()) {
                Object e = cursor.getKey();
                boolean inOther = toBooleanNode.executeBoolean(this, call(otherRec.has(), otherRec.set(), e));
                if (!inOther) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object other) {
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
        protected boolean isSupersetOf(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            JSHashMap thisSetData = set.getMap();
            int thisSize = thisSetData.size();
            if (thisSize < otherRec.size()) {
                return false;
            }
            IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
            while (true) {
                Object next = iteratorStepNode.execute(keysIter);
                if (next == Boolean.FALSE) {
                    break;
                }
                Object nextValue = normalize(iteratorValueNode.execute(next));
                if (!thisSetData.has(nextValue)) {
                    iteratorCloseNode.executeVoid(keysIter.getIterator());
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object other) {
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
        protected boolean isDisjointFrom(JSSetObject set, Object other,
                        @Cached("create(getContext())") GetSetRecordNode getSetRecordNode,
                        @Cached(inline = true) JSToBooleanNode toBooleanNode,
                        @Cached GetIteratorFromMethodNode getIteratorFromMethodNode,
                        @Cached InlinedBranchProfile thisSetSmallerProfile) {
            SetRecord otherRec = getSetRecordNode.execute(other);
            JSHashMap thisSetData = set.getMap();
            int thisSize = thisSetData.size();
            if (thisSize <= otherRec.size()) {
                thisSetSmallerProfile.enter(this);
                JSHashMap.Cursor cursor = thisSetData.getEntries();
                while (cursor.advance()) {
                    Object e = cursor.getKey();
                    boolean inOther = toBooleanNode.executeBoolean(this, call(otherRec.has(), otherRec.set(), e));
                    if (inOther) {
                        return false;
                    }
                }
            } else {
                IteratorRecord keysIter = getIteratorFromMethodNode.execute(this, otherRec.set(), otherRec.keys());
                while (true) {
                    Object next = iteratorStepNode.execute(keysIter);
                    if (next == Boolean.FALSE) {
                        break;
                    }
                    Object nextValue = normalize(iteratorValueNode.execute(next));
                    if (thisSetData.has(nextValue)) {
                        iteratorCloseNode.executeVoid(keysIter.getIterator());
                        return false;
                    }
                }
            }
            return true;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isJSSet(thisObj)")
        protected boolean notSet(Object thisObj, Object other) {
            throw Errors.createTypeErrorSetExpected();
        }
    }

    public abstract static class JSSetForEachNode extends JSBuiltinNode {

        public JSSetForEachNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isCallable.executeBoolean(callback)"})
        protected Object forEachFunction(JSSetObject thisObj, Object callback, Object thisArg,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable,
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
        @Specialization(guards = {"!isCallable.executeBoolean(callback)"})
        protected static Object forEachFunctionNoFunction(JSSetObject thisObj, Object callback, Object thisArg,
                        @Cached @Shared @SuppressWarnings("unused") IsCallableNode isCallable) {
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
