/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

#define SUITE ObjectTemplate

#ifdef SUITE_INTERNALS

void SimpleAccessorGetter(Local<Name> property, const PropertyCallbackInfo<Value>& info);
void SimpleAccessorSetter(Local<Name> property, Local<Value> value, const PropertyCallbackInfo<void>& info);
void EmptyPropertyEnumeratorCallback(const PropertyCallbackInfo<Array>& info) {};

#endif

// ObjectTemplate::NewInstance

EXPORT_TO_JS(NewInstance) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
    args.GetReturnValue().Set(objectTemplate->NewInstance(context).ToLocalChecked());
}

// ObjectTemplate::InternalFieldCount

EXPORT_TO_JS(DefaultInternalFieldCount) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(args.GetIsolate());
    args.GetReturnValue().Set(objectTemplate->InternalFieldCount());
}

// ObjectTemplate::SetInternalFieldCount

EXPORT_TO_JS(SetAndCheckInternalFieldCount) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(args.GetIsolate());

    int count = objectTemplate->InternalFieldCount();
    if (count != 0) {
        args.GetReturnValue().Set(false);
        return;
    }

    objectTemplate->SetInternalFieldCount(10);

    count = objectTemplate->InternalFieldCount();
    if (count != 10) {
        args.GetReturnValue().Set(false);
        return;
    }

    args.GetReturnValue().Set(true);
}

// Template::Set

EXPORT_TO_JS(Set) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);

    Local<Name> name = args[0].As<Name>();
    Local<Data> value = args[1];

    objectTemplate->Set(name, value); //TODO also set PropertyAttributes

    args.GetReturnValue().Set(objectTemplate->NewInstance(context).ToLocalChecked());
}

// Template::SetAccessor

EXPORT_TO_JS(CreateWithAccessor) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
    Local<Name> name = args[0].As<Name>();
    objectTemplate->SetAccessor(name, SimpleAccessorGetter, SimpleAccessorSetter);
    args.GetReturnValue().Set(objectTemplate->NewInstance(context).ToLocalChecked());
}

// ObjectTemplate::SetHandler

EXPORT_TO_JS(CheckNamedHandlerWithInternalFields) {
    Isolate* isolate = args.GetIsolate();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
    int expectedCount = 3;
    objectTemplate->SetInternalFieldCount(expectedCount);
    NamedPropertyHandlerConfiguration handler;
    objectTemplate->SetHandler(handler);

    Local<Context> context = isolate->GetCurrentContext();
    Local<Object> instance = objectTemplate->NewInstance(context).ToLocalChecked();
    int actualCount = instance->InternalFieldCount();
    args.GetReturnValue().Set(expectedCount == actualCount);
}

EXPORT_TO_JS(CreateWithEmptyIndexedEnumerator) {
    Isolate* isolate = args.GetIsolate();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
    IndexedPropertyHandlerConfiguration handler(
            nullptr, // getter
            nullptr, // setter
            nullptr, // query
            nullptr, // deleter
            EmptyPropertyEnumeratorCallback // enumerator
    );
    objectTemplate->SetHandler(handler);

    Local<Context> context = isolate->GetCurrentContext();
    Local<Object> instance = objectTemplate->NewInstance(context).ToLocalChecked();
    args.GetReturnValue().Set(instance);
}

EXPORT_TO_JS(CreateWithEmptyNamedEnumerator) {
    Isolate* isolate = args.GetIsolate();
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
    NamedPropertyHandlerConfiguration handler(
            nullptr, // getter
            nullptr, // setter
            nullptr, // query
            nullptr, // deleter
            EmptyPropertyEnumeratorCallback // enumerator
    );
    objectTemplate->SetHandler(handler);

    Local<Context> context = isolate->GetCurrentContext();
    Local<Object> instance = objectTemplate->NewInstance(context).ToLocalChecked();
    args.GetReturnValue().Set(instance);
}

#undef SUITE
