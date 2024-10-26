/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.function;

import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

@SuppressWarnings("hiding")
public class BuiltinArgumentBuilder {
    private int fixedArgumentCount;
    private boolean hasThis;
    private boolean varArgs;
    private boolean newTarget;
    private boolean hasFunction;
    public static final JavaScriptNode[] EMPTY_NODE_ARRAY = new JavaScriptNode[0];

    BuiltinArgumentBuilder() {
    }

    public static BuiltinArgumentBuilder builder() {
        boolean ea = false;
        assert !!(ea = true);
        if (ea) {
            return new BuiltinArgumentBuilder() {
                private int order;

                @Override
                protected void assertOrder(int order) {
                    assert this.order < order;
                    this.order = order;
                }
            };
        }
        return new BuiltinArgumentBuilder();
    }

    public BuiltinArgumentBuilder withThis() {
        return withThis(true);
    }

    public BuiltinArgumentBuilder withThis(boolean hasThis) {
        this.hasThis = hasThis;
        assertOrder(1);
        return this;
    }

    public BuiltinArgumentBuilder function() {
        return function(true);
    }

    public BuiltinArgumentBuilder function(boolean function) {
        this.hasFunction = function;
        assertOrder(2);
        return this;
    }

    public BuiltinArgumentBuilder newTarget() {
        return newTarget(true);
    }

    public BuiltinArgumentBuilder newTarget(boolean newTarget) {
        this.newTarget = newTarget;
        assertOrder(3);
        return this;
    }

    public BuiltinArgumentBuilder functionOrNewTarget(boolean newTarget) {
        return function(!newTarget).newTarget(newTarget);
    }

    public BuiltinArgumentBuilder fixedArgs(int fixedArgumentCount) {
        this.fixedArgumentCount = fixedArgumentCount;
        assertOrder(4);
        return this;
    }

    public BuiltinArgumentBuilder varArgs() {
        return varArgs(true);
    }

    public BuiltinArgumentBuilder varArgs(boolean varArgs) {
        this.varArgs = varArgs;
        assertOrder(5);
        return this;
    }

    public JavaScriptNode[] createArgumentNodes(JSContext context) {
        NodeFactory factory = NodeFactory.getInstance(context);
        int totalArgs = getTotalArgumentCount();
        JavaScriptNode[] callArgs = totalArgs == 0 ? EMPTY_NODE_ARRAY : new JavaScriptNode[totalArgs];
        int index = 0;
        if (hasThis) {
            callArgs[index++] = factory.createAccessThis();
        }
        if (hasFunction) {
            callArgs[index++] = factory.createAccessCallee(0);
        }
        int argIndex = 0;
        int size = varArgs ? totalArgs - 1 : totalArgs;
        for (int i = index; i < size; i++) {
            callArgs[i] = factory.createAccessArgument(argIndex++);
        }
        if (varArgs) {
            callArgs[size] = factory.createAccessVarArgs(argIndex++);
        }
        return callArgs;
    }

    private int getTotalArgumentCount() {
        return (hasThis ? 1 : 0) + (hasFunction ? 1 : 0) + (newTarget ? 1 : 0) + fixedArgumentCount + (varArgs ? 1 : 0);
    }

    /**
     * Ensures that the builder methods are called in a consistent order, which reflects the order
     * of the eventually created argument nodes.
     *
     * @param order desired order
     */
    protected void assertOrder(int order) {
    }
}
