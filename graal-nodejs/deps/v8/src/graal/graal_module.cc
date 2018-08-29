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
#include "graal_isolate.h"
#include "graal_module.h"
#include "graal_string.h"

GraalModule::GraalModule(GraalIsolate* isolate, jobject java_module) : GraalHandleContent(isolate, java_module) {
}

GraalHandleContent* GraalModule::CopyImpl(jobject java_object_copy) {
    return new GraalModule(Isolate(), java_object_copy);
}

v8::MaybeLocal<v8::Module> GraalModule::Compile(v8::Local<v8::String> source, v8::Local<v8::String> name) {
    GraalString* graal_source = reinterpret_cast<GraalString*> (*source);
    jobject java_source = graal_source->GetJavaObject();
    jobject java_name = name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source->Isolate();
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_module, graal_isolate, GraalAccessMethod::module_compile, Object, java_context, java_source, java_name);
    if (java_module == NULL) {
        return v8::Local<v8::Module>();
    } else {
        GraalModule* graal_module = new GraalModule(graal_isolate, java_module);
        v8::Local<v8::Module> v8_module = reinterpret_cast<v8::Module*> (graal_module);
        return v8_module;
    }
}

v8::Maybe<bool> GraalModule::InstantiateModule(v8::Local<v8::Context> context, v8::Module::ResolveCallback callback) {
    GraalIsolate* graal_isolate = Isolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    jlong java_callback = (jlong) callback;
    JNI_CALL_VOID(graal_isolate, GraalAccessMethod::module_instantiate, java_context, GetJavaObject(), java_callback);
    return v8::Just(!graal_isolate->GetJNIEnv()->ExceptionCheck());
}

v8::MaybeLocal<v8::Value> GraalModule::Evaluate(v8::Local<v8::Context> context) {
    GraalIsolate* graal_isolate = Isolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::module_evaluate, Object, java_context, GetJavaObject());
    if (java_result == NULL) {
        return v8::Local<v8::Value>();
    } else {
        GraalValue* graal_result = GraalValue::FromJavaObject(graal_isolate, java_result);
        v8::Local<v8::Value> v8_result = reinterpret_cast<v8::Value*> (graal_result);
        return v8_result;
    }
}

v8::Module::Status GraalModule::GetStatus() const {
    JNI_CALL(jint, java_status, Isolate(), GraalAccessMethod::module_get_status, Int, GetJavaObject());
    return static_cast<v8::Module::Status> (java_status);
}

int GraalModule::GetModuleRequestsLength() const {
    JNI_CALL(jint, length, Isolate(), GraalAccessMethod::module_get_requests_length, Int, GetJavaObject());
    return length;
}

v8::Local<v8::String> GraalModule::GetModuleRequest(int index) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_request, graal_isolate, GraalAccessMethod::module_get_request, Object, GetJavaObject(), (jint) index);
    GraalString* graal_request = new GraalString(graal_isolate, (jstring) java_request);
    return reinterpret_cast<v8::String*> (graal_request);
}

v8::Local<v8::Value> GraalModule::GetModuleNamespace() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_namespace, graal_isolate, GraalAccessMethod::module_get_namespace, Object, GetJavaObject());
    GraalValue* graal_namespace = GraalValue::FromJavaObject(graal_isolate, java_namespace);
    return reinterpret_cast<v8::Value*> (graal_namespace);
}

int GraalModule::GetIdentityHash() const {
    JNI_CALL(jint, hash, Isolate(), GraalAccessMethod::module_get_identity_hash, Int, GetJavaObject());
    return hash;
}
