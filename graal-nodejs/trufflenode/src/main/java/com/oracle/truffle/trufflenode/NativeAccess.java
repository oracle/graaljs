/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    public static native Object executeAccessorGetter(long functionPointer, Object holder, String propertyName, Object[] arguments, Object additionalData);

    public static native void executeAccessorSetter(long functionPointer, Object holder, String propertyName, Object[] arguments, Object additionalData);

    public static native Object executePropertyHandlerGetter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native void executePropertyHandlerSetter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native Object executePropertyHandlerQuery(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native boolean executePropertyHandlerDeleter(long functionPointer, Object holder, Object[] arguments, Object additionalData, boolean named);

    public static native Object executePropertyHandlerEnumerator(long functionPointer, Object holder, Object[] arguments, Object additionalData);

    public static native void deallocate(long pointer);

    public static native void weakCallback(long callback, long data, int type);

    public static native void notifyGCCallbacks(boolean prolog);

    public static native void polyglotEngineEntered(long callback, long isolate, long param1, long param2, int argc, long argv, int exec_argc, long exec_argv);

    public static native ByteBuffer getCoreModuleBinarySnapshot(String modulePath);

    public static native long createAsyncHandle(long loopAddress, Runnable onEventHandled);

    public static native void closeAsyncHandle(long asyncHandle);

    public static native void sendAsyncHandle(long asyncHandle);

    public static native void notifyPromiseHook(int changeType, Object promise, Object parentPromise);

    public static native void notifyPromiseRejectionTracker(Object promise, int operation, Object value);

    public static native Object executeResolveCallback(long callback, Object context, String specifier, Object referrer);

}
