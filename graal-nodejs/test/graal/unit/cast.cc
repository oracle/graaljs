/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Cast

// Array::Cast

EXPORT_TO_JS(Array) {
    args.GetReturnValue().Set(Local<Array>::Cast(args[0]));
}

// Date::Cast

EXPORT_TO_JS(Date) {
    args.GetReturnValue().Set(Local<Date>::Cast(args[0]));
}

// Function::Cast

EXPORT_TO_JS(Function) {
    args.GetReturnValue().Set(Local<Function>::Cast(args[0]));
}

// Integer::Cast

EXPORT_TO_JS(Integer) {
    args.GetReturnValue().Set(Local<Integer>::Cast(args[0]));
}

// Number::Cast

EXPORT_TO_JS(Number) {
    args.GetReturnValue().Set(Local<Number>::Cast(args[0]));
}

// Object::Cast

EXPORT_TO_JS(Object) {
    args.GetReturnValue().Set(Local<Object>::Cast(args[0]));
}

// RegExp::Cast

EXPORT_TO_JS(RegExp) {
    args.GetReturnValue().Set(Local<RegExp>::Cast(args[0]));
}

// String::Cast

EXPORT_TO_JS(String) {
    args.GetReturnValue().Set(Local<String>::Cast(args[0]));
}

// Symbol::Cast

EXPORT_TO_JS(Symbol) {
    args.GetReturnValue().Set(Local<Symbol>::Cast(args[0]));
}

#undef SUITE
