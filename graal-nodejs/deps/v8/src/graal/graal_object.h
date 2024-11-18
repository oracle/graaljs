/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GRAAL_OBJECT_H_
#define GRAAL_OBJECT_H_

#include "graal_value.h"

class GraalObject : public GraalValue {
public:
    inline static GraalObject* Allocate(GraalIsolate* isolate, jobject java_object);
    inline static GraalObject* Allocate(GraalIsolate* isolate, jobject java_object, void* placement);
    inline void ReInitialize(jobject java_object);
    bool IsObject() const;
    static v8::Local<v8::Object> New(v8::Isolate* isolate);
    bool Set(v8::Local<v8::Value> key, v8::Local<v8::Value> value);
    bool Set(uint32_t index, v8::Local<v8::Value> value);
    bool ForceSet(v8::Local<v8::Value> key, v8::Local<v8::Value> value, v8::PropertyAttribute attribs);
    v8::Local<v8::Value> Get(v8::Local<v8::Value> key);
    v8::Local<v8::Value> Get(uint32_t index);
    v8::Local<v8::Value> GetRealNamedProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key);
    v8::Maybe<v8::PropertyAttribute> GetRealNamedPropertyAttributes(v8::Local<v8::Context> context, v8::Local<v8::Name> key);
    bool Has(v8::Local<v8::Value> key);
    bool HasOwnProperty(v8::Local<v8::Name> key);
    bool HasRealNamedProperty(v8::Local<v8::Name> key);
    bool Delete(v8::Local<v8::Value> key);
    bool Delete(uint32_t index);
    v8::Maybe<bool> SetAccessor(
            v8::Local<v8::Name> name,
            v8::AccessorNameGetterCallback getter,
            v8::AccessorNameSetterCallback setter,
            v8::MaybeLocal<v8::Value> data,
            v8::AccessControl settings,
            v8::PropertyAttribute attributes);
    int InternalFieldCount();
    void SetInternalField(int index, v8::Local<v8::Data> value);
    void SetAlignedPointerInInternalField(int index, void* value);
    v8::Local<v8::Data> SlowGetInternalField(int index);
    void* SlowGetAlignedPointerFromInternalField(int index);
    v8::Local<v8::Object> Clone();
    v8::Local<v8::Value> GetPrototype();
    bool SetPrototype(v8::Local<v8::Value> prototype);
    v8::Local<v8::String> GetConstructorName();
    v8::Local<v8::Array> GetOwnPropertyNames();
    v8::MaybeLocal<v8::Array> GetPropertyNames(v8::Local<v8::Context> context, v8::KeyCollectionMode mode, v8::PropertyFilter property_filter, v8::IndexFilter index_filter, v8::KeyConversionMode key_conversion);
    v8::Local<v8::Context> CreationContext();
    v8::MaybeLocal<v8::Value> GetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::Maybe<bool> SetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key, v8::Local<v8::Value> value);
    v8::Maybe<bool> HasPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::Maybe<bool> DeletePrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::MaybeLocal<v8::Value> GetOwnPropertyDescriptor(v8::Local<v8::Context> context, v8::Local<v8::Name> key);
    v8::Maybe<bool> CreateDataProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key, v8::Local<v8::Value> value);
    v8::Maybe<bool> CreateDataProperty(v8::Local<v8::Context> context, uint32_t index, v8::Local<v8::Value> value);
    v8::Maybe<bool> DefineProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key, v8::PropertyDescriptor& descriptor);
    v8::MaybeLocal<v8::Array> PreviewEntries(bool* is_key_value);
    v8::Maybe<bool> SetIntegrityLevel(v8::Local<v8::Context> context, v8::IntegrityLevel level);
    bool IsConstructor() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
    inline v8::Local<v8::Value> HandleCallResult(jobject java_object);
    inline void Recycle() override;
    inline GraalObject(GraalIsolate* isolate, jobject java_object);
private:
    int internal_field_count_cache_;
    void SetInternalFieldImpl(int index, GraalHandleContent* handle);
};

#endif /* GRAAL_OBJECT_H_ */
