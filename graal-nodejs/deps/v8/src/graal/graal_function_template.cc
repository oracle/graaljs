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

#include "graal_context.h"
#include "graal_function.h"
#include "graal_function_template.h"
#include "graal_isolate.h"
#include "graal_object_template.h"
#include "graal_string.h"

#include "graal_function-inl.h"
#include "graal_function_template-inl.h"
#include "graal_object_template-inl.h"

GraalHandleContent* GraalFunctionTemplate::CopyImpl(jobject java_object_copy) {
    return new GraalFunctionTemplate(Isolate(), java_object_copy, id_);
}

void GraalFunctionTemplate::SetClassName(v8::Local<v8::String> name) {
    jobject java_name = reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_template_set_class_name, GetJavaObject(), java_name);
}

v8::Local<v8::FunctionTemplate> GraalFunctionTemplate::New(
        v8::Isolate* isolate, v8::FunctionCallback callback,
        v8::Local<v8::Value> data,
        v8::Local<v8::Signature> signature,
        int length,
        v8::ConstructorBehavior behavior,
        bool single_function_template) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jint id = graal_isolate->NextFunctionTemplateID();
    jlong callback_ptr = (jlong) callback;
    GraalValue* graal_data = reinterpret_cast<GraalValue*> (*data);
    if (graal_data == nullptr) {
        graal_data = graal_isolate->GetUndefined();
    } else {
        graal_data = reinterpret_cast<GraalValue*>(graal_data->Copy(true));
        graal_data->MakeWeak();
    }
    jobject java_data = graal_data->GetJavaObject();
    jint java_length = length;
    jboolean is_constructor = behavior == v8::ConstructorBehavior::kAllow;
    jboolean java_single_function = (jboolean) single_function_template;
    GraalFunctionTemplate* graal_signature = reinterpret_cast<GraalFunctionTemplate*> (*signature);
    jobject java_signature = (graal_signature == nullptr) ? NULL : graal_signature->GetJavaObject();
    JNI_CALL(jobject, java_object, isolate, GraalAccessMethod::function_template_new, Object, id, callback_ptr, java_data, java_signature, java_length, is_constructor, java_single_function);
    GraalFunctionTemplate* graal_function_template = new GraalFunctionTemplate(graal_isolate, java_object, id);
    graal_isolate->SetFunctionTemplateData(id, graal_data);
    graal_isolate->SetFunctionTemplateCallback(id, callback);
    v8::FunctionTemplate* v8_function_template = reinterpret_cast<v8::FunctionTemplate*> (graal_function_template);
    return v8::Local<v8::FunctionTemplate>::New(isolate, v8_function_template);
}

v8::Local<v8::ObjectTemplate> GraalFunctionTemplate::InstanceTemplate() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_instance_template, graal_isolate, GraalAccessMethod::function_template_instance_template, Object, GetJavaObject());
    GraalObjectTemplate* graal_object_template = GraalObjectTemplate::Allocate(graal_isolate, java_instance_template);
    v8::ObjectTemplate* v8_object_template = reinterpret_cast<v8::ObjectTemplate*> (graal_object_template);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::ObjectTemplate>::New(v8_isolate, v8_object_template);
}

v8::Local<v8::ObjectTemplate> GraalFunctionTemplate::PrototypeTemplate() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_prototype_template, graal_isolate, GraalAccessMethod::function_template_prototype_template, Object, GetJavaObject());
    GraalObjectTemplate* graal_object_template = GraalObjectTemplate::Allocate(graal_isolate, java_prototype_template);
    v8::ObjectTemplate* v8_object_template = reinterpret_cast<v8::ObjectTemplate*> (graal_object_template);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::ObjectTemplate>::New(v8_isolate, v8_object_template);
}

v8::Local<v8::Function> GraalFunctionTemplate::GetFunction(v8::Local<v8::Context> context) {
    GraalIsolate* graal_isolate = Isolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    JNI_CALL(jobject, java_function, Isolate(), GraalAccessMethod::function_template_get_function, Object, java_context, GetJavaObject());
    GraalFunction* graal_function = GraalFunction::Allocate(graal_isolate, java_function);
    v8::Function* v8_function = reinterpret_cast<v8::Function*> (graal_function);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Function>::New(v8_isolate, v8_function);
}

bool GraalFunctionTemplate::HasInstance(v8::Local<v8::Value> object) {
    jobject java_object = reinterpret_cast<GraalValue*> (*object)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::function_template_has_instance, Boolean, GetJavaObject(), java_object);
    return result;
}

void GraalFunctionTemplate::SetCallHandler(v8::FunctionCallback callback, v8::Local<v8::Value> data) {
    jlong callback_ptr = (jlong) callback;
    GraalValue* graal_data = reinterpret_cast<GraalValue*> (*data);
    if (graal_data == nullptr) {
        graal_data = Isolate()->GetUndefined();
    } else {
        graal_data = reinterpret_cast<GraalValue*>(graal_data->Copy(true));
        graal_data->MakeWeak();
    }
    Isolate()->SetFunctionTemplateData(id_, graal_data);
    Isolate()->SetFunctionTemplateCallback(id_, callback);
    jobject java_data = graal_data->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_template_set_call_handler, GetJavaObject(), callback_ptr, java_data);
}

void GraalFunctionTemplate::Inherit(v8::Local<v8::FunctionTemplate> parent) {
    jobject java_parent = reinterpret_cast<GraalFunctionTemplate*> (*parent)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_template_inherit, GetJavaObject(), java_parent);
}

void GraalFunctionTemplate::ReadOnlyPrototype() {
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_template_read_only_prototype, GetJavaObject());
}
