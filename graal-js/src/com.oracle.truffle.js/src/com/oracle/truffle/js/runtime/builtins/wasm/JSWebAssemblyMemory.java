/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins.wasm;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.wasm.WebAssemblyMemoryPrototypeBuiltins;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSConstructor;
import com.oracle.truffle.js.runtime.builtins.JSConstructorFactory;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.builtins.JSObjectFactory;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;

public class JSWebAssemblyMemory extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {
    public static final int MAX_MEMORY_SIZE = 32767;
    public static final TruffleString CLASS_NAME = Strings.constant("Memory");
    public static final TruffleString PROTOTYPE_NAME = Strings.constant("Memory.prototype");

    public static final TruffleString WEB_ASSEMBLY_MEMORY = Strings.constant("WebAssembly.Memory");

    public static final JSWebAssemblyMemory INSTANCE = new JSWebAssemblyMemory();

    @Override
    public TruffleString getClassName() {
        return CLASS_NAME;
    }

    public static boolean isJSWebAssemblyMemory(Object object) {
        return object instanceof JSWebAssemblyMemoryObject;
    }

    @Override
    public JSDynamicObject createPrototype(JSRealm realm, JSFunctionObject constructor) {
        JSObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(prototype, constructor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, WebAssemblyMemoryPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putAccessorsFromContainer(realm, prototype, WebAssemblyMemoryPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, WEB_ASSEMBLY_MEMORY);
        return prototype;
    }

    @Override
    public JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getWebAssemblyMemoryPrototype();
    }

    @Override
    public Shape makeInitialShape(JSContext ctx, JSDynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, INSTANCE, ctx);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    public static JSWebAssemblyMemoryObject create(JSContext context, JSRealm realm, Object wasmMemory, boolean shared) {
        return create(context, realm, INSTANCE.getIntrinsicDefaultProto(realm), wasmMemory, shared);
    }

    public static JSWebAssemblyMemoryObject create(JSContext context, JSRealm realm, JSDynamicObject proto, Object wasmMemory, boolean shared) {
        if (shared) {
            return createShared(context, realm, proto, wasmMemory);
        } else {
            Object embedderData = JSWebAssembly.getEmbedderData(realm, wasmMemory);
            if (embedderData instanceof JSWebAssemblyMemoryObject webAssemblyMemory) {
                return webAssemblyMemory;
            }
            JSWebAssemblyMemoryObject webAssemblyMemory = createImpl(context, realm, proto, wasmMemory, false);
            JSWebAssembly.setEmbedderData(realm, wasmMemory, webAssemblyMemory);
            return webAssemblyMemory;
        }
    }

    private static JSWebAssemblyMemoryObject createShared(JSContext context, JSRealm realm, JSDynamicObject proto, Object wasmMemory) {
        synchronized (wasmMemory) {
            Object embedderData = JSWebAssembly.getEmbedderData(realm, wasmMemory);
            EconomicMapHolder mapHolder;
            if (embedderData instanceof EconomicMapHolder) {
                mapHolder = (EconomicMapHolder) embedderData;
                JSWebAssemblyMemoryObject webAssemblyMemory = Boundaries.economicMapGet(mapHolder.map, realm.getAgent());
                if (webAssemblyMemory != null) {
                    return webAssemblyMemory;
                }
            } else {
                mapHolder = new EconomicMapHolder();
                JSWebAssembly.setEmbedderData(realm, wasmMemory, mapHolder);
            }
            JSWebAssemblyMemoryObject webAssemblyMemory = createImpl(context, realm, proto, wasmMemory, true);
            Boundaries.economicMapPut(mapHolder.map, realm.getAgent(), webAssemblyMemory);
            return webAssemblyMemory;
        }
    }

    private static JSWebAssemblyMemoryObject createImpl(JSContext context, JSRealm realm, JSDynamicObject proto, Object wasmMemory, boolean shared) {
        JSObjectFactory factory = context.getWebAssemblyMemoryFactory();
        var shape = factory.getShape(realm, proto);
        var object = factory.initProto(new JSWebAssemblyMemoryObject(shape, proto, wasmMemory, shared), realm, proto);
        return factory.trackAllocation(object);
    }

    // Invoked when the memory is resized
    public static void resetBuffers(JSRealm realm, Object wasmMemory) {
        Object embedderData = JSWebAssembly.getEmbedderData(realm, wasmMemory);
        if (embedderData instanceof JSWebAssemblyMemoryObject webAssemblyMemory) {
            webAssemblyMemory.resetBufferObject();
        } else if (embedderData instanceof EconomicMapHolder mapHolder) {
            synchronized (wasmMemory) {
                for (JSWebAssemblyMemoryObject webAssemblyMemory : mapHolder.map.getValues()) {
                    webAssemblyMemory.resetBufferObject();
                }
            }
        }

    }

    // EconomicMap is not an interop value => we cannot pass it to WasmMemory
    // => we need to wrap it in TruffleObject
    static class EconomicMapHolder implements TruffleObject {
        final EconomicMap<JSAgent, JSWebAssemblyMemoryObject> map = Boundaries.economicMapCreate();
    }

}
