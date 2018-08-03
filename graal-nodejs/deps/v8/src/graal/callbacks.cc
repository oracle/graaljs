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

// Call-backs from Java to C++

#include "callbacks.h"
#include "graal_context.h"
#include "graal_function_callback_info.h"
#include "graal_isolate.h"
#include "graal_missing_primitive.h"
#include "graal_module.h"
#include "graal_object.h"
#include "graal_promise.h"
#include "graal_property_callback_info.h"
#include "graal_string.h"
#include "graal_external.h"
#include "jni.h"
#include "../../../uv/include/uv.h"
#include "../../../../mxbuild/trufflenode/coremodules/node_snapshots.h"
#include <vector>
#include <stdlib.h>
#include <string.h>
#include <array>

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
    CALLBACK("executeAccessorGetter", "(JLjava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteAccessorGetter),
    CALLBACK("executeAccessorSetter", "(JLjava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/Object;)V", &GraalExecuteAccessorSetter),
    CALLBACK("executePropertyHandlerGetter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerGetter),
    CALLBACK("executePropertyHandlerSetter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)V", &GraalExecutePropertyHandlerSetter),
    CALLBACK("executePropertyHandlerQuery", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;", &GraalExecutePropertyHandlerQuery),
    CALLBACK("executePropertyHandlerDeleter", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;Z)Z", &GraalExecutePropertyHandlerDeleter),
    CALLBACK("executePropertyHandlerEnumerator", "(JLjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecutePropertyHandlerEnumerator),
    CALLBACK("deallocate", "(J)V", &GraalDeallocate),
    CALLBACK("weakCallback", "(JJI)V", &GraalWeakCallback),
    CALLBACK("notifyGCCallbacks", "(Z)V", &GraalNotifyGCCallbacks),
    CALLBACK("polyglotEngineEntered", "(JJJJIJIJ)V", &GraalPolyglotEngineEntered),
    CALLBACK("getCoreModuleBinarySnapshot", "(Ljava/lang/String;)Ljava/nio/ByteBuffer;", &GraalGetCoreModuleBinarySnapshot),
    CALLBACK("createAsyncHandle", "(JLjava/lang/Runnable;)J", &GraalCreateAsyncHandle),
    CALLBACK("closeAsyncHandle", "(J)V", &GraalCloseAsyncHandle),
    CALLBACK("sendAsyncHandle", "(J)V", &GraalSendAsyncHandle),
    CALLBACK("notifyPromiseHook", "(ILjava/lang/Object;Ljava/lang/Object;)V", &GraalNotifyPromiseHook),
    CALLBACK("notifyPromiseRejectionTracker", "(Ljava/lang/Object;ILjava/lang/Object;)V", &GraalNotifyPromiseRejectionTracker),
    CALLBACK("executeResolveCallback", "(JLjava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;", &GraalExecuteResolveCallback),
    CALLBACK("writeHostObject", "(JLjava/lang/Object;)V", &GraalWriteHostObject),
    CALLBACK("readHostObject", "(J)Ljava/lang/Object;", &GraalReadHostObject),
    CALLBACK("throwDataCloneError", "(JLjava/lang/String;)V", &GraalThrowDataCloneError),
    CALLBACK("getSharedArrayBufferId", "(JLjava/lang/Object;)I", &GraalGetSharedArrayBufferId)
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
    if (isolate->StackOverflowCheckEnabled() && isolate->StackOverflowCheck((long) &env)) { return nullptr; }
    bool context_mismatch = !isolate->GetJNIEnv()->IsSameObject(isolate->CurrentJavaContext(), java_context);
    v8::Local<v8::Context> context;
    if (context_mismatch) {
        GraalContext* graal_context = new GraalContext(isolate, java_context);
        context = reinterpret_cast<v8::Context*> (graal_context);
        isolate->ContextEnter(*context);
    }

    GraalFunctionCallbackInfo info(args);
    v8::FunctionCallback callback = (v8::FunctionCallback) isolate->GetFunctionTemplateCallback(id);
    callback(info);

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
    for (int i = 0; i < length - offset; i++) {
        jobject java_value = env->GetObjectArrayElement(arguments, i + 2);
        GraalValue* graal_value = GraalValue::FromJavaObject(isolate, java_value);
        values[length - offset - 1 - i] = graal_value;
    }
    jobject java_this = env->GetObjectArrayElement(arguments, 0);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, java_this);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
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
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), argc, is_new, true);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

GraalValue* AllocateNewTarget(GraalIsolate* isolate, jobject new_target, void* placement) {
    if (new_target == NULL) {
        return new(placement) GraalMissingPrimitive(isolate, isolate->GetUndefined()->GetJavaObject(), true);
    } else {
        return new(placement) GraalObject(isolate, new_target);
    }
}

jobject GraalExecuteFunction0(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    std::array<GraalValue*, 1> values;
    char memory[2][GraalValue::MAX_SIZE];
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[0]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[1]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 0, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteFunction1(JNIEnv* env, jclass nativeAccess, jint id,
        jobject this_object, jint this_type, jobject new_target,
        jobject argument1, jint argument1_type,
        jobject java_context) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->ResetSharedBuffer();
    std::array<GraalValue*, 2> values;
    char memory[3][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[1]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[2]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 1, new_target != NULL, false);
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
    char memory[4][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[2]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[3]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 2, new_target != NULL, false);
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
    char memory[5][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[3]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[4]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 3, new_target != NULL, false);
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
    char memory[6][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[4]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[5]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 4, new_target != NULL, false);
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
    char memory[7][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument5, argument5_type, true, memory[4]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[5]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[6]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 5, new_target != NULL, false);
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
    char memory[8][GraalValue::MAX_SIZE];
    int i = values.size() - 1;
    values[--i] = GraalValue::FromJavaObject(isolate, argument1, argument1_type, true, memory[0]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument2, argument2_type, true, memory[1]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument3, argument3_type, true, memory[2]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument4, argument4_type, true, memory[3]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument5, argument5_type, true, memory[4]);
    values[--i] = GraalValue::FromJavaObject(isolate, argument6, argument6_type, true, memory[5]);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, this_object, this_type, false, memory[6]);
    GraalValue* graal_callee = isolate->GetFunctionTemplateFunction(id);
    GraalValue* graal_new_target = AllocateNewTarget(isolate, new_target, memory[7]);
    GraalValue* graal_data = isolate->GetFunctionTemplateData(id);
    GraalFunctionCallbackArguments callbackArgs(isolate, graal_this, graal_callee, graal_new_target, graal_data, values.data(), 6, new_target != NULL, false);
    return GraalExecuteFunction(env, isolate, id, callbackArgs, java_context);
}

jobject GraalExecuteAccessorGetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jstring name, jobjectArray arguments, jobject data) {
    GraalIsolate* isolate = CurrentIsolateChecked();

    GraalString* graal_name = new GraalString(isolate, name);
    v8::String* property_name = reinterpret_cast<v8::String*> (graal_name);

    GraalPropertyCallbackInfo<v8::Value> info = GraalPropertyCallbackInfo<v8::Value>::New(isolate, arguments, 0, data, holder);
    v8::AccessorGetterCallback callback = (v8::AccessorGetterCallback)pointer;
    callback(property_name, info);

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    return isolate->CorrectReturnValue(**reinterpret_cast<GraalValue***> (&value), isolate->GetUndefined()->GetJavaObject());
}

void GraalExecuteAccessorSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jstring name, jobjectArray arguments, jobject data) {
    GraalIsolate* isolate = CurrentIsolateChecked();

    GraalString* graal_name = new GraalString(isolate, name);
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

void GraalExecutePropertyHandlerSetter(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();

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
}

jobject GraalExecutePropertyHandlerQuery(JNIEnv* env, jclass nativeAccess, jlong pointer, jobject holder, jobjectArray arguments, jobject data, jboolean named) {
    GraalIsolate* isolate = CurrentIsolateChecked();

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

    GraalPropertyCallbackInfo<v8::Array> info = GraalPropertyCallbackInfo<v8::Array>::New(isolate, arguments, 0, data, holder);
    v8::GenericNamedPropertyEnumeratorCallback callback = (v8::GenericNamedPropertyEnumeratorCallback)pointer;
    callback(info);

    v8::ReturnValue<v8::Value> value = info.GetReturnValue();
    GraalValue*** graal_value = reinterpret_cast<GraalValue***> (&value);
    jobject return_value = (**graal_value == nullptr) ? isolate->GetUndefined()->GetJavaObject() : env->NewLocalRef((**graal_value)->GetJavaObject());
    return return_value;
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
        v8::WeakCallbackInfo<void> callback_info = v8::WeakCallbackInfo<void>(v8::Isolate::GetCurrent(), (void*) data, internalFields, &second_callback);
        v8_callback(callback_info);
        if (second_callback) {
            second_callback(callback_info);
        }
    } else if (type == 2) {
        v8::WeakCallbackInfo<void>::Callback v8_callback = reinterpret_cast<v8::WeakCallbackInfo<void>::Callback> (callback);
        void** internalFields = (void**) data;
        v8::WeakCallbackInfo<void>::Callback second_callback = nullptr;
        v8::WeakCallbackInfo<void> callback_info = v8::WeakCallbackInfo<void>(v8::Isolate::GetCurrent(), internalFields[v8::kInternalFieldsInWeakCallback], internalFields, &second_callback);
        v8_callback(callback_info);
        if (second_callback) {
            second_callback(callback_info);
        }
        delete internalFields; // allocated in V8::MakeWeak
    }
}

extern "C" void uv_close(uv_handle_t* handle, uv_close_cb close_cb) __attribute__((weak));
extern "C" int uv_async_init(uv_loop_t* loop, uv_async_t* handle, uv_async_cb async_cb) __attribute__((weak));
extern "C" int uv_async_send(uv_async_t* handle) __attribute__((weak));

void GraalHandleClosed(uv_handle_t* handle) {
    free(handle);
}

void GraalNotifyGCCallbacks(JNIEnv* env, jclass nativeAccess, jboolean prolog) {
    GraalIsolate* isolate = CurrentIsolateChecked();
    isolate->NotifyGCCallbacks(prolog);
}

void GraalPolyglotEngineEntered(JNIEnv* env, jclass nativeAccess, jlong functionPointer, jlong isolate, jlong param1, jlong param2, jint argc, jlong argv, jint exec_argc, jlong exec_argv) {
    ((void (*) (void* isolate, void* param1, void* param2, int argc, void* argv, int exec_argc, void* exec_argv)) functionPointer)((void*) isolate, (void*) param1, (void*) param2, (int) argc, (void*) argv, (int) exec_argc, (void*) exec_argv);
}

GraalIsolate* CurrentIsolateChecked() {
    GraalIsolate* graal_isolate = CurrentIsolate();
    if (graal_isolate == nullptr) {
        fprintf(stderr, "Unable to find GraalIsolate for this thread! This code should be executed in the main thread!");
        exit(1);
    }
    return graal_isolate;
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

void GraalAsyncHandleTaskRunner(uv_async_t* handle) {
    JNIEnv* env = CurrentIsolateChecked()->GetJNIEnv();
    jobject runnable = (jobject) handle->data;
    jclass runnable_class = env->FindClass("java/lang/Runnable");
    jmethodID runMethodID = env->GetMethodID(runnable_class, "run", "()V");
    env->CallVoidMethod(runnable, runMethodID);
}

void GraalCloseAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong handlePtr) {
    uv_async_t* handle = reinterpret_cast<uv_async_t*> (handlePtr);
    jobject runnable = (jobject) handle->data;    
    env->DeleteGlobalRef(runnable);
    uv_close(reinterpret_cast<uv_handle_t*> (handle), &GraalHandleClosed);
}

void GraalSendAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong handle) {
    uv_async_send(reinterpret_cast<uv_async_t*> (handle));
}

jlong GraalCreateAsyncHandle(JNIEnv* env, jclass nativeAccess, jlong loopAddress, jobject runnable) {
    uv_loop_t* loop = (uv_loop_t*) loopAddress;
    uv_async_t* handle = (uv_async_t*) malloc(sizeof (uv_async_t));
    handle->data = env->NewGlobalRef(runnable);    
    uv_async_init(loop, handle, &GraalAsyncHandleTaskRunner);
    return (jlong) handle;
}

void GraalNotifyPromiseHook(JNIEnv* env, jclass nativeAccess, jint changeType, jobject java_promise, jobject java_parent) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    GraalPromise* graal_promise = new GraalPromise(graal_isolate, java_promise);
    GraalValue* graal_parent = GraalValue::FromJavaObject(graal_isolate, java_parent);
    v8::Local<v8::Promise> v8_promise = reinterpret_cast<v8::Promise*> (graal_promise);
    v8::Local<v8::Value> v8_parent = reinterpret_cast<v8::Value*> (graal_parent);
    graal_isolate->NotifyPromiseHook(static_cast<v8::PromiseHookType> (changeType), v8_promise, v8_parent);
}

void GraalNotifyPromiseRejectionTracker(JNIEnv* env, jclass nativeAccess, jobject java_promise, jint operation, jobject java_value) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    GraalPromise* graal_promise = new GraalPromise(graal_isolate, java_promise);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_value);
    v8::Local<v8::Promise> v8_promise = reinterpret_cast<v8::Promise*> (graal_promise);
    v8::PromiseRejectEvent event = static_cast<v8::PromiseRejectEvent> (operation);
    v8::Local<v8::Value> v8_value = reinterpret_cast<v8::Value*> (graal_value);
    graal_isolate->NotifyPromiseRejectCallback(v8::PromiseRejectMessage(v8_promise, event, v8_value, v8::Local<v8::StackTrace>()));
}

jobject GraalExecuteResolveCallback(JNIEnv* env, jclass nativeAccess, jlong callback, jobject java_context, jstring java_specifier, jobject java_referrer) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    GraalContext* graal_context = new GraalContext(graal_isolate, java_context);
    GraalString* graal_specifier = new GraalString(graal_isolate, java_specifier);
    GraalModule* graal_referrer = new GraalModule(graal_isolate, java_referrer);
    v8::Local<v8::Context> v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Local<v8::String> v8_specifier = reinterpret_cast<v8::String*> (graal_specifier);
    v8::Local<v8::Module> v8_referrer = reinterpret_cast<v8::Module*> (graal_referrer);
    v8::MaybeLocal<v8::Module> v8_result = ((v8::Module::ResolveCallback) callback)(v8_context, v8_specifier, v8_referrer);
    if (v8_result.IsEmpty()) {
        return NULL;
    } else {
        v8::Local<v8::Module> v8_module = v8_result.ToLocalChecked();
        GraalModule* graal_module = reinterpret_cast<GraalModule*> (*v8_module);
        return env->NewLocalRef(graal_module->GetJavaObject());
    }
}

void GraalWriteHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject java_object) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_object);
    v8::Object* object = reinterpret_cast<v8::Object*> (graal_value);
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    d->WriteHostObject(isolate, object);
}

jobject GraalReadHostObject(JNIEnv* env, jclass nativeAccess, jlong delegate) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    v8::ValueDeserializer::Delegate* d = reinterpret_cast<v8::ValueDeserializer::Delegate*> (delegate);
    v8::Local<v8::Object> object = d->ReadHostObject(isolate).ToLocalChecked();
    GraalValue* graal_value = reinterpret_cast<GraalValue*> (*object);
    return env->NewLocalRef(graal_value->GetJavaObject());
}

void GraalThrowDataCloneError(JNIEnv* env, jclass nativeAccess, jlong delegate, jstring java_message) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    GraalString* graal_message = new GraalString(graal_isolate, java_message);
    v8::String* message = reinterpret_cast<v8::String*> (graal_message);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    d->ThrowDataCloneError(message);
}

jint GraalGetSharedArrayBufferId(JNIEnv* env, jclass nativeAccess, jlong delegate, jobject sharedArrayBuffer) {
    GraalIsolate* graal_isolate = CurrentIsolateChecked();
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, sharedArrayBuffer);
    v8::SharedArrayBuffer* object = reinterpret_cast<v8::SharedArrayBuffer*> (graal_value);
    v8::ValueSerializer::Delegate* d = reinterpret_cast<v8::ValueSerializer::Delegate*> (delegate);
    return d->GetSharedArrayBufferId(isolate, object).FromJust();
}
