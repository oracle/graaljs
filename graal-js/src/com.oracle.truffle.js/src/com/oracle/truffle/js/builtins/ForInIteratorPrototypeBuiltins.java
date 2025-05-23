/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.builtins.ForInIteratorPrototypeBuiltinsFactory.ForInIteratorPrototypeNextNodeGen;
import com.oracle.truffle.js.builtins.helper.ListGetNode;
import com.oracle.truffle.js.builtins.helper.ListSizeNode;
import com.oracle.truffle.js.nodes.JSGuards;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.nodes.access.HasOnlyShapePropertiesNode;
import com.oracle.truffle.js.nodes.access.JSGetOwnPropertyNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSForInIterator;
import com.oracle.truffle.js.runtime.builtins.JSForInIteratorObject;
import com.oracle.truffle.js.runtime.builtins.JSObjectPrototype;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Functions of the %ForInIteratorPrototype% object.
 */
public final class ForInIteratorPrototypeBuiltins extends JSBuiltinsContainer.Switch {
    public static final JSBuiltinsContainer BUILTINS = new ForInIteratorPrototypeBuiltins();

    protected ForInIteratorPrototypeBuiltins() {
        super(JSForInIterator.PROTOTYPE_NAME);
        defineFunction(Strings.NEXT, 0);
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget) {
        if (Strings.equals(Strings.NEXT, builtin.getName())) {
            return ForInIteratorPrototypeNextNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class ForInIteratorPrototypeNextNode extends JSBuiltinNode {
        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private GetPrototypeNode getPrototypeNode;
        @Child private JSGetOwnPropertyNode getOwnPropertyNode;

        private static final Object DONE = null;
        private static final int MAX_PROTO_DEPTH = 1000;

        public ForInIteratorPrototypeNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            this.getPrototypeNode = GetPrototypeNode.create();
            this.getOwnPropertyNode = JSGetOwnPropertyNode.create(false);
        }

        @Specialization
        protected final JSObject next(VirtualFrame frame, JSForInIteratorObject state,
                        @Cached InlinedConditionProfile valuesProfile,
                        @Cached InlinedBranchProfile errorBranch,
                        @Cached InlinedBranchProfile growProfile,
                        @Cached InlinedConditionProfile fastOwnKeysProfile,
                        @Cached InlinedConditionProfile sameShapeProfile,
                        @Cached ListGetNode listGet,
                        @Cached ListSizeNode listSize,
                        @Cached HasOnlyShapePropertiesNode hasOnlyShapePropertiesNode) {
            Object nextValue = findNext(state,
                            errorBranch, growProfile, fastOwnKeysProfile, sameShapeProfile, listGet, listSize, hasOnlyShapePropertiesNode);
            boolean done = nextValue == DONE;
            if (done) {
                nextValue = Undefined.instance;
            } else {
                if (valuesProfile.profile(this, state.iterateValues)) {
                    nextValue = JSObject.get(state.object, nextValue);
                } else {
                    assert JSGuards.isString(nextValue);
                }
            }
            return createIterResultObjectNode.execute(frame, nextValue, done);
        }

        private Object findNext(JSForInIteratorObject state,
                        InlinedBranchProfile errorBranch, InlinedBranchProfile growProfile, InlinedConditionProfile fastOwnKeysProfile, InlinedConditionProfile sameShapeProfile,
                        ListGetNode listGet, ListSizeNode listSize, HasOnlyShapePropertiesNode hasOnlyShapePropertiesNode) {
            for (;;) {
                JSDynamicObject object = state.object;
                if (!state.objectWasVisited) {
                    JSClass jsclass = JSObject.getJSClass(object);
                    Shape objectShape = object.getShape();
                    boolean fastOwnKeys;
                    List<?> list;
                    int size;
                    if (fastOwnKeysProfile.profile(this, JSConfig.FastOwnKeys && hasOnlyShapePropertiesNode.execute(object, jsclass))) {
                        fastOwnKeys = true;
                        // if the object does not have enumerable properties, no need to enumerate
                        list = JSShape.getPropertiesIfHasEnumerablePropertyNames(objectShape);
                        size = list.size();
                    } else {
                        fastOwnKeys = false;
                        list = jsclass.ownPropertyKeys(object);
                        size = listSize.execute(list);
                    }
                    state.objectShape = objectShape;
                    state.remainingKeys = list;
                    state.remainingKeysSize = size;
                    state.remainingKeysIndex = 0;
                    state.fastOwnKeys = fastOwnKeys;
                    state.objectWasVisited = true;
                }

                assert state.remainingKeysSize == state.remainingKeys.size();
                while (state.remainingKeysIndex < state.remainingKeysSize) {
                    final Object next = listGet.execute(state.remainingKeys, state.remainingKeysIndex++);
                    final Object key = getKey(next);
                    if (!JSGuards.isString(key)) {
                        continue;
                    }
                    if (state.isVisitedKey(key)) {
                        continue;
                    }

                    if (fastOwnKeysProfile.profile(this, state.fastOwnKeys && next instanceof Property)) {
                        if (sameShapeProfile.profile(this, state.objectShape == object.getShape())) {
                            // same shape => can skip GetOwnProperty
                            if (JSProperty.isEnumerable((Property) next)) {
                                return key;
                            } else {
                                continue;
                            }
                        } else {
                            // shape has changed => must perform GetOwnProperty
                            addPreviouslyVisitedKeys(state);
                            state.fastOwnKeys = false;
                            // fall through
                        }
                    }

                    PropertyDescriptor desc = getOwnPropertyNode.execute(object, key);
                    // desc can be null if obj is a Proxy or the property has been deleted
                    if (desc != null) {
                        state.addVisitedKey(key);
                        if (desc.getEnumerable()) {
                            return key;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }

                JSDynamicObject proto = getPrototypeNode.execute(object);
                if (tryFastForwardImmutablePrototype(proto, hasOnlyShapePropertiesNode)) {
                    proto = Null.instance;
                }
                state.object = proto;
                state.objectWasVisited = false;
                if (proto == Null.instance) {
                    return DONE;
                } else {
                    if (fastOwnKeysProfile.profile(this, state.fastOwnKeys)) {
                        state.addVisitedShape(state.objectShape, this, growProfile);
                    } else {
                        // check for Proxy prototype cycles
                        if (++state.protoDepth > MAX_PROTO_DEPTH) {
                            errorBranch.enter(this);
                            throw Errors.createRangeErrorStackOverflow();
                        }
                    }
                }
            }
        }

        private static Object getKey(final Object next) {
            return next instanceof Property ? ((Property) next).getKey() : next;
        }

        @TruffleBoundary
        private static void addPreviouslyVisitedKeys(JSForInIteratorObject state) {
            for (int i = 0; i < state.remainingKeysIndex - 1; i++) {
                state.addVisitedKey(getKey(state.remainingKeys.get(i)));
            }
        }

        private static boolean tryFastForwardImmutablePrototype(JSDynamicObject proto, HasOnlyShapePropertiesNode hasOnlyShapePropertiesNode) {
            if (proto == Null.instance) {
                return false;
            }
            // If none of the remaining prototypes have enumerable properties, we are done.
            // If the object has an immutable prototype (i.e., Object.prototype, Module Namespace),
            // its prototype is always null and we can skip [[GetPrototypeOf]]().
            JSClass jsclass = JSObject.getJSClass(proto);
            if (jsclass == JSObjectPrototype.INSTANCE && hasOnlyShapePropertiesNode.execute(proto, jsclass) && JSShape.getEnumerablePropertyNames(proto.getShape()).isEmpty()) {
                assert JSObject.getPrototype(proto) == Null.instance;
                return true;
            } else {
                return false;
            }
        }

        @Fallback
        protected final JSObject invalidReceiver(Object thisObj) {
            throw Errors.createTypeErrorIncompatibleReceiver(getBuiltin().getName(), thisObj);
        }
    }

}
