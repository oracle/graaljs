/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_TEMPLATE_H_
#define GRAAL_TEMPLATE_H_

#include "graal_data.h"
#include "include/v8.h"
#include "jni.h"

class GraalTemplate : public GraalData {
public:
    GraalTemplate(GraalIsolate* isolate, jobject java_template);
    void Set(v8::Local<v8::Value> key, v8::Local<v8::Data> value, v8::PropertyAttribute attributes);
};

#endif /* GRAAL_TEMPLATE_H_ */
