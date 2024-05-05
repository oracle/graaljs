/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include "graal_message.h"
#include "graal_stack_trace.h"
#include "graal_string.h"

#include "graal_message-inl.h"
#include "graal_stack_trace-inl.h"
#include "graal_string-inl.h"

GraalHandleContent* GraalMessage::CopyImpl(jobject java_object_copy) {
    return new GraalMessage(Isolate(), java_object_copy);
}

v8::Local<v8::StackTrace> GraalMessage::GetStackTrace() const {
    JNI_CALL(jobject, java_stack_trace, Isolate(), GraalAccessMethod::message_get_stack_trace, Object, GetJavaObject());
    GraalStackTrace* graal_stack_trace = GraalStackTrace::Allocate(Isolate(), java_stack_trace);
    return reinterpret_cast<v8::StackTrace*> (graal_stack_trace);
}

v8::Local<v8::Value> GraalMessage::GetScriptResourceName() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_resource_name, graal_isolate, GraalAccessMethod::message_get_script_resource_name, Object, GetJavaObject());
    if (java_resource_name == nullptr) {
        return v8::String::NewFromUtf8(reinterpret_cast<v8::Isolate*> (graal_isolate), "unknown").ToLocalChecked();
    } else {
        GraalString* graal_resource_name = GraalString::Allocate(graal_isolate, java_resource_name);
        return reinterpret_cast<v8::Value*> (graal_resource_name);
    }
}

v8::Local<v8::String> GraalMessage::GetSourceLine() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_source_line, graal_isolate, GraalAccessMethod::message_get_source_line, Object, GetJavaObject());
    if (java_source_line == nullptr) {
        return v8::String::NewFromUtf8(reinterpret_cast<v8::Isolate*> (graal_isolate), "unknown").ToLocalChecked();
    } else {
        GraalString* graal_source_line = GraalString::Allocate(graal_isolate, java_source_line);
        return reinterpret_cast<v8::String*> (graal_source_line);
    }
}

int GraalMessage::GetStartColumn() const {
    JNI_CALL(jint, start_column, Isolate(), GraalAccessMethod::message_get_start_column, Int, GetJavaObject());
    return start_column;
}

int GraalMessage::GetEndColumn() const {
    return GetStartColumn() + 1;
}

v8::Maybe<int> GraalMessage::GetLineNumber() const {
    JNI_CALL(jint, line_number, Isolate(), GraalAccessMethod::message_get_line_number, Int, GetJavaObject());
    return v8::Just((int) line_number);
}

v8::Local<v8::String> GraalMessage::Get() const {
    JNI_CALL(jobject, java_message, Isolate(), GraalAccessMethod::message_get, Object, GetJavaObject());
    GraalString* graal_message = GraalString::Allocate(Isolate(), java_message);
    return reinterpret_cast<v8::String*> (graal_message);
}

int GraalMessage::GetStartPosition() const {
    JNI_CALL(jint, start_position, Isolate(), GraalAccessMethod::message_get_start_position, Int, GetJavaObject());
    return start_position;
}

int GraalMessage::GetEndPosition() const {
    JNI_CALL(jint, end_position, Isolate(), GraalAccessMethod::message_get_end_position, Int, GetJavaObject());
    return end_position;
}
