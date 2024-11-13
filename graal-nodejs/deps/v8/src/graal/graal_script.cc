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

#include "graal_data.h"
#include "graal_isolate.h"
#include "graal_script.h"
#include "graal_string.h"
#include "graal_unbound_script.h"
#include "graal_value.h"

#include "graal_script-inl.h"
#include "graal_unbound_script-inl.h"

v8::Local<v8::Script> GraalScript::Compile(v8::Local<v8::String> source_code, v8::ScriptOrigin* origin) {
    v8::Local<v8::String> file_name = origin == nullptr ? v8::Local<v8::String>() : origin->ResourceName().As<v8::String>();
    v8::Local<v8::Data> options = origin == nullptr ? v8::Local<v8::Data>() : origin->GetHostDefinedOptions();
    GraalString* graal_source_code = reinterpret_cast<GraalString*> (*source_code);
    jobject java_source_code = graal_source_code->GetJavaObject();
    jobject java_file_name = file_name.IsEmpty() ? NULL : reinterpret_cast<GraalString*> (*file_name)->GetJavaObject();
    jobject java_options = options.IsEmpty() ? NULL : reinterpret_cast<GraalData*> (*options)->GetJavaObject();
    GraalIsolate* graal_isolate = graal_source_code->Isolate();
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_script, graal_isolate, GraalAccessMethod::script_compile, Object, java_context, java_source_code, java_file_name, java_options)
    if (java_script == NULL) {
        return v8::Local<v8::Script>();
    } else {
        GraalScript* graal_script = GraalScript::Allocate(graal_isolate, java_script);
        return reinterpret_cast<v8::Script*> (graal_script);
    }
}

GraalHandleContent* GraalScript::CopyImpl(jobject java_object_copy) {
    return GraalScript::Allocate(Isolate(), java_object_copy);
}

v8::Local<v8::Value> GraalScript::Run() {
    JNI_CALL(jobject, java_result, Isolate(), GraalAccessMethod::script_run, Object, GetJavaObject());
    GraalValue* graal_value = (java_result == NULL) ? nullptr : GraalValue::FromJavaObject(Isolate(), java_result);
    return reinterpret_cast<v8::Value*> (graal_value);
}

v8::Local<v8::UnboundScript> GraalScript::GetUnboundScript() {
    JNI_CALL(jobject, java_unbound, Isolate(), GraalAccessMethod::script_get_unbound_script, Object, GetJavaObject());
    GraalUnboundScript* graal_unbound = GraalUnboundScript::Allocate(Isolate(), java_unbound);
    return reinterpret_cast<v8::UnboundScript*> (graal_unbound);
}
