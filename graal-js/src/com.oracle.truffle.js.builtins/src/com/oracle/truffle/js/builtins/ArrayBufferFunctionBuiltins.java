/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.ArrayBufferFunctionBuiltinsFactory.JSIsArrayBufferViewNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSDataView;

/**
 * Contains builtins for {@linkplain JSArrayBuffer} function (constructor).
 */
public final class ArrayBufferFunctionBuiltins extends JSBuiltinsContainer.Lambda {
    public ArrayBufferFunctionBuiltins() {
        super(JSArrayBuffer.CLASS_NAME);
        defineFunction("isView", 1, (context, builtin) -> JSIsArrayBufferViewNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context)));
    }

    public abstract static class JSIsArrayBufferViewNode extends JSBuiltinNode {
        public JSIsArrayBufferViewNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isArrayBufferView(Object object) {
            return JSArrayBufferView.isJSArrayBufferView(object) || JSDataView.isJSDataView(object);
        }
    }
}
