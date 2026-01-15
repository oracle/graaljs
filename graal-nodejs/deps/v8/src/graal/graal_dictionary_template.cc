/*
 * Copyright (c) 2025, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_isolate.h"

#include "graal_context-inl.h"
#include "graal_dictionary_template-inl.h"
#include "graal_object-inl.h"

GraalHandleContent* GraalDictionaryTemplate::CopyImpl(jobject java_object_copy) {
    return new GraalDictionaryTemplate(Isolate(), java_object_copy);
}

v8::Local<v8::DictionaryTemplate> GraalDictionaryTemplate::New(v8::Isolate* isolate, v8::MemorySpan<const std::string_view> names) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    int length = names.size();
    jobjectArray java_names = env->NewObjectArray(length, graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < length; i++) {
        jstring name = env->NewStringUTF(names[i].data());
        env->SetObjectArrayElement(java_names, i, name);
    }
    GraalDictionaryTemplate* graal_dictionary_template = new GraalDictionaryTemplate(graal_isolate, java_names);
    v8::DictionaryTemplate* v8_dictionary_template = reinterpret_cast<v8::DictionaryTemplate*> (graal_dictionary_template);
    return v8::Local<v8::DictionaryTemplate>::New(isolate, v8_dictionary_template);
}

v8::Local<v8::Object> GraalDictionaryTemplate::NewInstance(v8::Local<v8::Context> context, v8::MemorySpan<v8::MaybeLocal<v8::Value>> property_values) {
    v8::Isolate* v8_isolate = context->GetIsolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (v8_isolate);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    int length = property_values.size();
    jobjectArray java_values = env->NewObjectArray(length, graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < length; i++) {
        v8::MaybeLocal<v8::Value> value = property_values[i];
        jobject java_value;
        if (value.IsEmpty()) {
            java_value = NULL;
        } else {
            v8::Local<v8::Value> v8_value = value.ToLocalChecked();
            GraalHandleContent* graal_value = reinterpret_cast<GraalHandleContent*> (*v8_value);
            java_value = graal_value->GetJavaObject();
        }
        env->SetObjectArrayElement(java_values, i, java_value);
    }
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::dictionary_template_new_instance, Object, java_context, GetJavaObject(), java_values);
    GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
    v8::Object* v8_object = reinterpret_cast<v8::Object*> (graal_object);
    return v8::Local<v8::Object>::New(v8_isolate, v8_object);
}
