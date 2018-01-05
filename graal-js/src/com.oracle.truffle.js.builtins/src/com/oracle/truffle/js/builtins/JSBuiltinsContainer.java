/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.oracle.truffle.js.nodes.function.BuiltinArgumentBuilder;
import com.oracle.truffle.js.nodes.function.BuiltinNodeFactory;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

/**
 * Intended to be subclassed by definitions of builtin functions.
 */
public abstract class JSBuiltinsContainer {
    private final String name;
    final Map<String, JSBuiltin> builtins = new LinkedHashMap<>();

    protected JSBuiltinsContainer(String name) {
        this.name = name;
    }

    public final void putAll(JSBuiltinsContainer container) {
        builtins.putAll(container.builtins);
    }

    public final JSBuiltin lookupByName(String methodName) {
        return builtins.get(methodName);
    }

    public final void forEachBuiltin(Consumer<? super JSBuiltin> consumer) {
        builtins.values().forEach(consumer);
    }

    protected static BuiltinArgumentBuilder args() {
        return BuiltinArgumentBuilder.builder();
    }

    public final String getName() {
        return name;
    }

    /**
     * Builtins container for builtin nodes created via switch dispatch method.
     */
    public abstract static class Switch extends JSBuiltinsContainer {

        protected Switch(String name) {
            super(name);
        }

        protected final void defineFunction(String name, int length) {
            defineFunction(name, length, JSAttributes.getDefaultNotEnumerable());
        }

        protected final void defineFunction(String name, int length, int attributeFlags) {
            defineBuiltin(name, length, attributeFlags, false, false);
        }

        protected final void defineConstructor(String name, int length, boolean isNewTargetConstructor) {
            assert !name.isEmpty();
            defineBuiltin(name, length, JSAttributes.getDefaultNotEnumerable(), true, isNewTargetConstructor);
        }

        private void defineBuiltin(String name, int length, int attributeFlags, boolean isConstructor, boolean isNewTargetConstructor) {
            class FactoryImpl implements BuiltinNodeFactory {
                private final boolean construct;
                private final boolean newTarget;

                FactoryImpl(boolean construct, boolean newTarget) {
                    this.construct = construct;
                    this.newTarget = newTarget;
                }

                @Override
                public Object createObject(JSContext context, JSBuiltin builtin) {
                    return Switch.this.createNode(context, builtin, construct, newTarget);
                }
            }

            assert !name.isEmpty();
            BuiltinNodeFactory call = new FactoryImpl(false, false);
            BuiltinNodeFactory construct = isConstructor ? new FactoryImpl(true, false) : null;
            BuiltinNodeFactory constructNewTarget = isNewTargetConstructor ? new FactoryImpl(true, true) : null;
            builtins.put(name, new JSBuiltin(getName(), name, length, attributeFlags, 5, false, call, construct, constructNewTarget));
        }

        protected abstract Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget);
    }

    /**
     * Builtins container for builtin nodes created via switch-enum dispatch method.
     */
    public abstract static class SwitchEnum<E extends Enum<E> & BuiltinEnum<E>> extends JSBuiltinsContainer {
        private final Class<E> enumType;

        protected SwitchEnum(String name, Class<E> enumType) {
            super(name);
            this.enumType = enumType;
            for (E builtin : enumType.getEnumConstants()) {
                if (builtin.isEnabled() && (!JSTruffleOptions.SubstrateVM || builtin.isAOTSupported())) {
                    loadBuiltin(builtin);
                }
            }
        }

        private void loadBuiltin(E builtinEnum) {
            class FactoryImpl implements BuiltinNodeFactory {
                private final boolean construct;
                private final boolean newTarget;

                FactoryImpl(boolean construct, boolean newTarget) {
                    this.construct = construct;
                    this.newTarget = newTarget;
                }

                @Override
                public Object createObject(JSContext context, JSBuiltin builtin) {
                    return SwitchEnum.this.createNode(context, builtin, construct, newTarget, builtinEnum);
                }
            }

            BuiltinNodeFactory call = new FactoryImpl(false, false);
            BuiltinNodeFactory construct = builtinEnum.isConstructor() ? new FactoryImpl(true, false) : null;
            BuiltinNodeFactory constructNewTarget = builtinEnum.isNewTargetConstructor() ? new FactoryImpl(true, true) : null;
            builtins.put(builtinEnum.getName(), createBuiltin(builtinEnum, call, construct, constructNewTarget));
        }

        private JSBuiltin createBuiltin(E builtinEnum, BuiltinNodeFactory functionNodeFactory, BuiltinNodeFactory constructorNodeFactory, BuiltinNodeFactory newTargetConstructorFactory) {
            Object key = builtinEnum.getKey();
            assert JSRuntime.isPropertyKey(key);
            int length = builtinEnum.getLength();
            int attributeFlags = JSAttributes.fromConfigurableEnumerableWritable(builtinEnum.isConfigurable(), builtinEnum.isEnumerable(), builtinEnum.isWritable());
            return new JSBuiltin(getName(), key, length, attributeFlags, builtinEnum.getECMAScriptVersion(), builtinEnum.isAnnexB(), functionNodeFactory, constructorNodeFactory,
                            newTargetConstructorFactory);
        }

        public Class<E> getEnumType() {
            return enumType;
        }

        protected abstract Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, E builtinEnum);
    }

    /**
     * Builtins container for functions defined via BuiltinNodeFactory lambdas.
     */
    public abstract static class Lambda extends JSBuiltinsContainer {

        protected Lambda(String name) {
            super(name);
        }

        protected final void defineFunction(String name, int length, BuiltinNodeFactory nodeFactory) {
            assert !name.isEmpty();
            builtins.put(name, new JSBuiltin(getName(), name, length, JSAttributes.getDefaultNotEnumerable(), nodeFactory));
        }

        protected final void defineFunction(String name, int length, int attributeFlags, BuiltinNodeFactory nodeFactory) {
            assert !name.isEmpty();
            builtins.put(name, new JSBuiltin(getName(), name, length, attributeFlags, nodeFactory));
        }

        protected final void defineConstructor(String name, int length, BuiltinNodeFactory nodeFactory, BuiltinNodeFactory constructorFactory) {
            assert !name.isEmpty();
            builtins.put(name, new JSBuiltin(getName(), name, length, JSAttributes.getDefaultNotEnumerable(), 5, false, nodeFactory, constructorFactory, null));
        }
    }
}
