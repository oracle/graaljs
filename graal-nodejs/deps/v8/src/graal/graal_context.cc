/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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
#include "graal_function.h"
#include "graal_object.h"

#include "graal_context-inl.h"
#include "graal_object-inl.h"

// keep in sync with NODE_CONTEXT_EMBEDDER_DATA_INDEX
const int kNodeContextEmbedderDataIndex = 32;

GraalHandleContent* GraalContext::CopyImpl(jobject java_object_copy) {
    return GraalContext::Allocate(Isolate(), java_object_copy, cached_context_embedder_data_);
}

v8::Local<v8::Object> GraalContext::Global() {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::context_global, Object, GetJavaObject());
    GraalObject* graal_object = GraalObject::Allocate(Isolate(), java_object);
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

v8::Local<v8::Object> GraalContext::GetExtrasBindingObject() {
    JNI_CALL(jobject, java_extras, Isolate(), GraalAccessMethod::context_get_extras_binding_object, Object, GetJavaObject());
    GraalObject* graal_extras = GraalObject::Allocate(Isolate(), java_extras);
    return reinterpret_cast<v8::Object*> (graal_extras);
}

void GraalContext::SetPromiseHooks(v8::Local<v8::Function> init_hook, v8::Local<v8::Function> before_hook, v8::Local<v8::Function> after_hook, v8::Local<v8::Function> resolve_hook) {
    jobject java_init_hook = init_hook.IsEmpty() ? nullptr : reinterpret_cast<GraalFunction*> (*init_hook)->GetJavaObject();
    jobject java_before_hook = before_hook.IsEmpty() ? nullptr : reinterpret_cast<GraalFunction*> (*before_hook)->GetJavaObject();
    jobject java_after_hook = after_hook.IsEmpty() ? nullptr : reinterpret_cast<GraalFunction*> (*after_hook)->GetJavaObject();
    jobject java_resolve_hook = resolve_hook.IsEmpty() ? nullptr : reinterpret_cast<GraalFunction*> (*resolve_hook)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::context_set_promise_hooks, GetJavaObject(), java_init_hook, java_before_hook, java_after_hook, java_resolve_hook);
}

bool GraalContext::IsCodeGenerationFromStringsAllowed() const {
    JNI_CALL(jboolean, allowed, Isolate(), GraalAccessMethod::context_is_code_generation_from_strings_allowed, Boolean, GetJavaObject());
    return allowed;
}
