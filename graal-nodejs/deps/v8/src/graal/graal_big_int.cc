/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_big_int.h"
#include "graal_isolate.h"

#include "graal_big_int-inl.h"

GraalHandleContent* GraalBigInt::CopyImpl(jobject java_object_copy) {
    return new GraalBigInt(Isolate(), java_object_copy);
}

v8::Local<v8::BigInt> GraalBigInt::New(v8::Isolate* isolate, int64_t value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_big_int, graal_isolate, GraalAccessMethod::big_int_new, Object, (jlong) value);
    GraalBigInt* graal_big_int = new GraalBigInt(graal_isolate, java_big_int);
    v8::BigInt* v8_big_int = reinterpret_cast<v8::BigInt*> (graal_big_int);
    return v8::Local<v8::BigInt>::New(isolate, v8_big_int);
}

v8::Local<v8::BigInt> GraalBigInt::NewFromUnsigned(v8::Isolate* isolate, uint64_t value) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNI_CALL(jobject, java_big_int, graal_isolate, GraalAccessMethod::big_int_new_from_unsigned, Object, (jlong) value);
    GraalBigInt* graal_big_int = new GraalBigInt(graal_isolate, java_big_int);
    v8::BigInt* v8_big_int = reinterpret_cast<v8::BigInt*> (graal_big_int);
    return v8::Local<v8::BigInt>::New(isolate, v8_big_int);
}

v8::MaybeLocal<v8::BigInt> GraalBigInt::NewFromWords(v8::Local<v8::Context> context, int sign_bit, int word_count, const uint64_t* words) {
    v8::Isolate* isolate = context->GetIsolate();
    if (word_count < 0 || word_count > (1 << 30) / 64) {
        isolate->ThrowException(v8::Exception::RangeError(v8::String::NewFromUtf8(isolate, "Maximum BigInt size exceeded").ToLocalChecked()));
        return v8::MaybeLocal<v8::BigInt>();
    }

    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jlongArray java_words = env->NewLongArray(word_count);
    env->SetLongArrayRegion(java_words, 0, word_count, (jlong*) words);
    JNI_CALL(jobject, java_big_int, graal_isolate, GraalAccessMethod::big_int_new_from_words, Object, sign_bit, word_count, java_words);
    env->DeleteLocalRef(java_words);
    GraalBigInt* graal_big_int = new GraalBigInt(graal_isolate, java_big_int);
    v8::BigInt* v8_big_int = reinterpret_cast<v8::BigInt*> (graal_big_int);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::BigInt>::New(v8_isolate, v8_big_int);
}

bool GraalBigInt::IsBigInt() const {
    return true;
}

uint64_t GraalBigInt::Uint64Value(bool* lossless) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jlong, result, graal_isolate, GraalAccessMethod::big_int_uint64_value, Long, GetJavaObject());
    if (lossless) {
        graal_isolate->ResetSharedBuffer();
        *lossless = graal_isolate->ReadInt32FromSharedBuffer() != 0;
    }
    return result;
}

int64_t GraalBigInt::Int64Value(bool* lossless) const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jlong, result, graal_isolate, GraalAccessMethod::big_int_int64_value, Long, GetJavaObject());
    if (lossless) {
        graal_isolate->ResetSharedBuffer();
        *lossless = graal_isolate->ReadInt32FromSharedBuffer() != 0;
    }
    return result;
}

int GraalBigInt::WordCount() const {
    JNI_CALL(jint, result, Isolate(), GraalAccessMethod::big_int_word_count, Int, GetJavaObject());
    return result;
}

void GraalBigInt::ToWordsArray(int* sign_bit, int* word_count, uint64_t* words) const {
    GraalIsolate* graal_isolate = Isolate();
    int original_word_count = *word_count;
    JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::big_int_to_words_array, Object, GetJavaObject(), original_word_count);
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jlongArray java_array = (jlongArray) java_result;
    jlong* java_elements = env->GetLongArrayElements(java_array, NULL);
    *sign_bit = (int) java_elements[0];
    *word_count = (int) java_elements[1];
    for (int i = 0; i < std::min(*word_count, original_word_count); i++) {
        words[i] = java_elements[i + 2];
    }
    env->ReleaseLongArrayElements(java_array, java_elements, JNI_ABORT);
}
