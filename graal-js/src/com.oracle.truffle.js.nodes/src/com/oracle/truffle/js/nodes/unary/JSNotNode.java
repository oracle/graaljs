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
package com.oracle.truffle.js.nodes.unary;

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantBooleanNode;
import com.oracle.truffle.js.nodes.access.JSConstantNode.JSConstantIntegerNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.UnaryExpressionTag;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

@NodeInfo(shortName = "!")
public abstract class JSNotNode extends JSUnaryNode {

    protected JSNotNode(JavaScriptNode operand) {
        super(operand);
    }

    public static JavaScriptNode create(JavaScriptNode operand) {
        if (JSTruffleOptions.UseSuperOperations && operand instanceof JSNotNode) {
            // optimize "!!operand", but retain conversion to boolean if operand != boolean
            JSNotNode childNode = (JSNotNode) operand;
            JavaScriptNode childOperand = childNode.getOperand();
            if (childOperand.isResultAlwaysOfType(boolean.class)) {
                return childOperand;
            }
        } else if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantBooleanNode) {
            boolean value = (boolean) operand.execute(null);
            return JSConstantNode.createBoolean(!value);
        } else if (JSTruffleOptions.UseSuperOperations && operand instanceof JSConstantIntegerNode) {
            // used by minifiers, "!0" is shorter than "true"
            int value = (int) operand.execute(null);
            return JSConstantNode.createBoolean(value == 0); // negating it
        }
        return JSNotNodeGen.create(operand);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == UnaryExpressionTag.class) {
            return true;
        } else {
            return super.hasTag(tag);
        }
    }

    @Override
    public InstrumentableNode materializeInstrumentableNodes(Set<Class<? extends Tag>> materializedTags) {
        if (!getOperand().hasSourceSection()) {
            transferSourceSectionAndTags(this, getOperand());
        }
        return this;
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("operator", getClass().getAnnotation(NodeInfo.class).shortName());
    }

    @Specialization
    protected boolean doBoolean(boolean a) {
        return !a;
    }

    @Specialization
    protected boolean doNonBoolean(Object a,
                    @Cached("create()") JSToBooleanNode toBooleanNode) {
        return !toBooleanNode.executeBoolean(a);
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return clazz == boolean.class;
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return JSNotNodeGen.create(cloneUninitialized(getOperand()));
    }
}
