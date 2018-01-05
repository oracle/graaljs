/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_MESSAGE_H_
#define GRAAL_MESSAGE_H_

#include "graal_handle_content.h"
#include "graal_isolate.h"

class GraalMessage : public GraalHandleContent {
public:
    GraalMessage(GraalIsolate* isolate, jobject exception);
    v8::Local<v8::StackTrace> GetStackTrace() const;
    v8::Local<v8::Value> GetScriptResourceName() const;
    v8::Local<v8::String> GetSourceLine() const;
    int GetStartColumn() const;
    int GetEndColumn() const;
    int GetLineNumber() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy);
};

#endif /* GRAAL_MESSAGE_H_ */
