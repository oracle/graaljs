/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_array_buffer.h"
#include "graal_isolate.h"

GraalArrayBuffer::GraalArrayBuffer(GraalIsolate* isolate, jobject java_array_buffer) : GraalObject(isolate, java_array_buffer) {
}

GraalHandleContent* GraalArrayBuffer::CopyImpl(jobject java_object_copy) {
    return new GraalArrayBuffer(Isolate(), java_object_copy);
}

size_t GraalArrayBuffer::ByteLength() const {
    jobject java_buffer = Isolate()->JNIGetObjectFieldOrCall(GetJavaObject(), GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
    jlong capacity = Isolate()->GetJNIEnv()->GetDirectBufferCapacity(java_buffer);
    Isolate()->GetJNIEnv()->DeleteLocalRef(java_buffer);
    return capacity;
}

v8::Local<v8::ArrayBuffer> GraalArrayBuffer::New(v8::Isolate* isolate, size_t byte_length) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_array_buffer, graal_isolate, GraalAccessMethod::array_buffer_new, Object, java_context, (jint) byte_length);
    return reinterpret_cast<v8::ArrayBuffer*> (new GraalArrayBuffer(graal_isolate, java_array_buffer));
}

v8::Local<v8::ArrayBuffer> GraalArrayBuffer::New(v8::Isolate* isolate, void* data, size_t byte_length, v8::ArrayBufferCreationMode mode) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_byte_buffer = graal_isolate->GetJNIEnv()->NewDirectByteBuffer(data, byte_length);
    jlong pointer = (mode == v8::ArrayBufferCreationMode::kInternalized) ? (jlong) data : 0;
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_array_buffer, isolate, GraalAccessMethod::array_buffer_new_buffer, Object, java_context, java_byte_buffer, pointer);
    graal_isolate->GetJNIEnv()->DeleteLocalRef(java_byte_buffer);
    return reinterpret_cast<v8::ArrayBuffer*> (new GraalArrayBuffer(graal_isolate, java_array_buffer));
}

bool GraalArrayBuffer::IsArrayBuffer() const {
    return true;
}
