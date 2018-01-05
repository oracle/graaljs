/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE TryCatch

#ifdef SUITE_INTERNALS

void TryCatch_InvokeCallback(const FunctionCallbackInfo<Value>& args) {
    Isolate* isolate = args.GetIsolate();
    Local<Value> callback = args[0];
    Local<Value> callbackArgs[0];
    Local<Function>::Cast(callback)->Call(Object::New(isolate), 0, callbackArgs);
}

void TryCatch_ReThrowNestedHelper(const FunctionCallbackInfo<Value>& args, int depth) {
    if (depth == 0) {
        TryCatch tryCatch;
        TryCatch_ReThrowNestedHelper(args, depth - 1);
        tryCatch.ReThrow();
    } else {
        TryCatch_InvokeCallback(args);
    }
}

#endif

// TryCatch::HasCaught

EXPORT_TO_JS(HasCaughtNoException) {
    TryCatch tryCatch;
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtNativeException) {
    Isolate* isolate = args.GetIsolate();
    TryCatch tryCatch;
    // undefined.key triggers a reference error
    Local<Value> undefined = Undefined(isolate);
    Local<Object>::Cast(undefined)->Get(String::NewFromUtf8(isolate, "key"));
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtJSException) {
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtNestedOuter) {
    bool ok = true;
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    {
        TryCatch innerCatch;
        ok &= !innerCatch.HasCaught();
    }
    ok &= tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}


EXPORT_TO_JS(HasCaughtNestedInner) {
    bool ok = true;
    TryCatch tryCatch;
    {
        TryCatch innerCatch;
        TryCatch_InvokeCallback(args);
        ok &= innerCatch.HasCaught();
    }
    ok &= !tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}

EXPORT_TO_JS(HasCaughtNestedBoth) {
    bool ok = true;
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    {
        TryCatch innerCatch;
        TryCatch_InvokeCallback(args);
        ok &= innerCatch.HasCaught();
    }
    ok &= tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}

// TryCatch::HasTerminated

EXPORT_TO_JS(HasTerminatedNoException) {
    TryCatch tryCatch;
    //bool result = tryCatch.HasTerminated(); //TODO
    bool result = false;
    args.GetReturnValue().Set(result);
}

// TryCatch::Exception

EXPORT_TO_JS(ExceptionForNoExceptionIsEmpty) {
    TryCatch tryCatch;
    bool result = tryCatch.Exception().IsEmpty();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(ExceptionForThrownException) {
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    args.GetReturnValue().Set(tryCatch.Exception());
}

// TryCatch::ReThrow

EXPORT_TO_JS(ReThrowException) {
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    tryCatch.ReThrow();
}

EXPORT_TO_JS(ReThrowNested) {
    TryCatch tryCatch;
    TryCatch_ReThrowNestedHelper(args, 3);
    args.GetReturnValue().Set(tryCatch.Exception());
}

// TryCatch::SetVerbose

EXPORT_TO_JS(SetVerbose) {
    TryCatch tryCatch;
    bool b = args[0]->IsTrue();
    tryCatch.SetVerbose(b);
    args.GetReturnValue().Set(true);
}

// TryCatch::Message

EXPORT_TO_JS(MessageGetSourceLine) {
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    Local<Message> msg = tryCatch.Message();
    args.GetReturnValue().Set(msg->GetSourceLine());
}

#undef SUITE
