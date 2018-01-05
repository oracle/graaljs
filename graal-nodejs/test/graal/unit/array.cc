/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Array

// Array::Length

EXPORT_TO_JS(Length) {
    args.GetReturnValue().Set(args[0].As<Array>()->Length());
}

// Array::New

EXPORT_TO_JS(New) {
    int len = args[0]->ToInteger()->Value();
    args.GetReturnValue().Set(Array::New(args.GetIsolate(), len));
}

#undef SUITE
