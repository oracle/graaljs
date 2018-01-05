/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_FUNCTION_H_
#define GRAAL_FUNCTION_H_

#include "graal_object.h"

class _jobjectArray;
typedef _jobjectArray *jobjectArray;

class GraalFunction : public GraalObject {
public:
    GraalFunction(GraalIsolate* isolate, jobject java_function);
    bool IsFunction() const;
    v8::Local<v8::Object> NewInstance(int argc, v8::Local<v8::Value> argv[]) const;
    void SetName(v8::Local<v8::String> name);
    v8::Local<v8::Value> GetName() const;
    v8::Local<v8::Value> Call(v8::Local<v8::Value> recv, int argc, v8::Local<v8::Value> argv[]);
    jobject Call(jobject recv, int argc, jobject argv[]);
    v8::ScriptOrigin GetScriptOrigin() const;
    int GetScriptLineNumber() const;
    int GetScriptColumnNumber() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    jobject Call(jobject recv, jobjectArray arguments);
    jobjectArray CreateJavaObjectArray(int argc, v8::Local<v8::Value> argv[]) const;
};

#endif /* GRAAL_FUNCTION_H_ */

