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

#define SUITE TryCatch

#ifdef SUITE_INTERNALS

void TryCatch_InvokeCallback(const FunctionCallbackInfo<Value>& args) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    Local<Value> callback = args[0];
    Local<Function>::Cast(callback)->Call(context, Object::New(isolate), 0, nullptr);
}

void TryCatch_ReThrowNestedHelper(const FunctionCallbackInfo<Value>& args, int depth) {
    if (depth == 0) {
        TryCatch tryCatch(args.GetIsolate());
        TryCatch_ReThrowNestedHelper(args, depth - 1);
        tryCatch.ReThrow();
    } else {
        TryCatch_InvokeCallback(args);
    }
}

#endif

// TryCatch::HasCaught

EXPORT_TO_JS(HasCaughtNoException) {
    TryCatch tryCatch(args.GetIsolate());
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtNativeException) {
    Isolate* isolate = args.GetIsolate();
    TryCatch tryCatch(isolate);
    isolate->ThrowException(args[0]);
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtJSException) {
    TryCatch tryCatch(args.GetIsolate());
    TryCatch_InvokeCallback(args);
    bool result = tryCatch.HasCaught();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasCaughtNestedOuter) {
    Isolate* isolate = args.GetIsolate();
    bool ok = true;
    TryCatch tryCatch(isolate);
    TryCatch_InvokeCallback(args);
    {
        TryCatch innerCatch(isolate);
        ok &= !innerCatch.HasCaught();
    }
    ok &= tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}


EXPORT_TO_JS(HasCaughtNestedInner) {
    Isolate* isolate = args.GetIsolate();
    bool ok = true;
    TryCatch tryCatch(isolate);
    {
        TryCatch innerCatch(isolate);
        TryCatch_InvokeCallback(args);
        ok &= innerCatch.HasCaught();
    }
    ok &= !tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}

EXPORT_TO_JS(HasCaughtNestedBoth) {
    Isolate* isolate = args.GetIsolate();
    bool ok = true;
    TryCatch tryCatch(isolate);
    TryCatch_InvokeCallback(args);
    {
        TryCatch innerCatch(isolate);
        TryCatch_InvokeCallback(args);
        ok &= innerCatch.HasCaught();
    }
    ok &= tryCatch.HasCaught();
    args.GetReturnValue().Set(ok);
}

// TryCatch::HasTerminated

EXPORT_TO_JS(HasTerminatedNoException) {
    TryCatch tryCatch(args.GetIsolate());
    bool result = tryCatch.HasTerminated();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasTerminatedNestedOuter) {
    Isolate* isolate = args.GetIsolate();
    bool result = true;
    TryCatch tryCatch(isolate);
    result &= !tryCatch.HasTerminated();
    TryCatch_InvokeCallback(args);
    result &= tryCatch.HasTerminated();
    isolate->CancelTerminateExecution();
    result &= !tryCatch.HasTerminated();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(HasTerminatedNestedInner) {
    Isolate* isolate = args.GetIsolate();
    // The following TryCatch looks unused but we are testing
    // that it does not swallow the termination exception
    TryCatch tryCatch(isolate);
    isolate->TerminateExecution();
    TryCatch_InvokeCallback(args);
}

EXPORT_TO_JS(HasTerminatedBasic) {
    Isolate* isolate = args.GetIsolate();
    bool result = true;
    TryCatch tryCatch(isolate);
    result &= !tryCatch.HasTerminated();
    isolate->TerminateExecution();
    result &= !tryCatch.HasTerminated();
    TryCatch_InvokeCallback(args);
    result &= tryCatch.HasTerminated();
    isolate->CancelTerminateExecution();
    result &= !tryCatch.HasTerminated();
    args.GetReturnValue().Set(result);
}

// TryCatch::Exception

EXPORT_TO_JS(ExceptionForNoExceptionIsEmpty) {
    TryCatch tryCatch(args.GetIsolate());
    bool result = tryCatch.Exception().IsEmpty();
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(ExceptionForThrownException) {
    TryCatch tryCatch(args.GetIsolate());
    TryCatch_InvokeCallback(args);
    args.GetReturnValue().Set(tryCatch.Exception());
}

// TryCatch::ReThrow

EXPORT_TO_JS(ReThrowException) {
    TryCatch tryCatch(args.GetIsolate());
    TryCatch_InvokeCallback(args);
    tryCatch.ReThrow();
}

EXPORT_TO_JS(ReThrowNested) {
    TryCatch tryCatch(args.GetIsolate());
    TryCatch_ReThrowNestedHelper(args, 3);
    args.GetReturnValue().Set(tryCatch.Exception());
}

// TryCatch::SetVerbose

EXPORT_TO_JS(SetVerbose) {
    TryCatch tryCatch(args.GetIsolate());
    bool b = args[0]->IsTrue();
    tryCatch.SetVerbose(b);
    args.GetReturnValue().Set(true);
}

// TryCatch::Message

EXPORT_TO_JS(MessageGetSourceLine) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    TryCatch tryCatch(isolate);
    TryCatch_InvokeCallback(args);
    Local<Message> msg = tryCatch.Message();
    args.GetReturnValue().Set(msg->GetSourceLine(context).ToLocalChecked());
}

// TryCatch::Reset

EXPORT_TO_JS(Reset) {
    TryCatch tryCatch(args.GetIsolate());
    TryCatch_InvokeCallback(args);
    tryCatch.Reset();
    args.GetReturnValue().Set(tryCatch.HasCaught());
}

EXPORT_TO_JS(StackTrace) {
    Isolate* isolate = args.GetIsolate();
    Local<Context> context = isolate->GetCurrentContext();
    TryCatch tryCatch(isolate);
    TryCatch_InvokeCallback(args);
    MaybeLocal<Value> stack_trace = tryCatch.StackTrace(context);
    args.GetReturnValue().Set(stack_trace.IsEmpty() ? args[1] : stack_trace.ToLocalChecked());
}

#undef SUITE
