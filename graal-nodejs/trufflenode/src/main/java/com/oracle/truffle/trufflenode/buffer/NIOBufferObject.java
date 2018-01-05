/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.buffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.JSBuiltinLookup;
import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSBuiltinObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.trufflenode.ContextData;
import com.oracle.truffle.trufflenode.GraalJSAccess;

public final class NIOBufferObject extends JSBuiltinObject {
    private static final JSBuiltinsContainer NIO_BUFFER_BUILTINS = new NIOBufferBuiltins();

    public static final String NIO_BUFFER_MODULE_NAME = "internal/graal/buffer.js";

    private static final String CLASS_NAME = "NIOBuffer";

    private NIOBufferObject() {
    }

    private static DynamicObject create(JSContext context) {
        DynamicObject obj = context.getEmptyShape().newInstance();
        ((JSBuiltinLookup) context.getFunctionLookup()).defineBuiltins(NIO_BUFFER_BUILTINS);
        JSObjectUtil.putFunctionsFromContainer(context.getRealm(), obj, NIO_BUFFER_BUILTINS.getName());
        return obj;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @TruffleBoundary
    public static Object createInitFunction(ScriptNode scriptNode) {
        JSContext context = scriptNode.getContext();
        JSRealm realm = context.getRealm();

        // This JS function will be executed at node.js bootstrap time to register
        // the "default" Buffer API functions.
        JavaScriptRootNode wrapperNode = new JavaScriptRootNode() {
            @TruffleBoundary
            @Override
            public Object execute(VirtualFrame frame) {
                Object[] args = frame.getArguments();
                assert args.length == 4;
                DynamicObject nativeUtf8Write = (DynamicObject) args[2];
                DynamicObject nativeUtf8Slice = (DynamicObject) args[3];
                ContextData contextData = GraalJSAccess.getContextData(context);
                contextData.setNativeUtf8Write(nativeUtf8Write);
                contextData.setNativeUtf8Slice(nativeUtf8Slice);
                return create(context);
            }
        };
        JSFunctionData functionData = JSFunctionData.createCallOnly(context, Truffle.getRuntime().createCallTarget(wrapperNode), 2, "NIOBufferBuiltinsInitFunction");
        return JSFunction.create(realm, functionData);
    }

}
