/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_external.h"
#include "graal_isolate.h"
#include "include/v8.h"

GraalExternal::GraalExternal(GraalIsolate* isolate, void* value, jobject java_external) : GraalValue(isolate, java_external), value_(value) {
}

GraalHandleContent* GraalExternal::CopyImpl(jobject java_object_copy) {
    return new GraalExternal(Isolate(), value_, java_object_copy);
}

bool GraalExternal::IsExternal() const {
    return true;
}

bool GraalExternal::IsObject() const {
    return true;
}

v8::Local<v8::External> GraalExternal::New(v8::Isolate* isolate, void* value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_external, isolate, GraalAccessMethod::external_new, Object, java_context, (jlong) value);
    GraalExternal* graal_external = new GraalExternal(graal_isolate, value, java_external);
    return reinterpret_cast<v8::External*> (graal_external);
}
