/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
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

    private static final Object[] EMPTY_INITIALIZERS = ScriptArray.EMPTY_OBJECT_ARRAY;

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
            this.fieldsNode = PropertyNode.createGetHidden(context, null, JSFunction.CLASS_ELEMENTS_ID);
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

    protected abstract Object executeEvaluated(Object target, Object constructor, ClassElementDefinitionRecord[] fields, Object[] initializers, Object brand);

    @ExplodeLoop(kind = LoopExplosionKind.FULL_UNROLL)
    @Specialization
    protected static Object withFields(Object target, Object constructor, ClassElementDefinitionRecord[] fields, Object[] initializers, Object brand,
                    @Cached(value = "createBrandAddNode(brand)", neverDefault = false) @Shared PrivateFieldAddNode privateBrandAddNode,
                    @Cached("createFieldNodes(fields, context)") InitializeFieldOrAccessorNode[] fieldNodes,
                    @Cached("createCall()") JSFunctionCallNode callInit) {
        privateBrandAdd(target, constructor, fields, initializers, brand, privateBrandAddNode);

        executeInitializers(target, initializers, callInit);

        int size = fieldNodes.length;
        assert size == fields.length;
        for (int i = 0; i < size; i++) {
            fieldNodes[i].defineField(target, fields[i]);
        }
        return target;
    }

    private static void executeInitializers(Object target, Object[] initializers, JSFunctionCallNode callInit) {
        for (Object initializer : initializers) {
            callInit.executeCall(JSArguments.createZeroArg(target, initializer));
        }
    }

    @Specialization
    protected static Object privateBrandAdd(Object target, Object constructor, @SuppressWarnings("unused") Object fields, @SuppressWarnings("unused") Object initializers, Object brand,
                    @Cached(value = "createBrandAddNode(brand)", neverDefault = false) @Shared PrivateFieldAddNode privateBrandAddNode) {
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

    static PrivateFieldAddNode createBrandAddNode(Object brand) {
        CompilerAsserts.neverPartOfCompilation();
        if (brand != Undefined.instance) {
            return PrivateFieldAddNode.create();
        } else {
            return null;
        }
    }

    @NeverDefault
    static InitializeFieldOrAccessorNode[] createFieldNodes(ClassElementDefinitionRecord[] fields, JSContext context) {
        CompilerAsserts.neverPartOfCompilation();
        int size = fields.length;
        InitializeFieldOrAccessorNode[] fieldNodes = new InitializeFieldOrAccessorNode[size];
        for (int i = 0; i < size; i++) {
            ClassElementDefinitionRecord field = fields[i];
            Object key = field.getKey();
            Object initializer = field.getValue();
            boolean isAnonymousFunctionDefinition = field.isAnonymousFunction();
            boolean hasInitializer = initializer != Undefined.instance;
            Node writeNode = null;
            if (field.isAutoAccessor() || (field.isPrivate() && field.isField())) {
                assert field.getBackingStorageKey() != null : key;
                writeNode = PrivateFieldAddNode.create();
            } else if (field.isField()) {
                assert JSRuntime.isPropertyKey(key) : key;
                writeNode = WriteElementNode.create(context, true, true);
            } else if (!field.isStaticBlock()) {
                assert field.isMethod() || field.isAccessor() : field;
                hasInitializer = false;
            }
            JSFunctionCallNode callNode = null;
            if (hasInitializer) {
                callNode = JSFunctionCallNode.createCall();
            }
            fieldNodes[i] = new InitializeFieldOrAccessorNode(writeNode, callNode, isAnonymousFunctionDefinition);
        }
        return fieldNodes;
    }

    static final class InitializeFieldOrAccessorNode extends JavaScriptBaseNode {
        @Child Node writeNode;
        @Child JSFunctionCallNode callNode;
        @Child JSFunctionCallNode callInitializersNode;

        private final boolean isAnonymousFunctionDefinition;

        InitializeFieldOrAccessorNode(Node writeNode, JSFunctionCallNode callNode, boolean isAnonymousFunctionDefinition) {
            this.writeNode = writeNode;
            this.callNode = callNode;
            this.isAnonymousFunctionDefinition = isAnonymousFunctionDefinition;
        }

        void defineField(Object target, ClassElementDefinitionRecord record) {
            Object initValue = Undefined.instance;
            // run default (field or accessor) initializer or static initializer
            if (callNode != null) {
                Object initializer = record.getValue();
                Object key = record.getKey();
                // In the case of an anonymous function definition, the property key is supplied as
                // an extra internal, not user-visible, argument that is used for named evaluation.
                initValue = callNode.executeCall(isAnonymousFunctionDefinition
                                ? JSArguments.create(target, initializer, Undefined.instance, key)
                                : JSArguments.createOneArg(target, initializer, Undefined.instance));
            } else {
                assert record.getValue() == Undefined.instance || record.isMethod() || record.isAccessor() : record;
            }
            if (writeNode != null) {
                // run decorators-defined initializers
                for (int i = 0; i < record.getInitializersCount(); i++) {
                    Object initializer = record.getInitializers()[i];
                    initValue = callExtraInitializer(target, initializer, initValue);
                }
                writeValue(target, record, initValue);
            } else {
                assert (record.isMethod() || record.isAccessor() || record.isStaticBlock()) && record.getInitializersCount() == 0 : record;
            }
        }

        private Object callExtraInitializer(Object target, Object initializer, Object initValue) {
            return getInitializersCallNode().executeCall(JSArguments.createOneArg(target, initializer, initValue));
        }

        private JSFunctionCallNode getInitializersCallNode() {
            if (callInitializersNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callInitializersNode = insert(JSFunctionCallNode.createCall());
            }
            return callInitializersNode;
        }

        private void writeValue(Object target, ClassElementDefinitionRecord record, Object value) {
            if (writeNode instanceof PropertySetNode) {
                ((PropertySetNode) writeNode).setValue(target, value);
            } else if (writeNode instanceof PrivateFieldAddNode) {
                // private field or backing storage of an auto accessor
                assert record.getBackingStorageKey() != null : record.getKey();
                ((PrivateFieldAddNode) writeNode).execute(target, record.getBackingStorageKey(), value);
            } else if (writeNode != null) {
                // public field
                assert JSRuntime.isPropertyKey(record.getKey()) : record.getKey();
                ((WriteElementNode) writeNode).executeWithTargetAndIndexAndValue(target, record.getKey(), value);
            }
        }
    }
}
