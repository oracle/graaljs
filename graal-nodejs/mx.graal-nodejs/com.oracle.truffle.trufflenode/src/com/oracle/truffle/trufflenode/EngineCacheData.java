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
