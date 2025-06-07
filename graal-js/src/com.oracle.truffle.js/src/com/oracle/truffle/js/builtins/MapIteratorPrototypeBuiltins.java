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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.builtins.MapIteratorPrototypeBuiltinsFactory.MapIteratorNextNodeGen;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSMapIterator;
import com.oracle.truffle.js.runtime.builtins.JSMapIteratorObject;
import com.oracle.truffle.js.runtime.builtins.JSMapObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSHashMap;

/**
 * Contains functions of the %MapIteratorPrototype% object.
 */
public final class MapIteratorPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<MapIteratorPrototypeBuiltins.MapIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new MapIteratorPrototypeBuiltins();

    protected MapIteratorPrototypeBuiltins() {
        super(JSMapIterator.PROTOTYPE_NAME, MapIteratorPrototype.class);
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

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        public MapIteratorNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization
        protected final JSObject doMapIterator(JSMapIteratorObject iterator,
                        @Cached InlinedConditionProfile detachedProf,
                        @Cached InlinedConditionProfile doneProf,
                        @Cached InlinedConditionProfile iterKindKey,
                        @Cached InlinedConditionProfile iterKindValue) {
            Object map = iterator.getIteratedObject();
            if (detachedProf.profile(this, map == Undefined.instance)) {
                return createIterResultObjectNode.execute(Undefined.instance, true);
            }

            assert map instanceof JSMapObject;
            JSHashMap.Cursor mapCursor = iterator.getNextIndex();
            int itemKind = iterator.getIterationKind();

            if (doneProf.profile(this, !mapCursor.advance())) {
                iterator.setIteratedObject(Undefined.instance);
                return createIterResultObjectNode.execute(Undefined.instance, true);
            }

            Object elementKey = mapCursor.getKey();
            Object elementValue = mapCursor.getValue();
            Object result;
            if (iterKindKey.profile(this, itemKind == JSRuntime.ITERATION_KIND_KEY)) {
                result = elementKey;
            } else if (iterKindValue.profile(this, itemKind == JSRuntime.ITERATION_KIND_VALUE)) {
                result = elementValue;
            } else {
                assert itemKind == JSRuntime.ITERATION_KIND_KEY_PLUS_VALUE;
                result = JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{elementKey, elementValue});
            }
            return createIterResultObjectNode.execute(result, false);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static JSObject doIncompatibleReceiver(Object iterator) {
            throw Errors.createTypeError("not a Map Iterator");
        }
    }
}
