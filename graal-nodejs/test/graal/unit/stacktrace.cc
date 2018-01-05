/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <string.h>

#define SUITE StackTrace

#ifdef SUITE_INTERNALS

void TryCatch_InvokeCallback(const FunctionCallbackInfo<Value>& args);

Local<StackTrace> createStackTrace(const FunctionCallbackInfo<Value>& args) {
    V8::SetCaptureStackTraceForUncaughtExceptions(true, 100);

    TryCatch tryCatch;
    TryCatch_InvokeCallback(args);
    Local<Message> message = tryCatch.Message();
    Local<StackTrace> trace = message->GetStackTrace();
    return trace;
}

#endif

// StackTrace::GetFrameCount

EXPORT_TO_JS(GetFrameCount) {
    Local<StackTrace> trace = createStackTrace(args);
    if (trace.IsEmpty()) {
        args.GetReturnValue().Set(-1);
    } else {
        args.GetReturnValue().Set(trace->GetFrameCount());
    }
}

// StackTrace::GetFrame

EXPORT_TO_JS(CanGetFrame) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(!frame.IsEmpty());
}

// StackFrame::GetColumn

EXPORT_TO_JS(FrameGetColumn) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(frame->GetColumn());
}

// StackFrame::GetLineNumber

EXPORT_TO_JS(FrameGetLineNumber) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(frame->GetLineNumber());
}

// StackFrame::GetFunctionName

EXPORT_TO_JS(FrameGetFunctionName) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(frame->GetFunctionName());
}

// StackFrame::GetScriptName

EXPORT_TO_JS(FrameGetScriptName) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(frame->GetScriptName());
}

// StackFrame::GetScriptId

EXPORT_TO_JS(FrameGetScriptId) {
    Local<StackTrace> trace = createStackTrace(args);
    Local<StackFrame> frame = trace->GetFrame(0);
    //int id = frame->GetScriptId(); //TODO not available currently
    int id = -frame->GetLineNumber(); //negative number, ensures we fail
    args.GetReturnValue().Set(id);
}

// StackFrame::IsEval

EXPORT_TO_JS(FrameIsEval) {
    Local<StackTrace> trace = createStackTrace(args);

    //for (int i=0;i<trace->GetFrameCount();i++) {
    //    Local<StackFrame> frm = trace->GetFrame(i);
    //    printf("frame %d is eval %d\n",i,frm->IsEval());
    //}

    Local<StackFrame> frame = trace->GetFrame(0);
    args.GetReturnValue().Set(frame->IsEval());
}

#undef SUITE
