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

#include "graal_context.h"
#include "graal_object.h"

// keep in sync with NODE_CONTEXT_EMBEDDER_DATA_INDEX
const int kNodeContextEmbedderDataIndex = 32;

GraalContext::GraalContext(GraalIsolate* isolate, jobject java_context, void* cached_context_embedder_data) :
GraalHandleContent(isolate, java_context), cached_context_embedder_data_(cached_context_embedder_data) {
    UseDefaultSecurityToken();
}

GraalHandleContent* GraalContext::CopyImpl(jobject java_object_copy) {
    return new GraalContext(Isolate(), java_object_copy, cached_context_embedder_data_);
}

v8::Local<v8::Object> GraalContext::Global() {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::context_global, Object, GetJavaObject());
    GraalObject* graal_object = new GraalObject(Isolate(), java_object);
    return reinterpret_cast<v8::Object*> (graal_object);
}

void GraalContext::SetAlignedPointerInEmbedderData(int index, void* value) {
    if (index == kNodeContextEmbedderDataIndex) {
        if (value != nullptr && SlowGetAlignedPointerFromEmbedderData(index) != nullptr) {
            fprintf(stderr, "Context::SetAlignedPointerInEmbedderData(%d) called more than once! Its caching can be incorrect!", kNodeContextEmbedderDataIndex);
            abort();
        }
        cached_context_embedder_data_ = value;
    }
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::context_set_pointer_in_embedder_data, GetJavaObject(), (jint) index, (jlong) value);
}

void* GraalContext::SlowGetAlignedPointerFromEmbedderData(int index) {
    if (index == kNodeContextEmbedderDataIndex) {
        if (cached_context_embedder_data_ != nullptr) {
            return cached_context_embedder_data_;
        }
    }
    JNI_CALL(jlong, pointer, Isolate(), GraalAccessMethod::context_get_pointer_in_embedder_data, Long, GetJavaObject(), (jint) index);
    if (index == kNodeContextEmbedderDataIndex) {
        cached_context_embedder_data_ = (void*)pointer;
    }
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
