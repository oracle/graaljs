/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#define SUITE Object

#ifdef SUITE_INTERNALS

int simpleGetterCallCount = 0;
int simpleSetterCallCount = 0;

void SimpleAccessorGetter(Local<Name> property, const PropertyCallbackInfo<Value>& info) {
    Isolate* isolate = info.GetIsolate();
    simpleGetterCallCount++;
    Local<String> prefix = String::NewFromUtf8(isolate, "accessor getter called: ", v8::NewStringType::kNormal).ToLocalChecked();
    info.GetReturnValue().Set(String::Concat(isolate, prefix, property.As<String>()));
}

void SimpleAccessorSetter(Local<Name> property, Local<Value> value, const PropertyCallbackInfo<void>& info) {
    Isolate* isolate = info.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    simpleSetterCallCount++;
    Local<Object> obj = info.This();
    Local<String> key = String::NewFromUtf8(isolate, "mySetValue", v8::NewStringType::kNormal).ToLocalChecked();
    obj->Set(context, key, value);
}

#endif


// Object::GetOwnPropertyNames

EXPORT_TO_JS(GetOwnPropertyNames) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->GetOwnPropertyNames(context).ToLocalChecked());
}

// Object::GetPropertyNames

EXPORT_TO_JS(GetPropertyNames) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->GetPropertyNames(context).ToLocalChecked());
}

// Object::Get

EXPORT_TO_JS(GetByName) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    MaybeLocal<Value> result = args[0].As<Object>()->Get(context, args[1]);
    if (!result.IsEmpty()) {
        args.GetReturnValue().Set(result.ToLocalChecked());
    }
}

EXPORT_TO_JS(GetByIndex) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    uint32_t index = args[1].As<Integer>()->Value();
    args.GetReturnValue().Set(args[0].As<Object>()->Get(context, index).ToLocalChecked());
}

// Object::Set

EXPORT_TO_JS(SetByName) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<Value> key = args[1];
    Local<Value> value = args[2];
    args.GetReturnValue().Set(obj->Set(context, key, value).FromJust());
}

EXPORT_TO_JS(SetByIndex) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    uint32_t index = args[1].As<Integer>()->Value();
    Local<Value> value = args[2];
    args.GetReturnValue().Set(obj->Set(context, index, value).FromJust());
}

// Object::Has

EXPORT_TO_JS(HasByName) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->Has(context, args[1]).FromJust());
}

// Object::HasOwnProperty

EXPORT_TO_JS(HasOwnProperty) {
    args.GetReturnValue().Set(args[0].As<Object>()->HasOwnProperty(args.GetIsolate()->GetCurrentContext(), args[1].As<String>()).FromJust());
}

// Object::HasRealNamedProperty

EXPORT_TO_JS(HasRealNamedProperty) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->HasRealNamedProperty(context, args[1].As<String>()).FromJust());
}

// Object::HasRealIndexedProperty

EXPORT_TO_JS(HasRealIndexedProperty) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->HasRealIndexedProperty(context, args[1]->ToUint32(context).ToLocalChecked()->Value()).FromJust());
}

// Object::Delete

EXPORT_TO_JS(DeleteByName) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    args.GetReturnValue().Set(args[0].As<Object>()->Delete(context, args[1]).FromJust());
}

// Object::InternalFieldCount

EXPORT_TO_JS(InternalFieldCount) {
    args.GetReturnValue().Set(args[0].As<Object>()->InternalFieldCount());
}

// Object::GetConstructorName

EXPORT_TO_JS(GetConstructorName) {
    args.GetReturnValue().Set(args[0].As<Object>()->GetConstructorName());
}

// Object::GetPrototype

EXPORT_TO_JS(GetPrototype) {
    args.GetReturnValue().Set(args[0].As<Object>()->GetPrototype());
}

// Object::CreationContext

EXPORT_TO_JS(CreationContextIsCurrent) {
    Local<Object> obj = args[0].As<Object>();

    Local<Context> creationContext = obj->GetCreationContext().ToLocalChecked();
    Local<Context> currentContext = args.GetIsolate()->GetCurrentContext();
    bool result = (creationContext == currentContext);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(CreationContextNewContext) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> newContext = Context::New(isolate);

    newContext->Enter();
    Local<Object> object = Object::New(isolate);
    newContext->Exit();

    Local<Context> creationContext = object->GetCreationContext().ToLocalChecked();
    bool result = (creationContext == newContext);
    args.GetReturnValue().Set(result);
}

// Object::Clone

EXPORT_TO_JS(Clone) {
    Local<Object> obj = args[0].As<Object>();

    Local<Object> newObj = obj->Clone();
    args.GetReturnValue().Set(newObj);
}

// Object::SetAccessor

EXPORT_TO_JS(SetAccessor) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<String> key = args[1].As<String>();

    obj->SetAccessor(context, key, SimpleAccessorGetter, SimpleAccessorSetter);

    args.GetReturnValue().Set(true);
}

EXPORT_TO_JS(SetAccessorNoSetterWritable) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<String> key = args[1].As<String>();

    obj->SetAccessor(context, key, SimpleAccessorGetter);

    args.GetReturnValue().Set(true);
}

EXPORT_TO_JS(SetAccessorNoSetterReadOnly) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<String> key = args[1].As<String>();

    obj->SetAccessor(context, key, SimpleAccessorGetter, nullptr, MaybeLocal<Value>(), AccessControl::DEFAULT, PropertyAttribute::ReadOnly);

    args.GetReturnValue().Set(true);
}

EXPORT_TO_JS(GetRealNamedPropertyAttributes) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<String> key = args[1].As<String>();

    Maybe<PropertyAttribute> attrs = obj->GetRealNamedPropertyAttributes(context, key);
    int result = attrs.IsJust() ? static_cast<int> (attrs.FromJust()) : -1;

    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(SetIntegrityLevel) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    bool freeze = args[1].As<Boolean>()->Value();

    IntegrityLevel level = freeze ? IntegrityLevel::kFrozen : IntegrityLevel::kSealed;
    Maybe<bool> result = obj->SetIntegrityLevel(context, level);

    if (result.IsJust()) {
        args.GetReturnValue().Set(result.FromJust());
    }
}

EXPORT_TO_JS(CreateDataProperty) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    Local<Name> key = args[1].As<Name>();

    Maybe<bool> result = obj->CreateDataProperty(context, key, args[2]);

    if (result.IsJust()) {
        args.GetReturnValue().Set(result.FromJust());
    }
}

EXPORT_TO_JS(CreateDataPropertyIndex) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();
    uint32_t index = args[1]->Uint32Value(context).FromJust();

    Maybe<bool> result = obj->CreateDataProperty(context, index, args[2]);

    if (result.IsJust()) {
        args.GetReturnValue().Set(result.FromJust());
    }
}

EXPORT_TO_JS(CallAsConstructor) {
    Local<Context> context = args.GetIsolate()->GetCurrentContext();
    Local<Object> obj = args[0].As<Object>();

    MaybeLocal<Value> result = obj->CallAsConstructor(context, 0, nullptr);

    if (!result.IsEmpty()) {
        args.GetReturnValue().Set(result.ToLocalChecked());
    }
}

#undef SUITE
