/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.JSArgumentsObject;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

public abstract class JSArrayElementIndexNode extends JavaScriptBaseNode {
    protected static final int MAX_CACHED_ARRAY_TYPES = 4;
    protected final JSContext context;
    @Child private IsArrayNode isArrayNode = IsArrayNode.createIsFastOrTypedArray();

    protected JSArrayElementIndexNode(JSContext context) {
        this.context = context;
    }

    protected static boolean hasHoles(DynamicObject object) {
        return JSObject.getArray(object).hasHoles(object);
    }

    protected static ScriptArray getArrayType(DynamicObject object, boolean arrayCondition) {
        return JSObject.getArray(object, arrayCondition);
    }

    /**
     * Workaround for GR-830: Cached values are initialized before guards are evaluated.
     */
    protected static ScriptArray getArrayTypeIfArray(DynamicObject object, boolean arrayCondition) {
        if (!arrayCondition) {
            return null;
        }
        return getArrayType(object, arrayCondition);
    }

    protected final boolean isArraySuitableForEnumBasedProcessing(TruffleObject object, long length) {
        return length > JSTruffleOptions.BigArrayThreshold && !JSArrayBufferView.isJSArrayBufferView(object) && !JSProxy.isProxy(object) &&
                        (context.getArrayPrototypeNoElementsAssumption().isValid() || !JSObject.isJSObject(object) || JSObject.getPrototype((DynamicObject) object) == Null.instance);
    }

    /**
     * @param object dummy parameter to force evaluation of the guard by the DSL
     */
    protected final boolean hasPrototypeElements(DynamicObject object) {
        return !context.getArrayPrototypeNoElementsAssumption().isValid();
    }

    protected final boolean isArray(TruffleObject obj) {
        return isArrayNode.execute(obj);
    }

    protected static boolean isSupportedArray(DynamicObject object) {
        return JSArray.isJSFastArray(object) || JSArgumentsObject.isJSFastArgumentsObject(object) || JSArrayBufferView.isJSArrayBufferView(object);
    }
}
