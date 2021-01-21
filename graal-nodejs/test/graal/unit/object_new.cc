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

#define SUITE ObjectNew

// RegExp::New

EXPORT_TO_JS(RegExp) {
    Isolate* isolate = args.GetIsolate();
    Local<String> pattern = args[0].As<String>();
    MaybeLocal<RegExp> re = RegExp::New(isolate->GetCurrentContext(), pattern, RegExp::Flags::kGlobal);
    if (re.IsEmpty()) {
        return;
    }
    args.GetReturnValue().Set(re.ToLocalChecked());
}

// Date::New

EXPORT_TO_JS(Date) {
    Isolate* isolate = args.GetIsolate();
    Local<Number> number = args[0].As<Number>();
    Local<Value> date = Date::New(isolate->GetCurrentContext(), number->Value()).ToLocalChecked();
    args.GetReturnValue().Set(date);
}

EXPORT_TO_JS(DateMaybe) {
    Isolate* isolate = args.GetIsolate();
    Local<Number> number = args[0].As<Number>();
    MaybeLocal<Value> date = Date::New(isolate->GetCurrentContext(), number->Value());
    if (date.IsEmpty()) {
        return;
    }
    args.GetReturnValue().Set(date.ToLocalChecked());
}

// BooleanObject::New

EXPORT_TO_JS(BooleanObject) {
    Isolate* isolate = args.GetIsolate();
    Local<Boolean> boolean = args[0].As<Boolean>();
    Local<Value> booleanObj = BooleanObject::New(isolate, boolean->Value());
    args.GetReturnValue().Set(booleanObj);
}

// StringObject::New

EXPORT_TO_JS(StringObject) {
    Isolate* isolate = args.GetIsolate();
    Local<String> value = args[0].As<String>();
    Local<Value> stringObj = StringObject::New(isolate, value);
    args.GetReturnValue().Set(stringObj);
}

// NumberObject::New

EXPORT_TO_JS(NumberObject) {
    Isolate* isolate = args.GetIsolate();
    Local<Number> number = args[0].As<Number>();
    Local<Value> numberObj = NumberObject::New(isolate, number->Value());
    args.GetReturnValue().Set(numberObj);
}

#undef SUITE
