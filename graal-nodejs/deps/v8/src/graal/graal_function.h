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

#ifndef GRAAL_FUNCTION_H_
#define GRAAL_FUNCTION_H_

#include "graal_object.h"

class _jobjectArray;
typedef _jobjectArray *jobjectArray;

class GraalFunction : public GraalObject {
public:
    GraalFunction(GraalIsolate* isolate, jobject java_function);
    bool IsFunction() const;
    v8::Local<v8::Object> NewInstance(int argc, v8::Local<v8::Value> argv[]) const;
    void SetName(v8::Local<v8::String> name);
    v8::Local<v8::Value> GetName() const;
    v8::Local<v8::Value> Call(v8::Local<v8::Value> recv, int argc, v8::Local<v8::Value> argv[]);
    jobject Call(jobject recv, int argc, jobject argv[]);
    v8::ScriptOrigin GetScriptOrigin() const;
    int GetScriptLineNumber() const;
    int GetScriptColumnNumber() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    jobject Call(jobject recv, jobjectArray arguments);
    jobject Call0(jobject recv);
    jobject Call1(jobject recv, jobject arg0);
    jobject Call2(jobject recv, jobject arg0, jobject arg1);
    jobject Call3(jobject recv, jobject arg0, jobject arg1, jobject arg2);
    jobject Call4(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3);
    jobject Call5(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4);
    jobject Call6(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5);
    jobject Call7(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6);
    jobject Call8(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7);
    jobject Call9(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8);
    jobject CallResult(jobject java_object);
    jobjectArray CreateJavaObjectArray(int argc, v8::Local<v8::Value> argv[]) const;
};

#endif /* GRAAL_FUNCTION_H_ */

