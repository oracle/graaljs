/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

import com.oracle.js.parser.ir.FunctionNode;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * Embedder data shared between realms.
 */
public final class ContextData {
    private final Map<String, FunctionNode> functionNodeCache = new WeakHashMap<>();
    private final Map<Source, ScriptNode> scriptNodeCache = new WeakHashMap<>();
    private final List<Pair<JSFunctionData, JSFunctionData>> accessorPairs = new ArrayList<>();
    private final Shape externalObjectShape;
    private final JSContext context;
    private final AtomicReferenceArray<JSFunctionData> accessFunctionData = new AtomicReferenceArray<>(FunctionKey.LENGTH);
    private final EngineCacheData engineCacheData;

    public enum FunctionKey {
        ArrayBufferGetContents,
        ConstantFalse,
        ConstantUndefined,
        GcBuiltinRoot,
        PropertyHandlerPrototype,
        PropertyHandlerPrototypeGlobal,
        SetBreakPoint;

        static final int LENGTH = FunctionKey.values().length;
    }

    public ContextData(JSContext context) {
        this.context = context;
        this.externalObjectShape = JSExternal.makeInitialShape(context);
        this.engineCacheData = new EngineCacheData(context);
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

    public Map<Source, ScriptNode> getScriptNodeCache() {
        return scriptNodeCache;
    }

    public Map<String, FunctionNode> getFunctionNodeCache() {
        return functionNodeCache;
    }

    public EngineCacheData getEngineCacheData() {
        return engineCacheData;
    }

    public JSFunctionData getOrCreateFunctionData(FunctionKey key, Function<JSContext, JSFunctionData> factory) {
        int index = key.ordinal();
        JSFunctionData functionData = accessFunctionData.get(index);
        if (functionData != null) {
            return functionData;
        }
        functionData = factory.apply(context);
        if (accessFunctionData.compareAndSet(index, null, functionData)) {
            return functionData;
        } else {
            return accessFunctionData.get(index);
        }
    }
}
