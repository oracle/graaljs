/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_function_callback_arguments.h"
#include "graal_function_callback_info.h"
#include "graal_function.h"
#include "graal_primitive.h"

GraalFunctionCallbackArguments::GraalFunctionCallbackArguments(
        GraalIsolate* isolate,
        GraalValue* graal_this,
        GraalValue* graal_callee,
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
    graal_callee->ReferenceAdded();
    graal_new_target->ReferenceAdded();
    graal_data->ReferenceAdded();
    implicit_args_[GraalFunctionCallbackInfo::kHolderIndex] = graal_this;
    implicit_args_[GraalFunctionCallbackInfo::kIsolateIndex] = isolate;
    implicit_args_[GraalFunctionCallbackInfo::kReturnValueIndex] = construct_call ? graal_this : nullptr;
    implicit_args_[GraalFunctionCallbackInfo::kDataIndex] = graal_data;
    implicit_args_[GraalFunctionCallbackInfo::kCalleeIndex] = graal_callee;
    implicit_args_[GraalFunctionCallbackInfo::kNewTargetIndex] = graal_new_target;
    argv_[argc_] = graal_this;
    for (int i = 0; i < argc_; i++) {
        reinterpret_cast<GraalValue*>(argv_[i])->ReferenceAdded();
    }
}

GraalFunctionCallbackArguments::~GraalFunctionCallbackArguments() {
    reinterpret_cast<GraalValue*> (implicit_args_[GraalFunctionCallbackInfo::kDataIndex])->ReferenceRemoved();
    reinterpret_cast<GraalValue*> (implicit_args_[GraalFunctionCallbackInfo::kCalleeIndex])->ReferenceRemoved();
    GraalValue* return_value = reinterpret_cast<GraalValue*> (implicit_args_[GraalFunctionCallbackInfo::kReturnValueIndex]);
    if (return_value) {
        return_value->ReferenceRemoved();
    }
    if (args_on_heap_) {
        reinterpret_cast<GraalValue*> (implicit_args_[GraalFunctionCallbackInfo::kNewTargetIndex])->ReferenceRemoved();
        for (int i = 0; i <= argc_; i++) { // argc_ is this
            reinterpret_cast<GraalValue*>(argv_[i])->ReferenceRemoved();
        }
    } // else arguments are allocated on stack - they will be freed automatically =>
    // => no need to call ReferenceRemoved. In fact, we should not call this
    // method because if the reference count drops to zero then delete
    // is called automatically (which is not a good idea for an argument on stack).
}
