/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Null

// Null/MissingPrimitive::IsUndefined

EXPORT_TO_JS(IsUndefined) {
    args.GetReturnValue().Set(Null(args.GetIsolate())->IsUndefined());
}

// Null/MissingPrimitive::IsNull

EXPORT_TO_JS(IsNull) {
    args.GetReturnValue().Set(Null(args.GetIsolate())->IsNull());
}

// Null/MissingPrimitive::IsTrue

EXPORT_TO_JS(IsTrue) {
    args.GetReturnValue().Set(Null(args.GetIsolate())->IsTrue());
}

// Null/MissingPrimitive::IsFalse

EXPORT_TO_JS(IsFalse) {
    args.GetReturnValue().Set(Null(args.GetIsolate())->IsFalse());
}

#undef SUITE
