#include <node.h>

namespace jniprofilingtest {

using v8::FunctionCallbackInfo;
using v8::Isolate;
using v8::Local;
using v8::Object;
using v8::String;
using v8::Value;
using v8::Array;
using v8::Number;

void Method(const FunctionCallbackInfo<Value>& args) {

	Isolate* isolate = args.GetIsolate();

	Local<Array> from = args[0].As<Array>();
	size_t count = from->Length();

	Local<Object> obj = Object::New(isolate);
	Local<String> bar = String::NewFromUtf8(isolate, "bar");

    obj->Set(bar, Number::New(isolate, 0));

	for (size_t i = 0; i < count; i++) {

		Local<Value> val_v = obj->Get(bar);
		double val = val_v->NumberValue();

        obj->Set(bar, Number::New(isolate, val + from->Get(i)->NumberValue()));
	}

	args.GetReturnValue().Set(obj);
}

void init(Local<Object> exports) {
  NODE_SET_METHOD(exports, "execute", Method);
}

NODE_MODULE(addon, init)

}  // namespace jniprofilingtest