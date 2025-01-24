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
package com.oracle.truffle.js.nodes.cast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.IteratorCloseNode;
import com.oracle.truffle.js.nodes.access.IteratorStepNode;
import com.oracle.truffle.js.nodes.access.IteratorValueNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;

/**
 * Converts an arbitrary iterable object to a string list.
 * <p>
 * Throws {@code TypeError} if a non-string value is encountered.
 * </p>
 */
public abstract class JSStringListFromIterableNode extends JavaScriptBaseNode {

    protected final JSContext context;

    protected JSStringListFromIterableNode(JSContext context) {
        this.context = context;
    }

    public abstract List<String> executeIterable(Object value);

    @Specialization
    @TruffleBoundary
    protected static List<String> stringToList(TruffleString s) {
        int[] codePoints = Strings.toJavaString(s).codePoints().toArray();
        int length = codePoints.length;
        List<String> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(new String(codePoints, i, 1));
        }
        return result;
    }

    @Specialization(guards = {"!isUndefined(iterable)", "!isString(iterable)"})
    protected static List<String> toArray(Object iterable,
                    @Bind Node node,
                    @Cached(inline = true) GetIteratorNode getIteratorNode,
                    @Cached IteratorStepNode iteratorStepNode,
                    @Cached IteratorValueNode iteratorValueNode,
                    @Cached("create(context)") IteratorCloseNode iteratorCloseNode) {

        IteratorRecord iteratorRecord = getIteratorNode.execute(node, iterable);
        List<String> list = new ArrayList<>();
        Object next = true;

        while (!isFalse(next)) {

            next = iteratorStepNode.execute(iteratorRecord);

            if (!isFalse(next)) {

                Object nextValue = iteratorValueNode.execute(next);
                if (!(nextValue instanceof TruffleString nextStr)) {
                    iteratorCloseNode.executeAbrupt(iteratorRecord.getIterator());
                    throw Errors.createTypeError("nonString value encountered!");
                }
                Boundaries.listAdd(list, Strings.toJavaString(nextStr));
            }
        }
        return list;
    }

    private static boolean isFalse(Object o) {
        return o == Boolean.FALSE;
    }

    @Specialization(guards = "isUndefined(value)")
    protected List<String> doUndefined(@SuppressWarnings("unused") Object value) {
        return Collections.emptyList();
    }
}
