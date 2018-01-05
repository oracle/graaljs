/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_
#define GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_

#include "graal_isolate.h"
#include "graal_value.h"
#include "include/v8.h"

class GraalFunctionCallbackArguments {
public:
    GraalFunctionCallbackArguments(
            GraalIsolate* isolate,
            GraalValue* this_arg,
            GraalValue* callee,
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
        return argv_ + argc_ - 1;
    }
    int length() {
        return argc_;
    }

    ~GraalFunctionCallbackArguments();

private:
    GraalFunctionCallbackArguments(const GraalFunctionCallbackArguments&) = delete;
    GraalFunctionCallbackArguments& operator=(const GraalFunctionCallbackArguments&) = delete;

    static const int implicit_args_length = v8::FunctionCallbackInfo<v8::Value>::kArgsLength;
    void* implicit_args_[implicit_args_length];
    GraalValue** argv_;
    int argc_;
    bool args_on_heap_;
};

#endif /* GRAAL_FUNCTION_CALLBACK_ARGUMENTS_H_ */

