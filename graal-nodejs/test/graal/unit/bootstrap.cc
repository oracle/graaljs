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

#define SUITE Bootstrap

EXPORT_TO_JS(Undefined) {
    args.GetReturnValue().SetUndefined();
}

EXPORT_TO_JS(Null) {
    args.GetReturnValue().SetNull();
}

EXPORT_TO_JS(True) {
    args.GetReturnValue().Set(true);
}

EXPORT_TO_JS(False) {
    args.GetReturnValue().Set(false);
}

EXPORT_TO_JS(Int32) {
    int32_t value = 211;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(Uint32) {
    uint32_t value = 3000000000;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(Double) {
    double value = 3.14;
    args.GetReturnValue().Set(value);
}

EXPORT_TO_JS(GetNotSet) {
    args.GetReturnValue().Set(args.GetReturnValue().Get());
}

EXPORT_TO_JS(GetObject) {
    Local<Object> value = Object::New(args.GetIsolate());
    args.GetReturnValue().Set(value == args.GetReturnValue().Get());
}

EXPORT_TO_JS(GetInt32) {
    int32_t number = 42;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

EXPORT_TO_JS(GetUint32) {
    uint32_t number = 4000000000;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

EXPORT_TO_JS(GetDouble) {
    double number = 2.71828;
    args.GetReturnValue().Set(number);
    Local<Value> value = args.GetReturnValue().Get();
    args.GetReturnValue().Set(value->IsNumber() && value.As<Number>()->Value() == number);
}

#undef SUITE
