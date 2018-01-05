/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Arguments

#ifdef SUITE_INTERNALS

void Arguments_Function(const FunctionCallbackInfo<Value>& args) {
    bool constructCall = args.IsConstructCall();
    Isolate* isolate = args.GetIsolate();
    args[0].As<Object>()->Set(String::NewFromUtf8(isolate, "isConstructCall"), constructCall ? True(isolate) : False(isolate));
    args[0].As<Object>()->Set(String::NewFromUtf8(isolate, "thisValue"), args.This());
    args[0].As<Object>()->Set(String::NewFromUtf8(isolate, "holderValue"), args.Holder());
    args.GetReturnValue().Set(args.This());
}

#endif

// Arguments::IsConstructCall
// Arguments::This
// Arguments::Holder

// fields of Arguments can be read from a generic function

EXPORT_TO_JS(FunctionWithArguments) {
    Local<Function> func = FunctionTemplate::New(args.GetIsolate(), Arguments_Function)->GetFunction();
    args.GetReturnValue().Set(func);
}

#undef SUITE
