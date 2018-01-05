/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Exception

// Exception::Error

EXPORT_TO_JS(Error) {
    args.GetReturnValue().Set(Exception::Error(args[0].As<String>()));
}

EXPORT_TO_JS(RangeError) {
    args.GetReturnValue().Set(Exception::RangeError(args[0].As<String>()));
}

EXPORT_TO_JS(ReferenceError) {
    args.GetReturnValue().Set(Exception::ReferenceError(args[0].As<String>()));
}

EXPORT_TO_JS(SyntaxError) {
    args.GetReturnValue().Set(Exception::SyntaxError(args[0].As<String>()));
}

EXPORT_TO_JS(TypeError) {
    args.GetReturnValue().Set(Exception::TypeError(args[0].As<String>()));
}

#undef SUITE
