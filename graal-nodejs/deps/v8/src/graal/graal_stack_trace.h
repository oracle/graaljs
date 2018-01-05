/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_STACK_TRACE_H_
#define GRAAL_STACK_TRACE_H_

#include "graal_handle_content.h"
#include "graal_isolate.h"

class GraalStackTrace : public GraalHandleContent {
public:
    GraalStackTrace(GraalIsolate* isolate, jobject stack_trace);
    int GetFrameCount() const;
    v8::Local<v8::StackFrame> GetFrame(uint32_t index) const;
    static v8::Local<v8::StackTrace> CurrentStackTrace(v8::Isolate* isolate, int frame_limit, v8::StackTrace::StackTraceOptions options);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_STACK_TRACE_H_ */
