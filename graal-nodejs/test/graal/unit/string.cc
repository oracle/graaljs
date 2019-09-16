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

#define SUITE String

#include <string.h>

// String::Length

EXPORT_TO_JS(Length) {
    args.GetReturnValue().Set(args[0].As<String>()->Length());
}

// String::Concat

EXPORT_TO_JS(Concat) {
    args.GetReturnValue().Set(String::Concat(args.GetIsolate(), args[0].As<String>(), args[1].As<String>()));
}

// String::IsExternal

EXPORT_TO_JS(IsExternal) {
    args.GetReturnValue().Set(args[0].As<String>()->IsExternal());
}

// String::IsExternalOneByte

EXPORT_TO_JS(IsExternalOneByte) {
    bool result = args[0].As<String>()->IsExternalOneByte();
    args.GetReturnValue().Set(result);
}

// String::IsOneByte

EXPORT_TO_JS(IsOneByte) {
    bool result = args[0].As<String>()->IsOneByte();
    args.GetReturnValue().Set(result);
}

// String::Utf8Length

EXPORT_TO_JS(Utf8Length) {
    args.GetReturnValue().Set(args[0].As<String>()->Utf8Length(args.GetIsolate()));
}

// String::Utf8Value

EXPORT_TO_JS(Utf8Value) {
    String::Utf8Value utf8(args.GetIsolate(), args[0]);
    args.GetReturnValue().Set(utf8.length());
}

EXPORT_TO_JS(Utf8ValueEmpty) {
    Local<String> empty;
    String::Utf8Value utf8(args.GetIsolate(), empty);
    bool result = utf8.length() == 0 && *utf8 == nullptr;
    args.GetReturnValue().Set(result);
}

// String::Write

EXPORT_TO_JS(CheckWrite) {
    Isolate* isolate = args.GetIsolate();
    Local<String> str = args[0].As<String>();
    uint16_t buffer[20];
    int writtenBytes = str->Write(isolate, buffer);
    if (writtenBytes != 12) {
        Fail("length not as expected");
    }
    if (buffer[0] != 'a' || buffer[11] != '@') {
        Fail("content not as expected");
    }
    if (buffer[12] != 0) {
        Fail("NULL termination missing");
    }
    args.GetReturnValue().Set(true);
}

// String::WriteOneByte

EXPORT_TO_JS(CheckWriteOneByte) {
    Isolate* isolate = args.GetIsolate();
    Local<String> str = args[0].As<String>();
    char buffer[20];
    int writtenBytes = str->WriteOneByte(isolate, (uint8_t*) buffer);
    if (writtenBytes != 12) {
        Fail("length not as expected");
    }
    if (strcmp(buffer, "abcABC123!$@") != 0) {
        Fail("content not as expected");
    }
    args.GetReturnValue().Set(true);
}

// String::WriteUtf8

EXPORT_TO_JS(CheckWriteUtf8) {
    Isolate* isolate = args.GetIsolate();
    Local<String> str = args[0].As<String>();
    char buffer[20];
    int writtenBytes = str->WriteUtf8(isolate, buffer);
    if (writtenBytes != 13) { //only WriteUtf8 counts the NULL terminator
        Fail("length not as expected");
    }
    if (strcmp(buffer, "abcABC123!$@") != 0) {
        Fail("content not as expected");
    }
    args.GetReturnValue().Set(true);
}

#undef SUITE
