/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.decorators.CreateDecoratorContextObjectNode.DecorationState;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.access.ObjectLiteralNode.ObjectLiteralMemberNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

@ImportStatic({Strings.class})
public abstract class ApplyDecoratorsToElementDefinition extends Node {

    protected final JSContext context;
    @Child CreateDecoratorContextObjectNode createDecoratorContextNode;

    public ApplyDecoratorsToElementDefinition(JSContext context, CreateDecoratorContextObjectNode createDecoratorContextObjectNode) {
        this.context = context;
        this.createDecoratorContextNode = createDecoratorContextObjectNode;
    }

    public abstract void executeDecorator(VirtualFrame frame,
                    JSDynamicObject proto,
                    ClassElementDefinitionRecord record,
                    SimpleArrayList<Object> extraInitializers);

    public static ApplyDecoratorsToElementDefinition create(JSContext context, ObjectLiteralMemberNode member) {
        return ApplyDecoratorsToElementDefinitionNodeGen.create(context, CreateDecoratorContextObjectNode.create(context, member));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!record.hasDecorators()")
    protected static void noDecorators(VirtualFrame frame, JSDynamicObject proto, ClassElementDefinitionRecord record, SimpleArrayList<Object> extraInitializers) {
        // NOP
    }

    @Specialization(guards = {"record.isField()", "record.hasDecorators()"})
    protected void decorateField(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, SimpleArrayList<Object> extraInitializers,
                    @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Shared @Cached InlinedBranchProfile errorBranch) {
        for (Object decorator : record.getDecorators()) {
            Object value = Undefined.instance;
            Object newValue = executeDecoratorWithContext(frame, record, value, extraInitializers, decorator, createDecoratorContextNode, callNode);
            if (isCallableNode.executeBoolean(newValue)) {
                record.addInitializer(newValue);
            } else {
                checkUndefined(newValue, this, errorBranch);
            }
        }
        record.cleanDecorator();
    }

    @Specialization(guards = {"record.isMethod()", "record.hasDecorators()"})
    protected void decorateMethod(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, SimpleArrayList<Object> extraInitializers,
                    @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Shared @Cached InlinedBranchProfile errorBranch,
                    @Shared @Cached SetFunctionNameNode setFunctionName) {
        for (Object decorator : record.getDecorators()) {
            Object value = record.getValue();
            Object newValue = executeDecoratorWithContext(frame, record, value, extraInitializers, decorator, createDecoratorContextNode, callNode);
            if (isCallableNode.executeBoolean(newValue)) {
                setFunctionName.execute(newValue, record.getKey());
                record.setValue(newValue);
            } else {
                checkUndefined(newValue, this, errorBranch);
            }
        }
        record.cleanDecorator();
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"record.isGetter() || record.isSetter()", "record.hasDecorators()"})
    protected void decorateGetterSetter(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, SimpleArrayList<Object> extraInitializers,
                    @Bind Node node,
                    @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Shared @Cached InlinedBranchProfile errorBranch,
                    @Shared @Cached SetFunctionNameNode setFunctionName) {
        for (Object decorator : record.getDecorators()) {
            boolean isGetter = record.isGetter();
            Object value = isGetter ? record.getGetter() : record.getSetter();
            Object newValue = executeDecoratorWithContext(frame, record, value, extraInitializers, decorator, createDecoratorContextNode, callNode);
            if (isCallableNode.executeBoolean(newValue)) {
                setFunctionName.execute(newValue, record.getKey(), isGetter ? Strings.GET : Strings.SET);
                if (isGetter) {
                    record.setGetter(newValue);
                } else {
                    record.setSetter(newValue);
                }
            } else {
                checkUndefined(newValue, node, errorBranch);
            }
        }
        record.cleanDecorator();
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization(guards = {"record.isAutoAccessor()", "record.hasDecorators()"})
    protected void decorateAuto(VirtualFrame frame, @SuppressWarnings("unused") JSDynamicObject proto, ClassElementDefinitionRecord record, SimpleArrayList<Object> extraInitializers,
                    @Bind Node node,
                    @Shared @Cached("createCall()") JSFunctionCallNode callNode,
                    @Shared @Cached IsCallableNode isCallableNode,
                    @Cached("create(GET, context)") PropertyGetNode getGetterNode,
                    @Cached("create(SET, context)") PropertyGetNode getSetterNode,
                    @Cached("create(INIT, context)") PropertyGetNode getInitNode,
                    @Cached("create(context)") CreateObjectNode createObjectNode,
                    @Cached("create(context, GET)") CreateDataPropertyNode createGetDataPropertyNode,
                    @Cached("create(context, SET)") CreateDataPropertyNode createSetDataPropertyNode,
                    @Cached IsObjectNode isObjectNode,
                    @Shared @Cached InlinedBranchProfile errorBranch) {
        for (Object decorator : record.getDecorators()) {
            JSObject value = createObjectNode.execute(frame);
            createGetDataPropertyNode.executeVoid(value, Strings.GET, record.getGetter());
            createSetDataPropertyNode.executeVoid(value, Strings.SET, record.getSetter());
            Object newValue = executeDecoratorWithContext(frame, record, value, extraInitializers, decorator, createDecoratorContextNode, callNode);
            if (isObjectNode.executeBoolean(newValue)) {
                Object newGetter = getGetterNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newGetter)) {
                    record.setGetter(newGetter);
                } else {
                    checkUndefined(newGetter, node, errorBranch);
                }
                Object newSetter = getSetterNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newSetter)) {
                    record.setSetter(newSetter);
                } else {
                    checkUndefined(newSetter, node, errorBranch);
                }
                Object newInit = getInitNode.getValue(newValue);
                if (isCallableNode.executeBoolean(newInit)) {
                    record.addInitializer(newInit);
                } else {
                    checkUndefined(newInit, node, errorBranch);
                }
            } else {
                checkUndefined(newValue, node, errorBranch);
            }
        }
        record.cleanDecorator();
    }

    private static Object executeDecoratorWithContext(VirtualFrame frame, ClassElementDefinitionRecord record, Object value, SimpleArrayList<Object> extraInitializers, Object decorator,
                    CreateDecoratorContextObjectNode createDecoratorContextNode, JSFunctionCallNode callNode) {
        DecorationState state = new DecorationState();
        JSObject decoratorContext = createDecoratorContextNode.executeContext(frame, record, extraInitializers, state);
        Object newValue = callNode.executeCall(JSArguments.create(Undefined.instance, decorator, value, decoratorContext));
        state.finished = true;
        return newValue;
    }

    protected static void checkUndefined(Object value, Node node, InlinedBranchProfile errorProfile) {
        assert value != null;
        if (value != Undefined.instance) {
            errorProfile.enter(node);
            throw Errors.createTypeErrorWrongDecoratorReturn(node);
        }
    }
}
