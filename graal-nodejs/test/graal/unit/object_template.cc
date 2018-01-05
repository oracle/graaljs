/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE ObjectTemplate

#ifdef SUITE_INTERNALS

void SimpleAccessorGetter(Local<String> property, const PropertyCallbackInfo<Value>& info);
void SimpleAccessorSetter(Local<String> property, Local<Value> value, const PropertyCallbackInfo<void>& info);

#endif

// ObjectTemplate::NewInstance

EXPORT_TO_JS(NewInstance) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();
    args.GetReturnValue().Set(objectTemplate->NewInstance());
}

// ObjectTemplate::InternalFieldCount

EXPORT_TO_JS(DefaultInternalFieldCount) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();
    args.GetReturnValue().Set(objectTemplate->InternalFieldCount());
}

// ObjectTemplate::SetInternalFieldCount

EXPORT_TO_JS(SetAndCheckInternalFieldCount) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();

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
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();

    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    Local<Data> value = args[1];

    objectTemplate->Set(name, value); //TODO also set PropertyAttributes

    args.GetReturnValue().Set(objectTemplate->NewInstance());
}

// Template::SetAccessor

EXPORT_TO_JS(CreateWithAccessor) {
    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();
    Local<String> name = args[0].As<String>(); //TODO should be Local<Name>
    objectTemplate->SetAccessor(name, SimpleAccessorGetter, SimpleAccessorSetter);
    args.GetReturnValue().Set(objectTemplate->NewInstance());
}

#undef SUITE
