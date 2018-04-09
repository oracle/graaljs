/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
