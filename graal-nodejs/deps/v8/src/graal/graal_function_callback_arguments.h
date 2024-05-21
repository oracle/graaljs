/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_
#define GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_

#include "graal_isolate.h"
#include "graal_value.h"
#include "include/v8.h"

namespace v8 {
    namespace internal {
        class FunctionCallbackArguments {
            public:
                static constexpr int kHolderIndex = FunctionCallbackInfo<v8::Value>::kHolderIndex;
                static constexpr int kIsolateIndex = FunctionCallbackInfo<v8::Value>::kIsolateIndex;
                static constexpr int kReturnValueIndex = FunctionCallbackInfo<v8::Value>::kReturnValueIndex;
                static constexpr int kDataIndex = FunctionCallbackInfo<v8::Value>::kDataIndex;
                static constexpr int kNewTargetIndex = FunctionCallbackInfo<v8::Value>::kNewTargetIndex;
                static constexpr int kArgsLength = FunctionCallbackInfo<v8::Value>::kArgsLength;
        };
    }
}

class GraalFunctionCallbackArguments {
public:
    V8_INLINE GraalFunctionCallbackArguments(
            GraalIsolate* isolate,
            GraalValue* this_arg,
            GraalValue* new_target,
            GraalValue* data,
            GraalValue** argv,
            int argc,
            bool construct_call,
            bool args_on_heap);

    void ** implicit_args() {
        return implicit_args_;
    }
    GraalValue** values() {
        return argv_ + 1;
    }
    int length() {
        return argc_;
    }

    V8_INLINE ~GraalFunctionCallbackArguments();

private:
    GraalFunctionCallbackArguments(const GraalFunctionCallbackArguments&) = delete;
    GraalFunctionCallbackArguments& operator=(const GraalFunctionCallbackArguments&) = delete;

    static const int implicit_args_length = v8::internal::FunctionCallbackArguments::kArgsLength;
    void* implicit_args_[implicit_args_length];
    GraalValue** argv_;
    int argc_;
    bool args_on_heap_;
};

#endif /* GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_ */
