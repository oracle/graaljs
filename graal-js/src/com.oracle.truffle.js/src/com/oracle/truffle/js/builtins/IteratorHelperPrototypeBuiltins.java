/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.CreateIterResultObjectNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.HasHiddenKeyCacheNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorCompleteNode;
import com.oracle.truffle.js.nodes.access.IteratorNextNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayObject;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class IteratorHelperPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new IteratorHelperPrototypeBuiltins();

    public static final TruffleString PROTOTYPE_NAME = Strings.constant("IteratorHelper.prototype");

    public static final TruffleString CLASS_NAME = Strings.constant("IteratorHelper");

    public static final TruffleString TO_STRING_TAG = Strings.constant("Iterator Helper");

    public static final HiddenKey TARGET_ID = new HiddenKey("target");
    public static final HiddenKey MAPPER_ID = new HiddenKey("mapper");
    public static final HiddenKey HELPER_TYPE_ID = new HiddenKey("type");
    public static final HiddenKey VALUE_ID = new HiddenKey("value");
    public static final HiddenKey ALIVE_ID = new HiddenKey("alive");

    public enum HelperType {
        map,
        filter,
        take,
        drop,
        indexed,
        flatMap;
    }

    protected IteratorHelperPrototypeBuiltins() {
        super(JSArray.ITERATOR_PROTOTYPE_NAME, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype.class);
    }

    protected static class CreateIteratorHelperNode extends JavaScriptBaseNode {
        @Child private CreateObjectNode.CreateObjectWithPrototypeNode createObjectNode;
        @Child private PropertySetNode setTargetNode;
        @Child private PropertySetNode setHelperTypeNode;
        @Child private PropertySetNode setValueNode;
        @Child private PropertySetNode setMapperNode;
        @Child private PropertySetNode setAliveNode;

        private final JSContext context;

        public CreateIteratorHelperNode(JSContext context) {
            this.context = context;

            createObjectNode = CreateObjectNode.createOrdinaryWithPrototype(context);
            setTargetNode = PropertySetNode.createSetHidden(TARGET_ID, context);
            setHelperTypeNode = PropertySetNode.createSetHidden(HELPER_TYPE_ID, context);
            setValueNode = PropertySetNode.createSetHidden(VALUE_ID, context);
            setAliveNode = PropertySetNode.createSetHidden(ALIVE_ID, context);
            setMapperNode = PropertySetNode.createSetHidden(MAPPER_ID, context);
        }

        public JSDynamicObject execute(IteratorRecord target, HelperType type, int value) {
            JSDynamicObject iterator = createObjectNode.execute(JSRealm.get(this).getIteratorHelperPrototype());
            setTargetNode.setValue(iterator, target);
            setHelperTypeNode.setValue(iterator, type);
            setValueNode.setValueInt(iterator, value);
            return iterator;
        }

        public JSDynamicObject execute(IteratorRecord target, HelperType type, Number value) {
            JSDynamicObject iterator = createObjectNode.execute(JSRealm.get(this).getIteratorHelperPrototype());
            setTargetNode.setValue(iterator, target);
            setHelperTypeNode.setValue(iterator, type);
            setValueNode.setValue(iterator, value);
            return iterator;
        }

        public JSDynamicObject execute(IteratorRecord target, HelperType type, Object mapper) {
            JSDynamicObject iterator = createObjectNode.execute(JSRealm.get(this).getIteratorHelperPrototype());
            setTargetNode.setValue(iterator, target);
            setHelperTypeNode.setValue(iterator, type);
            setAliveNode.setValueBoolean(iterator, false);
            setMapperNode.setValue(iterator, mapper);

            return iterator;
        }

        public static CreateIteratorHelperNode create(JSContext context) {
            return new CreateIteratorHelperNode(context);
        }
    }

    public enum HelperIteratorPrototype implements BuiltinEnum<IteratorHelperPrototypeBuiltins.HelperIteratorPrototype> {
        next(1),
        return_(0);

        private final int length;

        HelperIteratorPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, IteratorHelperPrototypeBuiltins.HelperIteratorPrototype builtinEnum) {
        switch (builtinEnum) {
            case next:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextNodeGen.create(context, builtin, args().withThis().fixedArgs(1).createArgumentNodes(context));
            case return_:
                return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperReturnNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class IteratorHelperReturnNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private IteratorCloseNode iteratorCloseNode;
        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperReturnNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            iteratorCloseNode = IteratorCloseNode.create(context);
            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object close(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);

            iteratorCloseNode.executeVoid(iterated.getIterator());
            return createIterResultObjectNode.execute(frame, Undefined.instance, true);
        }
    }

    protected abstract static class GetHiddenValueNode extends JavaScriptBaseNode {
        @Child protected HasHiddenKeyCacheNode hasHiddenKeyCacheNode;
        @Child private PropertyGetNode getTargetNode;

        public GetHiddenValueNode(JSContext context, HiddenKey key) {
            hasHiddenKeyCacheNode = HasHiddenKeyCacheNode.create(key);
            getTargetNode = PropertyGetNode.createGetHidden(key, context);
        }

        public abstract Object execute(JSDynamicObject generator);

        @Specialization(guards = "hasHiddenKeyCacheNode.executeHasHiddenKey(generator)")
        protected Object get(JSDynamicObject generator) {
            return getTargetNode.getValue(generator);
        }

        @Specialization(guards = "!hasHiddenKeyCacheNode.executeHasHiddenKey(generator)")
        protected Object incompatible(JSDynamicObject generator) {
            throw Errors.createTypeErrorIncompatibleReceiver(generator);
        }

        public static GetHiddenValueNode create(JSContext context, HiddenKey key) {
            return IteratorHelperPrototypeBuiltinsFactory.GetHiddenValueNodeGen.create(context, key);
        }
    }

    @ImportStatic({HelperType.class})
    public abstract static class IteratorHelperNextNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getHelperTypeNode;

        private final ValueProfile objProfile = ValueProfile.createIdentityProfile();
        private final ValueProfile typeProfile = ValueProfile.createIdentityProfile();

        protected IteratorHelperNextNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getHelperTypeNode = GetHiddenValueNode.create(context, HELPER_TYPE_ID);
        }

        protected HelperType getHelperType(JSObject thisObj) {
            return typeProfile.profile((HelperType) this.getHelperTypeNode.execute(objProfile.profile(thisObj)));
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == map"})
        protected Object nextMap(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextMapNode nextMapNode) {
            return nextMapNode.next(frame, thisObj);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == filter"})
        protected Object nextFilter(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextFilterNode nextFilterNode) {
            return nextFilterNode.next(frame, thisObj);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == take"})
        protected Object nextTake(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextTakeNode nextTakeNode) {
            return nextTakeNode.next(frame, thisObj);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == drop"})
        protected Object nextDrop(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextDropNode nextDropNode) {
            return nextDropNode.next(frame, thisObj);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == indexed"})
        protected Object nextIndexed(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextIndexedNode nextIndexedNode) {
            return nextIndexedNode.next(frame, thisObj);
        }

        @Specialization(guards = {"isJSObject(thisObj)", "getHelperType(thisObj) == flatMap"})
        protected Object nextFlatMap(VirtualFrame frame, JSObject thisObj, Object value, @Cached("create(getContext(), getBuiltin())") IteratorHelperNextFlatMapNode nextFlatMapNode) {
            return nextFlatMapNode.next(frame, thisObj);
        }

        @Specialization(guards = "!isJSObject(thisObj)")
        protected Object incompatible(Object thisObj, Object value) {
            throw Errors.createTypeErrorGeneratorObjectExpected();
        }
    }

    protected abstract static class IteratorHelperNextMapNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getMapperNode;

        @Child private IteratorNextNode iteratorNextNode;
        @Child private IteratorCompleteNode iteratorCompleteNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        @Child private JSFunctionCallNode callNode;

        private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

        protected IteratorHelperNextMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getMapperNode = GetHiddenValueNode.create(context, MAPPER_ID);

            iteratorNextNode = IteratorNextNode.create();
            iteratorCompleteNode = IteratorCompleteNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);

            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);
            Object mapper = getMapperNode.execute(thisObj);

            Object next = iteratorNextNode.execute(iterated);
            boolean done = iteratorCompleteNode.execute(next);
            if (loopProfile.profile(done)) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            Object value = iteratorValueNode.execute(next);
            Object mapped = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, mapper, value));
            return createIterResultObjectNode.execute(frame, mapped, false);
        }

        protected static IteratorHelperNextMapNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextMapNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

    protected abstract static class IteratorHelperNextFilterNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getMapperNode;

        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        @Child private JSFunctionCallNode callNode;
        @Child private JSToBooleanNode toBooleanNode;

        protected IteratorHelperNextFilterNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getMapperNode = GetHiddenValueNode.create(context, MAPPER_ID);

            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);

            callNode = JSFunctionCallNode.createCall();
            toBooleanNode = JSToBooleanNode.create();
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);
            Object filterer = getMapperNode.execute(thisObj);

            while (true) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == (Boolean) false) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }

                Object value = iteratorValueNode.execute(next);
                Object selected = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, filterer, value));
                if (toBooleanNode.executeBoolean(selected)) {
                    return createIterResultObjectNode.execute(frame, value, false);
                }
            }
        }

        protected static IteratorHelperNextFilterNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextFilterNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

    protected abstract static class IteratorHelperNextTakeNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getValueNode;
        @Child private PropertySetNode setValueNode;

        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperNextTakeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getValueNode = GetHiddenValueNode.create(context, VALUE_ID);
            setValueNode = PropertySetNode.createSetHidden(VALUE_ID, context);

            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);
            Number remaining = (Number) getValueNode.execute(thisObj);
            if (remaining.doubleValue() == 0) {
                Object result = iteratorCloseNode.execute(iterated.getIterator(), Undefined.instance);
                return createIterResultObjectNode.execute(frame, result, true);
            }

            if (Double.isFinite(remaining.doubleValue())) {
                setValueNode.setValue(thisObj, remaining.doubleValue() - 1); // TODO: Optimize?
            }

            Object next = iteratorStepNode.execute(iterated);
            if (next == (Boolean) false) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            Object value = iteratorValueNode.execute(next);
            return createIterResultObjectNode.execute(frame, value, false);
        }

        protected static IteratorHelperNextTakeNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextTakeNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

    protected abstract static class IteratorHelperNextDropNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getValueNode;
        @Child private PropertySetNode setValueNode;

        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperNextDropNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getValueNode = GetHiddenValueNode.create(context, VALUE_ID);
            setValueNode = PropertySetNode.createSetHidden(VALUE_ID, context);

            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);
            double remaining = ((Number) getValueNode.execute(thisObj)).doubleValue(); // TODO: Optimize?
            while (remaining > 0) {
                Object next = iteratorStepNode.execute(iterated);
                if (next == (Boolean) false) {
                    return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                }

                if (Double.isFinite(remaining)) {
                    remaining--;
                }
            }

            setValueNode.setValue(thisObj, 0);

            Object next = iteratorStepNode.execute(iterated);
            if (next == (Boolean) false) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            Object value = iteratorValueNode.execute(next);
            return createIterResultObjectNode.execute(frame, value, false);
        }

        protected static IteratorHelperNextDropNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextDropNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

    protected abstract static class IteratorHelperNextIndexedNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getValueNode;
        @Child private PropertySetNode setValueNode;

        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;

        protected IteratorHelperNextIndexedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getValueNode = GetHiddenValueNode.create(context, VALUE_ID);
            setValueNode = PropertySetNode.createSetHidden(VALUE_ID, context);

            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            int index = (int) getValueNode.execute(thisObj);
            IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);

            Object next = iteratorStepNode.execute(iterated);
            if (next == (Boolean) false) {
                return createIterResultObjectNode.execute(frame, Undefined.instance, true);
            }

            Object value = iteratorValueNode.execute(next);
            JSArrayObject pair = JSArray.createConstantObjectArray(getContext(), getRealm(), new Object[]{index, value});
            setValueNode.setValue(thisObj, index + 1);

            return createIterResultObjectNode.execute(frame, pair, false);
        }

        protected static IteratorHelperNextIndexedNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextIndexedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

    protected abstract static class IteratorHelperNextFlatMapNode extends JSBuiltinNode {
        @Child private GetHiddenValueNode getTargetNode;
        @Child private GetHiddenValueNode getMapperNode;
        @Child private GetHiddenValueNode getValueNode;
        @Child private PropertySetNode setValueNode;
        @Child private GetHiddenValueNode getAliveNode;
        @Child private PropertySetNode setAliveNode;

        @Child private IteratorStepNode iteratorStepNode;
        @Child private IteratorValueNode iteratorValueNode;
        @Child private IteratorCloseNode iteratorCloseNode;

        @Child private CreateIterResultObjectNode createIterResultObjectNode;
        @Child private GetIteratorNode getIteratorNode;
        @Child private JSFunctionCallNode callNode;

        protected IteratorHelperNextFlatMapNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);

            getTargetNode = GetHiddenValueNode.create(context, TARGET_ID);
            getMapperNode = GetHiddenValueNode.create(context, MAPPER_ID);
            getValueNode = GetHiddenValueNode.create(context, VALUE_ID);
            setValueNode = PropertySetNode.createSetHidden(VALUE_ID, context);
            getAliveNode = GetHiddenValueNode.create(context, ALIVE_ID);
            setAliveNode = PropertySetNode.createSetHidden(ALIVE_ID, context);

            iteratorStepNode = IteratorStepNode.create(context);
            iteratorValueNode = IteratorValueNode.create(context);
            iteratorCloseNode = IteratorCloseNode.create(context);

            createIterResultObjectNode = CreateIterResultObjectNode.create(context);
            getIteratorNode = GetIteratorNode.create(context);

            callNode = JSFunctionCallNode.createCall();
        }

        @Specialization(guards = "isJSObject(thisObj)")
        public Object next(VirtualFrame frame, JSObject thisObj) {
            Object mapper = getMapperNode.execute(thisObj);
            boolean innerAlive = (boolean) getAliveNode.get(thisObj);

            while (true) {
                if (innerAlive) {
                    IteratorRecord iterated = (IteratorRecord) getValueNode.execute(thisObj);
                    Object next = iteratorStepNode.execute(iterated);
                    if (next == (Boolean) false) {
                        innerAlive = false;
                        setAliveNode.setValueBoolean(thisObj, false);
                        continue;
                    }

                    Object value = iteratorValueNode.execute(next);
                    return createIterResultObjectNode.execute(frame, value, false);
                } else {
                    IteratorRecord iterated = (IteratorRecord) getTargetNode.execute(thisObj);
                    Object next = iteratorStepNode.execute(iterated);
                    if (next == (Boolean) false) {
                        setAliveNode.setValueBoolean(thisObj, false);
                        return createIterResultObjectNode.execute(frame, Undefined.instance, true);
                    }

                    Object value = iteratorValueNode.execute(next);
                    Object mapped = callNode.executeCall(JSArguments.createOneArg(Undefined.instance, mapper, value));
                    IteratorRecord innerIterator = getIteratorNode.execute(mapped);
                    setValueNode.setValue(thisObj, innerIterator);
                    innerAlive = true;
                    setAliveNode.setValueBoolean(thisObj, true);
                }
            }
        }

        protected static IteratorHelperNextFlatMapNode create(JSContext context, JSBuiltin builtin) {
            return IteratorHelperPrototypeBuiltinsFactory.IteratorHelperNextFlatMapNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
    }

}
