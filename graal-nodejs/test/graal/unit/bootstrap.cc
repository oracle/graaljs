/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Bootstrap

EXPORT_TO_JS(Undefined) {
    args.GetReturnValue().SetUndefined();
}

EXPORT_TO_JS(Null) {
    args.GetReturnValue().SetNull();
}

EXPORT_TO_JS(True) {
    args.GetReturnValue().Set(true);
}

EXPORT_TO_JS(False) {
    args.GetReturnValue().Set(false);
}

EXPORT_TO_JS(Int32) {
    int32_t value = 211;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(Uint32) {
    uint32_t value = 3000000000;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(Double) {
    double value = 3.14;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(GetNotSet) {
    args.GetReturnValue().Set(args.GetReturnValue().Get());
}

EXPORT_TO_JS(GetObject) {
    Local<Object> value = Object::New(args.GetIsolate());
    args.GetReturnValue().Set(value == args.GetReturnValue().Get());
}

EXPORT_TO_JS(GetInt32) {
    int32_t number = 42;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

EXPORT_TO_JS(GetUint32) {
    uint32_t number = 4000000000;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

EXPORT_TO_JS(GetDouble) {
    double number = 2.71828;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

#undef SUITE
