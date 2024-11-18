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

#include "graal_array.h"
#include "graal_array_buffer.h"
#include "graal_array_buffer_view.h"
#include "graal_big_int.h"
#include "graal_boolean.h"
#include "graal_date.h"
#include "graal_external.h"
#include "graal_function.h"
#include "graal_isolate.h"
#include "graal_map.h"
#include "graal_missing_primitive.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_promise.h"
#include "graal_proxy.h"
#include "graal_regexp.h"
#include "graal_set.h"
#include "graal_string.h"
#include "graal_symbol.h"
#include "graal_value.h"
#include <algorithm>
#include <cmath>
#include <limits>

#include "graal_value-inl.h"
#include "graal_array-inl.h"
#include "graal_array_buffer-inl.h"
#include "graal_array_buffer_view-inl.h"
#include "graal_big_int-inl.h"
#include "graal_boolean-inl.h"
#include "graal_date-inl.h"
#include "graal_external-inl.h"
#include "graal_function-inl.h"
#include "graal_map-inl.h"
#include "graal_missing_primitive-inl.h"
#include "graal_number-inl.h"
#include "graal_object-inl.h"
#include "graal_promise-inl.h"
#include "graal_proxy-inl.h"
#include "graal_regexp-inl.h"
#include "graal_set-inl.h"
#include "graal_string-inl.h"
#include "graal_symbol-inl.h"

bool GraalValue::IsObject() const {
    return false;
}

bool GraalValue::IsFunction() const {
    return false;
}

bool GraalValue::IsExternal() const {
    return false;
}

bool GraalValue::IsArray() const {
    return false;
}

bool GraalValue::IsInt32() const {
    return false;
}

bool GraalValue::IsUint32() const {
    return false;
}

bool GraalValue::IsNumber() const {
    return false;
}

bool GraalValue::IsBoolean() const {
    return false;
}

bool GraalValue::IsArrayBuffer() const {
    return false;
}

bool GraalValue::IsArrayBufferView() const {
    return false;
}

bool GraalValue::IsDate() const {
    return false;
}

bool GraalValue::IsRegExp() const {
    return false;
}

bool GraalValue::IsUint8Array() const {
    return false;
}

bool GraalValue::IsUint8ClampedArray() const {
    return false;
}

bool GraalValue::IsInt8Array() const {
    return false;
}

bool GraalValue::IsUint16Array() const {
    return false;
}

bool GraalValue::IsInt16Array() const {
    return false;
}

bool GraalValue::IsUint32Array() const {
    return false;
}

bool GraalValue::IsInt32Array() const {
    return false;
}

bool GraalValue::IsFloat32Array() const {
    return false;
}

bool GraalValue::IsFloat64Array() const {
    return false;
}

bool GraalValue::IsBigInt64Array() const {
    return false;
}

bool GraalValue::IsBigUint64Array() const {
    return false;
}

bool GraalValue::IsMap() const {
    return false;
}

bool GraalValue::IsSet() const {
    return false;
}

bool GraalValue::IsPromise() const {
    return false;
}

bool GraalValue::IsProxy() const {
    return false;
}

bool GraalValue::IsSymbol() const {
    return false;
}

bool GraalValue::IsName() const {
    return false;
}

bool GraalValue::IsNull() const {
    return false;
}

bool GraalValue::IsUndefined() const {
    return false;
}

bool GraalValue::IsTrue() const {
    return false;
}

bool GraalValue::IsFalse() const {
    return false;
}

bool GraalValue::IsDataView() const {
    return false;
}

bool GraalValue::IsBigInt() const {
    return false;
}

bool GraalValue::IsNativeError() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_native_error, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsMapIterator() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_map_iterator, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsSetIterator() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_set_iterator, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsSharedArrayBuffer() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_shared_array_buffer, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsArgumentsObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_arguments_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsBooleanObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_boolean_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsNumberObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_number_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsStringObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_string_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsSymbolObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_symbol_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsBigIntObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_big_int_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsWeakMap() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_weak_map, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsWeakSet() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_weak_set, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsAsyncFunction() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_async_function, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsGeneratorFunction() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_generator_function, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsGeneratorObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_generator_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsModuleNamespaceObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_module_namespace_object, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsWasmMemoryObject() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_wasm_memory_object, Boolean, GetJavaObject());
    return result;
}

int32_t GraalValue::Int32Value() const {
    JNI_CALL(jint, result, Isolate(), GraalAccessMethod::value_int32_value, Int, GetJavaObject());
    return result;
}

uint32_t GraalValue::Uint32Value() const {
    JNI_CALL(jdouble, result, Isolate(), GraalAccessMethod::value_uint32_value, Double, GetJavaObject());
    return result;
}

int64_t GraalValue::IntegerValue() const {
    if (IsNumber()) {
        const GraalNumber* graal_number = reinterpret_cast<const GraalNumber*> (this);
        double value = graal_number->Value();
        return static_cast<int64_t>(value);
    } else {
        JNI_CALL(jlong, result, Isolate(), GraalAccessMethod::value_integer_value, Long, GetJavaObject());
        return result;
    }
}

v8::Maybe<int64_t> GraalValue::IntegerValue(v8::Local<v8::Context> context) const {
    int64_t result;
    if (IsNumber()) {
        const GraalNumber* graal_number = reinterpret_cast<const GraalNumber*> (this);
        double d = graal_number->Value();
        if (std::isnan(d))  {
            result = 0;
        } else if (d >= static_cast<double> (std::numeric_limits<int64_t>::max())) {
            result = std::numeric_limits<int64_t>::max();
        } else  if (d <= static_cast<double> (std::numeric_limits<int64_t>::min())) {
            result = std::numeric_limits<int64_t>::min();
        } else {
            result = static_cast<int64_t> (d);
        }
    } else {
        GraalIsolate* graal_isolate = Isolate();
        JNIEnv* env = graal_isolate->GetJNIEnv();
        jthrowable pending = env->ExceptionOccurred();
        if (pending) env->ExceptionClear();
        JNI_CALL(jlong, java_result, graal_isolate, GraalAccessMethod::value_integer_value, Long, GetJavaObject());
        if (env->ExceptionCheck()) {
            return v8::Nothing<int64_t>();
        }
        if (pending) env->Throw(pending);
        result = java_result;
    }
    return v8::Just<int64_t>(result);
}

bool GraalValue::BooleanValue() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_to_boolean, Boolean, GetJavaObject());
    return result;
}

double GraalValue::NumberValue() const {
    double value;
    if (IsNumber()) {
        value = reinterpret_cast<const GraalNumber*> (this)->Value();
    } else if (IsDate()) {
        value = reinterpret_cast<const GraalDate*> (this)->ValueOf();
    } else {
        JNI_CALL(jdouble, result, Isolate(), GraalAccessMethod::value_double, Double, GetJavaObject());
        value = result;
    }
    return value;
}

v8::Local<v8::Object> GraalValue::ToObject(v8::Isolate* isolate) const {
    if (IsObject()) {
        v8::Object* v8_object = reinterpret_cast<v8::Object*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::Object>::New(isolate, v8_object);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::value_to_object, Object, java_context, GetJavaObject());
    if (java_object == NULL) {
        return v8::Local<v8::Object>();
    }
    GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
    v8::Object* v8_object = reinterpret_cast<v8::Object*> (graal_object);
    return v8::Local<v8::Object>::New(isolate, v8_object);
}

v8::Local<v8::String> GraalValue::ToString(v8::Isolate* isolate) const {
    if (IsString()) {
        v8::String* v8_string = reinterpret_cast<v8::String*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::String>::New(isolate, v8_string);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_string, graal_isolate, GraalAccessMethod::value_to_string, Object, GetJavaObject());
    if (java_string == NULL) {
        return v8::Local<v8::String>();
    }
    GraalString* graal_string = GraalString::Allocate(graal_isolate, java_string);
    v8::String* v8_string = reinterpret_cast<v8::String*> (graal_string);
    return v8::Local<v8::String>::New(isolate, v8_string);
}

v8::Local<v8::Boolean> GraalValue::ToBoolean(v8::Isolate* isolate) const {
    if (IsBoolean()) {
        v8::Boolean* v8_boolean = reinterpret_cast<v8::Boolean*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::Boolean>::New(isolate, v8_boolean);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::value_to_boolean, Boolean, GetJavaObject());
    GraalBoolean* graal_boolean = result ? graal_isolate->GetTrue() : graal_isolate->GetFalse();
    v8::Boolean* v8_boolean = reinterpret_cast<v8::Boolean*> (graal_boolean);
    return v8::Local<v8::Boolean>::New(isolate, v8_boolean);
}

v8::Local<v8::Integer> GraalValue::ToInteger(v8::Isolate* isolate) const {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_integer, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = GraalNumber::Allocate(graal_isolate, value_double, java_number);
    v8::Integer* v8_number = reinterpret_cast<v8::Integer*> (graal_number);
    return v8::Local<v8::Integer>::New(isolate, v8_number);
}

v8::Local<v8::Int32> GraalValue::ToInt32(v8::Isolate* isolate) const {
    if (IsInt32()) {
        v8::Int32* v8_int32 = reinterpret_cast<v8::Int32*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::Int32>::New(isolate, v8_int32);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_int32, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = GraalNumber::Allocate(graal_isolate, value_double, java_number);
    v8::Int32* v8_int32 = reinterpret_cast<v8::Int32*> (graal_number);
    return v8::Local<v8::Int32>::New(isolate, v8_int32);
}

v8::Local<v8::Uint32> GraalValue::ToUint32(v8::Isolate* isolate) const {
    if (IsUint32()) {
        v8::Uint32* v8_uint32 = reinterpret_cast<v8::Uint32*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::Uint32>::New(isolate, v8_uint32);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_uint32, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = GraalNumber::Allocate(graal_isolate, value_double, java_number);
    v8::Uint32* v8_uint32 = reinterpret_cast<v8::Uint32*> (graal_number);
    return v8::Local<v8::Uint32>::New(isolate, v8_uint32);
}

v8::Local<v8::Number> GraalValue::ToNumber(v8::Isolate* isolate) const {
    if (IsNumber()) {
        v8::Number* v8_number = reinterpret_cast<v8::Number*> (const_cast<GraalValue*> (this));
        return v8::Local<v8::Number>::New(isolate, v8_number);
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_number, Object, GetJavaObject());
    GraalNumber* graal_number;
    if (java_number == NULL) {
        graal_number = nullptr;
    } else {
        JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
        graal_number = GraalNumber::Allocate(graal_isolate, value_double, java_number);
    }
    v8::Number* v8_number = reinterpret_cast<v8::Number*> (graal_number);
    return v8::Local<v8::Number>::New(isolate, v8_number);
}

v8::Local<v8::Uint32> GraalValue::ToArrayIndex() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_array_index, Object, GetJavaObject());
    GraalValue* graal_index;
    if (java_number == NULL) {
        graal_index = nullptr; // not an array index
    } else {
        graal_isolate->ResetSharedBuffer();
        graal_index = GraalValue::FromJavaObject(graal_isolate, java_number, 6/*NUMBER_VALUE*/, true);
    }
    v8::Uint32* v8_index = reinterpret_cast<v8::Uint32*> (graal_index);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Uint32>::New(v8_isolate, v8_index);
}

v8::Maybe<bool> GraalValue::Equals(v8::Local<v8::Value> that) const {
    GraalValue* graal_that = reinterpret_cast<GraalValue*> (*that);
    jobject java_that = graal_that->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_equals, Boolean, GetJavaObject(), java_that);
    return v8::Just((bool) result);
}

bool GraalValue::StrictEquals(v8::Local<v8::Value> that) const {
    GraalValue* graal_that = reinterpret_cast<GraalValue*> (*that);
    jobject java_that = graal_that->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_strict_equals, Boolean, GetJavaObject(), java_that);
    return result;
}

bool GraalValue::InstanceOf(v8::Local<v8::Object> object) {
    GraalObject* graal_object = reinterpret_cast<GraalObject*> (*object);
    jobject java_object = graal_object->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_instance_of, Boolean, GetJavaObject(), java_object);
    return result;
}

GraalValue* GraalValue::FromJavaObject(GraalIsolate* isolate, jobject java_object) {
    JNI_CALL(int, type, isolate, GraalAccessMethod::value_type, Int, java_object);
    return GraalValue::FromJavaObject(isolate, java_object, type, false);
}

GraalValue* GraalValue::FromJavaObject(GraalIsolate* isolate, jobject java_object, bool create_new_local_ref) {
    jobject actual_java_object = create_new_local_ref ? isolate->GetJNIEnv()->NewLocalRef(java_object) : java_object;
    return GraalValue::FromJavaObject(isolate, actual_java_object);
}

static inline GraalValue* CreateArrayBufferView(GraalIsolate* isolate, jobject java_object, int type, bool use_shared_buffer, void* placement) {
    GraalValue* array_buffer_view;
    if (use_shared_buffer) {
        int32_t byte_length = isolate->ReadInt32FromSharedBuffer();
        int32_t byte_offset = isolate->ReadInt32FromSharedBuffer();
        if (placement) {
            array_buffer_view = GraalArrayBufferView::Allocate(isolate, java_object, type, byte_length, byte_offset, placement);
        } else {
            array_buffer_view = GraalArrayBufferView::Allocate(isolate, java_object, type, byte_length, byte_offset);
        }
    } else {
        if (placement) {
            array_buffer_view = GraalArrayBufferView::Allocate(isolate, java_object, type, placement);
        } else {
            array_buffer_view = GraalArrayBufferView::Allocate(isolate, java_object, type);
        }
    }
    return array_buffer_view;
}

GraalValue* GraalValue::FromJavaObject(GraalIsolate* isolate, jobject java_object, int type, bool use_shared_buffer) {
    return FromJavaObject(isolate, java_object, type, use_shared_buffer, nullptr);
}

GraalValue* GraalValue::FromJavaObject(GraalIsolate* isolate, jobject java_object, int type, bool use_shared_buffer, void* placement) {
    if (java_object == NULL) {
        fprintf(stderr, "*** NULL passed to FromJavaObject ***\n");
        return isolate->GetNull();
    }

    GraalValue* result;
    switch (type) {
        case UNDEFINED_VALUE:
            if (placement) {
                result = GraalMissingPrimitive::Allocate(isolate, java_object, true, placement);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetUndefined();
            }
            break;
        case NULL_VALUE:
            if (placement) {
                result = GraalMissingPrimitive::Allocate(isolate, java_object, false, placement);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetNull();
            }
            break;
        case BOOLEAN_VALUE_TRUE:
            if (placement) {
                result = GraalBoolean::Allocate(isolate, true, java_object, placement);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetTrue();
            }
            break;
        case BOOLEAN_VALUE_FALSE:
            if (placement) {
                result = GraalBoolean::Allocate(isolate, false, java_object, placement);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetFalse();
            }
            break;
        case STRING_VALUE:
            if (placement) {
                result = GraalString::Allocate(isolate, java_object, placement);
            } else {
                result = GraalString::Allocate(isolate, java_object);
            }
            break;
        case NUMBER_VALUE:
            double value_double;
            if (use_shared_buffer) {
                value_double = isolate->ReadDoubleFromSharedBuffer();
            } else {
                JNI_CALL(double, result, isolate, GraalAccessMethod::value_double, Double, java_object);
                value_double = result;
            }
            if (placement) {
                result = GraalNumber::Allocate(isolate, value_double, java_object, placement);
            } else {
                result = GraalNumber::Allocate(isolate, value_double, java_object);
            }
            break;
        case EXTERNAL_OBJECT:
            JNI_CALL(jlong, value_external, isolate, GraalAccessMethod::value_external, Long, java_object);
            if (placement) {
                result = GraalExternal::Allocate(isolate, (void*) value_external, java_object, placement);
            } else {
                result = GraalExternal::Allocate(isolate, (void*) value_external, java_object);
            }
            break;
        case FUNCTION_OBJECT:
            if (placement) {
                result = GraalFunction::Allocate(isolate, java_object, placement);
            } else {
                result = GraalFunction::Allocate(isolate, java_object);
            }
            break;
        case ARRAY_OBJECT:
            if (placement) {
                result = GraalArray::Allocate(isolate, java_object, placement);
            } else {
                result = GraalArray::Allocate(isolate, java_object);
            }
            break;
        case DATE_OBJECT:
            JNI_CALL(double, time, isolate, GraalAccessMethod::date_value_of, Double, java_object);
            if (placement) {
                result = GraalDate::Allocate(isolate, time, java_object, placement);
            } else {
                result = GraalDate::Allocate(isolate, time, java_object);
            }
            break;
        case REGEXP_OBJECT:
            if (placement) {
                result = GraalRegExp::Allocate(isolate, java_object, placement);
            } else {
                result = GraalRegExp::Allocate(isolate, java_object);
            }
            break;
        case ORDINARY_OBJECT:
            if (placement) {
                result = GraalObject::Allocate(isolate, java_object, placement);
            } else {
                result = GraalObject::Allocate(isolate, java_object);
            }
            break;
        case LAZY_STRING_VALUE:
            JNI_CALL(jobject, value_string, isolate, GraalAccessMethod::value_string, Object, java_object);
            isolate->GetJNIEnv()->DeleteLocalRef(java_object);
            if (placement) {
                result = GraalString::Allocate(isolate, value_string, placement);
            } else {
                result = GraalString::Allocate(isolate, value_string);
            }
            break;
        case ARRAY_BUFFER_VIEW_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUnknownArray, use_shared_buffer, placement);
            break;
        case DIRECT_UINT8ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectUint8Array, use_shared_buffer, placement);
            break;
        case DIRECT_UINT8CLAMPEDARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectUint8ClampedArray, use_shared_buffer, placement);
            break;
        case DIRECT_INT8ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectInt8Array, use_shared_buffer, placement);
            break;
        case DIRECT_UINT16ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectUint16Array, use_shared_buffer, placement);
            break;
        case DIRECT_INT16ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectInt16Array, use_shared_buffer, placement);
            break;
        case DIRECT_UINT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectUint32Array, use_shared_buffer, placement);
            break;
        case DIRECT_INT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectInt32Array, use_shared_buffer, placement);
            break;
        case DIRECT_FLOAT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectFloat32Array, use_shared_buffer, placement);
            break;
        case DIRECT_FLOAT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectFloat64Array, use_shared_buffer, placement);
            break;
        case DATA_VIEW_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDataView, use_shared_buffer, placement);
            break;
        case DIRECT_BIGINT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectBigInt64Array, use_shared_buffer, placement);
            break;
        case DIRECT_BIGUINT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDirectBigUint64Array, use_shared_buffer, placement);
            break;
        case INTEROP_UINT8ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropUint8Array, use_shared_buffer, placement);
            break;
        case INTEROP_UINT8CLAMPEDARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropUint8ClampedArray, use_shared_buffer, placement);
            break;
        case INTEROP_INT8ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropInt8Array, use_shared_buffer, placement);
            break;
        case INTEROP_UINT16ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropUint16Array, use_shared_buffer, placement);
            break;
        case INTEROP_INT16ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropInt16Array, use_shared_buffer, placement);
            break;
        case INTEROP_UINT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropUint32Array, use_shared_buffer, placement);
            break;
        case INTEROP_INT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropInt32Array, use_shared_buffer, placement);
            break;
        case INTEROP_FLOAT32ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropFloat32Array, use_shared_buffer, placement);
            break;
        case INTEROP_FLOAT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropFloat64Array, use_shared_buffer, placement);
            break;
        case INTEROP_BIGINT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropBigInt64Array, use_shared_buffer, placement);
            break;
        case INTEROP_BIGUINT64ARRAY_OBJECT:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInteropBigUint64Array, use_shared_buffer, placement);
            break;
        case DIRECT_ARRAY_BUFFER_OBJECT:
            if (placement) {
                result = GraalArrayBuffer::Allocate(isolate, java_object, true, placement);
            } else {
                result = GraalArrayBuffer::Allocate(isolate, java_object, true);
            }
            break;
        case INTEROP_ARRAY_BUFFER_OBJECT:
            if (placement) {
                result = GraalArrayBuffer::Allocate(isolate, java_object, false, placement);
            } else {
                result = GraalArrayBuffer::Allocate(isolate, java_object, false);
            }
            break;
        case SYMBOL_VALUE:
            if (placement) {
                result = GraalSymbol::Allocate(isolate, java_object, placement);
            } else {
                result = GraalSymbol::Allocate(isolate, java_object);
            }
            break;
        case MAP_OBJECT:
            if (placement) {
                result = GraalMap::Allocate(isolate, java_object, placement);
            } else {
                result = GraalMap::Allocate(isolate, java_object);
            }
            break;
        case SET_OBJECT:
            if (placement) {
                result = GraalSet::Allocate(isolate, java_object, placement);
            } else {
                result = GraalSet::Allocate(isolate, java_object);
            }
            break;
        case PROMISE_OBJECT:
            if (placement) {
                result = GraalPromise::Allocate(isolate, java_object, placement);
            } else {
                result = GraalPromise::Allocate(isolate, java_object);
            }
            break;
        case PROXY_OBJECT:
            if (placement) {
                result = GraalProxy::Allocate(isolate, java_object, placement);
            } else {
                result = GraalProxy::Allocate(isolate, java_object);
            }
            break;
        case BIG_INT_VALUE:
            if (placement) {
                result = GraalBigInt::Allocate(isolate, java_object, placement);
            } else {
                result = GraalBigInt::Allocate(isolate, java_object);
            }
            break;
        default:
            // Unknown value (using its String representation as a fallback)
            JNI_CALL(jobject, value_unknown, isolate, GraalAccessMethod::value_unknown, Object, java_object);
            isolate->GetJNIEnv()->DeleteLocalRef(java_object);
            if (placement) {
                result = GraalString::Allocate(isolate, value_unknown, placement);
            } else {
                result = GraalString::Allocate(isolate, value_unknown);
            }
            break;
    }

    return result;
}

v8::Local<v8::String> GraalValue::TypeOf(v8::Isolate* isolate) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_value = GetJavaObject();
    JNI_CALL(jobject, java_type, graal_isolate, GraalAccessMethod::value_type_of, Object, java_value);
    GraalString* graal_type = GraalString::Allocate(graal_isolate, java_type);
    v8::String* v8_type = reinterpret_cast<v8::String*> (graal_type);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::String>::New(v8_isolate, v8_type);
}

v8::MaybeLocal<v8::String> GraalValue::ToDetailString(v8::Local<v8::Context> context) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_string, graal_isolate, GraalAccessMethod::value_to_detail_string, Object, GetJavaObject());
    GraalString* graal_string = GraalString::Allocate(graal_isolate, java_string);
    v8::String* v8_string = reinterpret_cast<v8::String*> (graal_string);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::String>::New(v8_isolate, v8_string);
}
