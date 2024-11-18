/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_wasm_streaming.h"

#include "graal_function-inl.h"

namespace v8 {

    WasmStreaming::WasmStreamingImpl::WasmStreamingImpl(GraalIsolate* isolate, jobject resolve, jobject reject) : isolate_(isolate) {
        JNIEnv* env = isolate->GetJNIEnv();
        resolve_ = env->NewGlobalRef(resolve);
        reject_ = env->NewGlobalRef(reject);
    }

    WasmStreaming::WasmStreamingImpl::~WasmStreamingImpl() {
        JNIEnv* env = isolate_->GetJNIEnv();
        env->DeleteGlobalRef(resolve_);
        env->DeleteGlobalRef(reject_);
    }

    void WasmStreaming::WasmStreamingImpl::Finish(bool can_use_compiled_module) {
        Isolate* v8_isolate = reinterpret_cast<Isolate*> (isolate_);

        Local<ArrayBuffer> array_buffer = ArrayBuffer::New(v8_isolate, bytes_.size());
        std::shared_ptr<BackingStore> backing_store = array_buffer->GetBackingStore();
        memcpy(backing_store->Data(), bytes_.data(), bytes_.size());

        JNIEnv* env = isolate_->GetJNIEnv();
        Local<Value> thiz = v8::Undefined(v8_isolate);
        Local<Value> arg = array_buffer.As<Value>();
        GraalFunction::Allocate(isolate_, env->NewLocalRef(resolve_))->Call(thiz, 1, &arg);
    }

    void WasmStreaming::WasmStreamingImpl::OnBytesReceived(const uint8_t* bytes, size_t size) {
        bytes_.insert(bytes_.end(), bytes, bytes+size);
    }

    void WasmStreaming::WasmStreamingImpl::Abort(MaybeLocal<Value> exception) {
        if (!exception.IsEmpty()) {
            Local<Value> exc = exception.ToLocalChecked();
            JNIEnv* env = isolate_->GetJNIEnv();
            Isolate* v8_isolate = reinterpret_cast<Isolate*> (isolate_);
            Local<Value> thiz = v8::Undefined(v8_isolate);
            GraalFunction::Allocate(isolate_, env->NewLocalRef(reject_))->Call(thiz, 1, &exc);
        }
    }
}
