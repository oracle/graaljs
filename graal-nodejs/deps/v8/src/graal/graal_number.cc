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

#include "graal_isolate.h"
#include "graal_number.h"
#include <cmath>

#include "graal_number-inl.h"

GraalHandleContent* GraalNumber::CopyImpl(jobject java_object_copy) {
    return GraalNumber::Allocate(Isolate(), value_, java_object_copy);
}

bool GraalNumber::IsInt32() const {
    return (value_ == (int32_t) value_) && !(value_ == 0 && std::signbit(value_));
}

bool GraalNumber::IsUint32() const {
    return (value_ == (uint32_t) value_) && !(value_ == 0 && std::signbit(value_));
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
    GraalNumber* graal_number = GraalNumber::Allocate(graal_isolate, value, java_number);
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
    GraalNumber* graal_number = GraalNumber::Allocate(graal_isolate, value, java_number);
    return reinterpret_cast<v8::Integer*> (graal_number);
}

double GraalNumber::Value() const {
    return value_;
}

GraalNumber* GraalNumber::NewNotCached(GraalIsolate* isolate, int value) {
    JNI_CALL(jobject, java_number, isolate, GraalAccessMethod::integer_new, Object, (jlong) value);
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    return GraalNumber::Allocate(graal_isolate, value, java_number);
}
