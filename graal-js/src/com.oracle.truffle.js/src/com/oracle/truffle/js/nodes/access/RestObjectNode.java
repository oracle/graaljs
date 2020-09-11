/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectNode;
import com.oracle.truffle.js.runtime.JSContext;

import java.util.Set;

public abstract class RestObjectNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    @Child private CopyDataPropertiesNode copyDataPropertiesNode;
    protected final JSContext context;

    protected RestObjectNode(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode) {
        this.context = context;
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
        this.copyDataPropertiesNode = CopyDataPropertiesNode.create(context);
    }

    public static JavaScriptNode create(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        if (excludedNode == null) {
            return RestObjectNodeGen.create(context, targetNode, sourceNode);
        } else {
            return RestObjectWithExcludedNodeGen.create(context, targetNode, sourceNode, excludedNode);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullOrUndefined(source)")
    protected static DynamicObject doNullOrUndefined(DynamicObject restObj, Object source) {
        return restObj;
    }

    @Specialization(guards = {"isJSObject(source)"})
    protected final DynamicObject copyDataProperties(DynamicObject restObj, DynamicObject source) {
        copyDataPropertiesNode.execute(restObj, source);
        return restObj;
    }

    @Specialization(guards = {"!isJSDynamicObject(source)"})
    protected final Object doOther(DynamicObject restObj, Object source,
                    @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode) {
        Object from = toObjectNode.execute(source);
        copyDataPropertiesNode.execute(restObj, from);
        return restObj;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return RestObjectNodeGen.create(context, cloneUninitialized(targetNode, materializedTags), cloneUninitialized(sourceNode, materializedTags));
    }
}

abstract class RestObjectWithExcludedNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    @Child @Executed protected JavaScriptNode excludedNode;
    @Child private CopyDataPropertiesNode copyDataPropertiesNode;
    protected final JSContext context;

    protected RestObjectWithExcludedNode(JSContext context, JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        this.context = context;
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
        this.excludedNode = JSToObjectArrayNode.create(context, excludedNode);
        this.copyDataPropertiesNode = CopyDataPropertiesNode.create(context);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullOrUndefined(source)")
    protected static DynamicObject doNullOrUndefined(DynamicObject restObj, Object source, Object[] excludedItems) {
        return restObj;
    }

    @Specialization(guards = {"isJSObject(source)"})
    protected final DynamicObject copyDataProperties(DynamicObject restObj, DynamicObject source, Object[] excludedItems) {
        copyDataPropertiesNode.execute(restObj, source, excludedItems);
        return restObj;
    }

    @Specialization(guards = {"!isJSDynamicObject(source)"})
    protected final Object doOther(DynamicObject restObj, Object source, Object[] excludedItems,
                    @Cached("createToObjectNoCheck(context)") JSToObjectNode toObjectNode) {
        Object from = toObjectNode.execute(source);
        copyDataPropertiesNode.execute(restObj, from, excludedItems);
        return restObj;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return RestObjectWithExcludedNodeGen.create(context, cloneUninitialized(targetNode, materializedTags), cloneUninitialized(sourceNode, materializedTags),
                        cloneUninitialized(excludedNode, materializedTags));
    }
}
