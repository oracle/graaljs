/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_OBJECT_H_
#define GRAAL_OBJECT_H_

#include "graal_value.h"

class GraalObject : public GraalValue {
public:
    GraalObject(GraalIsolate* isolate, jobject java_object);
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
    bool SetAccessor(
            v8::Local<v8::String> name,
            void (*getter)(v8::Local<v8::String>, v8::PropertyCallbackInfo<v8::Value> const&),
            void (*setter)(v8::Local<v8::String>, v8::Local<v8::Value>, v8::PropertyCallbackInfo<void> const&),
            v8::Local<v8::Value> data,
            v8::AccessControl settings,
            v8::PropertyAttribute attributes);
    int InternalFieldCount();
    void SetInternalField(int index, v8::Local<v8::Value> value);
    void SetAlignedPointerInInternalField(int index, void* value);
    v8::Local<v8::Value> SlowGetInternalField(int index);
    void* SlowGetAlignedPointerFromInternalField(int index);
    v8::Local<v8::Object> Clone();
    v8::Local<v8::Value> GetPrototype();
    bool SetPrototype(v8::Local<v8::Value> prototype);
    v8::Local<v8::String> GetConstructorName();
    v8::Local<v8::Array> GetOwnPropertyNames();
    v8::Local<v8::Array> GetPropertyNames();
    v8::Local<v8::Context> CreationContext();
    v8::MaybeLocal<v8::Value> GetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::Maybe<bool> SetPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key, v8::Local<v8::Value> value);
    v8::Maybe<bool> HasPrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::Maybe<bool> DeletePrivate(v8::Local<v8::Context> context, v8::Local<v8::Private> key);
    v8::MaybeLocal<v8::Value> GetOwnPropertyDescriptor(v8::Local<v8::Context> context, v8::Local<v8::Name> key);
    v8::Maybe<bool> DefineProperty(v8::Local<v8::Context> context, v8::Local<v8::Name> key, v8::PropertyDescriptor& descriptor);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    int internal_field_count_cache_;
};

#endif /* GRAAL_OBJECT_H_ */
