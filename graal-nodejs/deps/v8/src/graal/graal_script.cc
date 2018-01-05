/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_script.h"
#include "graal_string.h"
#include "graal_unbound_script.h"
#include "graal_value.h"

v8::Local<v8::Script> GraalScript::Compile(v8::Local<v8::String> source_code, v8::Local<v8::String> file_name) {
    GraalString* graal_source_code = reinterpret_cast<GraalString*> (*source_code);
    jobject java_source_code = graal_source_code->GetJavaObject();
    jobject java_file_name = file_name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*file_name)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source_code->Isolate();
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_script, graal_isolate, GraalAccessMethod::script_compile, Object, java_context, java_source_code, java_file_name)
    if (java_script == NULL) {
        return v8::Local<v8::Script>();
    } else {
        GraalScript* graal_script = new GraalScript(graal_isolate, java_script);
        return reinterpret_cast<v8::Script*> (graal_script);
    }
}

GraalScript::GraalScript(GraalIsolate* isolate, jobject java_script) : GraalHandleContent(isolate, java_script) {
}

GraalHandleContent* GraalScript::CopyImpl(jobject java_object_copy) {
    return new GraalScript(Isolate(), java_object_copy);
}

v8::Local<v8::Value> GraalScript::Run() {
    JNI_CALL(jobject, java_result, Isolate(), GraalAccessMethod::script_run, Object, GetJavaObject());
    GraalValue* graal_value = (java_result == NULL) ? nullptr : GraalValue::FromJavaObject(Isolate(), java_result);
    return reinterpret_cast<v8::Value*> (graal_value);
}

v8::Local<v8::UnboundScript> GraalScript::GetUnboundScript() {
    JNI_CALL(jobject, java_unbound, Isolate(), GraalAccessMethod::script_get_unbound_script, Object, GetJavaObject());
    GraalUnboundScript* graal_unbound = new GraalUnboundScript(Isolate(), java_unbound);
    return reinterpret_cast<v8::UnboundScript*> (graal_unbound);
}
