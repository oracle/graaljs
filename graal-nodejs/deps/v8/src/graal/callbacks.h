/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef CALLBACKS_H_
#define CALLBACKS_H_

#include "jni.h"
#include "graal_isolate.h"

bool RegisterCallbacks(JNIEnv* env, jclass callback_class);

jobject GraalExecuteFunction(JNIEnv* env, jclass nativeAccess, jint id, jobjectArray arguments, jboolean is_new, jboolean is_new_target, jobject context);

jobject GraalExecuteFunction0(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject java_context);

jobject GraalExecuteFunction1(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject java_context);

jobject GraalExecuteFunction2(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject java_context);

jobject GraalExecuteFunction3(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject java_context);

jobject GraalExecuteFunction4(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject java_context);

jobject GraalExecuteFunction5(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject argument5, jint argument5_type,
        jobject java_context);

jobject GraalExecuteFunction6(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject argument5, jint argument5_type,
        jobject argument6, jint argument6_type,
        jobject java_context);

jobject GraalExecuteAccessorGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jstring name, jobjectArray arguments, jobject data);

void GraalExecuteAccessorSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jstring name, jobjectArray arguments, jobject data);

jobject GraalExecutePropertyHandlerGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

void GraalExecutePropertyHandlerSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jobject GraalExecutePropertyHandlerQuery(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jboolean GraalExecutePropertyHandlerDeleter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jobject GraalExecutePropertyHandlerEnumerator(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data);

void GraalDeallocate(JNIEnv* env, jclass nativeAccess, jlong pointer);

void GraalWeakCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data, jint type);

void GraalNotifyGCCallbacks(JNIEnv* env, jclass nativeAccess, jboolean prolog);

void GraalPolyglotEngineEntered(JNIEnv* env, jclass nativeAccess, jlong functionPointer, jlong isolate, jlong param1, jlong param2, jint argc, jlong argv, jint exec_argc, jlong exec_argv);

GraalIsolate* CurrentIsolateChecked();

jobject GraalGetCoreModuleBinarySnapshot(JNIEnv* env, jclass nativeAccess, jstring modulePath);

jlong GraalCreateAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong loopAddress, jobject runnable);

void GraalCloseAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong handle);

void GraalSendAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong handlePtr);

void GraalNotifyPromiseHook(JNIEnv* env, jclass nativeAccess, jint changeType, jobject java_promise, jobject java_parent);

void GraalNotifyPromiseRejectionTracker(JNIEnv* env, jclass nativeAccess, jobject java_promise, jint operation, jobject java_value);

jobject GraalExecuteResolveCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jstring java_specifier, jobject java_referrer);

#endif /* CALLBACKS_H_ */
