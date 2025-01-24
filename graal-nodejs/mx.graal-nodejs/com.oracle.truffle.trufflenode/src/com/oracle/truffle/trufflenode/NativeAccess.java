/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

/**
 *
 * @author Jan Stola
 */
public final class NativeAccess {

    private NativeAccess() {
    }

    public static native Object executeFunction(int id, Object[] arguments, boolean isNew, boolean isNewTarget, Object context);

    public static native Object executeFunction0(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object context);

    public static native Object executeFunction1(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object context);

    public static native Object executeFunction2(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object context);

    public static native Object executeFunction3(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object context);

    public static native Object executeFunction4(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    Object context);

    public static native Object executeFunction5(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    Object argument5, int argument5Type,
                    Object context);

    public static native Object executeFunction6(
                    int id,
                    Object thisObject, int thisType, Object newTarget,
                    Object argument1, int argument1Type,
                    Object argument2, int argument2Type,
                    Object argument3, int argument3Type,
                    Object argument4, int argument4Type,
                    Object argument5, int argument5Type,
                    Object argument6, int argument6Type,
                    Object context);

    public static native Object executeAccessorGetter(long functionPointer, Object holder, Object propertyName, Object[] arguments, Object additionalData);

    public static native void executeAccessorSetter(long functionPointer, Object holder, Object propertyName, Object[] arguments, Object additionalData);

    public static native Object executePropertyHandlerGetter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native boolean executePropertyHandlerSetter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native Object executePropertyHandlerQuery(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native boolean executePropertyHandlerDeleter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native Object executePropertyHandlerEnumerator(long functionPointer, Object holder, Object[] arguments, Object additionalData);

    public static native Object executePropertyHandlerDefiner(long functionPointer, Object holder, Object value, Object set, Object get, int flags, Object[] arguments, Object additionalData,
                    boolean named);

    public static native Object executePropertyHandlerDescriptor(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native void deallocate(long pointer);

    public static native void weakCallback(long callback, long data, int type);

    public static native void deleterCallback(long callback, long data, int length, long deleterData);

    public static native void notifyGCCallbacks(boolean prolog);

    public static native void polyglotEngineEntered(long callback, long isolate, long param1, long param2, long args, long execArgs);

    public static native ByteBuffer getCoreModuleBinarySnapshot(String modulePath);

    public static native void notifyPromiseHook(int changeType, Object promise, Object parentPromise);

    public static native void notifyPromiseRejectionTracker(Object promise, int operation, Object value);

    public static native void notifyImportMetaInitializer(Object importMeta, Object module);

    public static native Object executeResolveCallback(long callback, Object context, Object specifier, Object importAssertions, Object referrer);

    public static native Object executeImportModuleDynamicallyCallback(Object context, Object hostDefinedOptions, Object resourceName, Object specifier, Object importAssertions);

    public static native Object executePrepareStackTraceCallback(Object context, Object error, Object structuredStackTrace);

    public static native boolean hasCustomHostObject(long delegate);

    public static native boolean isHostObject(long delegate, Object object);

    public static native void writeHostObject(long delegate, Object object);

    public static native Object readHostObject(long delegate);

    public static native void throwDataCloneError(long delegate, Object message);

    public static native int getSharedArrayBufferId(long delegate, Object sharedArrayBuffer);

    public static native int getWasmModuleTransferId(long delegate, Object wasmModule);

    public static native Object getSharedArrayBufferFromId(long delegate, int id);

    public static native Object getWasmModuleFromId(long delegate, int id);

    public static native Object syntheticModuleEvaluationSteps(long callback, Object context, Object module);

    public static native void executeInterruptCallback(long callback, long data);

    public static native void notifyWasmStreamingCallback(Object response, Object resolve, Object reject);

    public static native void postWakeUpTask(long taskRunnerPointer);

    public static native void postRunnableTask(long taskRunnerPointer, Object runnable);

}
