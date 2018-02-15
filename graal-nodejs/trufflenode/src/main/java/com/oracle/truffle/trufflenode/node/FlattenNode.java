/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.JSLazyString;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

@ImportStatic({JSObject.class, JSInteropUtil.class, JSRuntime.class})
abstract class FlattenNode extends JavaScriptBaseNode {
    protected abstract Object execute(Object value);

    @Specialization()
    protected static String doLazyString(JSLazyString value) {
        return value.toString();
    }

    @Specialization
    protected static Object doSymbol(Symbol value) {
        return value;
    }

    @Specialization(guards = "isJSObject(value)")
    protected static Object doJSObject(DynamicObject value) {
        return value;
    }

    @Specialization(guards = "isForeignObject(value)")
    protected static Object doForeignObject(TruffleObject value,
                    @Cached("createIsBoxed()") Node isBoxedNode,
                    @Cached("createUnbox()") Node unboxNode) {
        if (ForeignAccess.sendIsBoxed(isBoxedNode, value)) {
            Object unboxedValue = JSInteropNodeUtil.unbox(value, unboxNode);
            if (unboxedValue instanceof String) {
                // jobject reference in the native wrapper (GraalString)
                // must be jstring (to allow the usage of various String-specific
                // JNI functions) => return the unboxed string
                return unboxedValue;
            }
        }
        return value;
    }

    @Specialization(guards = {"!isLazyString(value)", "!isTruffleObject(value)"})
    protected static Object doOther(Object value) {
        return value;
    }

}
