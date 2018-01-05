/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Integer

// Integer::New

EXPORT_TO_JS(New) {
    Isolate* isolate = args.GetIsolate();
    int32_t value = (int32_t) args[0]->ToInt32()->Value();
    args.GetReturnValue().Set(Integer::New(isolate, value));
}

// Integer::NewFromUnsigned

EXPORT_TO_JS(NewFromUnsigned) {
    Isolate* isolate = args.GetIsolate();
    uint32_t value = (uint32_t) args[0]->ToUint32()->Value();
    args.GetReturnValue().Set(Integer::NewFromUnsigned(isolate, value));
}

#undef SUITE
