/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_array_buffer_view.h"
#include "graal_array_buffer.h"
#include "graal_isolate.h"

GraalArrayBufferView::GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type) : GraalArrayBufferView(isolate, java_array_buffer_view, type, -1, -1) {
}

GraalArrayBufferView::GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type, int byte_length, int byte_offset) :
GraalObject(isolate, java_array_buffer_view),
type_(type),
byte_length_(byte_length),
byte_offset_(byte_offset) {
}

GraalHandleContent* GraalArrayBufferView::CopyImpl(jobject java_object_copy) {
    return new GraalArrayBufferView(Isolate(), java_object_copy, type_);
}

v8::Local<v8::ArrayBuffer> GraalArrayBufferView::Buffer() {
    jobject java_array_buffer;
    if (IsDataView()) {
        JNI_CALL(jobject, java_buffer, Isolate(), GraalAccessMethod::array_buffer_view_buffer, Object, GetJavaObject());
        java_array_buffer = java_buffer;
    } else {
        java_array_buffer = Isolate()->JNIGetObjectFieldOrCall(GetJavaObject(), GraalAccessField::array_buffer_view_buffer, GraalAccessMethod::array_buffer_view_buffer);
    }
    GraalArrayBuffer* graal_array_buffer = new GraalArrayBuffer(Isolate(), java_array_buffer);
    return reinterpret_cast<v8::ArrayBuffer*> (graal_array_buffer);
}

bool GraalArrayBufferView::IsArrayBufferView() const {
    return true;
}

size_t GraalArrayBufferView::ByteLength() {
    if (byte_length_ == -1) {
        jobject java_context = Isolate()->CurrentJavaContext();
        JNI_CALL(jint, length, Isolate(), GraalAccessMethod::array_buffer_view_byte_length, Int, java_context, GetJavaObject());
        byte_length_ = length;
    }
    return byte_length_;
}

size_t GraalArrayBufferView::ByteOffset() {
    if (byte_offset_ == -1) {
        jobject java_context = Isolate()->CurrentJavaContext();
        JNI_CALL(jint, offset, Isolate(), GraalAccessMethod::array_buffer_view_byte_offset, Int, java_context, GetJavaObject());
        byte_offset_ = offset;
    }
    return byte_offset_;
}

bool GraalArrayBufferView::IsUint8Array() const {
    return type_ == kUint8Array;
}

bool GraalArrayBufferView::IsUint8ClampedArray() const {
    return type_ == kUint8ClampedArray;
}

bool GraalArrayBufferView::IsInt8Array() const {
    return type_ == kInt8Array;
}

bool GraalArrayBufferView::IsUint16Array() const {
    return type_ == kUint16Array;
}

bool GraalArrayBufferView::IsInt16Array() const {
    return type_ == kInt16Array;
}

bool GraalArrayBufferView::IsUint32Array() const {
    return type_ == kUint32Array;
}

bool GraalArrayBufferView::IsInt32Array() const {
    return type_ == kInt32Array;
}

bool GraalArrayBufferView::IsFloat32Array() const {
    return type_ == kFloat32Array;
}

bool GraalArrayBufferView::IsFloat64Array() const {
    return type_ == kFloat64Array;
}

bool GraalArrayBufferView::IsDataView() const {
    return type_ == kDataView;
}
