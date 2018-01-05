/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <node.h>

using namespace v8;

#define SUITE_INTERNALS
#define STRINGIFY(value) #value
#define EXPAND_AND_STRINGIFY(value) STRINGIFY(value)
#define CONCAT(left, right) left ## right
#define EXPAND_AND_CONCAT(left, right) CONCAT(left, right)
#define EXPORT_TO_JS(name) void EXPAND_AND_CONCAT(SUITE, _ ## name)(const FunctionCallbackInfo<Value>& args)
#define EXPORT_END

void Fail(const char* message) {
    Isolate* isolate = Isolate::GetCurrent();
    Local<Value> ex = Exception::TypeError(String::NewFromUtf8(isolate, message));
    isolate->ThrowException(ex);
}

#include "unit/_suites.cc"

void ExportToJS(Local<Object> exports, FunctionCallback callback, const char* name) {
    NODE_SET_METHOD(exports, name, callback);
}

#undef SUITE_INTERNALS
#undef EXPORT_TO_JS
#define EXPORT_TO_JS(name) ExportToJS(exports, EXPAND_AND_CONCAT(SUITE, _ ## name), EXPAND_AND_STRINGIFY(SUITE) "_" #name); if (false)

void InitHelper(Local<Object> exports, const FunctionCallbackInfo<Value>& args) {

#include "unit/_suites.cc"

}

void Init(Local<Object> exports) {
    const v8::FunctionCallbackInfo<Value>* args = nullptr;
    InitHelper(exports, *args);
}

NODE_MODULE(testNative, Init)
