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

#define SUITE GC

#ifdef SUITE_INTERNALS

void GC_InvokeGC(Isolate* isolate) {
    Local<Context> context = isolate->GetCurrentContext();
    Local<String> gc = String::NewFromUtf8(isolate, "gc()", v8::NewStringType::kNormal).ToLocalChecked();
    Script::Compile(context, gc).ToLocalChecked()->Run(context);
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
