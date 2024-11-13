/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GRAAL_PROPERTY_CALLBACK_INFO_H_
#define GRAAL_PROPERTY_CALLBACK_INFO_H_

#include "graal_isolate.h"
#include "graal_value.h"
#include "include/v8.h"

namespace v8 {
    namespace internal {
        class PropertyCallbackArguments {
            public:
                static constexpr int kHolderIndex = v8::PropertyCallbackInfo<v8::Value>::kHolderIndex;
                static constexpr int kIsolateIndex = v8::PropertyCallbackInfo<v8::Value>::kIsolateIndex;
                static constexpr int kReturnValueIndex = v8::PropertyCallbackInfo<v8::Value>::kReturnValueIndex;
                static constexpr int kDataIndex = v8::PropertyCallbackInfo<v8::Value>::kDataIndex;
                static constexpr int kThisIndex = v8::PropertyCallbackInfo<v8::Value>::kThisIndex;
                static constexpr int kArgsLength = v8::PropertyCallbackInfo<v8::Value>::kArgsLength;
        };
    }
}

template<typename T>
class GraalPropertyCallbackInfo : public v8::PropertyCallbackInfo<T> {
public:
    V8_INLINE static GraalPropertyCallbackInfo<T> New(
            GraalIsolate* isolate,
            jobjectArray arguments,
            int index_of_this,
            jobject data,
            jobject holder);
    V8_INLINE ~GraalPropertyCallbackInfo();

    GraalPropertyCallbackInfo(GraalPropertyCallbackInfo&&) = default;
    GraalPropertyCallbackInfo& operator=(GraalPropertyCallbackInfo&&) = default;
private:
    V8_INLINE GraalPropertyCallbackInfo(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_data,
        GraalValue* graal_holder);

    GraalPropertyCallbackInfo(const GraalPropertyCallbackInfo&) = delete;
    GraalPropertyCallbackInfo& operator=(const GraalPropertyCallbackInfo&) = delete;

    void* values_[v8::internal::PropertyCallbackArguments::kArgsLength];
};

extern template class GraalPropertyCallbackInfo<v8::Value>;
extern template class GraalPropertyCallbackInfo<v8::Integer>;
extern template class GraalPropertyCallbackInfo<v8::Boolean>;
extern template class GraalPropertyCallbackInfo<v8::Array>;
extern template class GraalPropertyCallbackInfo<void>;

#endif /* GRAAL_PROPERTY_CALLBACK_INFO_H_ */
