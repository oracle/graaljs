/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

/**
 * Absorb iterator to new array.
 */
public abstract class IteratorToArrayNode extends JavaScriptNode {
    private final JSContext context;
    @Child @Executed JavaScriptNode iteratorNode;
    @Child private IteratorNextNode nextNode;
    @Child private PropertyGetNode getDoneNode;
    @Child private IsJSObjectNode isObjectNode;
    @Child private JSToBooleanNode toBooleanNode;
    @Child private IteratorValueNode valueNode;

    @CompilerDirectives.CompilationFinal private int capacity = 0;
    @CompilerDirectives.CompilationFinal private boolean first = true;

    private final BranchProfile firstGrowProfile = BranchProfile.create();
    private final BranchProfile growProfile = BranchProfile.create();
    private final BranchProfile errorProfile = BranchProfile.create();

    protected IteratorToArrayNode(JSContext context, JavaScriptNode iteratorNode, IteratorValueNode valueNode) {
        this.context = context;
        this.iteratorNode = iteratorNode;
        this.valueNode = valueNode;

        nextNode = IteratorNextNode.create();
        getDoneNode = PropertyGetNode.create(Strings.DONE, false, context);
        isObjectNode = IsJSObjectNode.create();
        toBooleanNode = JSToBooleanNode.create();
    }

    public static IteratorToArrayNode create(JSContext context, JavaScriptNode iterator) {
        IteratorValueNode valueNode = IteratorValueNode.create(context);
        return IteratorToArrayNodeGen.create(context, iterator, valueNode);
    }

    @Specialization(guards = "!iteratorRecord.isDone()")
    protected Object doIterator(IteratorRecord iteratorRecord) {
        SimpleArrayList<Object> items = new SimpleArrayList<>(capacity);

        while (true) {
            Object next = nextNode.execute(iteratorRecord);
            if (!isObjectNode.executeBoolean(next)) {
                errorProfile.enter();
                throw Errors.createTypeErrorIterResultNotAnObject(next, this);
            }
            if (toBooleanNode.executeBoolean(getDoneNode.getValue(next))) {
                break;
            }

            items.add(valueNode.execute(next), first ? firstGrowProfile : growProfile);
        }

        if (CompilerDirectives.inInterpreter()) {
            if (first) {
                capacity = items.size();
                first = false;
            } else if (capacity != items.size()) {
                //Capacity is changing even though we are still in interpreter. Assume fluctuating capacity values.
                capacity = 0;
            }
        }

        iteratorRecord.setDone(true);

        return items;
    }

    @Specialization(guards = "iteratorRecord.isDone()")
    protected Object doDoneIterator(@SuppressWarnings("unused") IteratorRecord iteratorRecord) {
        return JSArray.createEmptyZeroLength(context, getRealm());
    }

    public abstract Object execute(VirtualFrame frame, IteratorRecord iteratorRecord);

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return IteratorToArrayNodeGen.create(context, cloneUninitialized(iteratorNode, materializedTags), cloneUninitialized(valueNode, materializedTags));
    }
}
