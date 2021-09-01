/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#define SUITE FunctionTemplate

#ifdef SUITE_INTERNALS

void FunctionTemplate_Function(const FunctionCallbackInfo<Value>& args) {
    args.GetReturnValue().Set(args.This());
}

void FunctionTemplate_Child(const FunctionCallbackInfo<Value>& args) {
    args.GetReturnValue().Set(args[0]);
}

void SimpleAccessorGetter(Local<Name> property, const PropertyCallbackInfo<Value>& info);
void SimpleAccessorSetter(Local<Name> property, Local<Value> value, const PropertyCallbackInfo<void>& info);

#endif

// FunctionTemplate::HasInstance

EXPORT_TO_JS(HasInstanceIsInstance) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<Function> function = functionTemplate->GetFunction(context).ToLocalChecked();
    Local<Object> instance = function->NewInstance(isolate->GetCurrentContext()).ToLocalChecked();
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
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<Function> function = functionTemplate->GetFunction(context).ToLocalChecked();
    Local<String> prototypeKey = String::NewFromUtf8(isolate, "prototype", v8::NewStringType::kNormal).ToLocalChecked();
    Local<Value> prototype = function->Get(context, prototypeKey).ToLocalChecked();
    Local<Object> instance = Object::New(isolate);
    instance->SetPrototype(context, prototype);
    bool result = functionTemplate->HasInstance(instance);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasInstanceInherits) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> parentTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<FunctionTemplate> childTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Child);
    childTemplate->Inherit(parentTemplate);
    Local<Function> function = childTemplate->GetFunction(context).ToLocalChecked();
    Local<Object> instance = function->NewInstance(context).ToLocalChecked();
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
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    args.GetReturnValue().Set(functionTemplate->GetFunction(context).ToLocalChecked());
}

// FunctionTemplate::ReadOnlyPrototype

EXPORT_TO_JS(GetFunctionWithReadOnlyPrototype) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    functionTemplate->ReadOnlyPrototype();
    args.GetReturnValue().Set(functionTemplate->GetFunction(context).ToLocalChecked());
}

// FunctionTemplate::SetLength

EXPORT_TO_JS(GetFunctionWithLength) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    functionTemplate->SetLength(args[0].As<Int32>()->Value());
    args.GetReturnValue().Set(functionTemplate->GetFunction(context).ToLocalChecked());
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

// Template::Set

EXPORT_TO_JS(SetOnInstanceTemplate) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> instanceTemplate = functionTemplate->InstanceTemplate();

    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    Local<Data> value = args[1];

    instanceTemplate->Set(name, value); //TODO also set PropertyAttributes

    Local<Function> function = functionTemplate->GetFunction(context).ToLocalChecked();
    Local<Object> instance = function->NewInstance(context).ToLocalChecked();

    args.GetReturnValue().Set(instance);
}

// Template::SetAccessor

EXPORT_TO_JS(CreateWithAccessor) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<FunctionTemplate> functionTemplate = FunctionTemplate::New(isolate, FunctionTemplate_Function);
    Local<ObjectTemplate> instanceTemplate = functionTemplate->InstanceTemplate();

    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    instanceTemplate->SetAccessor(name, SimpleAccessorGetter, SimpleAccessorSetter);
    Local<Function> function = functionTemplate->GetFunction(context).ToLocalChecked();
    Local<Object> instance = function->NewInstance(context).ToLocalChecked();
    args.GetReturnValue().Set(instance);
}

#undef SUITE
