/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_STRING_H_
#define GRAAL_STRING_H_

#include "graal_name.h"
#include "include/v8.h"

class GraalString : public GraalName {
public:
    GraalString(GraalIsolate* isolate, jstring java_string);
    static v8::Local<v8::String> NewFromOneByte(v8::Isolate* isolate, unsigned char const* data, v8::String::NewStringType type, int length);
    static v8::Local<v8::String> NewFromUtf8(v8::Isolate* isolate, char const* str, v8::String::NewStringType type, int length);
    static v8::Local<v8::String> NewFromTwoByte(v8::Isolate* isolate, const uint16_t* data, v8::String::NewStringType type, int length);
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

    /* Determines whether the given character is a continuation byte in UTF8 */
    static inline bool IsContinuationByte(const unsigned char c) {
        return (c & 0xC0) == 0x80;
    }
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_STRING_H_ */

