/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

import org.graalvm.collections.EconomicMap;

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
    final EconomicMap<String, JSBuiltin> builtins = EconomicMap.create();

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
        builtins.getValues().forEach(consumer);
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
