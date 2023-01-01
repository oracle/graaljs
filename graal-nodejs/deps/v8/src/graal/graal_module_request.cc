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
#include "graal_string.h"

#include "graal_fixed_array-inl.h"
#include "graal_module_request-inl.h"
#include "graal_string-inl.h"

GraalHandleContent* GraalModuleRequest::CopyImpl(jobject java_object_copy) {
   return new GraalModuleRequest(Isolate(), java_object_copy);
}

v8::Local<v8::String> GraalModuleRequest::GetSpecifier() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_specifier, graal_isolate, GraalAccessMethod::module_request_get_specifier, Object, GetJavaObject());
    GraalString* graal_specifier = GraalString::Allocate(graal_isolate, java_specifier);
    return reinterpret_cast<v8::String*> (graal_specifier);
}

v8::Local<v8::FixedArray> GraalModuleRequest::GetImportAssertions() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_assertions, graal_isolate, GraalAccessMethod::module_request_get_import_assertions, Object, GetJavaObject());
    GraalFixedArray* graal_assertions = GraalFixedArray::Allocate(graal_isolate, java_assertions);
    return reinterpret_cast<v8::FixedArray*> (graal_assertions);
}
