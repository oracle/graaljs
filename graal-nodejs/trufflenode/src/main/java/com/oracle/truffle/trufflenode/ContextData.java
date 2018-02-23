/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode;

import java.util.ArrayList;
import java.util.List;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ContextData {
    private Object securityToken;
    private final Map<Integer, Object> embedderData = new HashMap<>();
    private final Map<String, FunctionNode> functionNodeCache = new WeakHashMap<>();
    private final Map<Source, ScriptNode> scriptNodeCache = new WeakHashMap<>();
    private final List<Pair<JSFunctionData, JSFunctionData>> accessorPairs = new ArrayList<>();
    private final Shape externalObjectShape;

    private DynamicObject nativeUtf8Write;
    private DynamicObject nativeUtf8Slice;
    private DynamicObject resolverFactory;

    public ContextData(JSContext context) {
        this.externalObjectShape = JSExternalObject.makeInitialShape(context);
    }

    public void setSecurityToken(Object securityToken) {
        this.securityToken = securityToken;
    }

    public Object getSecurityToken() {
        return securityToken;
    }

    public Pair<JSFunctionData, JSFunctionData> getAccessorPair(int id) {
        if (id < accessorPairs.size()) {
            return accessorPairs.get(id);
        } else {
            return null;
        }
    }

    public void setAccessorPair(int id, Pair<JSFunctionData, JSFunctionData> pair) {
        while (accessorPairs.size() <= id) {
            accessorPairs.add(null);
        }
        accessorPairs.set(id, pair);
    }

    public Shape getExternalObjectShape() {
        return externalObjectShape;
    }

    public DynamicObject getNativeUtf8Write() {
        return nativeUtf8Write;
    }

    public void setNativeUtf8Write(DynamicObject nativeUtf8Write) {
        this.nativeUtf8Write = nativeUtf8Write;
    }

    public DynamicObject getNativeUtf8Slice() {
        return nativeUtf8Slice;
    }

    public void setNativeUtf8Slice(DynamicObject nativeUtf8Slice) {
        this.nativeUtf8Slice = nativeUtf8Slice;
    }

    public void setEmbedderData(int index, Object value) {
        embedderData.put(index, value);
    }

    public Object getEmbedderData(int index) {
        return embedderData.get(index);
    }

    public void setResolverFactory(DynamicObject resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    public DynamicObject getResolverFactory() {
        return resolverFactory;
    }

    public Map<Source, ScriptNode> getScriptNodeCache() {
        return scriptNodeCache;
    }

    public Map<String, FunctionNode> getFunctionNodeCache() {
        return functionNodeCache;
    }

}
