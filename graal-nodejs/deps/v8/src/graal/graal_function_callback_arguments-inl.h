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

#ifndef GRAAL_FUNCTION_CALLBACK_ARGUMENTS_INL_H_
#define GRAAL_FUNCTION_CALLBACK_ARGUMENTS_INL_H_

#include "graal_function_callback_arguments.h"
#include "graal_function_callback_info.h"

GraalFunctionCallbackArguments::GraalFunctionCallbackArguments(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_new_target,
        GraalValue* graal_data,
        GraalValue** argv,
        int argc,
        bool construct_call,
        bool args_on_heap) : argv_(argv), argc_(argc), args_on_heap_(args_on_heap) {
    graal_this->ReferenceAdded(); /* for holder and reference in values */
    if (construct_call) {
        graal_this->ReferenceAdded(); /* reference in return value */
    }
    graal_new_target->ReferenceAdded();
    graal_data->ReferenceAdded();
    implicit_args_[v8::internal::FunctionCallbackArguments::kHolderIndex] = graal_this;
    implicit_args_[v8::internal::FunctionCallbackArguments::kIsolateIndex] = isolate;
    implicit_args_[v8::internal::FunctionCallbackArguments::kReturnValueIndex] = construct_call ? graal_this : nullptr;
    implicit_args_[v8::internal::FunctionCallbackArguments::kDataIndex] = graal_data;
    implicit_args_[v8::internal::FunctionCallbackArguments::kNewTargetIndex] = graal_new_target;
    argv_[0] = graal_this;
    for (int i = 1; i <= argc_; i++) {
        reinterpret_cast<GraalValue*>(argv_[i])->ReferenceAdded();
    }
}

GraalFunctionCallbackArguments::~GraalFunctionCallbackArguments() {
    reinterpret_cast<GraalValue*> (implicit_args_[v8::internal::FunctionCallbackArguments::kDataIndex])->ReferenceRemoved();
    GraalValue* return_value = reinterpret_cast<GraalValue*> (implicit_args_[v8::internal::FunctionCallbackArguments::kReturnValueIndex]);
    if (return_value) {
        return_value->ReferenceRemoved();
    }
    if (args_on_heap_) {
        reinterpret_cast<GraalValue*> (implicit_args_[v8::internal::FunctionCallbackArguments::kNewTargetIndex])->ReferenceRemoved();
        for (int i = 0; i <= argc_; i++) { // this and args
            reinterpret_cast<GraalValue*>(argv_[i])->ReferenceRemoved();
        }
    } // else arguments are allocated on stack - they will be freed automatically =>
    // => no need to call ReferenceRemoved. In fact, we should not call this
    // method because if the reference count drops to zero then delete
    // is called automatically (which is not a good idea for an argument on stack).
}

#endif /* GRAAL_FUNCTION_CALLBACK_ARGUMENTS_INL_H_ */
