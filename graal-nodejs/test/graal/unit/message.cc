/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <string.h>

#define SUITE Message

#ifdef SUITE_INTERNALS

void TryCatch_InvokeCallback(const FunctionCallbackInfo<Value>& args);

Local<Message> createMessage(const FunctionCallbackInfo<Value>& args) {
    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    Local<Message> message = tryCatch.Message();
    return message;
}

#endif

// Message::GetStartColumn

EXPORT_TO_JS(GetStartColumn) {
    Local<Message> message = createMessage(args);
    args.GetReturnValue().Set(message->GetStartColumn());
}

// Message::GetEndColumn

EXPORT_TO_JS(GetEndColumn) {
    Local<Message> message = createMessage(args);
    args.GetReturnValue().Set(message->GetEndColumn());
}

// Message::GetLineNumber

EXPORT_TO_JS(GetLineNumber) {
    Local<Message> message = createMessage(args);
    args.GetReturnValue().Set(message->GetLineNumber());
}

// Message::GetSourceLine

EXPORT_TO_JS(GetSourceLine) {
    Local<Message> message = createMessage(args);
    args.GetReturnValue().Set(message->GetSourceLine());
}

// Message::GetScriptResourceName

EXPORT_TO_JS(GetScriptResourceName) {
    Local<Message> message = createMessage(args);
    args.GetReturnValue().Set(message->GetScriptResourceName());
}

#undef SUITE
