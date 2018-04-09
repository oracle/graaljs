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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class DeclareEvalVariableNode extends StatementNode {
    @Child private JavaScriptNode dynamicScopeNode;
    @Child private WriteNode initScopeNode;
    @Child private HasPropertyCacheNode hasProperty;
    @Child private PropertySetNode defineProperty;
    private final JSContext context;
    private final String varName;

    public DeclareEvalVariableNode(JSContext context, String varName, JavaScriptNode dynamicScopeNode, WriteNode writeDynamicScopeNode) {
        this.context = context;
        this.varName = varName;
        this.dynamicScopeNode = dynamicScopeNode;
        this.initScopeNode = writeDynamicScopeNode;
        this.hasProperty = HasPropertyCacheNode.create(varName, context);
        this.defineProperty = PropertySetNode.create(varName, false, context, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        DynamicObject dynamicScope = (DynamicObject) dynamicScopeNode.execute(frame);
        if (dynamicScope == Undefined.instance) {
            // NB: dynamic scope object must not have a prototype (visible to user code)
            Shape shape = context.getEmptyShapePrototypeInObject();
            dynamicScope = JSObject.create(context, shape);
            JSShape.getPrototypeProperty(shape).setSafe(dynamicScope, Null.instance, null);
            // (GR-2060) consider eager initialization of dynamic scope object when
            // the function/block owning it is entered instead of at use (here).
            initScopeNode.executeWrite(frame, dynamicScope);
        }
        assert isValidDynamicScopeObject(dynamicScope);
        if (!hasProperty.hasProperty(dynamicScope)) {
            // must not have the same name declared in frame, it's either there or here
            assert frame.getFrameDescriptor().findFrameSlot(varName) == null;
            defineProperty.setValue(dynamicScope, Undefined.instance);
        }
        return EMPTY;
    }

    private static boolean isValidDynamicScopeObject(DynamicObject dynamicScope) {
        return dynamicScope != Undefined.instance && dynamicScope != Null.instance && dynamicScope != null;
    }

    public String getName() {
        return varName;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return new DeclareEvalVariableNode(context, varName, cloneUninitialized(dynamicScopeNode), (WriteNode) cloneUninitialized((JavaScriptNode) initScopeNode));
    }
}
