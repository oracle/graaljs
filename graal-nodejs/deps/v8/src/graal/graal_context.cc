/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_context.h"
#include "graal_object.h"

GraalContext::GraalContext(GraalIsolate* isolate, jobject java_context) :
GraalHandleContent(isolate, java_context) {
    UseDefaultSecurityToken();
}

GraalHandleContent* GraalContext::CopyImpl(jobject java_object_copy) {
    return new GraalContext(Isolate(), java_object_copy);
}

v8::Local<v8::Object> GraalContext::Global() {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::context_global, Object, GetJavaObject());
    GraalObject* graal_object = new GraalObject(Isolate(), java_object);
    return reinterpret_cast<v8::Object*> (graal_object);
}

void GraalContext::SetAlignedPointerInEmbedderData(int index, void* value) {
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::context_set_pointer_in_embedder_data, GetJavaObject(), (jint) index, (jlong) value);
}

void* GraalContext::SlowGetAlignedPointerFromEmbedderData(int index) {
    JNI_CALL(jlong, pointer, Isolate(), GraalAccessMethod::context_get_pointer_in_embedder_data, Long, GetJavaObject(), (jint) index);
    return (void*) pointer;
}

void GraalContext::SetEmbedderData(int index, v8::Local<v8::Value> value) {
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::context_set_embedder_data, GetJavaObject(), (jint) index, java_value);
}

v8::Local<v8::Value> GraalContext::SlowGetEmbedderData(int index) {
    JNI_CALL(jobject, java_value, Isolate(), GraalAccessMethod::context_get_embedder_data, Object, GetJavaObject(), (jint) index);
    GraalValue* value = GraalValue::FromJavaObject(Isolate(), java_value);
    return reinterpret_cast<v8::Value*> (value);
}

void GraalContext::SetSecurityToken(v8::Local<v8::Value> token) {
    GraalValue* graal_token = reinterpret_cast<GraalValue*> (*token);
    jobject java_token = graal_token->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::context_set_security_token, GetJavaObject(), java_token);
}

v8::Local<v8::Value> GraalContext::GetSecurityToken() {
    JNI_CALL(jobject, java_token, Isolate(), GraalAccessMethod::context_get_security_token, Object, GetJavaObject());
    GraalValue* graal_token = GraalValue::FromJavaObject(Isolate(), java_token);
    return reinterpret_cast<v8::Value*> (graal_token);
}

void GraalContext::UseDefaultSecurityToken() {
    SetSecurityToken(Global());
}
