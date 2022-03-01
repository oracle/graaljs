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
package com.oracle.truffle.js.builtins;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Pair;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.BuiltinArgumentBuilder;
import com.oracle.truffle.js.nodes.function.BuiltinNodeFactory;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

/**
 * Intended to be subclassed by definitions of builtin functions.
 */
public class JSBuiltinsContainer {
    private final TruffleString name;
    private final EconomicMap<TruffleString, JSBuiltin> builtins = EconomicMap.create();
    private final EconomicMap<Object, Pair<JSBuiltin, JSBuiltin>> accessors = EconomicMap.create();

    protected JSBuiltinsContainer(TruffleString name) {
        assert name == null || JSRuntime.isPropertyKey(name);
        this.name = name;
    }

    public final JSBuiltin lookupFunctionByName(TruffleString methodName) {
        return builtins.get(methodName);
    }

    public final Pair<JSBuiltin, JSBuiltin> lookupAccessorByKey(Object key) {
        return accessors.get(key);
    }

    public final void forEachBuiltin(Consumer<? super JSBuiltin> consumer) {
        builtins.getValues().forEach(consumer);
    }

    public final void forEachAccessor(BiConsumer<? super JSBuiltin, ? super JSBuiltin> consumer) {
        accessors.getValues().forEach(pair -> consumer.accept(pair.getLeft(), pair.getRight()));
    }

    protected final void register(JSBuiltin builtin) {
        assert !builtins.containsKey(builtin.getName()) : builtin.getName();
        builtins.put(builtin.getName(), builtin);
        if (builtin.isGetter()) {
            Pair<JSBuiltin, JSBuiltin> existing = accessors.get(builtin.getKey(), Pair.empty());
            assert existing.getLeft() == null : builtin.getKey();
            accessors.put(builtin.getKey(), Pair.create(builtin, existing.getRight()));
        } else if (builtin.isSetter()) {
            Pair<JSBuiltin, JSBuiltin> existing = accessors.get(builtin.getKey(), Pair.empty());
            assert existing.getRight() == null : builtin.getKey();
            accessors.put(builtin.getKey(), Pair.create(existing.getLeft(), builtin));
        }
    }

    protected static BuiltinArgumentBuilder args() {
        return BuiltinArgumentBuilder.builder();
    }

    public final TruffleString getName() {
        return name;
    }

    public static <E extends Enum<E> & BuiltinEnum<E>> JSBuiltinsContainer fromEnum(TruffleString name, Class<E> builtinEnum) {
        return new SwitchEnum<>(name, builtinEnum);
    }

    public static <E extends Enum<E> & BuiltinEnum<E>> JSBuiltinsContainer fromEnum(Class<E> builtinEnum) {
        return fromEnum(null, builtinEnum);
    }

    /**
     * Builtins container for builtin nodes created via switch dispatch method.
     */
    public abstract static class Switch extends JSBuiltinsContainer {

        protected Switch(TruffleString name) {
            super(name);
        }

        protected final void defineFunction(TruffleString name, int length) {
            defineFunction(name, length, JSAttributes.getDefaultNotEnumerable());
        }

        protected final void defineFunction(TruffleString name, int length, int attributeFlags) {
            defineBuiltin(name, length, attributeFlags, false, false);
        }

        protected final void defineConstructor(TruffleString name, int length, boolean isNewTargetConstructor) {
            assert !Strings.isEmpty(name);
            defineBuiltin(name, length, JSAttributes.getDefaultNotEnumerable(), true, isNewTargetConstructor);
        }

        private void defineBuiltin(TruffleString name, int length, int attributeFlags, boolean isConstructor, boolean isNewTargetConstructor) {
            assert JSRuntime.isPropertyKey(name);
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

            assert !Strings.isEmpty(name);
            BuiltinNodeFactory call = new FactoryImpl(false, false);
            BuiltinNodeFactory construct = isConstructor ? new FactoryImpl(true, false) : null;
            BuiltinNodeFactory constructNewTarget = isNewTargetConstructor ? new FactoryImpl(true, true) : null;
            register(new JSBuiltin(getName(), name, name, length, attributeFlags, 5, false, call, construct, constructNewTarget));
        }

        protected abstract Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget);
    }

    /**
     * Builtins container for builtin nodes created via switch-enum dispatch method.
     */
    public static class SwitchEnum<E extends Enum<E> & BuiltinEnum<E>> extends JSBuiltinsContainer {
        private final Class<E> enumType;

        protected SwitchEnum(TruffleString name, Class<E> enumType) {
            super(name);
            this.enumType = enumType;
            for (E builtin : enumType.getEnumConstants()) {
                if (builtin.isEnabled() && (!JSConfig.SubstrateVM || builtin.isAOTSupported())) {
                    loadBuiltin(builtin);
                }
            }
        }

        protected SwitchEnum(Class<E> enumType) {
            this(null, enumType);
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
            assert JSRuntime.isPropertyKey(builtinEnum.getName());
            register(createBuiltin(builtinEnum, call, construct, constructNewTarget));
        }

        private JSBuiltin createBuiltin(E builtinEnum, BuiltinNodeFactory functionNodeFactory, BuiltinNodeFactory constructorNodeFactory, BuiltinNodeFactory newTargetConstructorFactory) {
            Object key = builtinEnum.getKey();
            assert JSRuntime.isPropertyKey(key);
            TruffleString name = builtinEnum.getName();
            int length = builtinEnum.getLength();
            int attributeFlags = JSAttributes.fromConfigurableEnumerableWritable(builtinEnum.isConfigurable(), builtinEnum.isEnumerable(), builtinEnum.isWritable());
            return new JSBuiltin(getName(), name, key, length, attributeFlags, builtinEnum.getECMAScriptVersion(), builtinEnum.isAnnexB(), functionNodeFactory,
                            constructorNodeFactory, newTargetConstructorFactory);
        }

        public Class<E> getEnumType() {
            return enumType;
        }

        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, E builtinEnum) {
            return builtinEnum.createNode(context, builtin, construct, newTarget);
        }
    }

    /**
     * Builtins container for functions defined via BuiltinNodeFactory lambdas.
     */
    public abstract static class Lambda extends JSBuiltinsContainer {

        protected Lambda(TruffleString name) {
            super(name);
        }

        protected final void defineFunction(TruffleString name, int length, BuiltinNodeFactory nodeFactory) {
            assert !Strings.isEmpty(name);
            register(new JSBuiltin(getName(), name, length, JSAttributes.getDefaultNotEnumerable(), nodeFactory));
        }

        protected final void defineFunction(TruffleString name, int length, int attributeFlags, BuiltinNodeFactory nodeFactory) {
            assert !Strings.isEmpty(name);
            register(new JSBuiltin(getName(), name, length, attributeFlags, nodeFactory));
        }

        protected final void defineConstructor(TruffleString name, int length, BuiltinNodeFactory nodeFactory, BuiltinNodeFactory constructorFactory) {
            assert !Strings.isEmpty(name);
            register(new JSBuiltin(getName(), name, name, length, JSAttributes.getDefaultNotEnumerable(), 5, false, nodeFactory, constructorFactory, null));
        }
    }
}
