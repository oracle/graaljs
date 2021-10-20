/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.trufflenode.info.FunctionTemplate;
import com.oracle.truffle.trufflenode.node.ExecuteNativePropertyHandlerNode;

public class EngineCacheData {

    enum CacheableSingletons {
        AlwaysFalse,
        AlwaysUndefined,
        InteropArrayBuffer,
        SetBreakPoint,
        GcBuiltinRoot,
        PropertyHandlerPrototype,
        PropertyHandlerPrototypeGlobal
    }

    private final JSContext context;
    private final ConcurrentHashMap<Descriptor, JSFunctionData> persistedTemplatesFunctionData;
    private final ConcurrentHashMap<Descriptor, JSFunctionData> persistedAccessorsFunctionData;
    private final ConcurrentHashMap<Descriptor, JSFunctionData> persistedNativePropertyHandlerData;

    private final JSFunctionData[] persistedBuiltins = new JSFunctionData[CacheableSingletons.values().length];

    public EngineCacheData(JSContext context) {
        this.context = context;
        this.persistedTemplatesFunctionData = new ConcurrentHashMap<>();
        this.persistedAccessorsFunctionData = new ConcurrentHashMap<>();
        this.persistedNativePropertyHandlerData = new ConcurrentHashMap<>();
    }

    public JSFunctionData getOrCreateFunctionDataFromTemplate(FunctionTemplate template, Function<JSContext, JSFunctionData> factory) {
        Descriptor descriptor = new Descriptor(template.getID(), template.getLength(), template.isSingleFunctionTemplate());
        return getOrStore(descriptor, factory, persistedTemplatesFunctionData);
    }

    public JSFunctionData getOrCreateFunctionDataFromAccessor(int id, boolean getter, Function<JSContext, JSFunctionData> factory) {
        Descriptor descriptor = new Descriptor(id, 0, getter);
        return getOrStore(descriptor, factory, persistedAccessorsFunctionData);
    }

    public JSFunctionData getOrCreateFunctionDataFromPropertyHandler(int templateId, ExecuteNativePropertyHandlerNode.Mode mode, Function<JSContext, JSFunctionData> factory) {
        Descriptor descriptor = new Descriptor(templateId, mode.ordinal(), false);
        return getOrStore(descriptor, factory, persistedNativePropertyHandlerData);
    }

    public JSFunctionData getOrCreateBuiltinFunctionData(CacheableSingletons builtin, Function<JSContext, JSFunctionData> factory) {
        if (persistedBuiltins[builtin.ordinal()] != null) {
            return persistedBuiltins[builtin.ordinal()];
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        synchronized (this) {
            if (persistedBuiltins[builtin.ordinal()] == null) {
                persistedBuiltins[builtin.ordinal()] = factory.apply(context);
            }
            return persistedBuiltins[builtin.ordinal()];
        }
    }

    private JSFunctionData getOrStore(Descriptor descriptor, Function<JSContext, JSFunctionData> factory, ConcurrentHashMap<Descriptor, JSFunctionData> storage) {
        if (storage.containsKey(descriptor)) {
            return storage.get(descriptor);
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        storage.computeIfAbsent(descriptor, (d) -> factory.apply(context));
        return storage.get(descriptor);
    }

    private static class Descriptor {

        private final int templateId;
        private final int length;
        private final boolean singleFunction;

        private Descriptor(int id, int length, boolean singleFunction) {
            this.templateId = id;
            this.length = length;
            this.singleFunction = singleFunction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Descriptor that = (Descriptor) o;
            return templateId == that.templateId && length == that.length && singleFunction == that.singleFunction;
        }

        @Override
        public int hashCode() {
            return Objects.hash(templateId, length, singleFunction);
        }
    }
}
