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

#ifndef GRAAL_STRING_H_
#define GRAAL_STRING_H_

#include "graal_name.h"
#include "graal_isolate.h"

class GraalString : public GraalName {
public:
    inline static GraalString* Allocate(GraalIsolate* isolate, jobject java_string);
    inline static GraalString* Allocate(GraalIsolate* isolate, jobject java_string, void* placement);
    inline void ReInitialize(jobject java_object);
    static v8::Local<v8::String> NewFromOneByte(v8::Isolate* isolate, unsigned char const* data, v8::NewStringType type, int length);
    static v8::Local<v8::String> NewFromUtf8(v8::Isolate* isolate, char const* str, v8::NewStringType type, int length);
    static v8::Local<v8::String> NewFromTwoByte(v8::Isolate* isolate, const uint16_t* data, v8::NewStringType type, int length);
    static v8::Local<v8::String> NewFromModifiedUtf8(v8::Isolate* isolate, char const* data);
    static v8::Local<v8::String> NewExternal(v8::Isolate* isolate, v8::String::ExternalOneByteStringResource* resource);
    static v8::Local<v8::String> NewExternal(v8::Isolate* isolate, v8::String::ExternalStringResource* resource);
    static void ExternalResourceDeallocator(const v8::WeakCallbackInfo<void>& data);
    static int Utf16Length(const unsigned char* input, int length);
    static void Utf16Write(const unsigned char* input, jchar* output, int length);
    static int Utf8Length(const jchar* input, int length);
    static int Utf8Write(const jchar* input, int input_length, char* output, int output_length, int* nchars_ref, int options);
    bool IsString() const override;
    bool IsName() const override;
    int Length() const;
    int Utf8Length() const;
    int WriteUtf8(char* buffer, int length, int* nchars_ref, int options) const;
    int WriteOneByte(uint8_t* buffer, int start, int length, int options) const;
    int Write(uint16_t* buffer, int start, int length, int options) const;
    bool ContainsOnlyOneByte() const;
    bool StringEquals(v8::Local<v8::String> str) const;

    /* Determines whether the given character is a continuation byte in UTF8 */
    static inline bool IsContinuationByte(const unsigned char c) {
        return (c & 0xC0) == 0x80;
    }
protected:
    inline void Recycle() override;
    inline GraalString(GraalIsolate* isolate, jobject java_string);
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_STRING_H_ */
