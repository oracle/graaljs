/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_stack_frame.h"
#include "graal_stack_trace.h"

#include "graal_stack_frame-inl.h"
#include "graal_stack_trace-inl.h"

GraalHandleContent* GraalStackTrace::CopyImpl(jobject java_object_copy) {
    return new GraalStackTrace(Isolate(), java_object_copy);
}

int GraalStackTrace::GetFrameCount() const {
    return Isolate()->GetJNIEnv()->GetArrayLength((jobjectArray) GetJavaObject());
}

v8::Local<v8::StackFrame> GraalStackTrace::GetFrame(uint32_t index) const {
    jobject java_frame = Isolate()->GetJNIEnv()->GetObjectArrayElement((jobjectArray) GetJavaObject(), index);
    GraalStackFrame* graal_frame = GraalStackFrame::Allocate(Isolate(), java_frame);
    return reinterpret_cast<v8::StackFrame*> (graal_frame);
}

v8::Local<v8::StackTrace> GraalStackTrace::CurrentStackTrace(v8::Isolate* isolate, int frame_limit, v8::StackTrace::StackTraceOptions options) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_stack_trace, graal_isolate, GraalAccessMethod::stack_trace_current_stack_trace, Object, (jint) frame_limit);
    GraalStackTrace* graal_stack_trace = new GraalStackTrace(graal_isolate, java_stack_trace);
    return reinterpret_cast<v8::StackTrace*> (graal_stack_trace);
}
