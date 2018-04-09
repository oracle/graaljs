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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;

public abstract class CopyDataPropertiesNode extends JavaScriptNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode sourceNode;
    @Child protected JavaScriptNode excludedNode;

    protected CopyDataPropertiesNode(JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        this.targetNode = targetNode;
        this.sourceNode = sourceNode;
        this.excludedNode = excludedNode;
    }

    public static CopyDataPropertiesNode create(JavaScriptNode targetNode, JavaScriptNode sourceNode, JavaScriptNode excludedNode) {
        return CopyDataPropertiesNodeGen.create(targetNode, sourceNode, excludedNode);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNullOrUndefined(value)")
    protected static DynamicObject doNullOrUndefined(DynamicObject restObj, Object value) {
        return restObj;
    }

    @Specialization(guards = {"isJSObject(value)", "excludedNode == null"})
    protected static DynamicObject doObject(DynamicObject restObj, DynamicObject value) {
        copyDataProperties(restObj, value, null);
        return restObj;
    }

    @Specialization(guards = {"isJSObject(value)", "excludedNode != null"})
    protected final DynamicObject doObject(VirtualFrame frame, DynamicObject restObj, DynamicObject value) {
        Object[] excludedItems = JSArray.toArray((DynamicObject) excludedNode.execute(frame));
        copyDataProperties(restObj, value, excludedItems);
        return restObj;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNullOrUndefined(value)", "!isJSObject(value)"})
    protected final Object doOther(Object object, Object value) {
        throw Errors.createTypeErrorNotAnObject(value, this);
    }

    @TruffleBoundary
    private static void copyDataProperties(DynamicObject target, DynamicObject from, Object[] excludedItems) {
        Iterable<Object> ownPropertyKeys = JSObject.ownPropertyKeys(from);
        for (Object nextKey : ownPropertyKeys) {
            boolean found = false;
            if (excludedItems != null) {
                for (Object e : excludedItems) {
                    if (e instanceof String) {
                        if (((String) e).equals(nextKey)) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                PropertyDescriptor desc = JSObject.getOwnProperty(from, nextKey);
                if (desc != null && desc.getEnumerable()) {
                    Object propValue = JSObject.get(from, nextKey);
                    JSRuntime.createDataProperty(target, nextKey, propValue);
                }
            }
        }
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(cloneUninitialized(targetNode), cloneUninitialized(sourceNode), cloneUninitialized(excludedNode));
    }
}
