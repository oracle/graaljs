/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_ARRAY_BUFFER_VIEW_H_
#define GRAAL_ARRAY_BUFFER_VIEW_H_

#include "graal_object.h"

class GraalIsolate;

class GraalArrayBufferView : public GraalObject {
public:
    GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type);
    GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type, int byte_length, int byte_offset);
    v8::Local<v8::ArrayBuffer> Buffer();
    bool IsArrayBufferView() const override;
    bool IsUint8Array() const override;
    bool IsUint8ClampedArray() const override;
    bool IsInt8Array() const override;
    bool IsUint16Array() const override;
    bool IsInt16Array() const override;
    bool IsUint32Array() const override;
    bool IsInt32Array() const override;
    bool IsFloat32Array() const override;
    bool IsFloat64Array() const override;
    bool IsDataView() const override;
    size_t ByteLength();
    size_t ByteOffset();
    static const int kUnknownArray = 0;
    static const int kUint8Array = 1;
    static const int kUint8ClampedArray = 2;
    static const int kInt8Array = 3;
    static const int kUint16Array = 4;
    static const int kInt16Array = 5;
    static const int kUint32Array = 6;
    static const int kInt32Array = 7;
    static const int kFloat32Array = 8;
    static const int kFloat64Array = 9;
    static const int kDataView = 10;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    int type_;
    int byte_length_;
    int byte_offset_;
};

#endif /* GRAAL_ARRAY_BUFFER_VIEW_H_ */
