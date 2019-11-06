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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.MapIteratorPrototypeBuiltinsFactory.MapIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSMap;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

/**
 * Contains functions of the %MapIteratorPrototype% object.
 */
public final class MapIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<MapIteratorPrototypeBuiltins.MapIteratorPrototype> {
    protected MapIteratorPrototypeBuiltins() {
        super(JSMap.ITERATOR_PROTOTYPE_NAME, MapIteratorPrototype.class);
    }

    public enum MapIteratorPrototype implements BuiltinEnum<MapIteratorPrototype> {
        next(0);

        private final int length;

        MapIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, MapIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return MapIteratorNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class MapIteratorNextNode extends JSBuiltinNode {
        @Child private HasHiddenKeyCacheNode isMapIteratorNode;
        @Child private PropertyGetNode getIteratedObjectNode;
        @Child private PropertyGetNode getNextIndexNode;
        @Child private PropertyGetNode getIterationKindNode;
        @Child private PropertySetNode setIteratedObjectNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        private final ConditionProfile detachedProf = ConditionProfile.createBinaryProfile();
        private final ConditionProfile doneProf = ConditionProfile.createBinaryProfile();
        private final ConditionProfile iterKindKey = ConditionProfile.createBinaryProfile();
        private final ConditionProfile iterKindValue = ConditionProfile.createBinaryProfile();

        public MapIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.isMapIteratorNode = HasHiddenKeyCacheNode.create(JSMap.MAP_ITERATION_KIND_ID);
            this.getIteratedObjectNode = PropertyGetNode.createGetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.getNextIndexNode = PropertyGetNode.createGetHidden(JSRuntime.ITERATOR_NEXT_INDEX, context);
            this.getIterationKindNode = PropertyGetNode.createGetHidden(JSMap.MAP_ITERATION_KIND_ID, context);
            this.setIteratedObjectNode = PropertySetNode.createSetHidden(JSRuntime.ITERATED_OBJECT_ID, context);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isMapIterator(iterator)")
        protected DynamicObject doMapIterator(VirtualFrame frame, DynamicObject iterator) {
            Object map = getIteratedObjectNode.getValue(iterator);
            if (detachedProf.profile(map == Undefined.instance)) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            JSHashMap.Cursor mapCursor = (JSHashMap.Cursor) getNextIndexNode.getValue(iterator);
            int itemKind = getIterationKind(iterator);

            if (doneProf.profile(!mapCursor.advance())) {
                setIteratedObjectNode.setValue(iterator, Undefined.instance);
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            Object elementKey = mapCursor.getKey();
            Object elementValue = mapCursor.getValue();
            Object result;
            if (iterKindKey.profile(itemKind == JSRuntime.ITERATION_KIND_KEY)) {
                result = elementKey;
            } else if (iterKindValue.profile(itemKind == JSRuntime.ITERATION_KIND_VALUE)) {
                result = elementValue;
            } else {
                assert itemKind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE;
                result = JSArray.createConstantObjectArray(getContext(), new Object[]{elementKey, elementValue});
            }
            return createIterResultObjectNode.execute(frame, result, false);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected DynamicObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not a Map Iterator");
        }

        protected final boolean isMapIterator(Object thisObj) {
            // If the [[MapIterationKind]] internal slot is present, the others must be as well.
            return isMapIteratorNode.executeHasHiddenKey(thisObj);
        }

        private int getIterationKind(DynamicObject iterator) {
            try {
                return getIterationKindNode.getValueInt(iterator);
            } catch (UnexpectedResultException e) {
                throw Errors.shouldNotReachHere();
            }
        }
    }
}
