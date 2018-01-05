/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_function_callback_info.h"
#include "graal_external.h"

GraalFunctionCallbackInfo::GraalFunctionCallbackInfo(GraalFunctionCallbackArguments& args)
: GraalFunctionCallbackInfo(args.implicit_args(), args.values(), args.length()) {
}

GraalFunctionCallbackInfo::GraalFunctionCallbackInfo(
        void** implicit_args,
        GraalValue** values,
        int length) : v8::FunctionCallbackInfo<v8::Value>(
                reinterpret_cast<v8::internal::Object**> (implicit_args),
                reinterpret_cast<v8::internal::Object**> (values),
                length) {
}
