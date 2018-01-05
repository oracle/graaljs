/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Context

// Context::Global

EXPORT_TO_JS(Global) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> ctx = isolate->GetCurrentContext();
    args.GetReturnValue().Set(ctx->Global());
}

// Context::GetCurrent
// Context::Enter
// Context::Exit
// Context::New

EXPORT_TO_JS(EnterAndExitNewContext) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> startCtx = isolate->GetCurrentContext();
    Local<Context> newCtx = Context::New(isolate);

    newCtx->Enter();
    Local<Context> currentCtx1 = isolate->GetCurrentContext();
    if (currentCtx1 == startCtx) {
        //after Enter(), we should be in a different context
        args.GetReturnValue().Set(false);
    }

    newCtx->Exit();
    Local<Context> currentCtx2 = isolate->GetCurrentContext();
    if (currentCtx2 != startCtx) {
        //after Exit(), we should be back in the startCtx
        args.GetReturnValue().Set(false);
    }

    args.GetReturnValue().Set(true);
}

// Context::GetSecurityToken

EXPORT_TO_JS(GetSecurityToken) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> ctx = isolate->GetCurrentContext();
    args.GetReturnValue().Set(ctx->GetSecurityToken());
}

// Context::SetSecurityToken

EXPORT_TO_JS(SetSecurityToken) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> ctx = isolate->GetCurrentContext();
    ctx->SetSecurityToken(args[0]);
    args.GetReturnValue().Set(true);
}

// Context::UseDefaultSecurityToken

EXPORT_TO_JS(UseDefaultSecurityToken) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> ctx = isolate->GetCurrentContext();
    ctx->UseDefaultSecurityToken();
    args.GetReturnValue().Set(true);
}

// Context::New

EXPORT_TO_JS(New) {
    Isolate* isolate = args.GetIsolate();
    Local<ObjectTemplate> templ = ObjectTemplate::New(isolate);
    Local<String> key = String::NewFromUtf8(isolate, "foo");
    int secret = 42;
    templ->Set(key, Integer::New(isolate, secret));
    Local<Context> context = Context::New(isolate, nullptr, templ, Local<Value>());

    context->Enter();

    Local<Object> global = context->Global();
    Local<Value> value = global->Get(key);
    bool result = value->IsNumber() && value.As<Number>()->Value() == secret;

    context->Exit();

    args.GetReturnValue().Set(result);
}

#undef SUITE
