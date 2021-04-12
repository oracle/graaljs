/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "graal_backing_store.h"
#include "graal_isolate.h"

#include "graal_array_buffer-inl.h"

GraalHandleContent* GraalArrayBuffer::CopyImpl(jobject java_object_copy) {
    return new GraalArrayBuffer(Isolate(), java_object_copy, IsDirect());
}

size_t GraalArrayBuffer::ByteLength() const {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_buffer;
    if (IsDirect()) {
        java_buffer = graal_isolate->JNIGetObjectFieldOrCall(GetJavaObject(), GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
    } else {
        JNI_CALL(jobject, java_not_direct_buffer, graal_isolate, GraalAccessMethod::array_buffer_get_contents, Object, GetJavaObject());
        java_buffer = java_not_direct_buffer;
    }
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jlong capacity = env->GetDirectBufferCapacity(java_buffer);
    env->DeleteLocalRef(java_buffer);
    return capacity;
}

v8::Local<v8::ArrayBuffer> GraalArrayBuffer::New(v8::Isolate* isolate, size_t byte_length) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_array_buffer, graal_isolate, GraalAccessMethod::array_buffer_new, Object, java_context, (jint) byte_length);
    return reinterpret_cast<v8::ArrayBuffer*> (new GraalArrayBuffer(graal_isolate, java_array_buffer, true));
}

v8::Local<v8::ArrayBuffer> GraalArrayBuffer::New(v8::Isolate* isolate, void* data, size_t byte_length, v8::ArrayBufferCreationMode mode) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_byte_buffer = graal_isolate->GetJNIEnv()->NewDirectByteBuffer(data, byte_length);
    jlong pointer = (mode == v8::ArrayBufferCreationMode::kInternalized) ? (jlong) data : 0;
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_array_buffer, isolate, GraalAccessMethod::array_buffer_new_buffer, Object, java_context, java_byte_buffer, pointer);
    graal_isolate->GetJNIEnv()->DeleteLocalRef(java_byte_buffer);
    return reinterpret_cast<v8::ArrayBuffer*> (new GraalArrayBuffer(graal_isolate, java_array_buffer, true));
}

v8::Local<v8::ArrayBuffer> GraalArrayBuffer::New(v8::Isolate* isolate, std::shared_ptr<v8::BackingStore> backing_store) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    jobject java_store = reinterpret_cast<GraalBackingStore*> (backing_store.get())->GetJavaStore();
    JNI_CALL(jobject, java_array_buffer, isolate, GraalAccessMethod::array_buffer_new_buffer, Object, java_context, java_store, 0);
    return reinterpret_cast<v8::ArrayBuffer*> (new GraalArrayBuffer(graal_isolate, java_array_buffer, true));
}

bool GraalArrayBuffer::IsArrayBuffer() const {
    return true;
}

bool GraalArrayBuffer::IsDirect() const {
    return direct_;
}

bool GraalArrayBuffer::IsExternal() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::array_buffer_is_external, Boolean, GetJavaObject());
    return result;
}

void GraalArrayBuffer::Detach() {
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::array_buffer_detach, GetJavaObject());
}

std::shared_ptr<v8::BackingStore> GraalArrayBuffer::GetBackingStore() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_array_buffer = GetJavaObject();
    jobject java_buffer;
    if (IsDirect()) {
        java_buffer = graal_isolate->JNIGetObjectFieldOrCall(java_array_buffer, GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
    } else {
        JNI_CALL(jobject, java_not_direct_buffer, graal_isolate, GraalAccessMethod::array_buffer_get_contents, Object, java_array_buffer);
        java_buffer = java_not_direct_buffer;
    }
    jobject java_store;
    if (java_buffer == nullptr) {
        java_store = nullptr; // detached buffer
    } else {
        JNIEnv* env = graal_isolate->GetJNIEnv();
        java_store = env->NewGlobalRef(java_buffer);
        env->DeleteLocalRef(java_buffer);
    }
    return std::shared_ptr<v8::BackingStore>(reinterpret_cast<v8::BackingStore*>(new GraalBackingStore(java_store)));
}
