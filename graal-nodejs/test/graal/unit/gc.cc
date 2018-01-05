/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE GC

#ifdef SUITE_INTERNALS

void GC_InvokeGC(Isolate* isolate) {
    Script::Compile(String::NewFromUtf8(isolate, "gc()"))->Run();
}

static int callback1_invocations;
static int callback2_invocations;

void GC_Callback1(Isolate* isolate, GCType type, GCCallbackFlags flags) {
    callback1_invocations++;
}

void GC_Callback2(Isolate* isolate, GCType type, GCCallbackFlags flags) {
    callback2_invocations++;
}

#endif

EXPORT_TO_JS(AddGCPrologueCallbackTest) {
    bool result = true;
    Isolate* isolate = args.GetIsolate();
    isolate->AddGCPrologueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    isolate->AddGCPrologueCallback(GC_Callback2);
    callback1_invocations = 0;
    callback2_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    result &= (callback2_invocations >= 1);
    isolate->RemoveGCPrologueCallback(GC_Callback1);
    isolate->RemoveGCPrologueCallback(GC_Callback2);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(AddGCEpilogueCallbackTest) {
    bool result = true;
    Isolate* isolate = args.GetIsolate();
    isolate->AddGCEpilogueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    isolate->AddGCEpilogueCallback(GC_Callback2);
    callback1_invocations = 0;
    callback2_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    result &= (callback2_invocations >= 1);
    isolate->RemoveGCEpilogueCallback(GC_Callback1);
    isolate->RemoveGCEpilogueCallback(GC_Callback2);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(AddDoubleGCCallbackTest) {
    bool result = true;
    Isolate* isolate = args.GetIsolate();
    isolate->AddGCPrologueCallback(GC_Callback1);
    isolate->AddGCEpilogueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 2);
    isolate->RemoveGCPrologueCallback(GC_Callback1);
    isolate->RemoveGCEpilogueCallback(GC_Callback1);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(RemoveGCPrologueCallbackTest) {
    bool result = true;
    Isolate* isolate = args.GetIsolate();
    isolate->AddGCPrologueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    isolate->RemoveGCPrologueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations == 0);
    args.GetReturnValue().Set(result);
}

EXPORT_TO_JS(RemoveGCEpilogueCallbackTest) {
    bool result = true;
    Isolate* isolate = args.GetIsolate();
    isolate->AddGCEpilogueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations >= 1);
    isolate->RemoveGCEpilogueCallback(GC_Callback1);
    callback1_invocations = 0;
    GC_InvokeGC(isolate);
    result &= (callback1_invocations == 0);
    args.GetReturnValue().Set(result);
}

#undef SUITE
