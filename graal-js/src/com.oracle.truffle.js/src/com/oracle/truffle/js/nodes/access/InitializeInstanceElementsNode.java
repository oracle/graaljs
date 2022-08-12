/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.access;

import java.util.Set;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * InitializeInstanceElements (O, constructor).
 *
 * Defines class instance fields using the provided field records from the constructor. For fields
 * with an initializer, the initializer function is called to obtain the initial value and, if it's
 * an anonymous function definition, its function name is set to the (computed) field name. Also
 * performs PrivateBrandAdd(O, constructor.[[PrivateBrand]]) if the brand is not undefined.
 *
 * Relies on the following per-class invariants:
 * <ul>
 * <li>The number of instance fields is constant.
 * <li>For each field index, the key will either always or never be a private name.
 * <li>For each field index, an initializer will either always or never be present.
 * <li>For each field index, [[IsAnonymousFunctionDefinition]] will never change.
 * <li>The [[Fields]] slot will either always or never be present.
 * <li>The [[PrivateBrand]] slot will either always or never be present.
 * </ul>
 *
 * This node is also used to define static fields ({@link #executeStaticElements}).
 */
public abstract class InitializeInstanceElementsNode extends JavaScriptNode {

    private static final JSFunctionObject[] EMPTY_INITIALIZERS = new JSFunctionObject[0];

    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode constructorNode;
    @Child @Executed(with = "constructorNode") protected JSTargetableNode fieldsNode;
    @Child @Executed(with = "constructorNode") protected JSTargetableNode initializersNode;
    @Child @Executed(with = "constructorNode") protected JSTargetableNode brandNode;
    protected final JSContext context;

    protected InitializeInstanceElementsNode(JSContext context, JavaScriptNode targetNode, JavaScriptNode constructorNode) {
        this.context = context;
        this.targetNode = targetNode;
        this.constructorNode = constructorNode;
        if (constructorNode != null) {
            this.fieldsNode = PropertyNode.createGetHidden(context, null, JSFunction.CLASS_FIELDS_ID);
            this.brandNode = PropertyNode.createGetHidden(context, null, JSFunction.PRIVATE_BRAND_ID);
            this.initializersNode = PropertyNode.createGetHidden(context, null, JSFunction.CLASS_INITIALIZERS_ID);
        }
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode targetNode, JavaScriptNode constructorNode) {
        return InitializeInstanceElementsNodeGen.create(context, targetNode, constructorNode);
    }

    public static InitializeInstanceElementsNode create(JSContext context) {
        return InitializeInstanceElementsNodeGen.create(context, null, null);
    }

    public final Object executeStaticElements(Object targetConstructor, ClassElementDefinitionRecord[] staticElements) {
        return executeEvaluated(targetConstructor, Undefined.instance, staticElements, EMPTY_INITIALIZERS, Undefined.instance);
    }

    protected abstract Object executeEvaluated(Object target, Object constructor, ClassElementDefinitionRecord[] fields, JSFunctionObject[] initializers, Object brand);

    @ExplodeLoop(kind = LoopExplosionKind.FULL_UNROLL)
    @Specialization
    protected static Object withFields(Object target, Object constructor, ClassElementDefinitionRecord[] fields, JSFunctionObject[] initializers, Object brand,
                    @Cached("createBrandAddNode(brand, context)") @Shared("privateBrandAdd") PrivateFieldAddNode privateBrandAddNode,
                    @Cached("createFieldNodes(fields, context)") DefineFieldNode[] fieldNodes,
                    @Cached("createCall()") JSFunctionCallNode callInit) {
        privateBrandAdd(target, constructor, fields, initializers, brand, privateBrandAddNode);

        executeInitializers(target, initializers, callInit);

        int size = fieldNodes.length;
        assert size == fields.length;
        for (int i = 0; i < size; i++) {
            ClassElementDefinitionRecord record = fields[i];
            // run default initializer
            Object initValue = fieldNodes[i].defineField(target, record);
            // run decorators-defined initializers
            Object[] fieldInitializers = record.getInitializers();
            for (Object initializer : fieldInitializers) {
                initValue = fieldNodes[i].defineFieldWithInitializer(target, record, initializer, initValue);
            }
        }
        return target;
    }

    private static void executeInitializers(Object target, JSFunctionObject[] initializers, JSFunctionCallNode callInit) {
        for (JSFunctionObject initializer : initializers) {
            callInit.executeCall(JSArguments.createZeroArg(target, initializer));
        }
    }

    @Specialization
    protected static Object privateBrandAdd(Object target, Object constructor, @SuppressWarnings("unused") Object fields, @SuppressWarnings("unused") Object initializers, Object brand,
                    @Cached("createBrandAddNode(brand, context)") @Shared("privateBrandAdd") PrivateFieldAddNode privateBrandAddNode) {
        // If constructor.[[PrivateBrand]] is not undefined,
        // Perform ? PrivateBrandAdd(O, constructor.[[PrivateBrand]]).
        assert (privateBrandAddNode != null) == (brand != Undefined.instance);
        if (privateBrandAddNode != null) {
            privateBrandAddNode.execute(target, brand, constructor);
        }
        return target;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, cloneUninitialized(targetNode, materializedTags), cloneUninitialized(constructorNode, materializedTags));
    }

    static PrivateFieldAddNode createBrandAddNode(Object brand, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        if (brand != Undefined.instance) {
            return PrivateFieldAddNode.create(context);
        } else {
            return null;
        }
    }

    static DefineFieldNode[] createFieldNodes(ClassElementDefinitionRecord[] fields, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        int size = fields.length;
        DefineFieldNode[] fieldNodes = new DefineFieldNode[size];
        for (int i = 0; i < size; i++) {
            ClassElementDefinitionRecord field = fields[i];
            Object key = field.getKey();
            Object initializer = field.getValue();
            boolean isAnonymousFunctionDefinition = (boolean) field.isAnonymousFunction();
            Node writeNode = null;
            if (field instanceof ClassElementDefinitionRecord.AutoAccessor) {
                writeNode = DynamicObjectLibrary.getFactory().createDispatched(5);
            } else if (key instanceof HiddenKey) {
                writeNode = PrivateFieldAddNode.create(context);
            } else if (key != null) {
                writeNode = WriteElementNode.create(context, true, true);
            }
            JSFunctionCallNode callNode = null;
            if (initializer != Undefined.instance) {
                callNode = JSFunctionCallNode.createCall();
            }
            fieldNodes[i] = new DefineFieldNode(writeNode, callNode, isAnonymousFunctionDefinition);
        }
        return fieldNodes;
    }

    static final class DefineFieldNode extends JavaScriptBaseNode {
        @Child Node writeNode;
        @Child JSFunctionCallNode callNode;
        @Child JSFunctionCallNode callInitializersNode;

        private final boolean isAnonymousFunctionDefinition;

        DefineFieldNode(Node writeNode, JSFunctionCallNode callNode, boolean isAnonymousFunctionDefinition) {
            this.writeNode = writeNode;
            this.callNode = callNode;
            this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        }

        Object defineField(Object target, ClassElementDefinitionRecord record) {
            assert (callNode != null) == (record.getValue() != Undefined.instance);
            Object value = Undefined.instance;
            if (callNode != null) {
                Object initializer = record.getValue();
                Object key = record.getKey();
                value = callNode.executeCall(
                                isAnonymousFunctionDefinition ? JSArguments.create(target, initializer, Undefined.instance, key) : JSArguments.createOneArg(target, initializer, Undefined.instance));
            }
            return writeValue(target, record, value);
        }

        Object defineFieldWithInitializer(Object target, ClassElementDefinitionRecord record, Object initializer, Object initValue) {
            assert record.getInitializers().length > 0;
            Object value = getInitializersCallNode().executeCall(
                            isAnonymousFunctionDefinition ? JSArguments.create(target, initializer, initValue, record.getKey()) : JSArguments.createOneArg(target, initializer, initValue));
            return writeValue(target, record, value);
        }

        private JSFunctionCallNode getInitializersCallNode() {
            if (callInitializersNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callInitializersNode = insert(JSFunctionCallNode.createCall());
            }
            return callInitializersNode;
        }

        private Object writeValue(Object target, ClassElementDefinitionRecord record, Object value) {
            if (writeNode instanceof PropertySetNode) {
                ((PropertySetNode) writeNode).setValue(target, value);
            } else if (writeNode instanceof PrivateFieldAddNode) {
                assert record.getKey() instanceof HiddenKey : record.getKey();
                ((PrivateFieldAddNode) writeNode).execute(target, record.getKey(), value);
            } else if (writeNode instanceof DynamicObjectLibrary) {
                ClassElementDefinitionRecord.AutoAccessor autoAccessor = (ClassElementDefinitionRecord.AutoAccessor) record;
                ((DynamicObjectLibrary) writeNode).put((DynamicObject) target, autoAccessor.getBackingStorageKey(), value);
            } else if (writeNode != null) {
                assert JSRuntime.isPropertyKey(record.getKey()) : record.getKey();
                ((WriteElementNode) writeNode).executeWithTargetAndIndexAndValue(target, record.getKey(), value);
            }
            return value;
        }
    }
}
