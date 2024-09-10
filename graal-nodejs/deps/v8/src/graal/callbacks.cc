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

// Call-backs from Java to C++

#include "callbacks.h"
#include "graal_array.h"
#include "graal_array_buffer.h"
#include "graal_array_buffer_view.h"
#include "graal_boolean.h"
#include "graal_context.h"
#include "graal_date.h"
#include "graal_external.h"
#include "graal_fixed_array.h"
#include "graal_function.h"
#include "graal_function_callback_info.h"
#include "graal_isolate.h"
#include "graal_map.h"
#include "graal_missing_primitive.h"
#include "graal_module.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_promise.h"
#include "graal_property_callback_info.h"
#include "graal_proxy.h"
#include "graal_script_or_module.h"
#include "graal_set.h"
#include "graal_string.h"
#include "graal_symbol.h"
#include "graal_wasm_streaming.h"
#include "jni.h"
#include <array>
#include <stdlib.h>
#include <string.h>
#include <vector>

#include "graal_context-inl.h"
#include "graal_fixed_array-inl.h"
#include "graal_function_callback_info-inl.h"
#include "graal_missing_primitive-inl.h"
#include "graal_module-inl.h"
#include "graal_promise-inl.h"
#include "graal_property_callback_info-inl.h"
#include "graal_script_or_module-inl.h"
#include "graal_string-inl.h"

#include "../../../../out/coremodules/node_snapshots.h"

#define CALLBACK(name, signature, pointer) {const_cast<char*>(name), const_cast<char*>(signature), reinterpret_cast<void*>(pointer)}

static const JNINativeMethod callbacks[] = {
    CALLBACK("executeFunction", "(I[Ljava/lang/Object;ZZLjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction),
    CALLBACK("executeFunction0", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction0),
    CALLBACK("executeFunction1", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction1),
    CALLBACK("executeFunction2", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction2),
    CALLBACK("executeFunction3", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction3),
    CALLBACK("executeFunction4", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction4),
    CALLBACK("executeFunction5", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction5),
    CALLBACK("executeFunction6", "(ILjava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", &GraalExecuteFunction6),
    CALLBACK("executeAccessorGetter", "(JLjava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteAccessorGetter),
    CALLBACK("executeAccessorSetter", "(JLjava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)V", &GraalExecuteAccessorSetter),
    CALLBACK("executePropertyHandlerGetter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerGetter),
    CALLBACK("executePropertyHandlerSetter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Z", &GraalExecutePropertyHandlerSetter),
    CALLBACK("executePropertyHandlerQuery", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerQuery),
    CALLBACK("executePropertyHandlerDeleter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Z", &GraalExecutePropertyHandlerDeleter),
    CALLBACK("executePropertyHandlerEnumerator", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecutePropertyHandlerEnumerator),
    CALLBACK("executePropertyHandlerDefiner", "(JLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerDefiner),
    CALLBACK("executePropertyHandlerDescriptor", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerDescriptor),
    CALLBACK("deallocate", "(J)V", &GraalDeallocate),
    CALLBACK("weakCallback", "(JJI)V", &GraalWeakCallback),
    CALLBACK("deleterCallback", "(JJIJ)V", &GraalDeleterCallback),
    CALLBACK("notifyGCCallbacks", "(Z)V", &GraalNotifyGCCallbacks),
    CALLBACK("polyglotEngineEntered", "(JJJJJJ)V", &GraalPolyglotEngineEntered),
    CALLBACK("getCoreModuleBinarySnapshot", "(Ljava/lang/String;)Ljava/nio/ByteBuffer;", &GraalGetCoreModuleBinarySnapshot),
    CALLBACK("notifyPromiseHook", "(ILjava/lang/Object;Ljava/lang/Object;)V", &GraalNotifyPromiseHook),
    CALLBACK("notifyPromiseRejectionTracker", "(Ljava/lang/Object;ILjava/lang/Object;)V", &GraalNotifyPromiseRejectionTracker),
    CALLBACK("notifyImportMetaInitializer", "(Ljava/lang/Object;Ljava/lang/Object;)V", &GraalNotifyImportMetaInitializer),
    CALLBACK("executeResolveCallback", "(JLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteResolveCallback),
    CALLBACK("executeImportModuleDynamicallyCallback", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteImportModuleDynamicallyCallback),
    CALLBACK("executePrepareStackTraceCallback", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecutePrepareStackTraceCallback),
    CALLBACK("writeHostObject", "(JLjava/lang/Object;)V", &GraalWriteHostObject),
    CALLBACK("readHostObject", "(J)Ljava/lang/Object;", &GraalReadHostObject),
    CALLBACK("throwDataCloneError", "(JLjava/lang/Object;)V", &GraalThrowDataCloneError),
    CALLBACK("getSharedArrayBufferId", "(JLjava/lang/Object;)I", &GraalGetSharedArrayBufferId),
    CALLBACK("getWasmModuleTransferId", "(JLjava/lang/Object;)I", &GraalGetWasmModuleTransferId),
    CALLBACK("getSharedArrayBufferFromId", "(JI)Ljava/lang/Object;", &GraalGetSharedArrayBufferFromId),
    CALLBACK("getWasmModuleFromId", "(JI)Ljava/lang/Object;", &GraalGetWasmModuleFromId),
    CALLBACK("syntheticModuleEvaluationSteps", "(JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalSyntheticModuleEvaluationSteps),
    CALLBACK("executeInterruptCallback", "(JJ)V", &GraalExecuteInterruptCallback),
    CALLBACK("notifyWasmStreamingCallback", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", &GraalNotifyWasmStreamingCallback),
    CALLBACK("postWakeUpTask", "(J)V", &GraalPostWakeUpTask),
    CALLBACK("postRunnableTask", "(JLjava/lang/Object;)V", &GraalPostRunnableTask),
 };

static const int CALLBACK_COUNT = sizeof(callbacks) / sizeof(*callbacks);

bool RegisterCallbacks(JNIEnv* env, jclass callback_class) {
    if (callback_class == NULL) {
        fprintf(stderr, "NativeAccess class not found!\n");
        return false;
    }

    for (int i = 0; i < CALLBACK_COUNT; i++) {
        JNINativeMethod callback = callbacks[i];
        jmethodID id = env->GetStaticMethodID(callback_class, callback.name, callback.signature);
        if (id == NULL) {
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
            }
            fprintf(stderr, "Cannot find method %s%s of NativeAccess!\n", callback.name, callback.signature);
            return false;
        }
    }

    env->RegisterNatives(callback_class, callbacks, CALLBACK_COUNT);

    return true;
}

jobject GraalExecuteFunction(JNIEnv* env, GraalIsolate* isolate, jint id, GraalFunctionCallbackArguments& args, jobject java_context) {
    if (isolate->StackOverflowCheckEnabled() && isolate->StackOverflowCheck((intptr_t) &env)) { return nullptr; }
    bool context_mismatch = !isolate->GetJNIEnv()->IsSameObject(isolate->CurrentJavaContext(), java_context);
    v8::Local<v8::Context> context;
    if (context_mismatch) {
        GraalContext* graal_context = GraalContext::Allocate(isolate, java_context);
        context = reinterpret_cast<v8::Context*> (graal_context);
        isolate->ContextEnter(*context);
    }

    // Enter "dummy TryCatch" to ensure that TryCatchExists() returns true
    // i.e. we do not invoke fatal error handler when the error can be caught
    // by JavaScript code that invoked this native method
    isolate->TryCatchEnter();

    GraalFunctionCallbackInfo info(args);
    v8::FunctionCallback callback = (v8::FunctionCallback) isolate->GetFunctionTemplateCallback(id);
    callback(info);

    // Exit "dummy TryCatch"
    isolate->TryCatchExit();

    if (context_mismatch) {
        isolate->ContextExit(*context);
    }

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), isolate->GetUndefined()->GetJavaObject());
}

jobject GraalExecuteFunction(JNIEnv* env, jclass nativeAccess, jint id, jobjectArray arguments, jboolean is_new, jboolean is_new_target, jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    int length = env->GetArrayLength(arguments); // first is "this", second is "callee"
    int offset = is_new_target ? 3 : 2;
    std::vector<GraalValue*> values(length - offset + 1);
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    for (int i = 0; i < length - offset; i++) {
        jobject java_value = env->GetObjectArrayElement(arguments, i + offset);
        GraalValue* graal_value = GraalValue::FromJavaObject(isolate, java_value);
        values[1 + i] = graal_value;
    }
    jobject java_this = env->GetObjectArrayElement(arguments, 0);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, java_this);
    GraalValue* graal_new_target;
    if (is_new_target) {
        jobject java_new_target = env->GetObjectArrayElement(arguments, 2);
        graal_new_target = GraalValue::FromJavaObject(isolate, java_new_target);
    } else if (is_new) {
        graal_new_target = graal_this;
    } else {
        graal_new_target = isolate->GetUndefined();
    }
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    int argc = length - offset;
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), argc, is_new, true);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

GraalValue* AllocateNewTarget(GraalIsolate* isolate, jobject new_target, void* placement) {
    if (new_target == NULL) {
        return GraalMissingPrimitive::Allocate(isolate, isolate->GetUndefined()->GetJavaObject(), true, placement);
    } else {
        return GraalObject::Allocate(isolate, new_target, placement);
    }
}

// Maximum size of GraalValue (excluding referenced objects) returned
// by GraalValue::FromJavaObject - used to reserve enough memory when the value
// is allocated on stack
static const int MAX_SIZE =
    (std::max)(sizeof(GraalArray),
    (std::max)(sizeof(GraalArrayBuffer),
    (std::max)(sizeof(GraalArrayBufferView),
    (std::max)(sizeof(GraalBoolean),
    (std::max)(sizeof(GraalDate),
    (std::max)(sizeof(GraalExternal),
    (std::max)(sizeof(GraalFunction),
    (std::max)(sizeof(GraalMap),
    (std::max)(sizeof(GraalNumber),
    (std::max)(sizeof(GraalObject),
    (std::max)(sizeof(GraalPromise),
    (std::max)(sizeof(GraalProxy),
    (std::max)(sizeof(GraalSet),
    (std::max)(sizeof(GraalString),
    sizeof(GraalSymbol)))))))))))))));

jobject GraalExecuteFunction0(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    std::array<GraalValue*, 1> values;
    char memory[2][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[0]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[1]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 0, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction1(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 2> values;
    char memory[3][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[1]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[2]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 1, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction2(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 3> values;
    char memory[4][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[2]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[3]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 2, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction3(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 4> values;
    char memory[5][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[3]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[4]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 3, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction4(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 5> values;
    char memory[6][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[4]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[5]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 4, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction5(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject argument5, jint argument5_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 6> values;
    char memory[7][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument5, argument5_type, true, memory[4]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[5]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[6]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 5, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction6(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject argument2, jint argument2_type,
        jobject argument3, jint argument3_type,
        jobject argument4, jint argument4_type,
        jobject argument5, jint argument5_type,
        jobject argument6, jint argument6_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 7> values;
    char memory[8][MAX_SIZE];
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument5, argument5_type, true, memory[4]);
    values[i++] = GraalValue::FromJavaObject(isolate, argument6, argument6_type, true, memory[5]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[6]);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[7]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 6, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteAccessorGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject name, jobjectArray arguments, jobject data) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    GraalValue* graal_name = GraalValue::FromJavaObject(isolate, name);
    v8::String* property_name = reinterpret_cast<v8::String*> (graal_name);

    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 0, data, holder);
    v8::AccessorGetterCallback callback = (v8::AccessorGetterCallback)pointer;
    callback(property_name, info);

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), isolate->GetUndefined()->GetJavaObject());
}

void GraalExecuteAccessorSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject name, jobjectArray arguments, jobject data) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    GraalValue* graal_name = GraalValue::FromJavaObject(isolate, name);
    v8::String* property_name = reinterpret_cast<v8::String*> (graal_name);

    jobject java_value = env->GetObjectArrayElement(arguments, 2);
    GraalValue* graal_value = GraalValue::FromJavaObject(isolate, java_value);
    v8::Value* property_value = reinterpret_cast<v8::Value*> (graal_value);

    GraalPropertyCallbackInfo<void> info = GraalPropertyCallbackInfo<void>::New(isolate, arguments, 0, data, holder);
    v8::AccessorSetterCallback callback = (v8::AccessorSetterCallback)pointer;
    callback(property_name, property_value, info);
}

jobject GraalExecutePropertyHandlerGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);
    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 4, data, holder);

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertyGetterCallback callback = (v8::GenericNamedPropertyGetterCallback)pointer;
        callback(property_name, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertyGetterCallback callback = (v8::IndexedPropertyGetterCallback)pointer;
        callback(index, info);
    }

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), nullptr);
}

jboolean GraalExecutePropertyHandlerSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);

    jobject java_value = env->GetObjectArrayElement(arguments, 4);
    GraalValue* graal_value = GraalValue::FromJavaObject(isolate, java_value);
    v8::Value* property_value = reinterpret_cast<v8::Value*> (graal_value);

    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 5, data, holder);

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertySetterCallback callback = (v8::GenericNamedPropertySetterCallback)pointer;
        callback(property_name, property_value, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertySetterCallback callback = (v8::IndexedPropertySetterCallback)pointer;
        callback(index, property_value, info);
    }

    v8::ReturnValue<v8::Value> result = info.GetReturnValue();
    GraalValue*** graal_result = reinterpret_cast<GraalValue***> (&result);
    return (**graal_result != nullptr);
}

jobject GraalExecutePropertyHandlerQuery(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);
    GraalPropertyCallbackInfo<v8::Integer> info = GraalPropertyCallbackInfo<v8::Integer>::New(isolate, arguments, 0, data, holder);

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertyQueryCallback callback = (v8::GenericNamedPropertyQueryCallback)pointer;
        callback(property_name, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertyQueryCallback callback = (v8::IndexedPropertyQueryCallback)pointer;
        callback(index, info);
    }

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), nullptr);
}

jboolean GraalExecutePropertyHandlerDeleter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);
    GraalPropertyCallbackInfo<v8::Boolean> info = GraalPropertyCallbackInfo<v8::Boolean>::New(isolate, arguments, 0, data, holder);

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertyDeleterCallback callback = (v8::GenericNamedPropertyDeleterCallback)pointer;
        callback(property_name, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertyDeleterCallback callback = (v8::IndexedPropertyDeleterCallback)pointer;
        callback(index, info);
    }

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    v8::Value* graal_value = * * reinterpret_cast<v8::Value***> (&value);
    jboolean result = (graal_value == nullptr) || graal_value->IsTrue();
    return result;
}

jobject GraalExecutePropertyHandlerEnumerator(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    GraalPropertyCallbackInfo<v8::Array> info = GraalPropertyCallbackInfo<v8::Array>::New(isolate, arguments, 0, data, holder);
    v8::GenericNamedPropertyEnumeratorCallback callback = (v8::GenericNamedPropertyEnumeratorCallback)pointer;
    callback(info);

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    GraalValue*** graal_value = reinterpret_cast<GraalValue***> (&value);
    jobject return_value = (**graal_value == nullptr) ? isolate->GetUndefined()->GetJavaObject() : env->NewLocalRef((**graal_value)->GetJavaObject());
    return return_value;
}

jobject GraalExecutePropertyHandlerDefiner(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobject value, jobject get, jobject set, int flags, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);
    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 0, data, holder);
    bool has_configurable = flags & (1 << 0);
    bool configurable = flags & (1 << 1);
    bool has_enumerable = flags & (1 << 2);
    bool enumerable = flags & (1 << 3);
    bool has_writable = flags & (1 << 4);
    bool writable = flags & (1 << 5);

    v8::PropertyDescriptor* descriptor;
    if (value != nullptr) {
        GraalValue* graal_value = GraalValue::FromJavaObject(isolate, value);
        v8::Local<v8::Value> v8_value = reinterpret_cast<v8::Value*> (graal_value);
        if (has_writable) {
            descriptor = new v8::PropertyDescriptor(v8_value, writable);
        } else {
            descriptor = new v8::PropertyDescriptor(v8_value);
        }
    } else if (get != nullptr || set != nullptr) {
        GraalValue* graal_get = (get == nullptr) ? nullptr : GraalValue::FromJavaObject(isolate, get);
        GraalValue* graal_set = (set == nullptr) ? nullptr : GraalValue::FromJavaObject(isolate, set);
        v8::Local<v8::Value> v8_get = reinterpret_cast<v8::Value*> (graal_get);
        v8::Local<v8::Value> v8_set = reinterpret_cast<v8::Value*> (graal_set);
        descriptor = new v8::PropertyDescriptor(v8_get, v8_set);
    } else {
        descriptor = new v8::PropertyDescriptor();
    }
    if (has_configurable) {
        descriptor->set_configurable(configurable);
    }
    if (has_enumerable) {
        descriptor->set_enumerable(enumerable);
    }

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertyDefinerCallback callback = (v8::GenericNamedPropertyDefinerCallback) pointer;
        callback(property_name, *descriptor, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertyDefinerCallback callback = (v8::IndexedPropertyDefinerCallback) pointer;
        callback(index, *descriptor, info);
    }
    delete descriptor;

    v8::ReturnValue<v8::Value> result = info.GetReturnValue();
    GraalValue*** graal_result = reinterpret_cast<GraalValue***> (&result);
    jobject java_result = (**graal_result == nullptr) ? isolate->GetUndefined()->GetJavaObject() : env->NewLocalRef((**graal_result)->GetJavaObject());
    return java_result;
}

jobject GraalExecutePropertyHandlerDescriptor(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));

    jobject java_key = env->GetObjectArrayElement(arguments, 3);
    GraalValue* graal_key = GraalValue::FromJavaObject(isolate, java_key);
    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 0, data, holder);

    if (named) {
        v8::Name* property_name = reinterpret_cast<v8::Name*> (graal_key);
        v8::GenericNamedPropertyDescriptorCallback callback = (v8::GenericNamedPropertyDescriptorCallback)pointer;
        callback(property_name, info);
    } else {
        uint32_t index = graal_key->ToUint32(reinterpret_cast<v8::Isolate*> (isolate))->Value();
        v8::IndexedPropertyDescriptorCallback callback = (v8::IndexedPropertyDescriptorCallback)pointer;
        callback(index, info);
    }

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), nullptr);
}

void GraalDeallocate(JNIEnv* env, jclass nativeAccess, jlong pointer) {
    free((void*) pointer);
}

void GraalWeakCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data, jint type) {
    if (type == 0) {
        fprintf(stderr, "GraalWeakCallback (type == 0) not supported anymore!\n");
        abort();
//        v8::WeakCallbackData<v8::Value, void>::Callback v8_callback = reinterpret_cast<v8::WeakCallbackData<v8::Value, void>::Callback> (callback);
//        v8_callback(v8::WeakCallbackData<v8::Value, void>(v8::Isolate::GetCurrent(), (void*) data, v8::Local<v8::Value>()));
    } else if (type == 1) {
        v8::WeakCallbackInfo<void>::Callback v8_callback = reinterpret_cast<v8::WeakCallbackInfo<void>::Callback> (callback);
        void* internalFields[v8::kInternalFieldsInWeakCallback];
        v8::WeakCallbackInfo<void>::Callback second_callback = nullptr;
        v8::Isolate* isolate = v8::Isolate::GetCurrent();
        v8::WeakCallbackInfo<void> callback_info = v8::WeakCallbackInfo<void>(isolate, (void*) data, internalFields, &second_callback);
        {
            v8::HandleScope scope(isolate);
            v8_callback(callback_info);
        }
        if (second_callback) {
            v8::HandleScope scope(isolate);
            second_callback(callback_info);
        }
    } else if (type == 2) {
        v8::WeakCallbackInfo<void>::Callback v8_callback = reinterpret_cast<v8::WeakCallbackInfo<void>::Callback> (callback);
        void** internalFields = (void**) data;
        v8::WeakCallbackInfo<void>::Callback second_callback = nullptr;
        v8::Isolate* isolate = v8::Isolate::GetCurrent();
        v8::WeakCallbackInfo<void> callback_info = v8::WeakCallbackInfo<void>(isolate, internalFields[v8::kInternalFieldsInWeakCallback], internalFields, &second_callback);
        {
            v8::HandleScope scope(isolate);
            v8_callback(callback_info);
        }
        if (second_callback) {
            v8::HandleScope scope(isolate);
            second_callback(callback_info);
        }
        delete internalFields; // allocated in V8::MakeWeak
    } else {
        fprintf(stderr, "Unknown GraalWeakCallback type: %d\n", type);
        abort();
    }
}

void GraalDeleterCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data, jint length, jlong deleterData) {
    v8::BackingStore::DeleterCallback v8_callback = reinterpret_cast<v8::BackingStore::DeleterCallback> (callback);
    v8_callback((void*) data, (size_t) length, (void*) deleterData);
}

void GraalNotifyGCCallbacks(JNIEnv* env, jclass nativeAccess, jboolean prolog) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (isolate));
    isolate->NotifyGCCallbacks(prolog);
}

void GraalPolyglotEngineEntered(JNIEnv* env, jclass nativeAccess, jlong functionPointer, jlong isolate, jlong param1, jlong param2, jlong args, jlong exec_args) {
    ((void (*) (void* isolate, void* param1, void* param2, void* args, void* exec_args)) functionPointer)((void*) isolate, (void*) param1, (void*) param2, (void*) args, (void*) exec_args);
}

GraalIsolate* CurrentIsolateChecked() {
    GraalIsolate* graal_isolate = CurrentIsolate();
    if (graal_isolate == nullptr) {
        NoCurrentIsolateError();
    }
    return graal_isolate;
}

void NoCurrentIsolateError() {
    fprintf(stderr, "Unable to find GraalIsolate for this thread! This code should be executed in the main thread!");
    exit(1);
}

jobject GraalGetCoreModuleBinarySnapshot(JNIEnv* env, jclass nativeAccess, jstring modulePath) {
    jobject retval = nullptr;
    int module_path_length = env->GetStringLength(modulePath);
    const jchar *module_path_utf16 = env->GetStringCritical(modulePath, nullptr);
    std::string module_path;
    for (int i = 0; i < module_path_length; i++) {
        module_path.push_back((char) module_path_utf16[i]);
    }
    env->ReleaseStringCritical(modulePath, module_path_utf16);

    auto lookup = node_snapshots::snapshots.find(module_path);
    if (lookup != node_snapshots::snapshots.end()) {
        const node_snapshots::byte_buffer_t snapshot_buf = lookup->second;
        retval = env->NewDirectByteBuffer(snapshot_buf.ptr, snapshot_buf.len);
    }

    return retval;
}

void GraalNotifyPromiseHook(JNIEnv* env, jclass nativeAccess, jint changeType, jobject java_promise, jobject java_parent) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalPromise* graal_promise = GraalPromise::Allocate(graal_isolate, java_promise);
    GraalValue* graal_parent = GraalValue::FromJavaObject(graal_isolate, java_parent);
    v8::Local<v8::Promise> v8_promise = reinterpret_cast<v8::Promise*> (graal_promise);
    v8::Local<v8::Value> v8_parent = reinterpret_cast<v8::Value*> (graal_parent);
    graal_isolate->NotifyPromiseHook(static_cast<v8::PromiseHookType> (changeType), v8_promise, v8_parent);
}

void GraalNotifyPromiseRejectionTracker(JNIEnv* env, jclass nativeAccess, jobject java_promise, jint operation, jobject java_value) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalPromise* graal_promise = GraalPromise::Allocate(graal_isolate, java_promise);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_value);
    v8::Local<v8::Promise> v8_promise = reinterpret_cast<v8::Promise*> (graal_promise);
    v8::PromiseRejectEvent event = static_cast<v8::PromiseRejectEvent> (operation);
    v8::Local<v8::Value> v8_value = reinterpret_cast<v8::Value*> (graal_value);
    graal_isolate->NotifyPromiseRejectCallback(v8::PromiseRejectMessage(v8_promise, event, v8_value));
}

void GraalNotifyImportMetaInitializer(JNIEnv* env, jclass nativeAccess, jobject java_import_meta, jobject java_module) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalObject* graal_import_meta = GraalObject::Allocate(graal_isolate, java_import_meta);
    GraalModule* graal_module = GraalModule::Allocate(graal_isolate, java_module);
    v8::Local<v8::Object> import_meta = reinterpret_cast<v8::Object*> (graal_import_meta);
    v8::Local<v8::Module> module = reinterpret_cast<v8::Module*> (graal_module);
    graal_isolate->NotifyImportMetaInitializer(import_meta, module);
}

jobject GraalExecuteResolveCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jobject java_specifier, jobject java_import_assertions, jobject java_referrer) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalContext* graal_context = GraalContext::Allocate(graal_isolate, java_context);
    GraalString* graal_specifier = GraalString::Allocate(graal_isolate, java_specifier);
    GraalModule* graal_referrer = GraalModule::Allocate(graal_isolate, java_referrer);
    GraalFixedArray* graal_import_assertions = GraalFixedArray::Allocate(graal_isolate, java_import_assertions);
    v8::Local<v8::Context> v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Local<v8::String> v8_specifier = reinterpret_cast<v8::String*> (graal_specifier);
    v8::Local<v8::Module> v8_referrer = reinterpret_cast<v8::Module*> (graal_referrer);
    v8::Local<v8::FixedArray> v8_import_assertions = reinterpret_cast<v8::FixedArray*> (graal_import_assertions);
    v8::MaybeLocal<v8::Module> v8_result = ((v8::Module::ResolveModuleCallback) callback)(v8_context, v8_specifier, v8_import_assertions, v8_referrer);
    if (v8_result.IsEmpty()) {
        return NULL;
    } else {
        v8::Local<v8::Module> v8_module = v8_result.ToLocalChecked();
        GraalModule* graal_module = reinterpret_cast<GraalModule*> (*v8_module);
        return env->NewLocalRef(graal_module->GetJavaObject());
    }
}

jobject GraalExecuteImportModuleDynamicallyCallback(JNIEnv* env, jclass nativeAccess, jobject java_context, jobject java_host_defined_options, jobject java_resource_name, jobject java_specifier, jobject java_import_assertions) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalContext* graal_context = GraalContext::Allocate(graal_isolate, java_context);
    GraalData* graal_host_defined_options = GraalFixedArray::Allocate(graal_isolate, java_host_defined_options);
    GraalString* graal_resource_name = GraalString::Allocate(graal_isolate, java_resource_name);
    GraalString* graal_specifier = GraalString::Allocate(graal_isolate, java_specifier);
    GraalFixedArray* graal_import_assertions = GraalFixedArray::Allocate(graal_isolate, java_import_assertions);
    v8::Local<v8::Context> v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Local<v8::Data> v8_host_defined_options = reinterpret_cast<v8::Data*> (graal_host_defined_options);
    v8::Local<v8::Value> v8_resource_name = reinterpret_cast<v8::Value*> (graal_resource_name);
    v8::Local<v8::String> v8_specifier = reinterpret_cast<v8::String*> (graal_specifier);
    v8::Local<v8::FixedArray> v8_import_assertions = reinterpret_cast<v8::FixedArray*> (graal_import_assertions);
    v8::MaybeLocal<v8::Promise> v8_result = graal_isolate->NotifyImportModuleDynamically(v8_context, v8_host_defined_options, v8_resource_name, v8_specifier, v8_import_assertions);
    if (v8_result.IsEmpty()) {
        return NULL;
    } else {
        v8::Local<v8::Promise> v8_promise = v8_result.ToLocalChecked();
        GraalPromise* graal_promise = reinterpret_cast<GraalPromise*> (*v8_promise);
        return env->NewLocalRef(graal_promise->GetJavaObject());
    }
}

jobject GraalExecutePrepareStackTraceCallback(JNIEnv* env, jclass nativeAccess, jobject java_context, jobject java_error, jobject java_stack_trace) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalContext* graal_context = GraalContext::Allocate(graal_isolate, java_context);
    GraalValue* graal_error = GraalValue::FromJavaObject(graal_isolate, java_error);
    GraalValue* graal_stack_trace = GraalValue::FromJavaObject(graal_isolate, java_stack_trace);
    v8::Local<v8::Context> v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Local<v8::Value> v8_error = reinterpret_cast<v8::Value*> (graal_error);
    v8::Local<v8::Array> v8_stack_trace = reinterpret_cast<v8::Array*> (graal_stack_trace);
    v8::MaybeLocal<v8::Value> v8_result = graal_isolate->NotifyPrepareStackTraceCallback(v8_context, v8_error, v8_stack_trace);
    if (v8_result.IsEmpty()) {
        return NULL;
    } else {
        v8::Local<v8::Value> v8_stack = v8_result.ToLocalChecked();
        GraalValue* graal_stack = reinterpret_cast<GraalValue*> (*v8_stack);
        return env->NewLocalRef(graal_stack->GetJavaObject());
    }
}

void GraalWriteHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_object) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_object);
    v8::Object* object = reinterpret_cast<v8::Object*> (graal_value);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    d->WriteHostObject(isolate, object);
}

jobject GraalReadHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    v8::ValueDeserializer::Delegate* d = reinterpret_cast<v8::ValueDeserializer::Delegate*> (delegate);
    v8::Local<v8::Object> object = d->ReadHostObject(isolate).ToLocalChecked();
    GraalValue* graal_value = reinterpret_cast<GraalValue*> (*object);
    return env->NewLocalRef(graal_value->GetJavaObject());
}

void GraalThrowDataCloneError(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_message) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalString* graal_message = GraalString::Allocate(graal_isolate, java_message);
    v8::String* message = reinterpret_cast<v8::String*> (graal_message);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    d->ThrowDataCloneError(message);
}

jint GraalGetSharedArrayBufferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject sharedArrayBuffer) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, sharedArrayBuffer);
    v8::SharedArrayBuffer* object = reinterpret_cast<v8::SharedArrayBuffer*> (graal_value);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    v8::Maybe<uint32_t> maybe_id = d->GetSharedArrayBufferId(isolate, object);
    return maybe_id.IsJust() ? maybe_id.FromJust() : -1;
}

jint GraalGetWasmModuleTransferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject wasmModule) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, wasmModule);
    v8::WasmModuleObject* object = reinterpret_cast<v8::WasmModuleObject*> (graal_value);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    v8::Maybe<uint32_t> maybe_id = d->GetWasmModuleTransferId(isolate, object);
    return maybe_id.IsJust() ? maybe_id.FromJust() : -1;
}

jobject GraalGetSharedArrayBufferFromId(JNIEnv* env, jclass nativeAccess, jlong delegate, jint id) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    v8::ValueDeserializer::Delegate* d = reinterpret_cast<v8::ValueDeserializer::Delegate*> (delegate);
    v8::MaybeLocal<v8::SharedArrayBuffer> v8_maybe_buffer = d->GetSharedArrayBufferFromId(isolate, id);
    if (v8_maybe_buffer.IsEmpty()) {
        return nullptr;
    } else {
        v8::Local<v8::SharedArrayBuffer> v8_buffer = v8_maybe_buffer.ToLocalChecked();
        return reinterpret_cast<GraalHandleContent*> (*v8_buffer)->GetJavaObject();
    }
}

jobject GraalGetWasmModuleFromId(JNIEnv* env, jclass nativeAccess, jlong delegate, jint id) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::HandleScope scope(isolate);
    v8::ValueDeserializer::Delegate* d = reinterpret_cast<v8::ValueDeserializer::Delegate*> (delegate);
    v8::MaybeLocal<v8::WasmModuleObject> v8_maybe_module = d->GetWasmModuleFromId(isolate, id);
    if (v8_maybe_module.IsEmpty()) {
        return nullptr;
    } else {
        v8::Local<v8::WasmModuleObject> v8_module = v8_maybe_module.ToLocalChecked();
        jobject java_module = reinterpret_cast<GraalHandleContent*> (*v8_module)->GetJavaObject();
        return env->NewLocalRef(java_module);
    }
}

jobject GraalSyntheticModuleEvaluationSteps(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jobject java_module) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (graal_isolate));
    GraalContext* graal_context = GraalContext::Allocate(graal_isolate, java_context);
    GraalModule* graal_module = GraalModule::Allocate(graal_isolate, java_module);
    v8::Local<v8::Context> v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Local<v8::Module> v8_module = reinterpret_cast<v8::Module*> (graal_module);
    v8::MaybeLocal<v8::Value> v8_result = ((v8::Module::SyntheticModuleEvaluationSteps) callback)(v8_context, v8_module);
    if (v8_result.IsEmpty()) {
        return NULL;
    } else {
        v8::Local<v8::Value> v8_value = v8_result.ToLocalChecked();
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*v8_value);
        return env->NewLocalRef(graal_value->GetJavaObject());
    }
}

void GraalExecuteInterruptCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jlong data) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    ((v8::InterruptCallback) callback)(isolate, (void*) data);
}

void GraalNotifyWasmStreamingCallback(JNIEnv* env, jclass nativeAccess, jobject response, jobject resolve, jobject reject) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (isolate);
    v8::WasmStreamingCallback callback = isolate->GetWasmStreamingCallback();

    std::array<GraalValue*, 2> values;
    v8::HandleScope scope(v8_isolate);
    int i = 1;
    values[i++] = GraalValue::FromJavaObject(isolate, response);
    GraalValue* graal_this = isolate->GetUndefined();
    GraalValue* graal_new_target = isolate->GetUndefined();
    v8::WasmStreaming::WasmStreamingImpl* wasm_streaming_impl = new v8::WasmStreaming::WasmStreamingImpl(isolate, resolve, reject);
    v8::WasmStreaming* wasm_streaming = new v8::WasmStreaming(std::unique_ptr<v8::WasmStreaming::WasmStreamingImpl>(wasm_streaming_impl));
    v8::Local<v8::External> v8_external = GraalExternal::New(v8_isolate, wasm_streaming);
    GraalValue* graal_data = reinterpret_cast<GraalValue*> (*v8_external);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_new_target, graal_data, values.data(), 1, false, true);

    GraalFunctionCallbackInfo info(callbackArgs);
    callback(info);
}

class WakeUpTask : public v8::Task {
public:
    WakeUpTask() {};
    void Run() {};
};

void GraalPostWakeUpTask(JNIEnv* env, jclass nativeAccess, jlong taskRunnerPointer) {
    std::unique_ptr<WakeUpTask> task = std::make_unique<WakeUpTask>();
    reinterpret_cast<v8::TaskRunner*> (taskRunnerPointer)->PostNonNestableTask(std::move(task));
}

class RunnableTask : public v8::Task {
public:
    RunnableTask(jobject runnable) : runnable_(runnable) {};
    void Run() {
        GraalIsolate* isolate = CurrentIsolateChecked();
        isolate->ExecuteRunnable(runnable_);
        isolate->GetJNIEnv()->DeleteGlobalRef(runnable_);
        runnable_ = nullptr;
    };
private:
    jobject runnable_;
};

void GraalPostRunnableTask(JNIEnv* env, jclass nativeAccess, jlong taskRunnerPointer, jobject runnable) {
    std::unique_ptr<RunnableTask> task = std::make_unique<RunnableTask>(env->NewGlobalRef(runnable));
    reinterpret_cast<v8::TaskRunner*> (taskRunnerPointer)->PostNonNestableTask(std::move(task));
}
