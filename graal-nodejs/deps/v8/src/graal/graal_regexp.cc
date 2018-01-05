/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_context.h"
#include "graal_isolate.h"
#include "graal_regexp.h"
#include "graal_string.h"

GraalRegExp::GraalRegExp(GraalIsolate* isolate, jobject java_regexp) : GraalObject(isolate, java_regexp) {
}

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
    GraalString* graal_source = new GraalString(graal_isolate, (jstring) java_source);
    return reinterpret_cast<v8::String*> (graal_source);
}

v8::RegExp::Flags GraalRegExp::GetFlags() const {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_regexp = GetJavaObject();
    JNI_CALL(jint, java_flags, graal_isolate, GraalAccessMethod::regexp_get_flags, Int, java_regexp);
    static_cast<v8::RegExp::Flags> (java_flags);
}
