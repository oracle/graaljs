/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_STACK_FRAME_H_
#define GRAAL_STACK_FRAME_H_

#include "graal_handle_content.h"
#include "graal_isolate.h"

class GraalStackFrame : public GraalHandleContent {
public:
    GraalStackFrame(GraalIsolate* isolate, jobject stack_frame);
    int GetLineNumber() const;
    int GetColumn() const;
    v8::Local<v8::String> GetScriptName() const;
    v8::Local<v8::String> GetFunctionName() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_STACK_FRAME_H_ */
