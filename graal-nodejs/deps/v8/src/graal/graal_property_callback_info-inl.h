/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GRAAL_PROPERTY_CALLBACK_INFO_INL_H_
#define GRAAL_PROPERTY_CALLBACK_INFO_INL_H_

#include "graal_property_callback_info.h"

template<typename T>
GraalPropertyCallbackInfo<T> GraalPropertyCallbackInfo<T>::New(
        GraalIsolate* isolate,
        jobjectArray arguments,
        int index_of_this,
        jobject data,
        jobject holder) {
    JNIEnv* env = isolate->GetJNIEnv();
    jobject java_this = env->GetObjectArrayElement(arguments, index_of_this);
    GraalValue* graal_this = GraalValue::FromJavaObject(isolate, java_this);
    GraalValue* graal_data = (data == nullptr) ? nullptr : GraalValue::FromJavaObject(isolate, data);
    GraalValue* graal_holder = GraalValue::FromJavaObject(isolate, holder);
    return GraalPropertyCallbackInfo<T>(isolate, graal_this, graal_data, graal_holder);
}

template<typename T>
GraalPropertyCallbackInfo<T>::GraalPropertyCallbackInfo(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_data,
        GraalValue* graal_holder) : v8::PropertyCallbackInfo<T>(reinterpret_cast<v8::internal::Address*>(values_)) {
    graal_this->ReferenceAdded();
    if (graal_data != nullptr) {
        graal_data->ReferenceAdded();
    }
    graal_holder->ReferenceAdded();
    values_[v8::internal::PropertyCallbackArguments::kHolderIndex] = graal_holder;
    values_[v8::internal::PropertyCallbackArguments::kIsolateIndex] = isolate;
    values_[v8::internal::PropertyCallbackArguments::kReturnValueIndex] = nullptr;
    values_[v8::internal::PropertyCallbackArguments::kDataIndex] = graal_data;
    values_[v8::internal::PropertyCallbackArguments::kThisIndex] = graal_this;
}

template<typename T>
GraalPropertyCallbackInfo<T>::~GraalPropertyCallbackInfo() {
    GraalValue* graal_data = reinterpret_cast<GraalValue*> (values_[v8::internal::PropertyCallbackArguments::kDataIndex]);
    if (graal_data != nullptr) {
        graal_data->ReferenceRemoved();
    }
    reinterpret_cast<GraalValue*> (values_[v8::internal::PropertyCallbackArguments::kThisIndex])->ReferenceRemoved();
    reinterpret_cast<GraalValue*> (values_[v8::internal::PropertyCallbackArguments::kHolderIndex])->ReferenceRemoved();
    GraalValue* return_value = reinterpret_cast<GraalValue*> (values_[v8::internal::PropertyCallbackArguments::kReturnValueIndex]);
    if (return_value != nullptr) {
        return_value->ReferenceRemoved();
    }
}

template class GraalPropertyCallbackInfo<v8::Value>;
template class GraalPropertyCallbackInfo<v8::Integer>;
template class GraalPropertyCallbackInfo<v8::Boolean>;
template class GraalPropertyCallbackInfo<v8::Array>;
template class GraalPropertyCallbackInfo<void>;

#endif /* GRAAL_PROPERTY_CALLBACK_INFO_INL_H_ */
