/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_name.h"
#include "graal_template.h"

GraalTemplate::GraalTemplate(GraalIsolate* isolate, jobject java_template) : GraalData(isolate, java_template) {
}

void GraalTemplate::Set(v8::Local<v8::Value> key, v8::Local<v8::Data> value, v8::PropertyAttribute attributes) {
    jobject java_key = reinterpret_cast<GraalValue*> (*key)->GetJavaObject();
    jobject java_value = reinterpret_cast<GraalValue*> (*value)->GetJavaObject();
    jint java_attributes = static_cast<jint> (attributes);
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::template_set, GetJavaObject(), java_key, java_value, java_attributes);
}
