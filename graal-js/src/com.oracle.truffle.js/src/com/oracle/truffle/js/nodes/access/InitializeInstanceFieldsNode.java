/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.ExplodeLoop.LoopExplosionKind;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.function.SetFunctionNameNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * InitializeInstanceFields (O, constructor.[[Fields]]).
 *
 * Relies on the following invariants:
 * <ul>
 * <li>The number of instance fields is constant.
 * <li>For each field index, the key will either always or never be a private name.
 * <li>For each field index, an initializer will either always or never be present.
 * <li>For each field index, [[IsAnonymousFunctionDefinition]] will never change.
 * </ul>
 */
public abstract class InitializeInstanceFieldsNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    protected final JSContext context;

    protected InitializeInstanceFieldsNode(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode) {
        this.context = context;
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode) {
        return InitializeInstanceFieldsNodeGen.create(context, targetNode, sourceNode);
    }

    @ExplodeLoop(kind = LoopExplosionKind.FULL_UNROLL)
    @Specialization
    protected static Object doObject(Object target, Object[] fields,
                    @Cached("createFieldNodes(fields)") FieldNode[] fieldNodes) {
        int size = fieldNodes.length;
        assert size == fields.length;
        for (int i = 0; i < size; i++) {
            Object[] field = (Object[]) fields[i];
            Object key = field[0];
            Object initializer = field[1];
            fieldNodes[i].defineField(target, key, initializer);
        }
        return target;
    }

    @Fallback
    protected static Object doOther(Object target, @SuppressWarnings("unused") Object source) {
        return target;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(targetNode), cloneUninitialized(sourceNode));
    }

    FieldNode[] createFieldNodes(Object[] fields) {
        CompilerAsserts.neverPartOfCompilation();
        int size = fields.length;
        FieldNode[] fieldNodes = new FieldNode[size];
        for (int i = 0; i < size; i++) {
            Object[] field = (Object[]) fields[i];
            Object key = field[0];
            Object initializer = field[1];
            boolean isAnonymousFunctionDefinition = (boolean) field[2];
            JavaScriptBaseNode writeNode;
            if (key instanceof HiddenKey) {
                writeNode = PrivateFieldAddNode.create(context);
            } else {
                writeNode = WriteElementNode.create(context, true, true);
            }
            JSFunctionCallNode callNode = null;
            if (initializer != Undefined.instance) {
                callNode = JSFunctionCallNode.createCall();
            }
            SetFunctionNameNode setFunctionNameNode = null;
            if (isAnonymousFunctionDefinition) {
                setFunctionNameNode = SetFunctionNameNode.create();
            }
            fieldNodes[i] = new FieldNode(writeNode, callNode, setFunctionNameNode);
        }
        return fieldNodes;
    }

    static final class FieldNode extends Node {
        @Child JavaScriptBaseNode writeNode;
        @Child JSFunctionCallNode callNode;
        @Child SetFunctionNameNode setFunctionNameNode;

        FieldNode(JavaScriptBaseNode writeNode, JSFunctionCallNode callNode, SetFunctionNameNode setFunctionNameNode) {
            this.writeNode = writeNode;
            this.callNode = callNode;
            this.setFunctionNameNode = setFunctionNameNode;
        }

        void defineField(Object target, Object key, Object initializer) {
            assert (writeNode instanceof PrivateFieldAddNode) == (key instanceof HiddenKey) && (callNode != null) == (initializer != Undefined.instance);
            Object value = Undefined.instance;
            if (callNode != null) {
                value = callNode.executeCall(JSArguments.createZeroArg(target, initializer));
                if (setFunctionNameNode != null) {
                    setFunctionNameNode.execute(value, key);
                }
            }
            if (writeNode instanceof PrivateFieldAddNode) {
                ((PrivateFieldAddNode) writeNode).execute(target, key, value);
            } else {
                ((WriteElementNode) writeNode).executeWithTargetAndIndexAndValue(target, key, value);
            }
        }
    }
}
