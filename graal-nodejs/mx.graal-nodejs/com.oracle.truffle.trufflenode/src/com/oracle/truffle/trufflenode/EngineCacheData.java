/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.SyntheticModuleRecord;
import com.oracle.truffle.trufflenode.info.Accessor;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;
import com.oracle.truffle.trufflenode.node.ExecuteNativePropertyHandlerNode;

public final class EngineCacheData {
    private static final int MaxSingletonSymbolsCacheSize = 64;

    private final JSContext context;
    private final ConcurrentHashMap<FunctionTemplate.Descriptor, JSFunctionData> persistedTemplatesFunctionData;
    private final ConcurrentHashMap<Accessor.Descriptor, JSFunctionData> persistedAccessorsFunctionData;
    private final ConcurrentHashMap<ObjectTemplate.Descriptor, JSFunctionData> persistedNativePropertyHandlerData;
    private final ConcurrentHashMap<SyntheticModuleDescriptor, SyntheticModuleRecord.SharedData> persistedSyntheticModulesData;
    private final Map<TruffleString, SingletonSymbolUsageDescriptor> cachedSingletonSymbols;

    public EngineCacheData(JSContext context) {
        this.context = context;
        this.persistedTemplatesFunctionData = new ConcurrentHashMap<>();
        this.persistedAccessorsFunctionData = new ConcurrentHashMap<>();
        this.persistedNativePropertyHandlerData = new ConcurrentHashMap<>();
        this.persistedSyntheticModulesData = new ConcurrentHashMap<>();
        this.cachedSingletonSymbols = new HashMap<>();
    }

    public JSFunctionData getOrCreateFunctionDataFromTemplate(FunctionTemplate template, Function<JSContext, JSFunctionData> factory) {
        if (context.isMultiContext()) {
            // when aux engine cache is enabled, load shared function data from the image.
            return persistedTemplatesFunctionData.computeIfAbsent(template.getEngineCacheDescriptor(), d -> factory.apply(context));
        } else {
            return factory.apply(context);
        }
    }

    public JSFunctionData getOrCreateFunctionDataFromAccessor(Accessor accessor, boolean getter, Function<JSContext, JSFunctionData> factory) {
        return persistedAccessorsFunctionData.computeIfAbsent(accessor.getEngineCacheDescriptor(getter), d -> factory.apply(context));
    }

    public JSFunctionData getOrCreateFunctionDataFromPropertyHandler(ObjectTemplate template, ExecuteNativePropertyHandlerNode.Mode mode, Function<JSContext, JSFunctionData> factory) {
        return persistedNativePropertyHandlerData.computeIfAbsent(template.getEngineCacheDescriptor(mode), d -> factory.apply(context));
    }

    public SyntheticModuleRecord.SharedData getOrCreateSyntheticModuleData(TruffleString moduleName, List<TruffleString> exportNames,
                    Function<SyntheticModuleDescriptor, SyntheticModuleRecord.SharedData> factory) {
        SyntheticModuleDescriptor descriptor = new SyntheticModuleDescriptor(moduleName, exportNames);
        return persistedSyntheticModulesData.computeIfAbsent(descriptor, factory);
    }

    @TruffleBoundary
    public Symbol createOrUseCachedSingleton(TruffleString name) {
        if (context.isMultiContext() && JSConfig.UseSingletonSymbols) {
            if (cachedSingletonSymbols.size() < MaxSingletonSymbolsCacheSize) {
                SingletonSymbolUsageDescriptor descriptor = cachedSingletonSymbols.computeIfAbsent(name, key -> new SingletonSymbolUsageDescriptor(Symbol.create(key)));
                Symbol newSymbol = descriptor.tryUsingSingleton(context);
                if (newSymbol != null) {
                    return newSymbol;
                }
            }
        }
        return Symbol.create(name);
    }

    public record SyntheticModuleDescriptor(TruffleString moduleName, List<TruffleString> exportNames) {
    }

    static class SingletonSymbolUsageDescriptor {

        private final Symbol singleton;
        private Object symbolUsageMarker;

        SingletonSymbolUsageDescriptor(Symbol symbol) {
            this.singleton = symbol;
        }

        public Symbol tryUsingSingleton(JSContext context) {
            Object currentMarker = context.getSymbolUsageMarker();
            if (symbolUsageMarker != currentMarker) {
                symbolUsageMarker = currentMarker;
                return singleton;
            }
            return null;
        }
    }
}
