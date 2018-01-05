/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_MODULE_H_
#define GRAAL_MODULE_H_

#include "graal_handle_content.h"

class GraalIsolate;

class GraalModule : public GraalHandleContent {
public:
    GraalModule(GraalIsolate* isolate, jobject java_module);
    static v8::MaybeLocal<v8::Module> Compile(v8::Local<v8::String> source, v8::Local<v8::String> name);
    v8::Maybe<bool> InstantiateModule(v8::Local<v8::Context> context, v8::Module::ResolveCallback callback);
    v8::MaybeLocal<v8::Value> Evaluate(v8::Local<v8::Context> context);
    v8::Module::Status GetStatus() const;
    int GetModuleRequestsLength() const;
    v8::Local<v8::String> GetModuleRequest(int index) const;
    v8::Local<v8::Value> GetModuleNamespace();
    int GetIdentityHash() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_MODULE_H_ */
