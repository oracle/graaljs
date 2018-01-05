/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <cmath>
#include "graal_isolate.h"
#include "graal_number.h"

GraalNumber::GraalNumber(GraalIsolate* isolate, double value, jobject java_number) : GraalPrimitive(isolate, java_number), value_(value) {
}

GraalHandleContent* GraalNumber::CopyImpl(jobject java_object_copy) {
    return new GraalNumber(Isolate(), value_, java_object_copy);
}

bool GraalNumber::IsInt32() const {
    return value_ == (int32_t) value_;
}

bool GraalNumber::IsUint32() const {
    return value_ == (uint32_t) value_;
}

bool GraalNumber::IsNumber() const {
    return true;
}

v8::Local<v8::Number> GraalNumber::New(v8::Isolate* isolate, double value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    int intValue = (int) value;
    if (intValue == value && !(value == 0 && std::signbit(value))) {
        GraalNumber* cached_number = graal_isolate->CachedNumber(intValue);
        if (cached_number != nullptr) {
            return reinterpret_cast<v8::Number*> (cached_number);
        }
    }
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::number_new, Object, (jdouble) value);
    GraalNumber* graal_number = new GraalNumber(graal_isolate, value, java_number);
    return reinterpret_cast<v8::Number*> (graal_number);
}

v8::Local<v8::Integer> GraalNumber::New(v8::Isolate* isolate, int value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalNumber* graal_number = graal_isolate->CachedNumber(value);
    if (graal_number == nullptr) {
        graal_number = NewNotCached(graal_isolate, value);
    }
    return reinterpret_cast<v8::Integer*> (graal_number);
}

v8::Local<v8::Integer> GraalNumber::NewFromUnsigned(v8::Isolate* isolate, uint32_t value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    if (value <= GraalIsolate::number_cache_high_) {
        GraalNumber* cached_number = graal_isolate->CachedNumber(value);
        if (cached_number != nullptr) {
            return reinterpret_cast<v8::Integer*> (cached_number);
        }
    }
    JNI_CALL(jobject, java_number, graal_isolate, GraalAccessMethod::integer_new, Object, (jlong) value);
    GraalNumber* graal_number = new GraalNumber(graal_isolate, value, java_number);
    return reinterpret_cast<v8::Integer*> (graal_number);
}

double GraalNumber::Value() const {
    return value_;
}

GraalNumber* GraalNumber::NewNotCached(GraalIsolate* isolate, int value) {
    JNI_CALL(jobject, java_number, isolate, GraalAccessMethod::integer_new, Object, (jlong) value);
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    return new GraalNumber(graal_isolate, value, java_number);
}
