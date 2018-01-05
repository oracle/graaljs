/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_context.h"
#include "graal_isolate.h"
#include "graal_promise.h"

GraalPromise::GraalPromise(GraalIsolate* isolate, jobject java_promise) : GraalObject(isolate, java_promise) {
}

GraalHandleContent* GraalPromise::CopyImpl(jobject java_object_copy) {
    return new GraalPromise(Isolate(), java_object_copy);
}

bool GraalPromise::IsPromise() const {
    return true;
}

v8::Local<v8::Value> GraalPromise::Result() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_promise = GetJavaObject();
    JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::promise_result, Object, java_promise);
    GraalValue* graal_result = GraalValue::FromJavaObject(graal_isolate, java_result);
    return reinterpret_cast<v8::Value*> (graal_result);
}

v8::Promise::PromiseState GraalPromise::State() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_promise = GetJavaObject();
    JNI_CALL(jint, state, graal_isolate, GraalAccessMethod::promise_state, Int, java_promise);
    return static_cast<v8::Promise::PromiseState> (state);
}

v8::MaybeLocal<v8::Promise::Resolver> GraalPromise::ResolverNew(v8::Local<v8::Context> context) {
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    GraalIsolate* graal_isolate = graal_context->Isolate();
    JNI_CALL(jobject, java_resolver, graal_isolate, GraalAccessMethod::promise_resolver_new, Object, java_context);
    GraalPromise* graal_resolver = new GraalPromise(graal_isolate, java_resolver);
    v8::Promise::Resolver* v8_resolver = reinterpret_cast<v8::Promise::Resolver*> (graal_resolver);
    return v8::Local<v8::Promise::Resolver>(v8_resolver);
}

v8::Maybe<bool> GraalPromise::ResolverResolve(v8::Promise::Resolver* resolver, v8::Local<v8::Value> value) {
    GraalHandleContent* graal_resolver = reinterpret_cast<GraalHandleContent*> (resolver);
    jobject java_resolver = graal_resolver->GetJavaObject();
    GraalHandleContent* graal_value = reinterpret_cast<GraalHandleContent*> (*value);
    jobject java_value = graal_value->GetJavaObject();
    GraalIsolate* graal_isolate = graal_resolver->Isolate();
    JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::promise_resolver_resolve, Boolean, java_resolver, java_value);
    return v8::Just<bool>(result);
}

v8::Maybe<bool> GraalPromise::ResolverReject(v8::Promise::Resolver* resolver, v8::Local<v8::Value> value) {
    GraalHandleContent* graal_resolver = reinterpret_cast<GraalHandleContent*> (resolver);
    jobject java_resolver = graal_resolver->GetJavaObject();
    GraalHandleContent* graal_value = reinterpret_cast<GraalHandleContent*> (*value);
    jobject java_value = graal_value->GetJavaObject();
    GraalIsolate* graal_isolate = graal_resolver->Isolate();
    JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::promise_resolver_reject, Boolean, java_resolver, java_value);
    return v8::Just<bool>(result);
}

v8::Local<v8::Promise> GraalPromise::ResolverGetPromise(v8::Promise::Resolver* resolver) {
    return reinterpret_cast<v8::Promise*> (resolver);
}
