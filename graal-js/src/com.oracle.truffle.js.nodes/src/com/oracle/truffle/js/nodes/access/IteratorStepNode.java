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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ES6 7.4.5 IteratorStep(iterator).
 */
public abstract class IteratorStepNode extends JavaScriptNode {
    @Child @Executed JavaScriptNode iteratorNode;
    @Child private IteratorNextNode iteratorNextNode;
    @Child private IteratorCompleteNode iteratorCompleteNode;
    private final JSContext context;

    protected IteratorStepNode(JSContext context, JavaScriptNode iteratorNode) {
        this.context = context;
        this.iteratorNode = iteratorNode;
    }

    public static IteratorStepNode create(JSContext context) {
        return create(context, null);
    }

    public static IteratorStepNode create(JSContext context, JavaScriptNode iterator) {
        return IteratorStepNodeGen.create(context, iterator);
    }

    @Specialization
    protected Object doIteratorStep(DynamicObject iterator) {
        if (iteratorNextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            iteratorNextNode = insert(IteratorNextNode.create(context));
        }
        if (iteratorCompleteNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            iteratorCompleteNode = insert(IteratorCompleteNode.create(context));
        }
        // passing undefined might be wrong, we should NOT pass "value"
        Object result = iteratorNextNode.execute(iterator, Undefined.instance);
        Object done = iteratorCompleteNode.execute((DynamicObject) result);
        if (done instanceof Boolean && ((Boolean) done) == Boolean.TRUE) {
            return false;
        }
        return result;
    }

    public abstract Object execute(DynamicObject iterator);

    @Override
    protected JavaScriptNode copyUninitialized() {
        return IteratorStepNodeGen.create(context, cloneUninitialized(iteratorNode));
    }
}
