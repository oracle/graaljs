/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_message.h"
#include "graal_stack_trace.h"
#include "graal_string.h"

GraalMessage::GraalMessage(GraalIsolate* isolate, jobject exception) : GraalHandleContent(isolate, exception) {
}

GraalHandleContent* GraalMessage::CopyImpl(jobject java_object_copy) {
    return new GraalMessage(Isolate(), java_object_copy);
}

v8::Local<v8::StackTrace> GraalMessage::GetStackTrace() const {
    JNI_CALL(jobject, java_stack_trace, Isolate(), GraalAccessMethod::message_get_stack_trace, Object, GetJavaObject());
    GraalStackTrace* graal_stack_trace = new GraalStackTrace(Isolate(), java_stack_trace);
    return reinterpret_cast<v8::StackTrace*> (graal_stack_trace);
}

v8::Local<v8::Value> GraalMessage::GetScriptResourceName() const {
    JNI_CALL(jobject, java_resource_name, Isolate(), GraalAccessMethod::message_get_script_resource_name, Object, GetJavaObject());
    GraalString* graal_resource_name = new GraalString(Isolate(), (jstring) java_resource_name);
    return reinterpret_cast<v8::Value*> (graal_resource_name);
}

v8::Local<v8::String> GraalMessage::GetSourceLine() const {
    JNI_CALL(jobject, java_source_line, Isolate(), GraalAccessMethod::message_get_source_line, Object, GetJavaObject());
    GraalString* graal_source_line = new GraalString(Isolate(), (jstring) java_source_line);
    return reinterpret_cast<v8::String*> (graal_source_line);
}

int GraalMessage::GetStartColumn() const {
    JNI_CALL(jint, start_column, Isolate(), GraalAccessMethod::message_get_start_column, Int, GetJavaObject());
    return start_column;
}

int GraalMessage::GetEndColumn() const {
    return GetStartColumn() + 1;
}

int GraalMessage::GetLineNumber() const {
    JNI_CALL(jint, line_number, Isolate(), GraalAccessMethod::message_get_line_number, Int, GetJavaObject());
    return line_number;
}
