/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_script.h"
#include "graal_string.h"
#include "graal_unbound_script.h"
#include <stdlib.h>

GraalUnboundScript::GraalUnboundScript(GraalIsolate* isolate, jobject java_script) : GraalHandleContent(isolate, java_script) {
}

v8::Local<v8::UnboundScript> GraalUnboundScript::Compile(v8::Local<v8::String> source_code, v8::Local<v8::String> file_name) {
    GraalString* graal_source_code = reinterpret_cast<GraalString*> (*source_code);
    jobject java_source_code = graal_source_code->GetJavaObject();
    jobject java_file_name = file_name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*file_name)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source_code->Isolate();
    JNI_CALL(jobject, java_script, graal_isolate, GraalAccessMethod::unbound_script_compile, Object, java_source_code, java_file_name)
    if (java_script == NULL) {
        return v8::Local<v8::UnboundScript>();
    } else {
        GraalUnboundScript* graal_script = new GraalUnboundScript(graal_isolate, java_script);
        return reinterpret_cast<v8::UnboundScript*> (graal_script);
    }
}

GraalHandleContent* GraalUnboundScript::CopyImpl(jobject java_object_copy) {
    return new GraalUnboundScript(Isolate(), java_object_copy);
}

v8::Local<v8::Script> GraalUnboundScript::BindToCurrentContext() {
    jobject java_context = Isolate()->CurrentJavaContext();
    JNI_CALL(jobject, java_bound, Isolate(), GraalAccessMethod::unbound_script_bind_to_context, Object, java_context, GetJavaObject());
    if (java_bound == NULL) {
        // should not happen
        fprintf(stderr, "UnboundScript::BindToCurrentContext() failed!\n");
        Isolate()->GetJNIEnv()->ExceptionDescribe();
        abort();
    } else {
        GraalScript* graal_script = new GraalScript(Isolate(), java_bound);
        return reinterpret_cast<v8::Script*> (graal_script);
    }
}

int GraalUnboundScript::GetId() {
    JNI_CALL(jint, id, Isolate(), GraalAccessMethod::unbound_script_get_id, Int, GetJavaObject());
    return id;
}
