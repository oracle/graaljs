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
#include "graal_array_buffer.h"
#include "graal_array_buffer_view.h"
#include "graal_boolean.h"
#include "graal_date.h"
#include "graal_external.h"
#include "graal_function.h"
#include "graal_isolate.h"
#include "graal_map.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_promise.h"
#include "graal_proxy.h"
#include "graal_regexp.h"
#include "graal_set.h"
#include "graal_string.h"
#include "graal_symbol.h"
#include "graal_value.h"
#include "graal_missing_primitive.h"
#include <algorithm>
#include <cmath>
#include <limits>

GraalValue::GraalValue(GraalIsolate* isolate, jobject java_object) : GraalData(isolate, java_object) {
}

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

bool GraalValue::IsWeakMap() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_weak_map, Boolean, GetJavaObject());
    return result;
}

bool GraalValue::IsWeakSet() const {
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_is_weak_set, Boolean, GetJavaObject());
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
        return std::isfinite(value) ? value : std::numeric_limits<int64_t>::min();
    } else {
        JNI_CALL(jlong, result, Isolate(), GraalAccessMethod::value_integer_value, Long, GetJavaObject());
        return result;
    }
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
        return reinterpret_cast<v8::Object*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::value_to_object, Object, java_context, GetJavaObject());
    if (java_object == NULL) {
        return v8::Local<v8::Object>();
    }
    GraalObject* graal_object = new GraalObject(graal_isolate, java_object);
    return reinterpret_cast<v8::Object*> (graal_object);
}

v8::Local<v8::String> GraalValue::ToString(v8::Isolate* isolate) const {
    if (IsString()) {
        return reinterpret_cast<v8::String*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_string, graal_isolate, GraalAccessMethod::value_to_string, Object, GetJavaObject());
    if (java_string == NULL) {
        return v8::Local<v8::String>();
    }
    GraalString* graal_string = new GraalString(graal_isolate, (jstring) java_string);
    return reinterpret_cast<v8::String*> (graal_string);
}

v8::Local<v8::Boolean> GraalValue::ToBoolean(v8::Isolate* isolate) const {
    if (IsBoolean()) {
        return reinterpret_cast<v8::Boolean*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::value_to_boolean, Boolean, GetJavaObject());
    GraalBoolean* graal_boolean = result ? graal_isolate->GetTrue() : graal_isolate->GetFalse();
    return reinterpret_cast<v8::Boolean*> (graal_boolean);
}

v8::Local<v8::Integer> GraalValue::ToInteger(v8::Isolate* isolate) const {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_integer, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = new GraalNumber(graal_isolate, value_double, java_number);
    return reinterpret_cast<v8::Integer*> (graal_number);
}

v8::Local<v8::Int32> GraalValue::ToInt32(v8::Isolate* isolate) const {
    if (IsInt32()) {
        return reinterpret_cast<v8::Int32*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_int32, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = new GraalNumber(graal_isolate, value_double, java_number);
    return reinterpret_cast<v8::Int32*> (graal_number);
}

v8::Local<v8::Uint32> GraalValue::ToUint32(v8::Isolate* isolate) const {
    if (IsUint32()) {
        return reinterpret_cast<v8::Uint32*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_uint32, Object, GetJavaObject());
    JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
    GraalNumber* graal_number = new GraalNumber(graal_isolate, value_double, java_number);
    return reinterpret_cast<v8::Uint32*> (graal_number);
}

v8::Local<v8::Number> GraalValue::ToNumber(v8::Isolate* isolate) const {
    if (IsNumber()) {
        return reinterpret_cast<v8::Number*> (const_cast<GraalValue*> (this));
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::value_to_number, Object, GetJavaObject());
    GraalNumber* graal_number;
    if (java_number == NULL) {
        graal_number = nullptr;
    } else {
        JNI_CALL(double, value_double, isolate, GraalAccessMethod::value_double, Double, java_number);
        graal_number = new GraalNumber(graal_isolate, value_double, java_number);
    }
    return reinterpret_cast<v8::Number*> (graal_number);
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
    return reinterpret_cast<v8::Uint32*> (graal_index);
}

bool GraalValue::Equals(v8::Local<v8::Value> that) const {
    GraalValue* graal_that = reinterpret_cast<GraalValue*> (*that);
    jobject java_that = graal_that->GetJavaObject();
    JNI_CALL(jboolean, result, Isolate(), GraalAccessMethod::value_equals, Boolean, GetJavaObject(), java_that);
    return result;
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
            array_buffer_view = new(placement) GraalArrayBufferView(isolate, java_object, type, byte_length, byte_offset);
        } else {
            array_buffer_view = new GraalArrayBufferView(isolate, java_object, type, byte_length, byte_offset);
        }
    } else {
        if (placement) {
            array_buffer_view = new(placement) GraalArrayBufferView(isolate, java_object, type);
        } else {
            array_buffer_view = new GraalArrayBufferView(isolate, java_object, type);
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
        case 1:
            if (placement) {
                result = new(placement) GraalMissingPrimitive(isolate, java_object, true);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetUndefined();
            }
            break;
        case 2:
            if (placement) {
                result = new(placement) GraalMissingPrimitive(isolate, java_object, false);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetNull();
            }
            break;
        case 3:
            if (placement) {
                result = new(placement) GraalBoolean(isolate, true, java_object);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetTrue();
            }
            break;
        case 4:
            if (placement) {
                result = new(placement) GraalBoolean(isolate, false, java_object);
            } else {
                isolate->GetJNIEnv()->DeleteLocalRef(java_object);
                result = isolate->GetFalse();
            }
            break;
        case 5:
            if (placement) {
                result = new(placement) GraalString(isolate, (jstring) java_object);
            } else {
                result = new GraalString(isolate, (jstring) java_object);
            }
            break;
        case 6:
            double value_double;
            if (use_shared_buffer) {
                value_double = isolate->ReadDoubleFromSharedBuffer();
            } else {
                JNI_CALL(double, result, isolate, GraalAccessMethod::value_double, Double, java_object);
                value_double = result;
            }
            if (placement) {
                result = new(placement) GraalNumber(isolate, value_double, java_object);
            } else {
                result = new GraalNumber(isolate, value_double, java_object);
            }
            break;
        case 7:
            JNI_CALL(long, value_external, isolate, GraalAccessMethod::value_external, Long, java_object);
            if (placement) {
                result = new(placement) GraalExternal(isolate, (void*) value_external, java_object);
            } else {
                result = new GraalExternal(isolate, (void*) value_external, java_object);
            }
            break;
        case 8:
            if (placement) {
                result = new(placement) GraalFunction(isolate, java_object);
            } else {
                result = new GraalFunction(isolate, java_object);
            }
            break;
        case 9:
            if (placement) {
                result = new(placement) GraalArray(isolate, java_object);
            } else {
                result = new GraalArray(isolate, java_object);
            }
            break;
        case 10:
            JNI_CALL(double, time, isolate, GraalAccessMethod::date_value_of, Double, java_object);
            if (placement) {
                result = new(placement) GraalDate(isolate, time, java_object);
            } else {
                result = new GraalDate(isolate, time, java_object);
            }
            break;
        case 11:
            if (placement) {
                result = new(placement) GraalRegExp(isolate, java_object);
            } else {
                result = new GraalRegExp(isolate, java_object);
            }
            break;
        case 12:
            if (placement) {
                result = new(placement) GraalObject(isolate, java_object);
            } else {
                result = new GraalObject(isolate, java_object);
            }
            break;
        case 13:
            JNI_CALL(jobject, value_string, isolate, GraalAccessMethod::value_string, Object, java_object);
            isolate->GetJNIEnv()->DeleteLocalRef(java_object);
            if (placement) {
                result = new(placement) GraalString(isolate, (jstring) value_string);
            } else {
                result = new GraalString(isolate, (jstring) value_string);
            }
            break;
        case 14:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUnknownArray, use_shared_buffer, placement);
            break;
        case 17:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUint8Array, use_shared_buffer, placement);
            break;
        case 18:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUint8ClampedArray, use_shared_buffer, placement);
            break;
        case 20:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInt8Array, use_shared_buffer, placement);
            break;
        case 21:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUint16Array, use_shared_buffer, placement);
            break;
        case 22:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInt16Array, use_shared_buffer, placement);
            break;
        case 19:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kUint32Array, use_shared_buffer, placement);
            break;
        case 23:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kInt32Array, use_shared_buffer, placement);
            break;
        case 24:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kFloat32Array, use_shared_buffer, placement);
            break;
        case 25:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kFloat64Array, use_shared_buffer, placement);
            break;
        case 30:
            result = CreateArrayBufferView(isolate, java_object, GraalArrayBufferView::kDataView, use_shared_buffer, placement);
            break;
        case 15:
            if (placement) {
                result = new(placement) GraalArrayBuffer(isolate, java_object);
            } else {
                result = new GraalArrayBuffer(isolate, java_object);
            }
            break;
        case 16:
            if (placement) {
                result = new(placement) GraalSymbol(isolate, java_object);
            } else {
                result = new GraalSymbol(isolate, java_object);
            }
            break;
        case 26:
            if (placement) {
                result = new(placement) GraalMap(isolate, java_object);
            } else {
                result = new GraalMap(isolate, java_object);
            }
            break;
        case 27:
            if (placement) {
                result = new(placement) GraalSet(isolate, java_object);
            } else {
                result = new GraalSet(isolate, java_object);
            }
            break;
        case 28:
            if (placement) {
                result = new(placement) GraalPromise(isolate, java_object);
            } else {
                result = new GraalPromise(isolate, java_object);
            }
            break;
        case 29:
            if (placement) {
                result = new(placement) GraalProxy(isolate, java_object);
            } else {
                result = new GraalProxy(isolate, java_object);
            }
            break;
        default:
            // Unknown value (using its String representation as a fallback)
            JNI_CALL(jobject, value_unknown, isolate, GraalAccessMethod::value_unknown, Object, java_object);
            isolate->GetJNIEnv()->DeleteLocalRef(java_object);
            if (placement) {
                result = new(placement) GraalString(isolate, (jstring) value_unknown);
            } else {
                result = new GraalString(isolate, (jstring) value_unknown);
            }
            break;
    }

    return result;
}

const int GraalValue::MAX_SIZE =
    std::max(sizeof(GraalArray),
    std::max(sizeof(GraalArrayBuffer),
    std::max(sizeof(GraalArrayBufferView),
    std::max(sizeof(GraalBoolean),
    std::max(sizeof(GraalDate),
    std::max(sizeof(GraalExternal),
    std::max(sizeof(GraalFunction),
    std::max(sizeof(GraalMap),
    std::max(sizeof(GraalNumber),
    std::max(sizeof(GraalObject),
    std::max(sizeof(GraalPromise),
    std::max(sizeof(GraalProxy),
    std::max(sizeof(GraalSet),
    std::max(sizeof(GraalString),
    sizeof(GraalSymbol)))))))))))))));
