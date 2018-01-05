/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_VALUE_H_
#define GRAAL_VALUE_H_

#include "graal_data.h"
#include "include/v8.h"

class GraalValue : public GraalData {
public:
    GraalValue(GraalIsolate* isolate, jobject java_object);
    static GraalValue* FromJavaObject(GraalIsolate* isolate, jobject java_object);
    static GraalValue* FromJavaObject(GraalIsolate* isolate, jobject java_object, bool create_new_local_ref);
    static GraalValue* FromJavaObject(GraalIsolate* isolate, jobject java_object, int type, bool use_shared_buffer);
    static GraalValue* FromJavaObject(GraalIsolate* isolate, jobject java_object, int type, bool use_shared_buffer, void* placement);
    virtual bool IsObject() const;
    virtual bool IsFunction() const;
    virtual bool IsExternal() const;
    virtual bool IsArray() const;
    virtual bool IsInt32() const;
    virtual bool IsUint32() const;
    virtual bool IsNumber() const;
    virtual bool IsBoolean() const;
    virtual bool IsArrayBuffer() const;
    virtual bool IsArrayBufferView() const;
    virtual bool IsDate() const;
    virtual bool IsRegExp() const;
    virtual bool IsUint8Array() const;
    virtual bool IsUint8ClampedArray() const;
    virtual bool IsInt8Array() const;
    virtual bool IsUint16Array() const;
    virtual bool IsInt16Array() const;
    virtual bool IsUint32Array() const;
    virtual bool IsInt32Array() const;
    virtual bool IsFloat32Array() const;
    virtual bool IsFloat64Array() const;
    virtual bool IsMap() const;
    virtual bool IsSet() const;
    virtual bool IsPromise() const;
    virtual bool IsProxy() const;
    virtual bool IsSymbol() const;
    virtual bool IsName() const;
    virtual bool IsNull() const;
    virtual bool IsUndefined() const;
    virtual bool IsTrue() const;
    virtual bool IsFalse() const;
    virtual bool IsDataView() const;
    bool IsNativeError() const;
    bool IsMapIterator() const;
    bool IsSetIterator() const;
    bool IsSharedArrayBuffer() const;
    int32_t Int32Value() const;
    uint32_t Uint32Value() const;
    int64_t IntegerValue() const;
    bool BooleanValue() const;
    double NumberValue() const;
    v8::Local<v8::Object> ToObject(v8::Isolate* isolate) const;
    v8::Local<v8::String> ToString(v8::Isolate* isolate) const;
    v8::Local<v8::Boolean> ToBoolean(v8::Isolate* isolate) const;
    v8::Local<v8::Integer> ToInteger(v8::Isolate* isolate) const;
    v8::Local<v8::Int32> ToInt32(v8::Isolate* isolate) const;
    v8::Local<v8::Uint32> ToUint32(v8::Isolate* isolate) const;
    v8::Local<v8::Number> ToNumber(v8::Isolate* isolate) const;
    v8::Local<v8::Uint32> ToArrayIndex() const;
    bool Equals(v8::Local<v8::Value> that) const;
    bool StrictEquals(v8::Local<v8::Value> that) const;
    bool InstanceOf(v8::Local<v8::Object> object);
    // Maximum size of GraalValue (excluding referenced objects) returned
    // by FromJavaObject - used to reserve enough memory when the value
    // is allocated on stack
    static const int MAX_SIZE;
};

#endif /* GRAAL_VALUE_H_ */
