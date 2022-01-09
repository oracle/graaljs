/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_array_buffer.h"
#include "graal_array_buffer_view.h"
#include "graal_isolate.h"

#include "graal_array_buffer-inl.h"
#include "graal_array_buffer_view-inl.h"

GraalHandleContent* GraalArrayBufferView::CopyImpl(jobject java_object_copy) {
    return new GraalArrayBufferView(Isolate(), java_object_copy, type_);
}

v8::Local<v8::ArrayBuffer> GraalArrayBufferView::Buffer() {
    jobject java_array_buffer;
    bool direct = IsDirect();
    if (direct && !IsDataView()) {
        java_array_buffer = Isolate()->JNIGetObjectFieldOrCall(GetJavaObject(), GraalAccessField::array_buffer_view_buffer, GraalAccessMethod::array_buffer_view_buffer);
    } else {
        JNI_CALL(jobject, java_buffer, Isolate(), GraalAccessMethod::array_buffer_view_buffer, Object, GetJavaObject());
        java_array_buffer = java_buffer;
    }
    GraalArrayBuffer* graal_array_buffer = GraalArrayBuffer::Allocate(Isolate(), java_array_buffer, direct);
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
    return type_ == kDirectUint8Array || type_ == kInteropUint8Array;
}

bool GraalArrayBufferView::IsUint8ClampedArray() const {
    return type_ == kDirectUint8ClampedArray || type_ == kInteropUint8ClampedArray;
}

bool GraalArrayBufferView::IsInt8Array() const {
    return type_ == kDirectInt8Array || type_ == kInteropInt8Array;
}

bool GraalArrayBufferView::IsUint16Array() const {
    return type_ == kDirectUint16Array || type_ == kInteropUint16Array;
}

bool GraalArrayBufferView::IsInt16Array() const {
    return type_ == kDirectInt16Array || type_ == kInteropInt16Array;
}

bool GraalArrayBufferView::IsUint32Array() const {
    return type_ == kDirectUint32Array || type_ == kInteropUint32Array;
}

bool GraalArrayBufferView::IsInt32Array() const {
    return type_ == kDirectInt32Array || type_ == kInteropInt32Array;
}

bool GraalArrayBufferView::IsFloat32Array() const {
    return type_ == kDirectFloat32Array || type_ == kInteropFloat32Array;
}

bool GraalArrayBufferView::IsFloat64Array() const {
    return type_ == kDirectFloat64Array || type_ == kInteropFloat64Array;
}

bool GraalArrayBufferView::IsBigInt64Array() const {
    return type_ == kDirectBigInt64Array || type_ == kInteropBigInt64Array;
}

bool GraalArrayBufferView::IsBigUint64Array() const {
    return type_ == kDirectBigUint64Array || type_ == kInteropBigUint64Array;
}

bool GraalArrayBufferView::IsDataView() const {
    return type_ == kDataView;
}

bool GraalArrayBufferView::IsDirect() const {
    return type_ >= kDirectUint8Array;
}
