/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE FunctionTemplate

#ifdef SUITE_INTERNALS

void FunctionTemplate_Function(const FunctionCallbackInfo<Value>& args) {
    args.GetReturnValue().Set(args.This());
}

void FunctionTemplate_Child(const FunctionCallbackInfo<Value>& args) {
    args.GetReturnValue().Set(args[0]);
}

void SimpleAccessorGetter(Local<String> property, const PropertyCallbackInfo<Value>& info);
void SimpleAccessorSetter(Local<String> property, Local<Value> value, const PropertyCallbackInfo<void>& info);

#endif

// FunctionTemplate::HasInstance

EXPORT_TO_JS(HasInstanceIsInstance) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<Function> function = functionTemplate->GetFunction();
    Local<Object> instance = function->NewInstance();
    bool result = functionTemplate->HasInstance(instance);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasInstanceIsNotInstance) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<Object> instance = Object::New(isolate);
    bool result = functionTemplate->HasInstance(instance);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasInstanceSamePrototype) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<Function> function = functionTemplate->GetFunction();
    Local<Value> prototype = function->Get(String::NewFromUtf8(isolate, "prototype"));
    Local<Object> instance = Object::New(isolate);
    instance->SetPrototype(prototype);
    bool result = functionTemplate->HasInstance(instance);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasInstanceInherits) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> parentTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<FunctionTemplate> childTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Child);
    childTemplate->Inherit(parentTemplate);
    Local<Function> function = childTemplate->GetFunction();
    Local<Object> instance = function->NewInstance();
    bool result = parentTemplate->HasInstance(instance) && childTemplate->HasInstance(instance);
    args.GetReturnValue().Set(result);
}

// FunctionTemplate::SetClassName

EXPORT_TO_JS(SetClassName) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    functionTemplate->SetClassName(args[0].As<String>());
    args.GetReturnValue().Set(true);
}

// FunctionTemplate::GetFunction

EXPORT_TO_JS(GetFunction) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    args.GetReturnValue().Set(functionTemplate->GetFunction());
}

// FunctionTemplate::InstanceTemplate

EXPORT_TO_JS(CheckInstanceTemplate) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> instanceTemplate = functionTemplate->InstanceTemplate();

    int count = instanceTemplate->InternalFieldCount();
    args.GetReturnValue().Set(count == 0);
}

// FunctionTemplate::PrototypeTemplate

EXPORT_TO_JS(CheckPrototypeTemplate) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> prototypeTemplate = functionTemplate->PrototypeTemplate();

    int count = prototypeTemplate->InternalFieldCount();
    args.GetReturnValue().Set(count == 0);
}

// FunctionTemplate::SetHiddenPrototype

EXPORT_TO_JS(CheckSetHiddenPrototype) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);

    functionTemplate->SetHiddenPrototype(true);
    //TODO check behaviour

    functionTemplate->SetHiddenPrototype(false);
    //TODO check behaviour

    args.GetReturnValue().Set(true);
}

// Template::Set

EXPORT_TO_JS(SetOnInstanceTemplate) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> instanceTemplate = functionTemplate->InstanceTemplate();

    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    Local<Data> value = args[1];

    instanceTemplate->Set(name, value); //TODO also set PropertyAttributes

    Local<Function> function = functionTemplate->GetFunction();
    Local<Object> instance = function->NewInstance();

    args.GetReturnValue().Set(instance);
}

// Template::SetAccessor

EXPORT_TO_JS(CreateWithAccessor) {
    Isolate* isolate = args.GetIsolate();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> instanceTemplate = functionTemplate->InstanceTemplate();

    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    instanceTemplate->SetAccessor(name, SimpleAccessorGetter, SimpleAccessorSetter);
    Local<Function> function = functionTemplate->GetFunction();
    Local<Object> instance = function->NewInstance();
    args.GetReturnValue().Set(instance);
}

#undef SUITE
