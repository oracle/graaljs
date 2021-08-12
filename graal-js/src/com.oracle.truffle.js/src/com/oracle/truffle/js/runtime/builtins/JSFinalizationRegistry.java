/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.FinalizationRegistryPrototypeBuiltins;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSFinalizationRegistry extends JSNonProxy implements JSConstructorFactory.Default, PrototypeSupplier {

    public static final JSFinalizationRegistry INSTANCE = new JSFinalizationRegistry();

    public static final String CLASS_NAME = "FinalizationRegistry";
    public static final String PROTOTYPE_NAME = "FinalizationRegistry.prototype";

    public static final HiddenKey FINALIZATION_REGISTRY_ID = new HiddenKey("FinalizationRegistry");

    private JSFinalizationRegistry() {
    }

    public static DynamicObject create(JSContext context, JSRealm realm, Object cleanupCallback) {
        JSObjectFactory factory = context.getFinalizationRegistryFactory();
        JSFinalizationRegistryObject obj = factory.initProto(new JSFinalizationRegistryObject(factory.getShape(realm), cleanupCallback, new ArrayList<>(), new ReferenceQueue<>()), realm);
        assert isJSFinalizationRegistry(obj);
        context.registerFinalizationRegistry(obj);
        context.trackAllocation(obj);
        return obj;
    }

    @Override
    public DynamicObject createPrototype(final JSRealm realm, DynamicObject ctor) {
        JSContext ctx = realm.getContext();
        DynamicObject prototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putConstructorProperty(ctx, prototype, ctor);
        JSObjectUtil.putFunctionsFromContainer(realm, prototype, FinalizationRegistryPrototypeBuiltins.BUILTINS);
        JSObjectUtil.putToStringTag(prototype, CLASS_NAME);
        return prototype;
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject prototype) {
        return JSObjectUtil.getProtoChildShape(prototype, JSFinalizationRegistry.INSTANCE, context);
    }

    public static JSConstructor createConstructor(JSRealm realm) {
        return INSTANCE.createConstructorAndPrototype(realm);
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return getClassName();
    }

    @Override
    @TruffleBoundary
    public String toDisplayStringImpl(DynamicObject obj, int depth, boolean allowSideEffects) {
        return "[" + getClassName() + "]";
    }

    public static boolean isJSFinalizationRegistry(Object obj) {
        return obj instanceof JSFinalizationRegistryObject;
    }

    @Override
    public DynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getFinalizationRegistryPrototype();
    }

    @TruffleBoundary
    public static void appendToCells(JSFinalizationRegistryObject finalizationRegistry, Object target, Object holdings, Object unregisterToken) {
        List<FinalizationRecord> cells = finalizationRegistry.getCells();
        ReferenceQueue<Object> queue = finalizationRegistry.getReferenceQueue();
        WeakReference<Object> weakTarget = new WeakReference<>(target, queue);
        cells.add(new FinalizationRecord(weakTarget, holdings, unregisterToken));
    }

    @TruffleBoundary
    public static boolean removeFromCells(JSFinalizationRegistryObject finalizationRegistry, Object unregisterToken) {
        List<FinalizationRecord> cells = finalizationRegistry.getCells();
        boolean removed = false;
        for (Iterator<FinalizationRecord> iterator = cells.iterator(); iterator.hasNext();) {
            FinalizationRecord record = iterator.next();
            if (JSRuntime.isSameValue(record.getUnregisterToken().get(), unregisterToken)) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    @TruffleBoundary
    public static void cleanupFinalizationRegistry(JSFinalizationRegistryObject finalizationRegistry, Object callbackArg) {
        Object callback = callbackArg == Undefined.instance ? finalizationRegistry.getCleanupCallback() : callbackArg;
        FinalizationRecord cell;
        while ((cell = removeCellEmptyTarget(finalizationRegistry)) != null) {
            assert (cell.getWeakRefTarget().get() == null);
            JSRuntime.call(callback, Undefined.instance, new Object[]{cell.getHeldValue()});
        }
    }

    @TruffleBoundary
    public static FinalizationRecord removeCellEmptyTarget(JSFinalizationRegistryObject finalizationRegistry) {
        List<FinalizationRecord> cells = finalizationRegistry.getCells();
        for (int i = 0; i < cells.size(); i++) {
            FinalizationRecord record = cells.get(i);
            if (record.getWeakRefTarget().get() == null) {
                cells.remove(i);
                return record;
            }
        }
        return null;
    }

    /**
     * 4.1.3 Execution and 4.1.4.1 HostCleanupFinalizationRegistry.
     */
    public static void hostCleanupFinalizationRegistry(JSFinalizationRegistryObject finalizationRegistry) {
        // if something can be polled, clean up the FinalizationRegistry
        ReferenceQueue<Object> queue = finalizationRegistry.getReferenceQueue();
        boolean queueNotEmpty = (queue.poll() != null);
        // Cleared WeakReferences may not appear in ReferenceQueue immediatelly
        // but V8 tests expect the invocation of the callbacks as soon as possible
        // => do not wait for enqueuing in TestV8 mode.
        boolean performCleanup = queueNotEmpty || JSObject.getJSContext(finalizationRegistry).getContextOptions().isTestV8Mode();
        if (performCleanup) {
            // empty the ReferenceQueue
            Object o;
            do {
                o = queue.poll();
            } while (o != null);
            cleanupFinalizationRegistry(finalizationRegistry, Undefined.instance);
        }
    }

}
