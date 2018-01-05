/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_array.h"
#include "graal_isolate.h"

GraalArray::GraalArray(GraalIsolate* isolate, jobject java_array) : GraalObject(isolate, java_array) {
}

GraalHandleContent* GraalArray::CopyImpl(jobject java_object_copy) {
    return new GraalArray(Isolate(), java_object_copy);
}

bool GraalArray::IsArray() const {
    return true;
}

v8::Local<v8::Array> GraalArray::New(v8::Isolate* isolate, int length) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, isolate, GraalAccessMethod::array_new, Object, java_context, length);
    GraalArray* graal_array = new GraalArray(graal_isolate, java_object);
    return reinterpret_cast<v8::Array*> (graal_array);
}

uint32_t GraalArray::Length() const {
    JNI_CALL(jlong, java_length, Isolate(), GraalAccessMethod::array_length, Long, GetJavaObject());
    return java_length;
}
