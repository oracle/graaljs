/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

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
    GraalValue* graal_data = GraalValue::FromJavaObject(isolate, data);
    GraalValue* graal_holder = GraalValue::FromJavaObject(isolate, holder);
    return GraalPropertyCallbackInfo<T>(isolate, graal_this, graal_data, graal_holder);
}

template<typename T>
GraalPropertyCallbackInfo<T>::GraalPropertyCallbackInfo(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_data,
        GraalValue* graal_holder) : v8::PropertyCallbackInfo<T>(reinterpret_cast<v8::internal::Object**>(values_)) {
    graal_this->ReferenceAdded();
    graal_data->ReferenceAdded();
    graal_holder->ReferenceAdded();
    values_[v8::PropertyCallbackInfo<T>::kHolderIndex] = graal_holder;
    values_[v8::PropertyCallbackInfo<T>::kIsolateIndex] = isolate;
    values_[v8::PropertyCallbackInfo<T>::kReturnValueIndex] = nullptr;
    values_[v8::PropertyCallbackInfo<T>::kDataIndex] = graal_data;
    values_[v8::PropertyCallbackInfo<T>::kThisIndex] = graal_this;
}

template<typename T>
GraalPropertyCallbackInfo<T>::~GraalPropertyCallbackInfo() {
    reinterpret_cast<GraalValue*> (values_[v8::PropertyCallbackInfo<T>::kDataIndex])->ReferenceRemoved();
    reinterpret_cast<GraalValue*> (values_[v8::PropertyCallbackInfo<T>::kThisIndex])->ReferenceRemoved();
    reinterpret_cast<GraalValue*> (values_[v8::PropertyCallbackInfo<T>::kHolderIndex])->ReferenceRemoved();
    GraalValue* return_value = reinterpret_cast<GraalValue*> (values_[v8::PropertyCallbackInfo<T>::kReturnValueIndex]);
    if (return_value) {
        return_value->ReferenceRemoved();
    }
}

template class GraalPropertyCallbackInfo<v8::Value>;
template class GraalPropertyCallbackInfo<v8::Integer>;
template class GraalPropertyCallbackInfo<v8::Boolean>;
template class GraalPropertyCallbackInfo<v8::Array>;
template class GraalPropertyCallbackInfo<void>;
