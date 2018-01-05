/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_PROMISE_H_
#define GRAAL_PROMISE_H_

#include "graal_object.h"

class GraalPromise : public GraalObject {
public:
    GraalPromise(GraalIsolate* isolate, jobject java_promise);
    bool IsPromise() const override;
    v8::Local<v8::Value> Result();
    v8::Promise::PromiseState State();
    static v8::MaybeLocal<v8::Promise::Resolver> ResolverNew(v8::Local<v8::Context> context);
    static v8::Maybe<bool> ResolverResolve(v8::Promise::Resolver* resolver, v8::Local<v8::Value> value);
    static v8::Maybe<bool> ResolverReject(v8::Promise::Resolver* resolver, v8::Local<v8::Value> value);
    static v8::Local<v8::Promise> ResolverGetPromise(v8::Promise::Resolver* resolver);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_PROMISE_H_ */
