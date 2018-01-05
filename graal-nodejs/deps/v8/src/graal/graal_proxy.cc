/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_proxy.h"
#include "graal_value.h"

GraalProxy::GraalProxy(GraalIsolate* isolate, jobject java_proxy) : GraalObject(isolate, java_proxy) {
}

GraalHandleContent* GraalProxy::CopyImpl(jobject java_object_copy) {
    return new GraalProxy(Isolate(), java_object_copy);
}

bool GraalProxy::IsFunction() const {
    JNI_CALL(bool, result, Isolate(), GraalAccessMethod::proxy_is_function, Boolean, GetJavaObject());
    return result;
}

bool GraalProxy::IsProxy() const {
    return true;
}

v8::Local<v8::Object> GraalProxy::GetTarget() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_proxy = GetJavaObject();
    JNI_CALL(jobject, java_target, graal_isolate, GraalAccessMethod::proxy_get_target, Object, java_proxy);
    GraalValue* graal_target = GraalValue::FromJavaObject(graal_isolate, java_target);
    return reinterpret_cast<v8::Object*> (graal_target);
}

v8::Local<v8::Value> GraalProxy::GetHandler() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_proxy = GetJavaObject();
    JNI_CALL(jobject, java_handler, graal_isolate, GraalAccessMethod::proxy_get_handler, Object, java_proxy);
    GraalValue* graal_handler = GraalValue::FromJavaObject(graal_isolate, java_handler);
    return reinterpret_cast<v8::Object*> (graal_handler);
}
