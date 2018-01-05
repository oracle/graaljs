/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_CONTEXT_H_
#define GRAAL_CONTEXT_H_

#include "graal_handle_content.h"
#include "graal_isolate.h"
#include "include/v8.h"
#include <vector>

class GraalContext : public GraalHandleContent {
public:
    GraalContext(GraalIsolate* isolate, jobject java_context);
    v8::Local<v8::Object> Global();
    void SetAlignedPointerInEmbedderData(int index, void* value);
    void* SlowGetAlignedPointerFromEmbedderData(int index);
    void SetEmbedderData(int index, v8::Local<v8::Value> value);
    v8::Local<v8::Value> SlowGetEmbedderData(int index);
    void SetSecurityToken(v8::Local<v8::Value> token);
    void UseDefaultSecurityToken();
    v8::Local<v8::Value> GetSecurityToken();

    inline v8::Isolate* GetIsolate() {
        return reinterpret_cast<v8::Isolate*> (Isolate());
    }

    inline void Enter() {
        Isolate()->ContextEnter(reinterpret_cast<v8::Context*> (this));
    }

    inline void Exit() {
        Isolate()->ContextExit(reinterpret_cast<v8::Context*> (this));
    }
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_CONTEXT_H_ */
