/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_ARRAY_BUFFER_H_
#define GRAAL_ARRAY_BUFFER_H_

#include "graal_object.h"

class GraalIsolate;

class GraalArrayBuffer : public GraalObject {
public:
    GraalArrayBuffer(GraalIsolate* isolate, jobject java_array_buffer);
    size_t ByteLength() const;
    bool IsArrayBuffer() const;
    static v8::Local<v8::ArrayBuffer> New(v8::Isolate* isolate, size_t byte_length);
    static v8::Local<v8::ArrayBuffer> New(v8::Isolate* isolate, void* data, size_t byte_length, v8::ArrayBufferCreationMode mode);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_ARRAY_BUFFER_H_ */

