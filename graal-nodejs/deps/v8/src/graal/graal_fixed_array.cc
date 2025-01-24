/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_fixed_array.h"
#include "graal_module_request.h"
#include "graal_value.h"

#include "graal_fixed_array-inl.h"
#include "graal_module_request-inl.h"

GraalHandleContent* GraalFixedArray::CopyImpl(jobject java_object_copy) {
   return new GraalFixedArray(Isolate(), java_object_copy);
}

int GraalFixedArray::Length() const {
    JNI_CALL(jint, length, Isolate(), GraalAccessMethod::fixed_array_length, Int, GetJavaObject());
    return length;
}

v8::Local<v8::Data> GraalFixedArray::Get(v8::Local<v8::Context> context, int i) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_element, graal_isolate, GraalAccessMethod::fixed_array_get, Object, GetJavaObject(), (jint) i);
    graal_isolate->ResetSharedBuffer();
    int32_t value_t = graal_isolate->ReadInt32FromSharedBuffer();
    GraalHandleContent* graal_element;
    if (value_t == ValueType::MODULE_REQUEST) {
        graal_element = GraalModuleRequest::Allocate(graal_isolate, java_element);
    } else {
        graal_element = GraalValue::FromJavaObject(graal_isolate, java_element, value_t, true);
    }
    v8::Data* v8_element = reinterpret_cast<v8::Data*> (graal_element);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Data>::New(v8_isolate, v8_element);
}
