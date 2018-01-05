/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_FUNCTION_TEMPLATE_H_
#define GRAAL_FUNCTION_TEMPLATE_H_

#include "graal_template.h"

class GraalFunctionTemplate : public GraalTemplate {
public:
    GraalFunctionTemplate(GraalIsolate* isolate, jobject java_template, int id);
    static v8::Local<v8::FunctionTemplate> New(
            v8::Isolate* isolate, v8::FunctionCallback callback,
            v8::Local<v8::Value> data,
            v8::Local<v8::Signature> signature,
            int length);
    void SetClassName(v8::Local<v8::String> name);
    v8::Local<v8::ObjectTemplate> InstanceTemplate();
    v8::Local<v8::ObjectTemplate> PrototypeTemplate();
    v8::Local<v8::Function> GetFunction(v8::Local<v8::Context> context);
    bool HasInstance(v8::Local<v8::Value> object);
    void SetCallHandler(v8::FunctionCallback callback, v8::Local<v8::Value> data);
    void Inherit(v8::Local<v8::FunctionTemplate> parent);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    int id_;
};

#endif /* GRAAL_FUNCTION_TEMPLATE_H_ */

