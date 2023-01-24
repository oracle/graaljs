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

#define SUITE BigInt

#include <limits>

// BigInt::New

EXPORT_TO_JS(NewMinValue) {
    Local<BigInt> bigInt = BigInt::New(args.GetIsolate(), std::numeric_limits<int64_t>::min());
    args.GetReturnValue().Set(bigInt);
}

EXPORT_TO_JS(NewMaxValue) {
    Local<BigInt> bigInt = BigInt::New(args.GetIsolate(), std::numeric_limits<int64_t>::max());
    args.GetReturnValue().Set(bigInt);
}

// BigInt::NewFromUnsigned

EXPORT_TO_JS(NewFromUnsignedMinValue) {
    Local<BigInt> bigInt = BigInt::NewFromUnsigned(args.GetIsolate(), std::numeric_limits<uint64_t>::min());
    args.GetReturnValue().Set(bigInt);
}

EXPORT_TO_JS(NewFromUnsignedMaxValue) {
    Local<BigInt> bigInt = BigInt::NewFromUnsigned(args.GetIsolate(), std::numeric_limits<uint64_t>::max());
    args.GetReturnValue().Set(bigInt);
}

// BigInt::Int64Value

EXPORT_TO_JS(Int64Value) {
    int64_t int64Value = args[0].As<BigInt>()->Int64Value();
    Local<BigInt> bigInt = BigInt::New(args.GetIsolate(), int64Value);
    args.GetReturnValue().Set(bigInt);
}

EXPORT_TO_JS(Int64ValueLossLess) {
    bool lossless;
    args[0].As<BigInt>()->Int64Value(&lossless);
    args.GetReturnValue().Set(lossless);
}

// BigInt::Uint64Value

EXPORT_TO_JS(Uint64Value) {
    uint64_t uint64Value = args[0].As<BigInt>()->Uint64Value();
    Local<BigInt> bigInt = BigInt::NewFromUnsigned(args.GetIsolate(), uint64Value);
    args.GetReturnValue().Set(bigInt);
}

EXPORT_TO_JS(Uint64ValueLossLess) {
    bool lossless;
    args[0].As<BigInt>()->Uint64Value(&lossless);
    args.GetReturnValue().Set(lossless);
}

// BigInt::WordCount

EXPORT_TO_JS(WordCount) {
    args.GetReturnValue().Set(args[0].As<BigInt>()->WordCount());
}

EXPORT_TO_JS(NewFromWords) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    int sign_bit = args[0].As<Int32>()->Value();
    int word_count = args[1].As<Int32>()->Value();
    uint64_t* words = new uint64_t[word_count];
    for (int i = 0; i < word_count; i++) {
        // supporting words that fit into double only (for simplicity)
        words[i] = (uint64_t) args[2].As<Object>()->Get(context, i).ToLocalChecked().As<Number>()->Value();
    }

    Local<BigInt> result = BigInt::NewFromWords(context, sign_bit, word_count, words).ToLocalChecked();

    args.GetReturnValue().Set(result);
    delete[] words;
}

EXPORT_TO_JS(ToWordsArray) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    int sign_bit;
    int word_count = args[1].As<Int32>()->Value();
    int original_word_count = word_count;
    uint64_t* words = new uint64_t[word_count];

    args[0].As<BigInt>()->ToWordsArray(&sign_bit, &word_count, words);

    int effective_word_count = std::min(word_count, original_word_count);
    Local<Array> result = Array::New(isolate, effective_word_count + 2);
    result->Set(context, 0, Integer::New(isolate, sign_bit));
    result->Set(context, 1, Integer::New(isolate, word_count));
    for (int i = 0; i < effective_word_count; i++) {
        // supporting words that fit into double only (for simplicity)
        result->Set(context, i + 2, Number::New(isolate, (double) words[i]));
    }

    args.GetReturnValue().Set(result);
    delete[] words;
}

#undef SUITE
