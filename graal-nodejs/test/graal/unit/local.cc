/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Local

// Local::IsEmpty

EXPORT_TO_JS(IsEmpty) {
    Local<Data> handle = args[0];
    args.GetReturnValue().Set(handle.IsEmpty());
}

EXPORT_TO_JS(IsEmptyAfterClear) {
    Local<Data> handle = args[0];
    handle.Clear();
    args.GetReturnValue().Set(handle.IsEmpty());
}

// Local::operator==

EXPORT_TO_JS(OperatorEquals) {
    Local<Data> handle1 = args[0];
    Local<Data> handle2 = args[1];
    args.GetReturnValue().Set(handle1 == handle2);
}

// Local::operator!=

EXPORT_TO_JS(OperatorNotEquals) {
    Local<Data> handle1 = args[0];
    Local<Data> handle2 = args[1];
    args.GetReturnValue().Set(handle1 != handle2);
}

#undef SUITE
