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
#include "graal_fixed_array.h"
#include "graal_isolate.h"
#include "graal_module.h"
#include "graal_primitive_array.h"
#include "graal_string.h"
#include "graal_unbound_script.h"

#include "graal_fixed_array-inl.h"
#include "graal_module-inl.h"
#include "graal_string-inl.h"
#include "graal_unbound_script-inl.h"

GraalHandleContent* GraalModule::CopyImpl(jobject java_object_copy) {
    return new GraalModule(Isolate(), java_object_copy);
}

v8::MaybeLocal<v8::Module> GraalModule::Compile(v8::Local<v8::String> source, v8::Local<v8::String> name, v8::Local<v8::Data> options) {
    GraalString* graal_source = reinterpret_cast<GraalString*> (*source);
    jobject java_source = graal_source->GetJavaObject();
    jobject java_name = name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    jobject java_options = options.IsEmpty() ? NULL : reinterpret_cast<GraalPrimitiveArray*> (*options)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source->Isolate();
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_module, graal_isolate, GraalAccessMethod::module_compile, Object, java_context, java_source, java_name, java_options);
    if (java_module == NULL) {
        return v8::Local<v8::Module>();
    } else {
        GraalModule* graal_module = new GraalModule(graal_isolate, java_module);
        v8::Module* v8_module = reinterpret_cast<v8::Module*> (graal_module);
        v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
        return v8::Local<v8::Module>::New(v8_isolate, v8_module);
    }
}

v8::Maybe<bool> GraalModule::InstantiateModule(v8::Local<v8::Context> context, v8::Module::ResolveModuleCallback callback) {
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
        v8::Value* v8_result = reinterpret_cast<v8::Value*> (graal_result);
        v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
        return v8::Local<v8::Value>::New(v8_isolate, v8_result);
    }
}

v8::Module::Status GraalModule::GetStatus() const {
    JNI_CALL(jint, java_status, Isolate(), GraalAccessMethod::module_get_status, Int, GetJavaObject());
    return static_cast<v8::Module::Status> (java_status);
}

v8::Local<v8::Value> GraalModule::GetModuleNamespace() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_namespace, graal_isolate, GraalAccessMethod::module_get_namespace, Object, GetJavaObject());
    GraalValue* graal_namespace = GraalValue::FromJavaObject(graal_isolate, java_namespace);
    v8::Value* v8_namespace = reinterpret_cast<v8::Value*> (graal_namespace);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_namespace);
}

int GraalModule::GetIdentityHash() const {
    JNI_CALL(jint, hash, Isolate(), GraalAccessMethod::module_get_identity_hash, Int, GetJavaObject());
    return hash;
}

v8::Local<v8::Value> GraalModule::GetException() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_error, graal_isolate, GraalAccessMethod::module_get_exception, Object, GetJavaObject());
    GraalValue* graal_error = GraalValue::FromJavaObject(graal_isolate, java_error);
    v8::Value* v8_error = reinterpret_cast<v8::Value*> (graal_error);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_error);
}

v8::Local<v8::Module> GraalModule::CreateSyntheticModule(
        v8::Isolate* isolate, v8::Local<v8::String> module_name,
        const v8::MemorySpan<const v8::Local<v8::String>>& export_names,
        v8::Module::SyntheticModuleEvaluationSteps evaluation_steps) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    GraalString* graal_module_name = reinterpret_cast<GraalString*> (*module_name);
    jobjectArray java_export_names = env->NewObjectArray(export_names.size(), graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < export_names.size(); i++) {
        GraalString* graal_export_name = reinterpret_cast<GraalString*> (*export_names[i]);
        jobject java_export_name = graal_export_name->GetJavaObject();
        env->SetObjectArrayElement(java_export_names, i, java_export_name);
    }
    jobject java_module_name = graal_module_name->GetJavaObject();
    jlong java_callback = (jlong) evaluation_steps;
    JNI_CALL(jobject, java_module, graal_isolate, GraalAccessMethod::module_create_synthetic_module, Object, java_module_name, java_export_names, java_callback);
    GraalModule* graal_module = new GraalModule(graal_isolate, java_module);
    v8::Module* v8_module = reinterpret_cast<v8::Module*> (graal_module);
    return v8::Local<v8::Module>::New(isolate, v8_module);
}

v8::Maybe<bool> GraalModule::SetSyntheticModuleExport(v8::Local<v8::String> export_name, v8::Local<v8::Value> export_value) {
    GraalIsolate* graal_isolate = Isolate();
    GraalString* graal_name = reinterpret_cast<GraalString*> (*export_name);
    GraalValue* graal_value = reinterpret_cast<GraalValue*> (*export_value);
    jobject java_name = graal_name->GetJavaObject();
    jobject java_value = graal_value->GetJavaObject();
    JNI_CALL_VOID(graal_isolate, GraalAccessMethod::module_set_synthetic_module_export, GetJavaObject(), java_name, java_value);
    return graal_isolate->GetJNIEnv()->ExceptionCheck() ? v8::Nothing<bool>() : v8::Just<bool>(true);
}

v8::Local<v8::UnboundModuleScript> GraalModule::GetUnboundModuleScript() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_script, graal_isolate, GraalAccessMethod::module_get_unbound_module_script, Object, GetJavaObject());
    GraalUnboundScript* graal_script = GraalUnboundScript::Allocate(graal_isolate, java_script);
    v8::UnboundModuleScript* v8_script = reinterpret_cast<v8::UnboundModuleScript*> (graal_script);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::UnboundModuleScript>::New(v8_isolate, v8_script);
}

v8::Local<v8::FixedArray> GraalModule::GetModuleRequests() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_requests, graal_isolate, GraalAccessMethod::module_get_module_requests, Object, GetJavaObject());
    GraalFixedArray* graal_requests = GraalFixedArray::Allocate(graal_isolate, java_requests);
    v8::FixedArray* v8_requests = reinterpret_cast<v8::FixedArray*> (graal_requests);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::FixedArray>::New(v8_isolate, v8_requests);
}

bool GraalModule::IsGraphAsync() const {
    JNI_CALL(jboolean, java_is_graph_async, Isolate(), GraalAccessMethod::module_is_graph_async, Boolean, GetJavaObject());
    return (bool) java_is_graph_async;
}

bool GraalModule::IsSourceTextModule() const {
    JNI_CALL(jboolean, java_is_source_text, Isolate(), GraalAccessMethod::module_is_source_text_module, Boolean, GetJavaObject());
    return (bool) java_is_source_text;
}
