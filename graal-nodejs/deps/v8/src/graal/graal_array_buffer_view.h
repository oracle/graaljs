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

#ifndef GRAAL_ARRAY_BUFFER_VIEW_H_
#define GRAAL_ARRAY_BUFFER_VIEW_H_

#include "graal_object.h"

class GraalIsolate;

class GraalArrayBufferView : public GraalObject {
public:
    inline static GraalArrayBufferView* Allocate(GraalIsolate* isolate, jobject java_array_buffer_view, int type);
    inline static GraalArrayBufferView* Allocate(GraalIsolate* isolate, jobject java_array_buffer_view, int type, void* placement);
    inline static GraalArrayBufferView* Allocate(GraalIsolate* isolate, jobject java_array_buffer_view, int type, int byte_length, int byte_offset);
    inline static GraalArrayBufferView* Allocate(GraalIsolate* isolate, jobject java_array_buffer_view, int type, int byte_length, int byte_offset, void* placement);
    v8::Local<v8::ArrayBuffer> Buffer();
    bool IsDirect() const;
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
    bool IsBigInt64Array() const override;
    bool IsBigUint64Array() const override;
    bool IsDataView() const override;
    size_t ByteLength();
    size_t ByteOffset();
    static const int kUnknownArray = 0;
    static const int kInteropUint8Array = 1;
    static const int kInteropUint8ClampedArray = 2;
    static const int kInteropInt8Array = 3;
    static const int kInteropUint16Array = 4;
    static const int kInteropInt16Array = 5;
    static const int kInteropUint32Array = 6;
    static const int kInteropInt32Array = 7;
    static const int kInteropFloat32Array = 8;
    static const int kInteropFloat64Array = 9;
    static const int kInteropBigInt64Array = 10;
    static const int kInteropBigUint64Array = 11;
    static const int kDataView = 12;
    static const int kDirectUint8Array = 13;
    static const int kDirectUint8ClampedArray = 14;
    static const int kDirectInt8Array = 15;
    static const int kDirectUint16Array = 16;
    static const int kDirectInt16Array = 17;
    static const int kDirectUint32Array = 18;
    static const int kDirectInt32Array = 19;
    static const int kDirectFloat32Array = 20;
    static const int kDirectFloat64Array = 21;
    static const int kDirectBigInt64Array = 22;
    static const int kDirectBigUint64Array = 23;
protected:
    inline GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type);
    inline GraalArrayBufferView(GraalIsolate* isolate, jobject java_array_buffer_view, int type, int byte_length, int byte_offset);
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    int type_;
    int byte_length_;
    int byte_offset_;
    inline void Recycle() override {
        delete this;
    }
};

#endif /* GRAAL_ARRAY_BUFFER_VIEW_H_ */
