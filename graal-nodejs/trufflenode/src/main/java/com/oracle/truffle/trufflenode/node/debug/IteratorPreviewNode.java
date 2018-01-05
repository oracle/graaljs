/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node.debug;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.util.JSHashMap;
import java.util.ArrayList;
import java.util.List;

public class IteratorPreviewNode extends JavaScriptRootNode {
    public static final String NAME = "preview";
    private final JSContext context;

    public IteratorPreviewNode(JSContext context) {
        this.context = context;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object thiz = JSArguments.getThisObject(frame.getArguments());
        Object original = ((DynamicObject) thiz).get(MakeMirrorNode.ORIGINAL_KEY);
        Object[] array = toArray((DynamicObject) original);
        return JSArray.createConstantObjectArray(context, array);
    }

    @CompilerDirectives.TruffleBoundary
    private static Object[] toArray(DynamicObject iterator) {
        DynamicObject copy = iterator.copy(iterator.getShape());
        JSHashMap.Cursor cursor = (JSHashMap.Cursor) iterator.get(JSRuntime.ITERATOR_NEXT_INDEX);
        iterator.set(JSRuntime.ITERATOR_NEXT_INDEX, cursor.copy());
        Object nextFn = JSObject.get(copy, JSRuntime.NEXT);
        List<Object> list = new ArrayList<>();
        while (true) {
            DynamicObject result = (DynamicObject) JSRuntime.call(nextFn, copy, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            Object done = JSObject.get(result, JSRuntime.DONE);
            if (done == Boolean.TRUE) {
                break;
            }
            Object value = JSObject.get(result, JSRuntime.VALUE);
            list.add(value);
        }
        return list.toArray(new Object[list.size()]);

    }

}
