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

#include "graal_context.h"
#include "graal_isolate.h"
#include "graal_regexp.h"
#include "graal_string.h"

#include "graal_regexp-inl.h"
#include "graal_string-inl.h"

GraalHandleContent* GraalRegExp::CopyImpl(jobject java_object_copy) {
    return new GraalRegExp(Isolate(), java_object_copy);
}

bool GraalRegExp::IsRegExp() const {
    return true;
}

v8::Local<v8::RegExp> GraalRegExp::New(v8::Local<v8::Context> context, v8::Local<v8::String> pattern, v8::RegExp::Flags flags) {
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    jobject java_context = graal_context->GetJavaObject();
    GraalString* graal_pattern = reinterpret_cast<GraalString*> (*pattern);
    jobject java_pattern = graal_pattern->GetJavaObject();
    GraalIsolate* graal_isolate = graal_pattern->Isolate();
    jint java_flags = static_cast<jint> (flags);
    JNI_CALL(jobject, java_regexp, graal_isolate, GraalAccessMethod::regexp_new, Object, java_context, java_pattern, java_flags);
    GraalRegExp* graal_regexp = new GraalRegExp(graal_isolate, java_regexp);
    return reinterpret_cast<v8::RegExp*> (graal_regexp);
}

v8::Local<v8::String> GraalRegExp::GetSource() const {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_regexp = GetJavaObject();
    JNI_CALL(jobject, java_source, graal_isolate, GraalAccessMethod::regexp_get_source, Object, java_regexp);
    GraalString* graal_source = GraalString::Allocate(graal_isolate, java_source);
    return reinterpret_cast<v8::String*> (graal_source);
}

v8::RegExp::Flags GraalRegExp::GetFlags() const {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_regexp = GetJavaObject();
    JNI_CALL(jint, java_flags, graal_isolate, GraalAccessMethod::regexp_get_flags, Int, java_regexp);
    return static_cast<v8::RegExp::Flags> (java_flags);
}
