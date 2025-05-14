/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_array-inl.h"
#include "graal_context-inl.h"
#include "graal_object-inl.h"
#include "graal_string-inl.h"

GraalHandleContent* GraalObject::CopyImpl(jobject java_object_copy) {
    return GraalObject::Allocate(Isolate(), java_object_copy);
}

bool GraalObject::IsObject() const {
    return true;
}

v8::Local<v8::Object> GraalObject::New(v8::Isolate* isolate) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, isolate, GraalAccessMethod::object_new, Object, java_context);
    GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
    v8::Object* v8_object = reinterpret_cast<v8::Object*> (graal_object);
    return v8::Local<v8::Object>::New(isolate, v8_object);
}

bool GraalObject::Set(v8::Local<v8::Value> key, v8::Local<v8::Value> value) {
    GraalIsolate* graal_isolate = Isolate();
    if (!graal_isolate->CheckJSExecutionAllowed()) {
        return false;
    }
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    JNI_CALL(bool, success, graal_isolate, GraalAccessMethod::object_set, Boolean, GetJavaObject(), java_key, java_value);
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
    return HandleCallResult(java_object);
}

v8::Local<v8::Value> GraalObject::Get(uint32_t index) {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_get_index, Object, GetJavaObject(), (jint) index);
    return HandleCallResult(java_object);
}

v8::Local<v8::Value> GraalObject::GetRealNamedProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key) {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::object_get_real_named_property, Object, GetJavaObject(), java_key);
    if (java_object == NULL) {
        return v8::Local<v8::Value>();
    } else {
        GraalValue* graal_value = GraalObject::FromJavaObject(graal_isolate, java_object);
        v8::Value* v8_value = reinterpret_cast<v8::Value*> (graal_value);
        v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
        return v8::Local<v8::Value>::New(v8_isolate, v8_value);
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

void GraalObject::SetInternalFieldImpl(int index, GraalHandleContent* handle) {
    jobject java_value = handle->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_set_internal_field, GetJavaObject(), (jint) index, java_value);
}

void GraalObject::SetInternalField(int index, v8::Local<v8::Data> value) {
    SetInternalFieldImpl(index, reinterpret_cast<GraalHandleContent*> (*value));
}

v8::Local<v8::Data> GraalObject::SlowGetInternalField(int index) {
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::object_slow_get_internal_field, Object, GetJavaObject(), (jint) index);
    return HandleCallResult(java_object);
}

void GraalObject::SetAlignedPointerInInternalField(int index, void* value) {
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::object_set_aligned_pointer_in_internal_field, GetJavaObject(), (jint) index, value);
}

void* GraalObject::SlowGetAlignedPointerFromInternalField(int index) {
    JNI_CALL(jlong, result, Isolate(), GraalAccessMethod::object_slow_get_aligned_pointer_from_internal_field, Long, GetJavaObject(), (jint) index);
    return (void *) result;
}

v8::Local<v8::Object> GraalObject::Clone() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_clone, graal_isolate, GraalAccessMethod::object_clone, Object, GetJavaObject());
    GraalValue* graal_clone = GraalValue::FromJavaObject(graal_isolate, java_clone);
    v8::Object* v8_clone = reinterpret_cast<v8::Object*> (graal_clone);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Object>::New(v8_isolate, v8_clone);
}

v8::Local<v8::Value> GraalObject::GetPrototype() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_prototype, graal_isolate, GraalAccessMethod::object_get_prototype, Object, GetJavaObject());
    GraalValue* graal_prototype = (java_prototype == NULL) ? graal_isolate->GetNull() : GraalValue::FromJavaObject(graal_isolate, java_prototype);
    v8::Object* v8_prototype = reinterpret_cast<v8::Object*> (graal_prototype);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_prototype);
}

bool GraalObject::SetPrototype(v8::Local<v8::Value> prototype) {
    GraalValue* graal_prototype = reinterpret_cast<GraalValue*> (*prototype);
    jobject java_prototype = graal_prototype->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_set_prototype, Boolean, GetJavaObject(), java_prototype);
    return result;
}

v8::Local<v8::String> GraalObject::GetConstructorName() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_name, graal_isolate, GraalAccessMethod::object_get_constructor_name, Object, GetJavaObject());
    GraalString* graal_name = GraalString::Allocate(graal_isolate, java_name);
    v8::String* v8_name = reinterpret_cast<v8::String*> (graal_name);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::String>::New(v8_isolate, v8_name);
}

v8::Local<v8::Array> GraalObject::GetOwnPropertyNames() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_names, graal_isolate, GraalAccessMethod::object_get_own_property_names, Object, GetJavaObject());
    GraalArray* graal_names = GraalArray::Allocate(graal_isolate, java_names);
    v8::Array* v8_names = reinterpret_cast<v8::Array*> (graal_names);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Array>::New(v8_isolate, v8_names);
}

v8::MaybeLocal<v8::Array> GraalObject::GetPropertyNames(v8::Local<v8::Context> context, v8::KeyCollectionMode mode, v8::PropertyFilter property_filter, v8::IndexFilter index_filter, v8::KeyConversionMode key_conversion) {
    jboolean ownOnly = mode == v8::KeyCollectionMode::kOwnOnly;
    jboolean enumerableOnly = (property_filter & v8::PropertyFilter::ONLY_ENUMERABLE) != 0;
    jboolean configurableOnly = (property_filter & v8::PropertyFilter::ONLY_CONFIGURABLE) != 0;
    jboolean writableOnly = (property_filter & v8::PropertyFilter::ONLY_WRITABLE) != 0;
    jboolean skipIndices = index_filter == v8::IndexFilter::kSkipIndices;
    jboolean skipSymbols = (property_filter & v8::PropertyFilter::SKIP_SYMBOLS) != 0;
    jboolean skipStrings = (property_filter & v8::PropertyFilter::SKIP_STRINGS) != 0;
    jboolean keepNumbers = key_conversion == v8::KeyConversionMode::kKeepNumbers;
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_names, graal_isolate, GraalAccessMethod::object_get_property_names, Object, GetJavaObject(),
            ownOnly, enumerableOnly, configurableOnly, writableOnly, skipIndices, skipSymbols, skipStrings, keepNumbers);
    GraalArray* graal_names = GraalArray::Allocate(graal_isolate, java_names);
    v8::Array* v8_names = reinterpret_cast<v8::Array*> (graal_names);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Array>::New(v8_isolate, v8_names);
}

v8::Local<v8::Context> GraalObject::CreationContext() {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_context, graal_isolate, GraalAccessMethod::object_creation_context, Object, GetJavaObject());
    GraalContext* graal_context = GraalContext::Allocate(graal_isolate, java_context);
    v8::Context* v8_context = reinterpret_cast<v8::Context*> (graal_context);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Context>::New(v8_isolate, v8_context);
}

v8::MaybeLocal<v8::Value> GraalObject::GetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::object_get_private, Object, GetJavaObject(), java_key);
    GraalValue* graal_value = GraalObject::FromJavaObject(graal_isolate, java_object);
    v8::Value* v8_value = reinterpret_cast<v8::Value*> (graal_value);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_value);
}

v8::Maybe<bool> GraalObject::SetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key, v8::Local<v8::Value> value) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    JNI_CALL(bool, success, Isolate(), GraalAccessMethod::object_set_private, Boolean, GetJavaObject(), java_key, java_value);
    return v8::Just(success);
}

v8::Maybe<bool> GraalObject::HasPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(bool, result, Isolate(), GraalAccessMethod::object_has_private, Boolean, GetJavaObject(), java_key);
    return v8::Just(result);
}

v8::Maybe<bool> GraalObject::DeletePrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_delete_private, Boolean, GetJavaObject(), java_key);
    return v8::Just((bool) result);
}

v8::MaybeLocal<v8::Value> GraalObject::GetOwnPropertyDescriptor(v8::Local<v8::Context> context, v8::Local<v8::Name> key) {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    JNI_CALL(jobject, result, graal_isolate, GraalAccessMethod::object_get_own_property_descriptor, Object, GetJavaObject(), java_key);
    GraalValue* graal_result = GraalValue::FromJavaObject(graal_isolate, result);
    v8::Value* v8_result = reinterpret_cast<v8::Value*> (graal_result);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_result);
}

v8::Maybe<bool> GraalObject::CreateDataProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key, v8::Local<v8::Value> value) {
    jobject java_key = reinterpret_cast<GraalHandleContent*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalHandleContent*> (*value)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_create_data_property, Boolean, GetJavaObject(), java_key, java_value);
    return v8::Just((bool) result);
}

v8::Maybe<bool> GraalObject::CreateDataProperty(v8::Local<v8::Context> context, uint32_t index, v8::Local<v8::Value> value) {
    jlong java_index = (jlong) index;
    jobject java_value = reinterpret_cast<GraalHandleContent*> (*value)->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_create_data_property_index, Boolean, GetJavaObject(), java_index, java_value);
    return v8::Just((bool) result);
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
        v8::Array* v8_entries = reinterpret_cast<v8::Array*> (graal_entries);
        v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
        return v8::Local<v8::Array>::New(v8_isolate, v8_entries);
    }
}

v8::Maybe<bool> GraalObject::SetIntegrityLevel(v8::Local<v8::Context> context, v8::IntegrityLevel level) {
    GraalIsolate* graal_isolate = Isolate();
    jboolean freeze = (level == v8::IntegrityLevel::kFrozen);
    JNI_CALL_VOID(graal_isolate, GraalAccessMethod::object_set_integrity_level, GetJavaObject(), freeze);
    return graal_isolate->GetJNIEnv()->ExceptionCheck() ? v8::Nothing<bool>() : v8::Just<bool>(true);
}

bool GraalObject::IsConstructor() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::object_is_constructor, Boolean, GetJavaObject());
    return result;
}

v8::Maybe<bool> GraalObject::SetLazyDataProperty(
        v8::Local<v8::Context> context,
        v8::Local<v8::Name> name,
        v8::AccessorNameGetterCallback getter,
        v8::Local<v8::Value> data,
        v8::PropertyAttribute attributes,
        v8::SideEffectType getter_side_effect_type,
        v8::SideEffectType setter_side_effect_type) {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_key = reinterpret_cast<GraalValue*> (*name)->GetJavaObject();
    jlong java_getter = (jlong) getter;
    jobject java_data;
    if (data.IsEmpty()) {
        java_data = graal_isolate->GetUndefined()->GetJavaObject();
    } else {
        java_data = reinterpret_cast<GraalValue*> (*data)->GetJavaObject();
    }
    jint java_attribs = attributes;
    JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::object_set_lazy_data_property, Boolean, GetJavaObject(), java_key, java_getter, java_data, java_attribs);
    return v8::Just((bool) result);
}
