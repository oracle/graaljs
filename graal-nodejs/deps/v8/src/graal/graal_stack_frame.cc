/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_stack_frame.h"
#include "graal_string.h"

GraalStackFrame::GraalStackFrame(GraalIsolate* isolate, jobject stack_frame) : GraalHandleContent(isolate, stack_frame) {
}

GraalHandleContent* GraalStackFrame::CopyImpl(jobject java_object_copy) {
    return new GraalStackFrame(Isolate(), java_object_copy);
}

int GraalStackFrame::GetLineNumber() const {
    JNI_CALL(jint, line_number, Isolate(), GraalAccessMethod::stack_frame_get_line_number, Int, GetJavaObject());
    return line_number;
}

int GraalStackFrame::GetColumn() const {
    JNI_CALL(jint, column, Isolate(), GraalAccessMethod::stack_frame_get_column, Int, GetJavaObject());
    return column;
}

v8::Local<v8::String> GraalStackFrame::GetScriptName() const {
    JNI_CALL(jobject, script_name, Isolate(), GraalAccessMethod::stack_frame_get_script_name, Object, GetJavaObject());
    GraalString* graal_script_name = new GraalString(Isolate(), (jstring) script_name);
    return reinterpret_cast<v8::String*> (graal_script_name);
}

v8::Local<v8::String> GraalStackFrame::GetFunctionName() const {
    JNI_CALL(jobject, function_name, Isolate(), GraalAccessMethod::stack_frame_get_function_name, Object, GetJavaObject());
    GraalString* graal_function_name = new GraalString(Isolate(), (jstring) function_name);
    return reinterpret_cast<v8::String*> (graal_function_name);
}
