/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_boolean.h"
#include "graal_isolate.h"

GraalBoolean::GraalBoolean(GraalIsolate* isolate, bool value) :
GraalBoolean(isolate, value, isolate->GetJNIEnv()->NewLocalRef(value ? isolate->GetJavaTrue() : isolate->GetJavaFalse())) {
}

GraalBoolean::GraalBoolean(GraalIsolate* isolate, bool value, jobject java_value) : GraalPrimitive(isolate, java_value),
value_(value) {
}

GraalHandleContent* GraalBoolean::CopyImpl(jobject java_object_copy) {
    return new GraalBoolean(Isolate(), value_, java_object_copy);
}

bool GraalBoolean::IsBoolean() const {
    return true;
}

bool GraalBoolean::IsTrue() const {
    return value_;
}

bool GraalBoolean::IsFalse() const {
    return !value_;
}

bool GraalBoolean::Value() const {
    return value_;
}
