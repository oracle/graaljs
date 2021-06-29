/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.tuples;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Represents the abstract operation IsConcatSpreadable.
 */
public abstract class JSIsConcatSpreadableNode extends JavaScriptBaseNode {

    @Child private PropertyGetNode getSpreadableNode;
    @Child private JSToBooleanNode toBooleanNode;
    @Child private JSIsTupleNode isTupleNode;
    @Child private JSIsArrayNode isArrayNode;

    protected final JSContext context;

    protected JSIsConcatSpreadableNode(JSContext context) {
        super();
        this.context = context;
    }

    public abstract boolean execute(Object operand);

    @Specialization
    protected boolean doObject(Object o) {
        if (!JSRuntime.isObject(o) && !JSRuntime.isTuple(o)) {
            return false;
        }
        if (JSDynamicObject.isJSDynamicObject(o)) {
            Object spreadable = getSpreadableProperty(o);
            if (spreadable != Undefined.instance) {
                return toBoolean(spreadable);
            }
        }
        return isTuple(o) || isArray(o);
    }

    private boolean isArray(Object object) {
        if (isArrayNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isArrayNode = insert(JSIsArrayNode.createIsArrayLike());
        }
        return isArrayNode.execute(object);
    }

    private boolean isTuple(Object object) {
        if (isTupleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isTupleNode = insert(JSIsTupleNode.create());
        }
        return isTupleNode.execute(object);
    }

    private Object getSpreadableProperty(Object obj) {
        if (getSpreadableNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getSpreadableNode = insert(PropertyGetNode.create(Symbol.SYMBOL_IS_CONCAT_SPREADABLE, context));
        }
        return getSpreadableNode.getValue(obj);
    }

    protected boolean toBoolean(Object target) {
        if (toBooleanNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toBooleanNode = insert(JSToBooleanNode.create());
        }
        return toBooleanNode.executeBoolean(target);
    }

    public static JSIsConcatSpreadableNode create(JSContext context) {
        return JSIsConcatSpreadableNodeGen.create(context);
    }
}
