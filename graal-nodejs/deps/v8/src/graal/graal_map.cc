/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "graal_map.h"

#include "graal_map-inl.h"

GraalHandleContent* GraalMap::CopyImpl(jobject java_object_copy) {
    return new GraalMap(Isolate(), java_object_copy);
}

bool GraalMap::IsMap() const {
    return true;
}

v8::Local<v8::Map> GraalMap::New(v8::Isolate* isolate) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jobject java_context = graal_isolate->CurrentJavaContext();
    JNI_CALL(jobject, java_map, graal_isolate, GraalAccessMethod::map_new, Object, java_context);
    GraalMap* graal_map = new GraalMap(graal_isolate, java_map);
    v8::Map* v8_map = reinterpret_cast<v8::Map*> (graal_map);
    return v8::Local<v8::Map>::New(isolate, v8_map);
}

v8::MaybeLocal<v8::Map> GraalMap::Set(v8::Local<v8::Context> context, v8::Local<v8::Value> key, v8::Local<v8::Value> value) {
    GraalValue* graal_map = reinterpret_cast<GraalValue*> (this);
    GraalValue* graal_key = reinterpret_cast<GraalValue*> (*key);
    GraalValue* graal_value = reinterpret_cast<GraalValue*> (*value);
    GraalIsolate* graal_isolate = graal_map->Isolate();
    jobject java_map = graal_map->GetJavaObject();
    jobject java_key = graal_key->GetJavaObject();
    jobject java_value = graal_value->GetJavaObject();
    JNI_CALL_VOID(graal_isolate, GraalAccessMethod::map_set, java_map, java_key, java_value);
    v8::Map* v8_map = reinterpret_cast<v8::Map*> (this);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Map>::New(v8_isolate, v8_map);
}
