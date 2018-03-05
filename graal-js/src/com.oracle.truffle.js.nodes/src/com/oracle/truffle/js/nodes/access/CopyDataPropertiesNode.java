/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    protected static Object doOther(Object object, Object value) {
        throw Errors.createTypeErrorNotAnObject(value);
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
