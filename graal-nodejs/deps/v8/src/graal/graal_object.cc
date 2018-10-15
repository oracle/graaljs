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

#include "graal_array.h"
#include "graal_context.h"
#include "graal_external.h"
#include "graal_isolate.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_string.h"
#include <string>

GraalObject::GraalObject(GraalIsolate* isolate, jobject java_object) : GraalValue(isolate, java_object), internal_field_count_cache_(-1) {
}

GraalHandleContent* GraalObject::CopyImpl(jobject java_object_copy) {
    return new GraalObject(Isolate(), java_object_copy);
}

bool GraalObject::IsObject() const {
    return true;
}

v8::Local<v8::Object> GraalObject::New(v8::Isolate* isolate) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, isolate, GraalAccessMethod::object_new, Object, java_context);
    GraalObject* graal_object = new GraalObject(graal_isolate, java_object);
    return reinterpret_cast<v8::Object*> (graal_object);
}

bool GraalObject::Set(v8::Local<v8::Value> key, v8::Local<v8::Value> value) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    JNI_CALL(bool, success, Isolate(), GraalAccessMethod::object_set, Boolean, GetJavaObject(), java_key, java_value);
    return success;
}

bool GraalObject::Set(uint32_t index, v8::Local<v8::Value> value) {
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    JNI_CALL(bool, success, Isolate(), GraalAccessMethod::object_set_index, Boolean, GetJavaObject(), index, java_value);
    return success;
}

bool GraalObject::ForceSet(v8::Local<v8::Value> key, v8::Local<v8::Value> value, v8::PropertyAttribute attribs) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    jint java_attribs = attribs;
    JNI_CALL(bool, success, Isolate(), GraalAccessMethod::object_force_set, Boolean, GetJavaObject(), java_key, java_value, java_attribs);
    return success;
}

v8::Local<v8::Value> GraalObject::Get(v8::Local<v8::Value> key) {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::object_get, Object, GetJavaObject(), java_key);
    if (java_object == NULL) {
        graal_isolate->HandleEmptyCallResult();
        return v8::Local<v8::Value>();
    } else {
        graal_isolate->ResetSharedBuffer();
        int32_t value_t = graal_isolate->ReadInt32FromSharedBuffer();
        GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_object, value_t, true);
        return reinterpret_cast<v8::Value*> (graal_value);
    }
}

v8::Local<v8::Value> GraalObject::Get(uint32_t index) {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_get_index, Object, GetJavaObject(), (jint) index);
    if (java_object == NULL) {
        return v8::Local<v8::Value>();
    } else {
        Isolate()->ResetSharedBuffer();
        int32_t value_t = Isolate()->ReadInt32FromSharedBuffer();
        GraalValue* graal_value = GraalValue::FromJavaObject(Isolate(), java_object, value_t, true);
        return reinterpret_cast<v8::Value*> (graal_value);
    }
}

v8::Local<v8::Value> GraalObject::GetRealNamedProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_get_real_named_property, Object, GetJavaObject(), java_key);
    if (java_object == NULL) {
        return v8::Local<v8::Value>();
    } else {
        GraalValue* graal_value = GraalObject::FromJavaObject(Isolate(), java_object);
        return reinterpret_cast<v8::Value*> (graal_value);
    }
}

v8::Maybe<v8::PropertyAttribute> GraalObject::GetRealNamedPropertyAttributes(v8::Local<v8::Context> context, v8::Local<v8::Name> key) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jint, result, Isolate(), GraalAccessMethod::object_get_real_named_property_attributes, Int, GetJavaObject(), java_key);
    if (result == -1) {
        return v8::Nothing<v8::PropertyAttribute>();
    } else {
        return v8::Just<v8::PropertyAttribute>(static_cast<v8::PropertyAttribute> (result));
    }
}

bool GraalObject::Has(v8::Local<v8::Value> key) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, java_has, Isolate(), GraalAccessMethod::object_has, Boolean, GetJavaObject(), java_key);
    return java_has;
}

bool GraalObject::HasOwnProperty(v8::Local<v8::Name> key) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, java_has_own_property, Isolate(), GraalAccessMethod::object_has_own_property, Boolean, GetJavaObject(), java_key);
    return java_has_own_property;
}

bool GraalObject::HasRealNamedProperty(v8::Local<v8::Name> key) {
    jobject java_key = reinterpret_cast<GraalName*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, java_has, Isolate(), GraalAccessMethod::object_has_real_named_property, Boolean, GetJavaObject(), java_key);
    return java_has;
}

bool GraalObject::Delete(v8::Local<v8::Value> key) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_delete, Boolean, GetJavaObject(), java_key);
    return result;
}

bool GraalObject::Delete(uint32_t index) {
    jlong java_index = (jlong) index;
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_delete_index, Boolean, GetJavaObject(), java_index);
    return result;
}

v8::Maybe<bool> GraalObject::SetAccessor(
        v8::Local<v8::Name> name,
        v8::AccessorNameGetterCallback getter,
        v8::AccessorNameSetterCallback setter,
        v8::MaybeLocal<v8::Value> data,
        v8::AccessControl settings,
        v8::PropertyAttribute attributes) {
    jobject java_name = reinterpret_cast<GraalValue*> (*name)->GetJavaObject();
    jlong java_getter = (jlong) getter;
    jlong java_setter = (jlong) setter;
    jobject java_data;
    GraalIsolate* graal_isolate = Isolate();
    if (data.IsEmpty()) {
        java_data = graal_isolate->GetUndefined()->GetJavaObject();
    } else {
        GraalValue* graal_data = reinterpret_cast<GraalValue*> (*(data.ToLocalChecked()));
        java_data = graal_data->GetJavaObject();
    }
    jint java_attribs = attributes;
    JNI_CALL(jboolean, success, Isolate(), GraalAccessMethod::object_set_accessor, Boolean, GetJavaObject(), java_name, java_getter, java_setter, java_data, java_attribs);
    return v8::Just((bool) success);
}

int GraalObject::InternalFieldCount() {
    if (internal_field_count_cache_ == -1) {
        if (IsPromise()) {
            internal_field_count_cache_ = 1;
        } else {
            JNI_CALL(jint, result, Isolate(), GraalAccessMethod::object_internal_field_count, Int, GetJavaObject());
            internal_field_count_cache_ = result;
        }
    }
    return internal_field_count_cache_;
}

void GraalObject::SetInternalField(int index, v8::Local<v8::Value> value) {
    Set(Isolate()->InternalFieldKey(index), value);
}

v8::Local<v8::Value> GraalObject::SlowGetInternalField(int index) {
    return Get(Isolate()->InternalFieldKey(index));
}

void GraalObject::SetAlignedPointerInInternalField(int index, void* value) {
    if (index == 0) {
        JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_set_aligned_pointer_in_internal_field, GetJavaObject(), value);
    } else {
        v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (Isolate());
        v8::Local<v8::External> external = GraalExternal::New(isolate, value);
        SetInternalField(index, external);
    }
}

void* GraalObject::SlowGetAlignedPointerFromInternalField(int index) {
    if (index == 0) {
        JNI_CALL(jlong, result, Isolate(), GraalAccessMethod::object_slow_get_aligned_pointer_from_internal_field, Long, GetJavaObject());
        return (void *) result;
    }    
    v8::Local<v8::Value> value = SlowGetInternalField(index);
    if (value->IsExternal()) {
        return value.As<v8::External>()->Value();
    } else {
        return nullptr;
    }
}

v8::Local<v8::Object> GraalObject::Clone() {
    JNI_CALL(jobject, java_clone, Isolate(), GraalAccessMethod::object_clone, Object, GetJavaObject());
    GraalValue* graal_clone = GraalValue::FromJavaObject(Isolate(), java_clone);
    return reinterpret_cast<v8::Object*> (graal_clone);
}

v8::Local<v8::Value> GraalObject::GetPrototype() {
    JNI_CALL(jobject, java_prototype, Isolate(), GraalAccessMethod::object_get_prototype, Object, GetJavaObject());
    GraalValue* graal_prototype = (java_prototype == NULL) ? Isolate()->GetNull() : GraalValue::FromJavaObject(Isolate(), java_prototype);
    return reinterpret_cast<v8::Object*> (graal_prototype);
}

bool GraalObject::SetPrototype(v8::Local<v8::Value> prototype) {
    GraalValue* graal_prototype = reinterpret_cast<GraalValue*> (*prototype);
    jobject java_prototype = graal_prototype->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_set_prototype, Boolean, GetJavaObject(), java_prototype);
    return result;
}

v8::Local<v8::String> GraalObject::GetConstructorName() {
    JNI_CALL(jobject, java_name, Isolate(), GraalAccessMethod::object_get_constructor_name, Object, GetJavaObject());
    GraalString* graal_name = new GraalString(Isolate(), (jstring) java_name);
    return reinterpret_cast<v8::String*> (graal_name);
}

v8::Local<v8::Array> GraalObject::GetOwnPropertyNames() {
    JNI_CALL(jobject, java_names, Isolate(), GraalAccessMethod::object_get_own_property_names, Object, GetJavaObject());
    GraalArray* graal_names = new GraalArray(Isolate(), java_names);
    return reinterpret_cast<v8::Array*> (graal_names);
}

v8::Local<v8::Array> GraalObject::GetPropertyNames() {
    JNI_CALL(jobject, java_names, Isolate(), GraalAccessMethod::object_get_property_names, Object, GetJavaObject());
    GraalArray* graal_names = new GraalArray(Isolate(), java_names);
    return reinterpret_cast<v8::Array*> (graal_names);
}

v8::Local<v8::Context> GraalObject::CreationContext() {
    JNI_CALL(jobject, java_context, Isolate(), GraalAccessMethod::object_creation_context, Object, GetJavaObject());
    GraalContext* graal_context = new GraalContext(Isolate(), java_context);
    return reinterpret_cast<v8::Context*> (graal_context);
}

v8::MaybeLocal<v8::Value> GraalObject::GetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_get_private, Object, GetJavaObject(), java_key);
    if (java_object == NULL) {
        return v8::Undefined(reinterpret_cast<v8::Isolate*> (Isolate()));
    } else {
        GraalValue* graal_value = GraalObject::FromJavaObject(Isolate(), java_object);
        v8::Local<v8::Value> v8_value = reinterpret_cast<v8::Value*> (graal_value);
        return v8_value;
    }
}

v8::Maybe<bool> GraalObject::SetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key, v8::Local<v8::Value> value) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    jobject java_context = Isolate()->CurrentJavaContext();
    JNI_CALL(bool, success, Isolate(), GraalAccessMethod::object_set_private, Boolean, java_context, GetJavaObject(), java_key, java_value);
    return v8::Just(success);
}

v8::Maybe<bool> GraalObject::HasPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_get_private, Object, GetJavaObject(), java_key);
    return v8::Just(java_object != NULL);
}

v8::Maybe<bool> GraalObject::DeletePrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_delete_private, Boolean, GetJavaObject(), java_key);
    return v8::Just((bool) result);
}

v8::MaybeLocal<v8::Value> GraalObject::GetOwnPropertyDescriptor(v8::Local<v8::Context> context, v8::Local<v8::Name> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jobject, result, Isolate(), GraalAccessMethod::object_get_own_property_descriptor, Object, GetJavaObject(), java_key);
    GraalValue* graal_result = GraalValue::FromJavaObject(Isolate(), result);
    v8::Local<v8::Value> v8_result = reinterpret_cast<v8::Value*> (graal_result);
    return v8_result;
}

v8::Maybe<bool> GraalObject::DefineProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key, v8::PropertyDescriptor& descriptor) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    jobject value = descriptor.has_value() ? reinterpret_cast<GraalHandleContent*> (*descriptor.value())->GetJavaObject() : NULL;
    jobject get = descriptor.has_get() ? reinterpret_cast<GraalHandleContent*> (*descriptor.get())->GetJavaObject() : NULL;
    jobject set = descriptor.has_set() ? reinterpret_cast<GraalHandleContent*> (*descriptor.set())->GetJavaObject() : NULL;
    jboolean has_enumerable = descriptor.has_enumerable();
    jboolean enumerable = has_enumerable ? descriptor.enumerable() : false;
    jboolean has_configurable = descriptor.has_configurable();
    jboolean configurable = has_configurable ? descriptor.configurable() : false;
    jboolean has_writable = descriptor.has_writable();
    jboolean writable = has_writable ? descriptor.writable() : false;
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_define_property, Boolean, GetJavaObject(), java_key,
            value, get, set, has_enumerable, enumerable, has_configurable, configurable, has_writable, writable);
    return v8::Just((bool) result);
}

v8::MaybeLocal<v8::Array> GraalObject::PreviewEntries(bool* is_key_value) {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_entries, graal_isolate, GraalAccessMethod::object_preview_entries, Object, GetJavaObject());
    if (java_entries == NULL) {
        return v8::MaybeLocal<v8::Array>();
    } else {
        graal_isolate->ResetSharedBuffer();
        *is_key_value = graal_isolate->ReadInt32FromSharedBuffer() != 0;
        GraalValue* graal_entries = GraalValue::FromJavaObject(graal_isolate, java_entries);
        v8::Local<v8::Array> v8_entries = reinterpret_cast<v8::Array*> (graal_entries);
        return v8_entries;
    }
}