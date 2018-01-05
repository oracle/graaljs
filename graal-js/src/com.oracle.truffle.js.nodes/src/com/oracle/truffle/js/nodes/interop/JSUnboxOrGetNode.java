/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropNodeUtil;
import com.oracle.truffle.js.runtime.truffleinterop.JSInteropUtil;

/**
 * Convert a (foreign) TruffleObject to a primitive value.
 *
 * @see JSRuntime#toPrimitiveFromForeign(TruffleObject)
 */
@ImportStatic(value = JSInteropUtil.class)
public abstract class JSUnboxOrGetNode extends JavaScriptBaseNode {

    public abstract Object executeWithTarget(TruffleObject target);

    @Specialization
    public Object foreign(TruffleObject value,
                    @Cached("createIsBoxed()") Node isBoxed,
                    @Cached("createUnbox()") Node unbox,
                    @Cached("createIsNull()") Node isNull,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("create()") JSForeignToJSTypeNode toJSType) {
        if (ForeignAccess.sendIsNull(isNull, value)) {
            return Null.instance;
        } else if (ForeignAccess.sendIsBoxed(isBoxed, value)) {
            try {
                return toJSType.executeWithTarget(ForeignAccess.sendUnbox(unbox, value));
            } catch (UnsupportedMessageException e) {
                return Null.instance;
            }
        } else {
            boolean hasSize = JSInteropNodeUtil.hasSize(value, hasSizeNode);
            return JSRuntime.objectToConsoleString(value, hasSize ? null : "foreign");
        }
    }

    public static JSUnboxOrGetNode create() {
        return JSUnboxOrGetNodeGen.create();
    }

}
