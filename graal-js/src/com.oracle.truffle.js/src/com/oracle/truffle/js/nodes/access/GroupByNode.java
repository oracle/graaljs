/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.builtins.helper.JSCollectionsNormalizeNode;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNode;
import com.oracle.truffle.js.nodes.cast.LongToIntOrDoubleNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class GroupByNode extends JavaScriptBaseNode {
    protected final boolean toPropertyKeyCoercion;
    protected final JSContext context;

    protected GroupByNode(JSContext context, boolean toPropertyKeyCoercion) {
        this.context = context;
        this.toPropertyKeyCoercion = toPropertyKeyCoercion;
    }

    public abstract Map<Object, List<Object>> execute(Object items, Object callbackfn);

    @Specialization
    protected static Map<Object, List<Object>> groupBy(Object items, Object callbackfn,
                    @Bind Node node,
                    @Cached RequireObjectCoercibleNode requireObjectCoercibleNode,
                    @Cached IsCallableNode isCallableNode,
                    @Cached(inline = true) GetIteratorNode getIteratorNode,
                    @Cached IteratorStepNode iteratorStepNode,
                    @Cached IteratorValueNode iteratorValueNode,
                    @Cached("create(context)") IteratorCloseNode iteratorCloseNode,
                    @Cached("createCall()") JSFunctionCallNode callNode,
                    @Cached(inline = true) LongToIntOrDoubleNode toIntOrDoubleNode,
                    @Cached(value = "maybeCreateToPropertyKeyNode(toPropertyKeyCoercion)", neverDefault = false) JSToPropertyKeyNode toPropertyKeyNode,
                    @Cached(value = "maybeCreateNormalizeKeyNode(toPropertyKeyCoercion)", neverDefault = false) JSCollectionsNormalizeNode normalizeKeyNode,
                    @Cached InlinedBranchProfile errorBranch) {

        requireObjectCoercibleNode.execute(items);
        if (!isCallableNode.executeBoolean(callbackfn)) {
            errorBranch.enter(node);
            throw Errors.createTypeErrorNotAFunction(callbackfn, node);
        }

        Map<Object, List<Object>> groups = initGroups();
        IteratorRecord iteratorRecord = getIteratorNode.execute(node, items);
        long k = 0;
        try {
            while (true) {
                Object next = iteratorStepNode.execute(iteratorRecord);
                if (next == Boolean.FALSE) {
                    return groups;
                }
                Object value = iteratorValueNode.execute(next);
                Object key = callNode.executeCall(JSArguments.create(Undefined.instance, callbackfn, value, toIntOrDoubleNode.execute(node, k)));
                if (toPropertyKeyNode != null) { // toPropertyKeyCoercion
                    key = toPropertyKeyNode.execute(key);
                }
                if (normalizeKeyNode != null) { // !toPropertyKeyCoercion
                    key = normalizeKeyNode.execute(key);
                }
                addValueToKeyedGroup(groups, key, value);
                k++;
            }
        } catch (AbstractTruffleException ex) {
            errorBranch.enter(node);
            iteratorCloseNode.executeAbrupt(iteratorRecord.getIterator());
            throw ex;
        }
    }

    @TruffleBoundary
    private static Map<Object, List<Object>> initGroups() {
        return new LinkedHashMap<>();
    }

    @TruffleBoundary
    private static void addValueToKeyedGroup(Map<Object, List<Object>> groups, Object key, Object value) {
        List<Object> group = groups.get(key);
        if (group == null) {
            group = new ArrayList<>();
            groups.put(key, group);
        }
        group.add(value);
    }

    protected static JSToPropertyKeyNode maybeCreateToPropertyKeyNode(boolean toPropertyKeyCoercion) {
        return toPropertyKeyCoercion ? JSToPropertyKeyNode.create() : null;
    }

    protected static JSCollectionsNormalizeNode maybeCreateNormalizeKeyNode(boolean toPropertyKeyCoercion) {
        return toPropertyKeyCoercion ? null : JSCollectionsNormalizeNode.create();
    }

}
