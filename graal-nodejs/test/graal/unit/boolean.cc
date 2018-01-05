/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Boolean

// Boolean::IsUndefined

EXPORT_TO_JS(IsUndefinedForTrue) {
    bool result = True(args.GetIsolate())->IsUndefined();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(IsUndefinedForFalse) {
    bool result = False(args.GetIsolate())->IsUndefined();
    args.GetReturnValue().Set(result);
}

// Boolean::IsNull

EXPORT_TO_JS(IsNullForTrue) {
    bool result = True(args.GetIsolate())->IsNull();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(IsNullForFalse) {
    bool result = False(args.GetIsolate())->IsNull();
    args.GetReturnValue().Set(result);
}

// Boolean::IsTrue

EXPORT_TO_JS(IsTrueForTrue) {
    bool result = True(args.GetIsolate())->IsTrue();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(IsTrueForFalse) {
    bool result = False(args.GetIsolate())->IsTrue();
    args.GetReturnValue().Set(result);
}

// Boolean::IsFalse

EXPORT_TO_JS(IsFalseForTrue) {
    bool result = True(args.GetIsolate())->IsFalse();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(IsFalseForFalse) {
    bool result = False(args.GetIsolate())->IsFalse();
    args.GetReturnValue().Set(result);
}

#undef SUITE
