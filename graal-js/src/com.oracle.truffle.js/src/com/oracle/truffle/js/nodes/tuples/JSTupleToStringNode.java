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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Implementation of the abstract operation TupleToString(argument).
 */
public class JSTupleToStringNode extends JavaScriptBaseNode {

    private final ConditionProfile isEmptyTuple = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isSingleValue = ConditionProfile.createBinaryProfile();

    @Child private JSToStringNode toStringNode;

    protected JSTupleToStringNode() {
        super();
    }

    public static JSTupleToStringNode create() {
        return new JSTupleToStringNode();
    }

    public String execute(Tuple argument) {
        // TODO: re-evaluate, check proposal for changes
        // TODO: the following code isn't strictly according to spec
        // TODO: as I didn't find a way to call %Array.prototype.join% from this node...
        return join(argument);
    }

    private String join(Tuple tuple) {
        long len = tuple.getArraySize();
        if (isEmptyTuple.profile(len == 0)) {
            return "";
        }
        if (isSingleValue.profile(len == 1)) {
            return toString(tuple.getElement(0));
        }
        StringBuilder sb = new StringBuilder();
        for (long k = 0; k < len; k++) {
            if (k > 0) {
                Boundaries.builderAppend(sb, ',');
            }
            Object element = tuple.getElement(k);
            if (element != Undefined.instance && element != Null.instance) {
                Boundaries.builderAppend(sb, toString(element));
            }
        }
        return Boundaries.builderToString(sb);
    }

    private String toString(Object obj) {
        if (toStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toStringNode = insert(JSToStringNode.create());
        }
        return toStringNode.executeString(obj);
    }
}
