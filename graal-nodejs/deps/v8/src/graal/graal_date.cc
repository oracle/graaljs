/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_date.h"
#include "graal_isolate.h"

GraalDate::GraalDate(GraalIsolate* isolate, double time, jobject java_date) : GraalObject(isolate, java_date), time_(time) {
}

GraalHandleContent* GraalDate::CopyImpl(jobject java_object_copy) {
    return new GraalDate(Isolate(), time_, java_object_copy);
}

v8::Local<v8::Value> GraalDate::New(v8::Isolate* isolate, double time) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_date, isolate, GraalAccessMethod::date_new, Object, java_context, (jdouble) time);
    GraalDate* graal_date = new GraalDate(graal_isolate, time, java_date);
    return reinterpret_cast<v8::Value*> (graal_date);
}

double GraalDate::ValueOf() const {
    return time_;
}

bool GraalDate::IsDate() const {
    return true;
}
