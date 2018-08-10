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

void GraalWriteHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_object);

jobject GraalReadHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate);

void GraalThrowDataCloneError(JNIEnv* env, jclass nativeAccess, jlong delegate, jstring java_message);

jint GraalGetSharedArrayBufferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject sharedArrayBuffer);

#endif /* CALLBACKS_H_ */
