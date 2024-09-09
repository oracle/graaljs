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

jobject GraalExecuteAccessorGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject name, jobjectArray arguments, jobject data);

void GraalExecuteAccessorSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject name, jobjectArray arguments, jobject data);

jobject GraalExecutePropertyHandlerGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jboolean GraalExecutePropertyHandlerSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jobject GraalExecutePropertyHandlerQuery(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jboolean GraalExecutePropertyHandlerDeleter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

jobject GraalExecutePropertyHandlerEnumerator(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data);

jobject GraalExecutePropertyHandlerDefiner(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject value, jobject get, jobject set, int flags, jobjectArray arguments, jobject data, jboolean named);

jobject GraalExecutePropertyHandlerDescriptor(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named);

void GraalDeallocate(JNIEnv* env, jclass nativeAccess, jlong pointer);

void GraalWeakCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data, jint type);

void GraalDeleterCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data, jint length, jlong deleterData);

void GraalNotifyGCCallbacks(JNIEnv* env, jclass nativeAccess, jboolean prolog);

void GraalPolyglotEngineEntered(JNIEnv* env, jclass nativeAccess, jlong functionPointer, jlong isolate, jlong param1, jlong param2, jlong args, jlong exec_args);

GraalIsolate* CurrentIsolateChecked();

void NoCurrentIsolateError();

jobject GraalGetCoreModuleBinarySnapshot(JNIEnv* env, jclass nativeAccess, jstring modulePath);

void GraalNotifyPromiseHook(JNIEnv* env, jclass nativeAccess, jint changeType, jobject java_promise, jobject java_parent);

void GraalNotifyPromiseRejectionTracker(JNIEnv* env, jclass nativeAccess, jobject java_promise, jint operation, jobject java_value);

void GraalNotifyImportMetaInitializer(JNIEnv* env, jclass nativeAccess, jobject java_import_meta, jobject java_module);

jobject GraalExecuteImportModuleDynamicallyCallback(JNIEnv* env, jclass nativeAccess, jobject java_context, jobject java_host_defined_options, jobject java_resource_name, jobject java_specifier, jobject java_import_assertions);

jobject GraalExecuteResolveCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jobject java_specifier, jobject java_import_assertions, jobject java_referrer);

void GraalWriteHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_object);

jobject GraalReadHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate);

void GraalThrowDataCloneError(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_message);

jint GraalGetSharedArrayBufferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject sharedArrayBuffer);

jint GraalGetWasmModuleTransferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject wasmModule);

jobject GraalGetSharedArrayBufferFromId(JNIEnv* env, jclass nativeAccess, jlong delegate, jint id);

jobject GraalGetWasmModuleFromId(JNIEnv* env, jclass nativeAccess, jlong delegate, jint id);

jobject GraalExecutePrepareStackTraceCallback(JNIEnv* env, jclass nativeAccess, jobject java_context, jobject java_error, jobject java_stack_trace);

jobject GraalSyntheticModuleEvaluationSteps(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jobject java_module);

void GraalExecuteInterruptCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data);

void GraalNotifyWasmStreamingCallback(JNIEnv* env, jclass nativeAccess, jobject response, jobject resolve, jobject reject);

void GraalPostWakeUpTask(JNIEnv* env, jclass nativeAccess, jlong taskRunnerPointer);

void GraalPostRunnableTask(JNIEnv* env, jclass nativeAccess, jlong taskRunnerPointer, jobject runnable);

#endif /* CALLBACKS_H_ */
