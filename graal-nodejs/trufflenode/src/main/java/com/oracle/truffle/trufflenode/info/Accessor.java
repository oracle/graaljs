/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.info;

import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.trufflenode.ContextData;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.node.ExecuteNativeAccessorNode;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Jan Stola
 */
public class Accessor {

    private static final AtomicInteger idGenerator = new AtomicInteger();

    private final GraalJSAccess graalAccess;
    private final String name;
    private final long getterPtr;
    private final long setterPtr;
    private final Object data;
    private final FunctionTemplate signature;
    private final int id;
    private final int attributes;

    public Accessor(GraalJSAccess graalAccess, Object name, long getterPtr, long setterPtr, Object data, FunctionTemplate signature, int attributes) {
        this.graalAccess = graalAccess;
        this.name = (String) name;
        this.getterPtr = getterPtr;
        this.setterPtr = setterPtr;
        this.data = data;
        this.signature = signature;
        this.id = idGenerator.getAndIncrement();
        this.attributes = attributes;
    }

    public long getGetterPtr() {
        return getterPtr;
    }

    public long getSetterPtr() {
        return setterPtr;
    }

    public String getName() {
        return name;
    }

    public Object getData() {
        return data;
    }

    public FunctionTemplate getSignature() {
        return signature;
    }

    public int getAttributes() {
        return attributes;
    }

    private Pair<JSFunctionData, JSFunctionData> createFunctions(JSContext context) {
        JSFunctionData getter = (getterPtr == 0) ? null : createFunction(context, true);
        JSFunctionData setter = (setterPtr == 0) ? null : createFunction(context, false);
        return new Pair<>(getter, setter);
    }

    private JSFunctionData createFunction(JSContext context, boolean getter) {
        return GraalJSAccess.functionDataFromRootNode(context, new ExecuteNativeAccessorNode(graalAccess, context, this, getter));
    }

    public Pair<JSFunctionData, JSFunctionData> getFunctions(JSContext context) {
        ContextData contextData = GraalJSAccess.getContextData(context);
        Pair<JSFunctionData, JSFunctionData> functions = contextData.getAccessorPair(id);
        if (functions == null) {
            functions = createFunctions(context);
            contextData.setAccessorPair(id, functions);
        }
        return functions;
    }

}
