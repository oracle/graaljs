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

#ifndef GRAAL_CONTEXT_H_
#define GRAAL_CONTEXT_H_

#include "graal_handle_content.h"
#include "graal_isolate.h"
#include "include/v8.h"
#include <vector>

class GraalContext : public GraalHandleContent {
public:
    GraalContext(GraalIsolate* isolate, jobject java_context, void* cached_context_embedder_data = nullptr);
    v8::Local<v8::Object> Global();
    void SetAlignedPointerInEmbedderData(int index, void* value);
    void* SlowGetAlignedPointerFromEmbedderData(int index);
    void SetEmbedderData(int index, v8::Local<v8::Value> value);
    v8::Local<v8::Value> SlowGetEmbedderData(int index);
    void SetSecurityToken(v8::Local<v8::Value> token);
    void UseDefaultSecurityToken();
    v8::Local<v8::Value> GetSecurityToken();

    inline v8::Isolate* GetIsolate() {
        return reinterpret_cast<v8::Isolate*> (Isolate());
    }

    inline void Enter() {
        Isolate()->ContextEnter(reinterpret_cast<v8::Context*> (this));
    }

    inline void Exit() {
        Isolate()->ContextExit(reinterpret_cast<v8::Context*> (this));
    }
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;

private:
    void* cached_context_embedder_data_;
};

#endif /* GRAAL_CONTEXT_H_ */
