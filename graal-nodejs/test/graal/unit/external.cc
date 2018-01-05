/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE External

#ifdef SUITE_INTERNALS

char externalBuffer[20] = "test_12345";

#endif

// External::New

EXPORT_TO_JS(New) {
    Isolate* isolate = args.GetIsolate();
    void* buf = (void*) externalBuffer;
    Local<External> ext = External::New(isolate, buf);

    Local<ObjectTemplate> objectTemplate = ObjectTemplate::New();
    objectTemplate->SetInternalFieldCount(1);
    Local<Object> obj = objectTemplate->NewInstance();

    obj->SetInternalField(0, ext);
    return args.GetReturnValue().Set(obj);
}

// Value::IsExternal

EXPORT_TO_JS(CheckInternalFieldIsExtern) {
    Local<Object> obj = args[0].As<Object>();
    Local<Value> val = obj->GetInternalField(args[1]->ToInt32()->Value());
    args.GetReturnValue().Set(val->IsExternal());
}

#undef SUITE
