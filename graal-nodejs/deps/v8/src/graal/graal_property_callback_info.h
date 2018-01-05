/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_PROPERTY_CALLBACK_INFO_H_
#define GRAAL_PROPERTY_CALLBACK_INFO_H_

#include "graal_isolate.h"
#include "graal_value.h"
#include "include/v8.h"

template<typename T>
class GraalPropertyCallbackInfo : public v8::PropertyCallbackInfo<T> {
public:
    static GraalPropertyCallbackInfo<T> New(
            GraalIsolate* isolate,
            jobjectArray arguments,
            int index_of_this,
            jobject data,
            jobject holder);
    ~GraalPropertyCallbackInfo();

    GraalPropertyCallbackInfo(GraalPropertyCallbackInfo&&) = default;
    GraalPropertyCallbackInfo& operator=(GraalPropertyCallbackInfo&&) = default;
private:
    GraalPropertyCallbackInfo(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_data,
        GraalValue* graal_holder);

    GraalPropertyCallbackInfo(const GraalPropertyCallbackInfo&) = delete;
    GraalPropertyCallbackInfo& operator=(const GraalPropertyCallbackInfo&) = delete;

    void* values_[v8::PropertyCallbackInfo<T>::kArgsLength];
};

extern template class GraalPropertyCallbackInfo<v8::Value>;
extern template class GraalPropertyCallbackInfo<v8::Integer>;
extern template class GraalPropertyCallbackInfo<v8::Boolean>;
extern template class GraalPropertyCallbackInfo<v8::Array>;
extern template class GraalPropertyCallbackInfo<void>;

#endif /* GRAAL_PROPERTY_CALLBACK_INFO_H_ */

