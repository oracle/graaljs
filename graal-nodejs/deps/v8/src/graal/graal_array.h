/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_ARRAY_H_
#define GRAAL_ARRAY_H_

#include "graal_object.h"

class GraalIsolate;

class GraalArray : public GraalObject {
public:
    GraalArray(GraalIsolate* isolate, jobject java_array);
    bool IsArray() const;
    static v8::Local<v8::Array> New(v8::Isolate* isolate, int length);
    uint32_t Length() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_ARRAY_H_ */

