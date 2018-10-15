/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_isolate.h"
#include "graal_string.h"
#include <string.h>
#include <limits.h>

GraalString::GraalString(GraalIsolate* isolate, jstring java_string) : GraalName(isolate, java_string) {
}

GraalHandleContent* GraalString::CopyImpl(jobject java_object_copy) {
    return new GraalString(Isolate(), (jstring) java_object_copy);
}

v8::Local<v8::String> GraalString::NewFromOneByte(v8::Isolate* isolate, unsigned char const* data, v8::String::NewStringType type, int length) {
    // JVM does not support this kind of encoding directly => we check whether
    // the given buffer can be reused as if it was in null-terminated modified
    // utf8 encoding, we transform the data into utf16 encoding otherwise
    bool utf8 = false;
    if (length == -1) {
        utf8 = true;
        const unsigned char* current = data;
        while (*current) {
            if (*current & 0x80) {
                // characters > 127 have a different encoding in utf8
                // => we cannot look at this buffer as if it was utf8 encoded
                utf8 = false;
            }
            current++;
        }
        length = current - data;
    } // else no utf8 encoding because we have no way to ensure its null-termination
    if (length > v8::String::kMaxLength) {
        return v8::Local<v8::String>();
    }
    if (utf8) {
        // one-byte and utf8 encodings of the given string are the same
        return NewFromModifiedUtf8(isolate, (const char*) data);
    } else {
        uint16_t* java_data = new uint16_t[length];
        for (int i = 0; i < length; i++) {
            java_data[i] = data[i];
        }
        v8::Local<v8::String> result = NewFromTwoByte(isolate, java_data, type, length);
        delete[] java_data;
        return result;
    }
}

int GraalString::Utf16Length(const unsigned char* input, int length) {
    int result = 0;
    int position = 0;
    while (position < length) {
        const unsigned char c = input[position++];
        if (c & 0x80) { // else 0xxxxxxx
            if (c & 0x40) { // else continuation byte on the first position => invalid
                if ((c & 0x20) == 0) { // 110xxxxx
                    // one continuation byte should follow
                    if (position != length) {
                        const unsigned char c2 = input[position];
                        if (IsContinuationByte(c2) && (c & 0x1E) != 0) {
                            position++;
                        } // else invalid
                    }
                } else {
                    if ((c & 0x10) == 0) { // 1110xxxx
                        // two continuation bytes should follow
                        if (position != length) {
                            const unsigned char c2 = input[position];
                            if (IsContinuationByte(c2)) {
                                position++;
                                if (position != length) {
                                    const unsigned char c3 = input[position];
                                    if (IsContinuationByte(c3)) {
                                        // if (!overlong encoding && !code point for a surrogate) {
                                        if (((c & 0x0F) | (c2 & 0x20)) != 0 && ((c & 0x0F) != 0x0D || (c2 & 0x20) == 0)) {
                                            position++;
                                        } else {
                                            position--;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if ((c & 0x08) == 0) { // 11110xxx
                            // three continuation bytes should follow
                            if (position != length) {
                                const unsigned char c2 = input[position];
                                if (IsContinuationByte(c2)) {
                                    position++;
                                    if (position != length) {
                                        const unsigned char c3 = input[position];
                                        if (IsContinuationByte(c3)) {
                                            position++;
                                            if (position != length) {
                                                const unsigned char c4 = input[position];
                                                if (IsContinuationByte(c4)) {
                                                    position++;
                                                    if (((c & 7) | (c2 & 0x30)) != 0) {
                                                        // corresponds to a pair of UTF16 surrogates
                                                        result++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        result++;
    }
    return result;
}

void GraalString::Utf16Write(const unsigned char* input, jchar* output, int length) {
    int position = 0;
    while (position < length) {
        const unsigned char c = input[position++];
        if ((c & 0x80) == 0) { // 0xxxxxxx
            *output++ = c;
        } else {
            bool invalid = true;
            if ((c & 0x40) != 0) { // else 10xxxxxx i.e. continuation byte on the first position => invalid
                if ((c & 0x20) == 0) { // 110xxxxx
                    // one continuation byte should follow
                    if (position != length) {
                        const unsigned char c2 = input[position];
                        if (IsContinuationByte(c2) && (c & 0x1E) != 0) {
                            *output++ = ((c & 0x1F) << 6) | (c2 & 0x3F);
                            position++;
                            invalid = false;
                        }
                    }
                } else {
                    if ((c & 0x10) == 0) { // 1110xxxx
                        // two continuation bytes should follow
                        if (position != length) {
                            const unsigned char c2 = input[position];
                            if (IsContinuationByte(c2)) {
                                position++;
                                if (position != length) {
                                    const unsigned char c3 = input[position];
                                    if (IsContinuationByte(c3)) {
                                        // if (!overlong encoding && !code point for a surrogate) {
                                        if (((c & 0x0F) | (c2 & 0x20)) != 0 && ((c & 0x0F) != 0x0D || (c2 & 0x20) == 0)) {
                                            *output++ = ((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | (c3 & 0x3F);
                                            invalid = false;
                                            position++;
                                        } else {
                                            position--;
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        if ((c & 0x08) == 0) { // 11110xxx
                            // three continuation bytes should follow
                            if (position != length) {
                                const unsigned char c2 = input[position];
                                if (IsContinuationByte(c2)) {
                                    position++;
                                    if (position != length) {
                                        const unsigned char c3 = input[position];
                                        if (IsContinuationByte(c3)) {
                                            position++;
                                            if (position != length) {
                                                const unsigned char c4 = input[position];
                                                if (IsContinuationByte(c4)) {
                                                    position++;
                                                    if (((c & 7) | (c2 & 0x30)) != 0) {
                                                        // corresponds to a pair of UTF16 surrogates that are encoded
                                                        // using 4 bytes in UTF8 encoding
                                                        int code_point = ((c & 0x07) << 18) + ((c2 & 0x3F) << 12) + ((c3 & 0x3F) << 6) + (c4 & 0x3F);
                                                        *output++ = ((code_point - 0x10000) >> 10) | 0xD800;
                                                        *output++ = (code_point & 0x3FF) | 0xDC00;
                                                        invalid = false;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (invalid) {
                // replacement character
                *output++ = 0xFFFD;
            }
        }
    }
}

v8::Local<v8::String> GraalString::NewFromUtf8(v8::Isolate* isolate, char const* data, v8::String::NewStringType type, int length) {
    if (length == -1) {
        // determine the length of the input (without the null-termination)
        const char* current = data;
        while (*current++);
        length = current - data - 1;
    }
    if (length > v8::String::kMaxLength) {
        return v8::Local<v8::String>();
    }

    int new_length = Utf16Length((const unsigned char*) data, length);
    jchar* new_data = new jchar[new_length];
    Utf16Write((const unsigned char*) data, new_data, length);
    v8::Local<v8::String> result = NewFromTwoByte(isolate, new_data, type, new_length);
    delete[] new_data;
    return result;
}

v8::Local<v8::String> GraalString::NewFromModifiedUtf8(v8::Isolate* isolate, const char* data) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jstring java_string = graal_isolate->GetJNIEnv()->NewStringUTF(data);
    GraalString* graal_string = new GraalString(graal_isolate, java_string);
    return reinterpret_cast<v8::String*> (graal_string);
}

v8::Local<v8::String> GraalString::NewFromTwoByte(v8::Isolate* isolate, const uint16_t* data, v8::String::NewStringType type, int length) {
    if (length == -1) {
        // determine the length of the input (without the null-termination)
        const uint16_t* current = data;
        while (*current++);
        length = current - data - 1;
    }
    if (length > v8::String::kMaxLength) {
        return v8::Local<v8::String>();
    }
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    jstring java_string = graal_isolate->GetJNIEnv()->NewString(data, length);
    GraalString* graal_string = new GraalString(graal_isolate, java_string);
    return reinterpret_cast<v8::String*> (graal_string);
}

bool GraalString::IsString() const {
    return true;
}

bool GraalString::IsName() const {
    return true;
}

int GraalString::Length() const {
    return Isolate()->GetJNIEnv()->GetStringLength((jstring) GetJavaObject());
}

int GraalString::Utf8Length(const jchar* input, int input_length) {
    int length = 0;
    bool seen_surrogate = false;
    for (int i = 0; i < input_length; i++) {
        jchar c = input[i];
        if (seen_surrogate) {
            seen_surrogate = false;
            if ((c & 0xFC00) == 0xDC00) { // low surrogate
                length += 4;
                continue;
            } else {
                length += 3; // replacement character for the unmatched surrogate
            }
        }
        if (c < 1 << 7) {
            length++;
        } else if (c < 1 << 11) {
            length += 2;
        } else if ((c & 0xFC00) == 0xD800) { // high surrogate
            seen_surrogate = true;
        } else {
            length += 3;
        }
    }
    if (seen_surrogate) {
        length += 3;
    }
    return length;
}

int GraalString::Utf8Length() const {
    // we cannot use JNI function GetStringUTFLength() because it returns
    // the length of the modified utf8 encoding of the string which may differ
    // from the length of the utf8 encoding
    jstring java_string = (jstring) GetJavaObject();
    JNIEnv* env = Isolate()->GetJNIEnv();
    int utf16length = env->GetStringLength(java_string);
    const jchar* data = env->GetStringCritical(java_string, nullptr);
    int utf8length = Utf8Length(data, utf16length);
    env->ReleaseStringCritical(java_string, data);
    return utf8length;
}

int GraalString::Utf8Write(const jchar* input, int input_length, char* buffer, int buffer_length, int* nchars_ref, int options) {
    int chars = 0;
    int available = buffer_length;
    const jchar* current = input;
    for (int i = 0; i < input_length && available > 0; i++) {
        jchar c = *current++;
        if (c < 1 << 7) {
            // 0xxxxxxx
            *buffer++ = c;
            available--;
        } else if (c < 1 << 11) {
            // 110xxxxx 10xxxxxx
            if (available < 2) {
                break;
            }
            *buffer++ = 0xC0 | (0x1F & (c >> 6));
            *buffer++ = 0x80 | (0x3F & c);
            available -= 2;
        } else {
            if ((c & 0xF800) == 0xD800) { // surrogate
                if ((c & 0xFC00) == 0xD800) { // high surrogate
                    int high_surrogate = c;
                    if (i != input_length - 1) {
                        int low_surrogate = *current;
                        if ((low_surrogate & 0xFC00) == 0xDC00) {
                            unsigned int code_point = 0x010000 + ((high_surrogate & 0x03FF) << 10) + (low_surrogate & 0x03FF);
                            if (available < 4) {
                                break;
                            }
                            // 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                            *buffer++ = 0xF0 | (0x07 & (code_point >> 18));
                            *buffer++ = 0x80 | (0x3F & (code_point >> 12));
                            *buffer++ = 0x80 | (0x3F & (code_point >> 6));
                            *buffer++ = 0x80 | (0x3F & code_point);
                            available -= 4;
                            current++;
                            i++;
                            continue;
                        }
                    } // else unmatched surrogate as the last character
                }
                c = 0xFFFD; // replacement character
            }
            if (available < 3) {
                break;
            }
            // 1110xxxx 10xxxxxx 10xxxxxx
            *buffer++ = 0xE0 | (0xF & (c >> 12));
            *buffer++ = 0x80 | (0x3F & (c >> 6));
            *buffer++ = 0x80 | (0x3F & c);
            available -= 3;
        }
        chars++;
    }
    if ((options & v8::String::NO_NULL_TERMINATION) == 0 && available != 0) {
        *buffer = 0;
        available--;
    }
    if (nchars_ref != nullptr) {
        *nchars_ref = chars;
    }
    return buffer_length - available;
}

int GraalString::WriteUtf8(char* buffer, int length, int* nchars_ref, int options) const {
    jstring java_string = (jstring) GetJavaObject();
    JNIEnv* env = Isolate()->GetJNIEnv();
    int str_length = env->GetStringLength(java_string);
    const jchar* str = env->GetStringCritical(java_string, NULL);
    if (length == -1) {
        length = INT_MAX;
    }
    int bytesWritten = Utf8Write(str, str_length, buffer, length, nchars_ref, options);
    env->ReleaseStringCritical(java_string, str);
    return bytesWritten;
}

int GraalString::WriteOneByte(uint8_t* buffer, int start, int length, int options) const {
    JNIEnv* env = Isolate()->GetJNIEnv();
    jstring java_string = (jstring) GetJavaObject();
    int str_length = env->GetStringLength(java_string);
    if ((length == -1) || (start + length > str_length)) {
        length = str_length - start;
    }
    int end = start + length;
    const jchar* data = env->GetStringCritical(java_string, NULL);
    const jchar* current = data;
    for (int i = start; i < end; i++) {
        *buffer++ = *current & 0xFF;
        current++;
    }
    env->ReleaseStringCritical(java_string, data);
    if ((options & v8::String::NO_NULL_TERMINATION) == 0) {
        *buffer = 0;
    }
    return length;
}

int GraalString::Write(uint16_t* buffer, int start, int length, int options) const {
    jstring java_string = (jstring) GetJavaObject();
    JNIEnv* env = Isolate()->GetJNIEnv();
    int str_length = env->GetStringLength(java_string);
    if ((length == -1) || (start + length > str_length)) {
        length = str_length - start;
    }
    const jchar *native_string = env->GetStringChars(java_string, NULL);
    memcpy(buffer, native_string + start, sizeof (jchar) * length);
    env->ReleaseStringChars(java_string, native_string);
    if ((options & v8::String::NO_NULL_TERMINATION) == 0) {
        *(buffer + length) = 0;
    }
    return length;
}

namespace v8 {
    namespace internal {

        class Heap {
            static void DisposeExternalString(v8::String::ExternalStringResourceBase* external_string) {
                external_string->Dispose();
            }
            friend GraalString;
        };
    }
}

void GraalString::ExternalResourceDeallocator(const v8::WeakCallbackInfo<void>& data) {
    v8::internal::Heap::DisposeExternalString(reinterpret_cast<v8::String::ExternalStringResourceBase*> (data.GetParameter()));
}

v8::Local<v8::String> GraalString::NewExternal(v8::Isolate* isolate, v8::String::ExternalOneByteStringResource* resource) {
    v8::Local<v8::String> result = GraalString::NewFromOneByte(isolate, (const unsigned char*) resource->data(), v8::String::kNormalString, resource->length());
    if (!result.IsEmpty()) {
        GraalString* graal_string = reinterpret_cast<GraalString*> (*result);
        JNI_CALL_VOID(isolate, GraalAccessMethod::string_external_resource_callback, graal_string->GetJavaObject(), (long) resource, (jlong) & ExternalResourceDeallocator);
    }
    return result;
}

v8::Local<v8::String> GraalString::NewExternal(v8::Isolate* isolate, v8::String::ExternalStringResource* resource) {
    v8::Local<v8::String> result = GraalString::NewFromTwoByte(isolate, resource->data(), v8::String::kNormalString, resource->length());
    if (!result.IsEmpty()) {
        GraalString* graal_string = reinterpret_cast<GraalString*> (*result);
        JNI_CALL_VOID(isolate, GraalAccessMethod::string_external_resource_callback, graal_string->GetJavaObject(), (long) resource, (jlong) & ExternalResourceDeallocator);
    }
    return result;
}
