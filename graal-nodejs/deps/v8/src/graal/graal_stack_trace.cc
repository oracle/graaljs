/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_stack_trace.h"
#include "graal_stack_frame.h"

GraalStackTrace::GraalStackTrace(GraalIsolate* isolate, jobject stack_trace) : GraalHandleContent(isolate, stack_trace) {
}

GraalHandleContent* GraalStackTrace::CopyImpl(jobject java_object_copy) {
    return new GraalStackTrace(Isolate(), java_object_copy);
}

int GraalStackTrace::GetFrameCount() const {
    return Isolate()->GetJNIEnv()->GetArrayLength((jobjectArray) GetJavaObject());
}

v8::Local<v8::StackFrame> GraalStackTrace::GetFrame(uint32_t index) const {
    jobject java_frame = Isolate()->GetJNIEnv()->GetObjectArrayElement((jobjectArray) GetJavaObject(), index);
    GraalStackFrame* graal_frame = new GraalStackFrame(Isolate(), java_frame);
    return reinterpret_cast<v8::StackFrame*> (graal_frame);
}

v8::Local<v8::StackTrace> GraalStackTrace::CurrentStackTrace(v8::Isolate* isolate, int frame_limit, v8::StackTrace::StackTraceOptions options) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_stack_trace, graal_isolate, GraalAccessMethod::stack_trace_current_stack_trace, Object);
    GraalStackTrace* graal_stack_trace = new GraalStackTrace(graal_isolate, java_stack_trace);
    return reinterpret_cast<v8::StackTrace*> (graal_stack_trace);
}
