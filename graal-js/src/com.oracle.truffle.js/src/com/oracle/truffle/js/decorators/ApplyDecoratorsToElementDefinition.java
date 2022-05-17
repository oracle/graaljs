/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.decorators;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.decorators.CreateDecoratorContextObjectNode.Record;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

import java.util.List;

@ImportStatic(ClassElementDefinitionRecord.class)
public abstract class ApplyDecoratorsToElementDefinition extends Node {

    protected final JSContext context;
    private final boolean isStatic;
    private final BranchProfile errorProfile = BranchProfile.create();

    public ApplyDecoratorsToElementDefinition(JSContext context, boolean isStatic) {
        this.isStatic = isStatic;
        this.context = context;
    }

    public abstract void executeDecorator(VirtualFrame frame,
                    JSDynamicObject proto,
                    ClassElementDefinitionRecord record,
                    List<Object> extraInitializers);

    public static ApplyDecoratorsToElementDefinition create(JSContext context, boolean isStatic) {
        return ApplyDecoratorsToElementDefinitionNodeGen.create(context, isStatic);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!hasDecorators(record)")
    public void noDecorators(VirtualFrame frame, JSDynamicObject proto, ClassElementDefinitionRecord record, List<Object> extraInitializers) {
        // NOP
    }

    @Specialization(guards = {"record.isField()", "hasDecorators(record)"})
    public void decorateField(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, List<Object> extraInitializers,
                    @Cached("createDecoratorContextObjectNode()") CreateDecoratorContextObjectNode createDecoratorContextNode,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached("create()") IsCallableNode isCallableNode) {
        for (Object decorator : record.getDecorators()) {
            Record state = new Record();
            JSDynamicObject decoratorContext = createDecoratorContextNode.executeContext(frame, record, extraInitializers, state);
            Object value = Undefined.instance;
            Object newValue = callNode.executeCall(JSArguments.create(Undefined.instance, decorator, value, decoratorContext));
            state.finished = true;
            if (isCallableNode.executeBoolean(newValue)) {
                record.appendInitializer(newValue);
            } else {
                checkUndefined(newValue);
            }
        }
        record.cleanDecorator();
    }

    @Specialization(guards = {"record.isMethod()", "hasDecorators(record)"})
    public void decorateMethod(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, List<Object> extraInitializers,
                    @Cached("createDecoratorContextObjectNode()") CreateDecoratorContextObjectNode createDecoratorContextNode,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached("create()") SetFunctionNameNode setFunctionName,
                    @Cached("create()") IsCallableNode isCallableNode) {
        for (Object decorator : record.getDecorators()) {
            Object newValue = executeDecoratorWithContext(frame, record, extraInitializers, createDecoratorContextNode, callNode, decorator);
            if (isCallableNode.executeBoolean(newValue)) {
                if (newValue instanceof JSObject) {
                    // cannot set function name of foreign objects
                    setFunctionName.execute(newValue, record.getKey());
                }
                record.setValue(newValue);
            } else {
                checkUndefined(newValue);
            }
        }
        record.cleanDecorator();
    }

    protected static boolean isGetterOrSetter(ClassElementDefinitionRecord record) {
        return record.isGetter() || record.isSetter();
    }

    @Specialization(guards = {"hasDecorators(record)", "isGetterOrSetter(record)"})
    public void decorateGetterSetter(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, List<Object> extraInitializers,
                    @Cached("createDecoratorContextObjectNode()") CreateDecoratorContextObjectNode createDecoratorContextNode,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached("create()") IsCallableNode isCallableNode,
                    @Cached("create()") SetFunctionNameNode setFunctionNameNode,
                    @Cached("createSymbolToString()") JSToStringNode toStringNode,
                    @Cached("create()") TruffleString.ConcatNode concatNode) {
        for (Object decorator : record.getDecorators()) {
            Object newValue = executeDecoratorWithContext(frame, record, extraInitializers, createDecoratorContextNode, callNode, decorator);
            if (isCallableNode.executeBoolean(newValue)) {
                TruffleString tsKey = toStringNode.executeString(record.getKey());
                TruffleString keyName = Strings.concat(concatNode, record.isGetter() ? Strings.GET_SPC : Strings.SET_SPC, tsKey);
                if (newValue instanceof JSObject) {
                    // set function name of JS objects, not foreign ones.
                    setFunctionNameNode.execute(newValue, keyName);
                }
                record.setValue(newValue);
            } else {
                checkUndefined(newValue);
            }
        }
        record.cleanDecorator();
    }

    @Specialization(guards = {"record.isAutoAccessor()", "hasDecorators(record)"})
    public void decorateAuto(VirtualFrame frame, JSDynamicObject proto, ClassElementDefinitionRecord.AutoAccessor record, List<Object> extraInitializers,
                    @Cached("createDecoratorContextObjectNode()") CreateDecoratorContextObjectNode createDecoratorContextNode,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached("createGetterNode()") PropertyGetNode getGetterNode,
                    @Cached("createSetterNode()") PropertyGetNode getSetterNode,
                    @Cached("createInitNode()") PropertyGetNode getInitNode,
                    @Cached("create()") IsCallableNode isCallableNode,
                    @Cached("create(context)") CreateObjectNode createObjectNode,
                    @Cached("create()") IsObjectNode isObjectNode) {
        for (Object decorator : record.getDecorators()) {
            Record state = new Record();
            JSDynamicObject decoratorContext = createDecoratorContextNode.executeContext(frame, record, extraInitializers, state);
            JSDynamicObject value = createObjectNode.execute(frame);
            JSRuntime.createDataPropertyOrThrow(value, Strings.GET, record.getGetter());
            JSRuntime.createDataPropertyOrThrow(value, Strings.SET, record.getSetter());
            Object newValue = callNode.executeCall(JSArguments.create(Undefined.instance, decorator, value, decoratorContext));
            state.finished = true;
            if (isObjectNode.executeBoolean(newValue)) {
                Object newGetter = getGetterNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newGetter)) {
                    record.setGetter(newGetter);
                } else {
                    checkUndefined(newGetter);
                }
                Object newSetter = getSetterNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newSetter)) {
                    record.setSetter(newSetter);
                } else {
                    checkUndefined(newSetter);
                }
                patchAutoAccessor(proto, record);
                Object newInit = getInitNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newInit)) {
                    record.appendInitializer(newInit);
                } else {
                    checkUndefined(newInit);
                }
            } else {
                checkUndefined(newValue);
            }
        }
        record.cleanDecorator();
    }

    protected static boolean hasDecorators(ClassElementDefinitionRecord record) {
        return record.getDecorators() != null && record.getDecorators().length > 0;
    }

    private static Object executeDecoratorWithContext(VirtualFrame frame, ClassElementDefinitionRecord record, List<Object> extraInitializers,
                    CreateDecoratorContextObjectNode createDecoratorContextNode, JSFunctionCallNode callNode, Object decorator) {
        Record state = new Record();
        JSDynamicObject decoratorContext = createDecoratorContextNode.executeContext(frame, record, extraInitializers, state);
        Object value = record.getValue();
        Object newValue = callNode.executeCall(JSArguments.create(Undefined.instance, decorator, value, decoratorContext));
        state.finished = true;
        return newValue;
    }

    @TruffleBoundary
    private static void patchAutoAccessor(JSDynamicObject proto, ClassElementDefinitionRecord elementRecord) {
        int propertyFlags = 0;
        if (JSObject.hasProperty(proto, elementRecord.getKey())) {
            propertyFlags = JSObject.getPropertyFlags(proto, elementRecord.getKey());
        }
        JSDynamicObject getterV = (JSDynamicObject) elementRecord.getGetter();
        JSDynamicObject setterV = (JSDynamicObject) elementRecord.getSetter();
        Accessor newAccessor = new Accessor(getterV, setterV);
        JSObjectUtil.defineAccessorProperty(proto, elementRecord.getKey(), newAccessor, propertyFlags);
    }

    protected CreateDecoratorContextObjectNode createDecoratorContextObjectNode() {
        return CreateDecoratorContextObjectNode.create(context, isStatic);
    }

    protected PropertyGetNode createGetterNode() {
        return PropertyGetNode.create(Strings.GET, context);
    }

    protected PropertyGetNode createSetterNode() {
        return PropertyGetNode.create(Strings.SET, context);
    }

    protected PropertyGetNode createInitNode() {
        return PropertyGetNode.create(Strings.INIT, context);
    }

    public void checkUndefined(Object value) {
        assert value != null;
        if (value != Undefined.instance) {
            errorProfile.enter();
            throw Errors.createTypeErrorWrongDecoratorReturn(this);
        }
    }
}
