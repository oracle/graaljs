/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_isolate.h"
#include "graal_proxy.h"
#include "graal_value.h"

#include "graal_proxy-inl.h"

GraalHandleContent* GraalProxy::CopyImpl(jobject java_object_copy) {
    return new GraalProxy(Isolate(), java_object_copy);
}

bool GraalProxy::IsFunction() const {
    JNI_CALL(bool, result, Isolate(), GraalAccessMethod::proxy_is_function, Boolean, GetJavaObject());
    return result;
}

bool GraalProxy::IsProxy() const {
    return true;
}

v8::Local<v8::Value> GraalProxy::GetTarget() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_proxy = GetJavaObject();
    JNI_CALL(jobject, java_target, graal_isolate, GraalAccessMethod::proxy_get_target, Object, java_proxy);
    GraalValue* graal_target = GraalValue::FromJavaObject(graal_isolate, java_target);
    return reinterpret_cast<v8::Value*> (graal_target);
}

v8::Local<v8::Value> GraalProxy::GetHandler() {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_proxy = GetJavaObject();
    JNI_CALL(jobject, java_handler, graal_isolate, GraalAccessMethod::proxy_get_handler, Object, java_proxy);
    GraalValue* graal_handler = GraalValue::FromJavaObject(graal_isolate, java_handler);
    return reinterpret_cast<v8::Object*> (graal_handler);
}
