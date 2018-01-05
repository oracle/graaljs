/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_FUNCTION_CALLBACK_INFO_H_
#define GRAAL_FUNCTION_CALLBACK_INFO_H_

#include "graal_isolate.h"
#include "graal_value.h"
#include "include/v8.h"
#include "graal_function_callback_arguments.h"

class GraalFunctionCallbackInfo : public v8::FunctionCallbackInfo<v8::Value> {
public:
    GraalFunctionCallbackInfo(GraalFunctionCallbackArguments& args);
private:
    GraalFunctionCallbackInfo(
            void** implicit_args,
            GraalValue** values,
            int length);

    GraalFunctionCallbackInfo(const GraalFunctionCallbackInfo&) = delete;
    GraalFunctionCallbackInfo& operator=(const GraalFunctionCallbackInfo&) = delete;

    friend class GraalFunctionCallbackArguments;
};

#endif /* GRAAL_FUNCTION_CALLBACK_INFO_H_ */

