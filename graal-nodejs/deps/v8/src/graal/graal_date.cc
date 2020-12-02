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

#include "graal_context.h"
#include "graal_date.h"
#include "graal_isolate.h"

#include "graal_date-inl.h"

GraalHandleContent* GraalDate::CopyImpl(jobject java_object_copy) {
    return new GraalDate(Isolate(), time_, java_object_copy);
}

v8::MaybeLocal<v8::Value> GraalDate::New(v8::Local<v8::Context> context, double time) {
    GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
    GraalIsolate* graal_isolate = graal_context->Isolate();
    jobject java_context = graal_context->GetJavaObject();
    JNI_CALL(jobject, java_date, graal_isolate, GraalAccessMethod::date_new, Object, java_context, (jdouble) time);
    GraalDate* graal_date = new GraalDate(graal_isolate, time, java_date);
    v8::Local<v8::Value> v8_date = reinterpret_cast<v8::Value*> (graal_date);
    return v8_date;
}

double GraalDate::ValueOf() const {
    return time_;
}

bool GraalDate::IsDate() const {
    return true;
}
