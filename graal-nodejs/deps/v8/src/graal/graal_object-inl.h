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

#ifndef GRAAL_OBJECT_INL_H_
#define GRAAL_OBJECT_INL_H_

#include "graal_object.h"

#include "graal_value-inl.h"

inline GraalObject::GraalObject(GraalIsolate* isolate, jobject java_object) : GraalValue(isolate, java_object), internal_field_count_cache_(-1) {
}

inline GraalObject* GraalObject::Allocate(GraalIsolate* isolate, jobject java_object) {
    GraalObjectPool<GraalObject>* pool = isolate->GetGraalObjectPool();
    if (pool->IsEmpty()) {
        return new GraalObject(isolate, java_object);
    } else {
        GraalObject* the_object = pool->Pop();
        the_object->ReInitialize(java_object);
        return the_object;
    }
}

inline GraalObject* GraalObject::Allocate(GraalIsolate* isolate, jobject java_object, void* placement) {
    return new (placement) GraalObject(isolate, java_object);
}

inline void GraalObject::ReInitialize(jobject java_object) {
    internal_field_count_cache_ = -1;
    GraalHandleContent::ReInitialize(java_object);
}

inline void GraalObject::Recycle() {
    GraalObjectPool<GraalObject>* pool = Isolate()->GetGraalObjectPool();
    if (!pool->IsFull()) {
        DeleteJavaRef();
        pool->Push(this);
    } else {
        delete this;
    }
}

inline v8::Local<v8::Value> GraalObject::HandleCallResult(jobject java_object) {
    GraalIsolate* graal_isolate = Isolate();
    if (java_object == NULL) {
        graal_isolate->HandleEmptyCallResult();
        return v8::Local<v8::Value>();
    } else {
        graal_isolate->ResetSharedBuffer();
        int32_t value_t = graal_isolate->ReadInt32FromSharedBuffer();
        GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_object, value_t, true);
        v8::Value* v8_value = reinterpret_cast<v8::Value*> (graal_value);
        v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
        return v8::Local<v8::Value>::New(v8_isolate, v8_value);
    }
}

#endif /* GRAAL_OBJECT_INL_H_ */
