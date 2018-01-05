/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Persistent

// Persistent::New

EXPORT_TO_JS(New) {
    Persistent<Value> pers(args.GetIsolate(), args[0]);
    args.GetReturnValue().Set(pers);
}

// Persistent::New

EXPORT_TO_JS(Reset) {
    Persistent<Value> pers(args.GetIsolate(), args[0]);
    pers.Reset();
    args.GetReturnValue().Set(true);
}

#undef SUITE
