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

#include "graal_array.h"
#include "graal_isolate.h"

#include "graal_array-inl.h"

GraalHandleContent* GraalArray::CopyImpl(jobject java_object_copy) {
    return GraalArray::Allocate(Isolate(), java_object_copy);
}

bool GraalArray::IsArray() const {
    return true;
}

v8::Local<v8::Array> GraalArray::New(v8::Isolate* isolate, int length) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, isolate, GraalAccessMethod::array_new, Object, java_context, length);
    GraalArray* graal_array = GraalArray::Allocate(graal_isolate, java_object);
    return reinterpret_cast<v8::Array*> (graal_array);
}

v8::Local<v8::Array> GraalArray::New(v8::Isolate* isolate, v8::Local<v8::Value>* elements, size_t length) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jobjectArray java_elements = env->NewObjectArray(length, graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < length; i++) {
        GraalValue* graal_element = reinterpret_cast<GraalValue*> (*elements[i]);
        jobject java_element = graal_element->GetJavaObject();
        env->SetObjectArrayElement(java_elements, i, java_element);
    }
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_array, isolate, GraalAccessMethod::array_new_from_elements, Object, java_context, java_elements);
    env->DeleteLocalRef(java_elements);
    GraalArray* graal_array = GraalArray::Allocate(graal_isolate, java_array);
    return reinterpret_cast<v8::Array*> (graal_array);
}

uint32_t GraalArray::Length() const {
    JNI_CALL(jlong, java_length, Isolate(), GraalAccessMethod::array_length, Long, GetJavaObject());
    return java_length;
}
