/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_NUMBER_H_
#define GRAAL_NUMBER_H_

#include "graal_primitive.h"

class GraalNumber : public GraalPrimitive {
public:
    GraalNumber(GraalIsolate* isolate, double value, jobject java_number);
    bool IsInt32() const;
    bool IsUint32() const;
    bool IsNumber() const;
    static v8::Local<v8::Number> New(v8::Isolate* isolate, double value);
    static v8::Local<v8::Integer> New(v8::Isolate* isolate, int value);
    static v8::Local<v8::Integer> NewFromUnsigned(v8::Isolate* isolate, uint32_t value);
    double Value() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    double value_;
    static GraalNumber* NewNotCached(GraalIsolate* isolate, int value);
    friend class GraalIsolate;
};

#endif /* GRAAL_NUMBER_H_ */

