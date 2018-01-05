/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_context.h"
#include "graal_isolate.h"
#include "graal_module.h"
#include "graal_string.h"

GraalModule::GraalModule(GraalIsolate* isolate, jobject java_module) : GraalHandleContent(isolate, java_module) {
}

GraalHandleContent* GraalModule::CopyImpl(jobject java_object_copy) {
    return new GraalModule(Isolate(), java_object_copy);
}

v8::MaybeLocal<v8::Module> GraalModule::Compile(v8::Local<v8::String> source, v8::Local<v8::String> name) {
    GraalString* graal_source = reinterpret_cast<GraalString*> (*source);
    jobject java_source = graal_source->GetJavaObject();
    jobject java_name = name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source->Isolate();
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_module, graal_isolate, GraalAccessMethod::module_compile, Object, java_context, java_source, java_name);
    if (java_module == NULL) {
        return v8::Local<v8::Module>();
    } else {
        GraalModule* graal_module = new GraalModule(graal_isolate, java_module);
        v8::Local<v8::Module> v8_module = reinterpret_cast<v8::Module*> (graal_module);
        return v8_module;
    }
}

v8::Maybe<bool> GraalModule::InstantiateModule(v8::Local<v8::Context> context, v8::Module::ResolveCallback callback) {
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    jlong java_callback = (jlong) callback;
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::module_instantiate, java_context, GetJavaObject(), java_callback);
    return v8::Just(true);
}

v8::MaybeLocal<v8::Value> GraalModule::Evaluate(v8::Local<v8::Context> context) {
    GraalIsolate* graal_isolate = Isolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::module_evaluate, Object, java_context, GetJavaObject());
    if (java_result == NULL) {
        return v8::Local<v8::Value>();
    } else {
        GraalValue* graal_result = GraalValue::FromJavaObject(graal_isolate, java_result);
        v8::Local<v8::Value> v8_result = reinterpret_cast<v8::Value*> (graal_result);
        return v8_result;
    }
}

v8::Module::Status GraalModule::GetStatus() const {
    JNI_CALL(jint, java_status, Isolate(), GraalAccessMethod::module_get_status, Int, GetJavaObject());
    return static_cast<v8::Module::Status> (java_status);
}

int GraalModule::GetModuleRequestsLength() const {
    JNI_CALL(jint, length, Isolate(), GraalAccessMethod::module_get_requests_length, Int, GetJavaObject());
    return length;
}

v8::Local<v8::String> GraalModule::GetModuleRequest(int index) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_request, graal_isolate, GraalAccessMethod::module_get_request, Object, GetJavaObject(), (jint) index);
    GraalString* graal_request = new GraalString(graal_isolate, (jstring) java_request);
    return reinterpret_cast<v8::String*> (graal_request);
}

v8::Local<v8::Value> GraalModule::GetModuleNamespace() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_namespace, graal_isolate, GraalAccessMethod::module_get_namespace, Object, GetJavaObject());
    GraalValue* graal_namespace = GraalValue::FromJavaObject(graal_isolate, java_namespace);
    return reinterpret_cast<v8::Value*> (graal_namespace);
}

int GraalModule::GetIdentityHash() const {
    JNI_CALL(jint, hash, Isolate(), GraalAccessMethod::module_get_identity_hash, Int, GetJavaObject());
    return hash;
}
