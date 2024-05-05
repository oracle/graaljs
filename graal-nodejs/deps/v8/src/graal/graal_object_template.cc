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
#include "graal_function_template.h"
#include "graal_isolate.h"
#include "graal_object.h"
#include "graal_object_template.h"
#include "graal_string.h"
#include "include/v8.h"

#include "graal_object-inl.h"
#include "graal_object_template-inl.h"

v8::Local<v8::ObjectTemplate> GraalObjectTemplate::New(v8::Isolate* isolate, v8::Local<v8::FunctionTemplate> constructor) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_constructor = constructor.IsEmpty() ? nullptr : reinterpret_cast<GraalFunctionTemplate*> (*constructor)->GetJavaObject();
    JNI_CALL(jobject, java_object_template, isolate, GraalAccessMethod::object_template_new, Object, java_constructor);
    GraalObjectTemplate* graal_object_template = new GraalObjectTemplate(graal_isolate, java_object_template);
    return reinterpret_cast<v8::ObjectTemplate*> (graal_object_template);
}

GraalHandleContent* GraalObjectTemplate::CopyImpl(jobject java_object_copy) {
    return new GraalObjectTemplate(Isolate(), java_object_copy);
}

v8::Local<v8::Object> GraalObjectTemplate::NewInstance(v8::Local<v8::Context> context) {
    GraalIsolate* graal_isolate = Isolate();
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::object_template_new_instance, Object, java_context, GetJavaObject());
    GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
    return reinterpret_cast<v8::Object*> (graal_object);
}

void GraalObjectTemplate::SetInternalFieldCount(int count) {
    internal_field_count_ = count;
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_template_set_internal_field_count, GetJavaObject(), (jint) count);
}

void GraalObjectTemplate::SetAccessor(
        v8::Local<v8::String> name,
        v8::AccessorGetterCallback getter,
        v8::AccessorSetterCallback setter,
        v8::Local<v8::Value> data,
        v8::AccessControl settings,
        v8::PropertyAttribute attribute) {
    jobject java_name = reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    jlong java_getter = (jlong) getter;
    jlong java_setter = (jlong) setter;
    if (data.IsEmpty()) {
        data = v8::Undefined(reinterpret_cast<v8::Isolate*> (Isolate()));
    }
    jobject java_data = reinterpret_cast<GraalValue*> (*data)->GetJavaObject();
    jint java_attrs = attribute;
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_template_set_accessor, GetJavaObject(), java_name, java_getter, java_setter, java_data, java_attrs);
}

void GraalObjectTemplate::SetHandler(const v8::NamedPropertyHandlerConfiguration& configuration) {
    jobject java_data = configuration.data.IsEmpty() ? NULL : reinterpret_cast<GraalValue*> (*configuration.data)->GetJavaObject();
    JNI_CALL_VOID(Isolate(),
            GraalAccessMethod::object_template_set_handler,
            GetJavaObject(),
            (jlong) configuration.getter,
            (jlong) configuration.setter,
            (jlong) configuration.query,
            (jlong) configuration.deleter,
            (jlong) configuration.enumerator,
            (jlong) configuration.definer,
            (jlong) configuration.descriptor,
            java_data,
            true,
            configuration.flags == v8::PropertyHandlerFlags::kOnlyInterceptStrings);
}

void GraalObjectTemplate::SetHandler(const v8::IndexedPropertyHandlerConfiguration& configuration) {
    jobject java_data = configuration.data.IsEmpty() ? NULL : reinterpret_cast<GraalValue*> (*configuration.data)->GetJavaObject();
    JNI_CALL_VOID(Isolate(),
            GraalAccessMethod::object_template_set_handler,
            GetJavaObject(),
            (jlong) configuration.getter,
            (jlong) configuration.setter,
            (jlong) configuration.query,
            (jlong) configuration.deleter,
            (jlong) configuration.enumerator,
            (jlong) configuration.definer,
            (jlong) configuration.descriptor,
            java_data,
            false,
            false);
}

void GraalObjectTemplate::SetCallAsFunctionHandler(v8::FunctionCallback callback, v8::Local<v8::Value> data) {
    int id = Isolate()->NextFunctionTemplateID();
    jlong callback_ptr = (jlong) callback;
    GraalValue* graal_data = reinterpret_cast<GraalValue*> (*data);
    if (graal_data == nullptr) {
        graal_data = Isolate()->GetUndefined();
    } else {
        graal_data = reinterpret_cast<GraalValue*> (graal_data->Copy(true));
        graal_data->MakeWeak();
    }
    Isolate()->SetFunctionTemplateData(id, graal_data);
    Isolate()->SetFunctionTemplateCallback(id, callback);
    jobject java_data = graal_data->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_template_set_call_as_function_handler, GetJavaObject(), id, callback_ptr, java_data);
}
