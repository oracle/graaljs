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

#include "graal_context.h"
#include "graal_isolate.h"
#include "graal_promise.h"

#include "graal_promise-inl.h"

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
    v8::Value* v8_result = reinterpret_cast<v8::Value*> (graal_result);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_result);
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
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Promise::Resolver>::New(v8_isolate, v8_resolver);
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
    v8::Promise* v8_promise = reinterpret_cast<v8::Promise*> (resolver);
    return v8::Local<v8::Promise>::New(v8_promise->GetIsolate(), v8_promise);
}
