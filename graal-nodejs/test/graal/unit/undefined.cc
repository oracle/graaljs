/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Undefined

// Undefined/MissingPrimitive::IsUndefined

EXPORT_TO_JS(IsUndefined) {
    args.GetReturnValue().Set(Undefined(args.GetIsolate())->IsUndefined());
}

// Undefined/MissingPrimitive::IsNull

EXPORT_TO_JS(IsNull) {
    args.GetReturnValue().Set(Undefined(args.GetIsolate())->IsNull());
}

// Undefined/MissingPrimitive::IsTrue

EXPORT_TO_JS(IsTrue) {
    args.GetReturnValue().Set(Undefined(args.GetIsolate())->IsTrue());
}

// Undefined/MissingPrimitive::IsFalse

EXPORT_TO_JS(IsFalse) {
    args.GetReturnValue().Set(Undefined(args.GetIsolate())->IsFalse());
}

#undef SUITE
