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
package com.oracle.truffle.trufflenode.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DefinePropertyUtil;
import com.oracle.truffle.trufflenode.GraalJSAccess;
import com.oracle.truffle.trufflenode.NativeAccess;
import com.oracle.truffle.trufflenode.info.ObjectTemplate;
import com.oracle.truffle.trufflenode.info.PropertyHandler;

public class ExecuteNativePropertyHandlerNode extends JavaScriptRootNode {
    private final GraalJSAccess graalAccess;
    private final JSContext context;
    private final PropertyHandler namedHandler;
    private final PropertyHandler indexedHandler;
    private final Object proxy;
    private final Object namedHandlerData;
    private final Object indexedHandlerData;
    private final Mode mode;
    private final boolean stringKeysOnly;

    @Child private PropertyGetNode holderPropertyGetNode;

    public enum Mode {
        GETTER,
        SETTER,
        QUERY,
        DELETER,
        OWN_KEYS,
        GET_OWN_PROPERTY_DESCRIPTOR,
        DEFINE_PROPERTY
    }

    public ExecuteNativePropertyHandlerNode(GraalJSAccess graalAccess, JSContext context, ObjectTemplate template, Object proxy, Mode mode) {
        this.graalAccess = graalAccess;
        this.context = context;
        this.indexedHandler = template.getIndexedPropertyHandler();
        this.indexedHandlerData = (indexedHandler == null) ? null : indexedHandler.getData();
        this.namedHandler = template.getNamedPropertyHandler();
        this.namedHandlerData = (namedHandler == null) ? null : namedHandler.getData();
        this.stringKeysOnly = template.getStringKeysOnly();
        this.proxy = proxy;
        this.mode = mode;
        this.holderPropertyGetNode = PropertyGetNode.createGetHidden(GraalJSAccess.HOLDER_KEY, context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        Object holder = holderPropertyGetNode.getValue(arguments[1]);
        return executePropertyHandlerMethod(holder, arguments);
    }

    private Object executePropertyHandlerMethod(Object holder, Object[] arguments) {
        switch (mode) {
            case GETTER:
                return executeGetter(holder, arguments);
            case SETTER:
                return executeSetter(holder, arguments);
            case QUERY:
                return executeQuery(holder, arguments);
            case GET_OWN_PROPERTY_DESCRIPTOR:
                return executeGetOwnPropertyDescriptor(holder, arguments);
            case DELETER:
                return executeDeleter(holder, arguments);
            case OWN_KEYS:
                return executeOwnKeys(holder, arguments);
            case DEFINE_PROPERTY:
                return executeDefiner(holder, arguments);
            default:
                CompilerDirectives.transferToInterpreter();
                throw new IllegalArgumentException();
        }
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeGetter(Object holder, Object[] arguments) {
        Object result = null;
        Object key = arguments[3];
        if (!(key instanceof HiddenKey)) {
            if (JSRuntime.isArrayIndex(key)) {
                if (indexedHandler != null) {
                    result = NativeAccess.executePropertyHandlerGetter(indexedHandler.getGetter(), holder, arguments, indexedHandlerData, false);
                }
            } else if (namedHandler != null) {
                if (!(key instanceof Symbol)) {
                    key = JSRuntime.toString(key);
                }
                if (!stringKeysOnly || JSRuntime.isString(key)) {
                    result = NativeAccess.executePropertyHandlerGetter(namedHandler.getGetter(), holder, arguments, namedHandlerData, true);
                }
            }
        }
        if (result == null) {
            result = JSObject.get((DynamicObject) arguments[2], key);
        } else {
            result = graalAccess.correctReturnValue(result);
        }
        return result;
    }

    @CompilerDirectives.TruffleBoundary
    private boolean executeSetter(Object holder, Object[] arguments) {
        Object key = arguments[3];
        boolean handled = false;
        if (JSRuntime.isArrayIndex(key)) {
            if (indexedHandler != null && indexedHandler.getSetter() != 0) {
                handled = NativeAccess.executePropertyHandlerSetter(indexedHandler.getSetter(), holder, arguments, indexedHandlerData, false);
            }
        } else if (!(key instanceof HiddenKey) && (!stringKeysOnly || JSRuntime.isString(key))) {
            if (namedHandler != null && namedHandler.getSetter() != 0) {
                handled = NativeAccess.executePropertyHandlerSetter(namedHandler.getSetter(), holder, arguments, namedHandlerData, true);
            }
        }
        if (handled) {
            return true;
        } else {
            return JSObject.set((DynamicObject) arguments[2], key, arguments[4]);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeQuery(Object holder, Object[] arguments) {
        Object key = arguments[3];
        if (JSRuntime.isArrayIndex(key)) {
            if (indexedHandler != null) {
                Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2], arguments[3]);
                if (indexedHandler.getQuery() != 0) {
                    return (NativeAccess.executePropertyHandlerQuery(indexedHandler.getQuery(), holder, nativeCallArgs, indexedHandlerData, false) != null);
                } else if (indexedHandler.getDescriptor() != 0) {
                    Object result = NativeAccess.executePropertyHandlerDescriptor(indexedHandler.getDescriptor(), holder, nativeCallArgs, indexedHandlerData, false);
                    if (result != null) {
                        return true;
                    }
                }
            }
        } else if (!stringKeysOnly || JSRuntime.isString(key)) {
            if (namedHandler != null) {
                Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2], arguments[3]);
                if (namedHandler.getQuery() != 0) {
                    return (NativeAccess.executePropertyHandlerQuery(namedHandler.getQuery(), holder, nativeCallArgs, namedHandlerData, true) != null);
                } else if (namedHandler.getDescriptor() != 0) {
                    Object result = NativeAccess.executePropertyHandlerDescriptor(namedHandler.getDescriptor(), holder, nativeCallArgs, namedHandlerData, true);
                    if (result != null) {
                        return true;
                    }
                }
            }
        }
        DynamicObject target = (DynamicObject) arguments[2];
        return JSObject.hasProperty(target, key);
    }

    @CompilerDirectives.TruffleBoundary
    private boolean executeDeleter(Object holder, Object[] arguments) {
        boolean success = true;
        Object key = arguments[3];
        if (JSRuntime.isArrayIndex(key)) {
            if (indexedHandler != null && indexedHandler.getDeleter() != 0) {
                Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2], arguments[3]);
                success = NativeAccess.executePropertyHandlerDeleter(indexedHandler.getDeleter(), holder, nativeCallArgs, indexedHandlerData, false);
            }
        } else if (!stringKeysOnly || JSRuntime.isString(key)) {
            if (namedHandler != null && namedHandler.getDeleter() != 0) {
                Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2], arguments[3]);
                success = NativeAccess.executePropertyHandlerDeleter(namedHandler.getDeleter(), holder, nativeCallArgs, namedHandlerData, true);
            }
        }
        // Delete properties introduced through defineProperty trap
        if (JSObject.hasOwnProperty((DynamicObject) arguments[2], arguments[3])) {
            success &= JSObject.delete((DynamicObject) arguments[2], arguments[3]);
        }
        return success;
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeGetOwnPropertyDescriptor(Object holder, Object[] arguments) {
        Object key = arguments[3];
        PropertyDescriptor desc = null;
        if (JSRuntime.isArrayIndex(key)) {
            if (indexedHandler != null) {
                if (indexedHandler.getDescriptor() != 0) {
                    Object result = executeDescriptorCallback(holder, arguments, false);
                    if (result != null) {
                        return result;
                    }
                } else {
                    desc = executeGetOwnPropertyDescriptorHelper(holder, arguments, false);
                }
            }
            if (desc == null) {
                desc = JSObject.getOwnProperty((DynamicObject) arguments[2], arguments[3]);
            }
        } else if (!stringKeysOnly || JSRuntime.isString(key)) {
            if (namedHandler != null) {
                if (namedHandler.getDescriptor() != 0) {
                    Object result = executeDescriptorCallback(holder, arguments, true);
                    if (result != null) {
                        return result;
                    }
                } else {
                    desc = executeGetOwnPropertyDescriptorHelper(holder, arguments, true);
                }
            }
            if (desc == null) {
                desc = JSObject.getOwnProperty((DynamicObject) arguments[2], arguments[3]);
                if (desc == null && indexedHandler != null) {
                    // handles a suspicious part of indexedinterceptors-test in nan package
                    desc = executeGetOwnPropertyDescriptorHelper(holder, arguments, false);
                }
            }
        }
        return (desc == null) ? Undefined.instance : JSRuntime.fromPropertyDescriptor(desc, context);
    }

    private Object executeDescriptorCallback(Object holder, Object[] arguments, boolean named) {
        PropertyHandler handler = named ? namedHandler : indexedHandler;
        Object handlerData = named ? namedHandlerData : indexedHandlerData;
        return NativeAccess.executePropertyHandlerDescriptor(handler.getDescriptor(), holder, arguments, handlerData, named);
    }

    @CompilerDirectives.TruffleBoundary
    private PropertyDescriptor executeGetOwnPropertyDescriptorHelper(Object holder, Object[] arguments, boolean named) {
        PropertyDescriptor desc = null;
        PropertyHandler handler = named ? namedHandler : indexedHandler;
        Object handlerData = named ? namedHandlerData : indexedHandlerData;
        Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2], arguments[3]);
        Object attributes = null;
        if (handler.getQuery() != 0) {
            attributes = NativeAccess.executePropertyHandlerQuery(handler.getQuery(), holder, nativeCallArgs, handlerData, named);
            attributes = graalAccess.correctReturnValue(attributes);
        }
        if (attributes == null && handler.getEnumerator() != 0) {
            nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2]);
            DynamicObject ownKeys = (DynamicObject) NativeAccess.executePropertyHandlerEnumerator(handler.getEnumerator(), holder, nativeCallArgs, handlerData);
            if (JSRuntime.isArray(ownKeys) && arrayContains(ownKeys, arguments[3])) {
                desc = PropertyDescriptor.undefinedDataDesc;
            }
        } else {
            desc = JSObject.getOwnProperty((DynamicObject) arguments[2], arguments[3]);
            if (desc == null) {
                Object value = executeGetter(holder, JSArguments.create(arguments[0], arguments[1], arguments[2], arguments[3], proxy));
                desc = GraalJSAccess.propertyDescriptor(((Number) attributes).intValue(), value);
            }
        }
        return desc;
    }

    private static boolean arrayContains(DynamicObject array, Object item) {
        for (Object object : JSAbstractArray.toArray(array)) {
            if (object.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeOwnKeys(Object holder, Object[] arguments) {
        Object[] nativeCallArgs = JSArguments.create(proxy, arguments[1], arguments[2]);
        DynamicObject ownKeys = null;
        if (namedHandler != null && namedHandler.getEnumerator() != 0) {
            ownKeys = (DynamicObject) NativeAccess.executePropertyHandlerEnumerator(namedHandler.getEnumerator(), holder, nativeCallArgs, namedHandlerData);
        }
        if (indexedHandler != null && indexedHandler.getEnumerator() != 0) {
            DynamicObject ownKeys2 = (DynamicObject) NativeAccess.executePropertyHandlerEnumerator(indexedHandler.getEnumerator(), holder, nativeCallArgs, indexedHandlerData);
            if (JSArray.isJSArray(ownKeys2)) {
                // indexed handler returns array of numbers but we need an array of property keys
                long length = JSAbstractArray.arrayGetLength(ownKeys2);
                for (long i = 0; i < length; i++) {
                    Object key = JSObject.get(ownKeys2, i);
                    JSObject.set(ownKeys2, i, JSRuntime.toString(key));
                }
                ownKeys = concatArrays(ownKeys, ownKeys2);
            }
        }
        if (!JSArray.isJSArray(ownKeys)) {
            ownKeys = JSArray.createEmpty(context, getRealm(), 0);
        }
        return ownKeys;
    }

    private DynamicObject concatArrays(DynamicObject array1, DynamicObject array2) {
        if (JSArray.isJSArray(array1)) {
            if (JSArray.isJSArray(array2)) {
                List<Object> keys = new ArrayList<>(Arrays.asList(JSArray.toArray(array1)));
                for (Object key : JSArray.toArray(array2)) {
                    if (!keys.contains(key)) {
                        keys.add(key);
                    }
                }
                return JSArray.createConstant(context, getRealm(), keys.toArray());
            } else {
                return array1;
            }
        } else {
            return array2;
        }
    }

    @CompilerDirectives.TruffleBoundary
    private Object executeDefiner(Object holder, Object[] arguments) {
        Object key = arguments[3];
        PropertyDescriptor descriptor = JSRuntime.toPropertyDescriptor(arguments[4]);
        int flags = (descriptor.hasConfigurable() ? (1 << 0) : 0) +
                        (descriptor.getConfigurable() ? (1 << 1) : 0) +
                        (descriptor.hasEnumerable() ? (1 << 2) : 0) +
                        (descriptor.getEnumerable() ? (1 << 3) : 0) +
                        (descriptor.hasWritable() ? (1 << 4) : 0) +
                        (descriptor.getWritable() ? (1 << 5) : 0);
        DynamicObject target = (DynamicObject) arguments[2];
        Object result = Undefined.instance;
        if (JSRuntime.isArrayIndex(key)) {
            if (indexedHandler != null && indexedHandler.getDefiner() != 0) {
                result = NativeAccess.executePropertyHandlerDefiner(
                                indexedHandler.getDefiner(),
                                holder,
                                descriptor.getValue(),
                                descriptor.getGet(),
                                descriptor.getSet(),
                                flags,
                                arguments,
                                indexedHandlerData,
                                false);
            }
        } else if (!stringKeysOnly || JSRuntime.isString(key)) {
            if (namedHandler != null && namedHandler.getDefiner() != 0) {
                result = NativeAccess.executePropertyHandlerDefiner(
                                namedHandler.getDefiner(),
                                holder,
                                descriptor.getValue(),
                                descriptor.getGet(),
                                descriptor.getSet(),
                                flags,
                                arguments,
                                namedHandlerData,
                                true);
            }
        }
        if (result == Undefined.instance) {
            // request not intercepted by the definer
            PropertyDescriptor currentDesc = JSObject.getOwnProperty((DynamicObject) holder, key);
            if (DefinePropertyUtil.isCompatiblePropertyDescriptor(JSObject.isExtensible((DynamicObject) holder), descriptor, currentDesc)) {
                result = JSObject.defineOwnProperty(target, key, descriptor);
                if (descriptor.hasValue()) {
                    JSObject.set((DynamicObject) holder, key, descriptor.getValue());
                }
            } else {
                result = false;
            }
        }
        return result;
    }

}
