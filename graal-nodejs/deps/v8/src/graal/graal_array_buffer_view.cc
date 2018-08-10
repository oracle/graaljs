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
        JNI_CALL(jint, length, Isolate(), GraalAccessMethod::array_buffer_view_byte_length, Int, GetJavaObject());
        byte_length_ = length;
    }
    return byte_length_;
}

size_t GraalArrayBufferView::ByteOffset() {
    if (byte_offset_ == -1) {
        JNI_CALL(jint, offset, Isolate(), GraalAccessMethod::array_buffer_view_byte_offset, Int, GetJavaObject());
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
