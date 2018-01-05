/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_function.h"
#include "graal_isolate.h"
#include "graal_string.h"

GraalFunction::GraalFunction(GraalIsolate* isolate, jobject java_function) : GraalObject(isolate, java_function) {
}

GraalHandleContent* GraalFunction::CopyImpl(jobject java_object_copy) {
    return new GraalFunction(Isolate(), java_object_copy);
}

bool GraalFunction::IsFunction() const {
    return true;
}

jobjectArray GraalFunction::CreateJavaObjectArray(int argc, v8::Local<v8::Value> argv[]) const {
    JNIEnv* jni_env = Isolate()->GetJNIEnv();
    jobjectArray array = jni_env->NewObjectArray(argc, Isolate()->GetObjectClass(), NULL);
    for (int i = 0; i < argc; i++) {
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*argv[i]);
        jobject element = graal_value->GetJavaObject();
        jni_env->SetObjectArrayElement(array, i, element);
    }
    return array;
}

v8::Local<v8::Object> GraalFunction::NewInstance(int argc, v8::Local<v8::Value> argv[]) const {
    JNIEnv* env = Isolate()->GetJNIEnv();
    env->ExceptionClear();
    jobjectArray arguments = CreateJavaObjectArray(argc, argv);
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::function_new_instance, Object, GetJavaObject(), arguments);
    env->DeleteLocalRef(arguments);
    EXCEPTION_CHECK(env, v8::Object)
    GraalValue* graal_value = GraalValue::FromJavaObject(Isolate(), java_object);
    return reinterpret_cast<v8::Object*> (graal_value);
}

void GraalFunction::SetName(v8::Local<v8::String> name) {
    jobject java_name = reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_set_name, GetJavaObject(), java_name);
}

v8::Local<v8::Value> GraalFunction::GetName() const {
    JNI_CALL(jobject, java_name, Isolate(), GraalAccessMethod::function_get_name, Object, GetJavaObject());
    GraalString* graal_name = new GraalString(Isolate(), (jstring) java_name);
    return reinterpret_cast<v8::Value*> (graal_name);
}

v8::Local<v8::Value> GraalFunction::Call(v8::Local<v8::Value> recv, int argc, v8::Local<v8::Value> argv[]) {
    jobject java_receiver = reinterpret_cast<GraalValue*> (*recv)->GetJavaObject();
    jobjectArray arguments = CreateJavaObjectArray(argc, argv);
    jobject java_object = Call(java_receiver, arguments);
    if (java_object == NULL) {
        return v8::Local<v8::Value>();
    } else {
        GraalValue* graal_value = GraalValue::FromJavaObject(Isolate(), java_object);
        return reinterpret_cast<v8::Object*> (graal_value);
    }
}

jobject GraalFunction::Call(jobject recv, int argc, jobject argv[]) {
    GraalIsolate* graal_isolate = Isolate();
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jobjectArray array = env->NewObjectArray(argc, graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(array, i, argv[i]);
    }
    return Call(recv, array);
}

jobject GraalFunction::Call(jobject recv, jobjectArray arguments) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call, Object, GetJavaObject(), recv, arguments);
    graal_isolate->calls_on_stack_--;
    JNIEnv* env = graal_isolate->GetJNIEnv();
    env->DeleteLocalRef(arguments);
    if (java_object == NULL) {
        if (!graal_isolate->TryCatchExists()) {
            jthrowable java_exception = env->ExceptionOccurred();
            env->ExceptionClear();
            jobject java_context = graal_isolate->CurrentJavaContext();
            JNI_CALL(jobject, exception_object, graal_isolate, GraalAccessMethod::try_catch_exception, Object, java_context, java_exception);
            GraalValue* graal_exception = GraalValue::FromJavaObject(graal_isolate, exception_object);
            v8::Value* exception = reinterpret_cast<v8::Value*> (graal_exception);
            graal_isolate->SendMessage(v8::Exception::CreateMessage(exception), exception, java_exception);
            if (graal_isolate->error_to_ignore_ != nullptr) {
                env->DeleteGlobalRef(graal_isolate->error_to_ignore_);
                graal_isolate->error_to_ignore_ = nullptr;
            }
            if (graal_isolate->calls_on_stack_ != 0) {
                // If the process was not terminated then we have to ensure that
                // the rest of the script is not executed => we re-throw
                // the exception but remember that we should not report it again.
                graal_isolate->error_to_ignore_ = env->NewGlobalRef(java_exception);
                env->Throw(java_exception);
            }
        }
    }
    return java_object;
}

v8::ScriptOrigin GraalFunction::GetScriptOrigin() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_resource_name, graal_isolate, GraalAccessMethod::function_resource_name, Object, GetJavaObject());
    GraalValue* resource_name = (java_resource_name == nullptr) ? graal_isolate->GetUndefined() : new GraalString(graal_isolate, (jstring) java_resource_name);
    return v8::ScriptOrigin(reinterpret_cast<v8::Value*> (resource_name));
}

int GraalFunction::GetScriptLineNumber() const {
    JNI_CALL(jint, line, Isolate(), GraalAccessMethod::function_get_script_line_number, Int, GetJavaObject());
    return line;
}

int GraalFunction::GetScriptColumnNumber() const {
    JNI_CALL(jint, column, Isolate(), GraalAccessMethod::function_get_script_column_number, Int, GetJavaObject());
    return column;
}
