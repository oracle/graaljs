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

EXPORT_TO_JS(IndexOfEnvironment) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    int env_index = -1;
    for (int i = 0; i <= 128; i++) {
        void* pointer = context->GetAlignedPointerFromEmbedderData(i);
        // There is not a clean an reliable way to get the actual Environment
        // pointer (as it is an implementation detail of Node.js).
        // So, we look for non-null pointer only. It works in the current
        // implementation and this test serves as a guard for implementation
        // changes only anyway.
        if (pointer != nullptr) {
            env_index = i;
            break;
        }
    }
    args.GetReturnValue().Set(env_index);
}

#undef SUITE
