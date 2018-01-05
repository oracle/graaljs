/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE String

#include <string.h>

// String::Length

EXPORT_TO_JS(Length) {
    args.GetReturnValue().Set(args[0].As<String>()->Length());
}

// String::Concat

EXPORT_TO_JS(Concat) {
    args.GetReturnValue().Set(String::Concat(args[0].As<String>(), args[1].As<String>()));
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
    args.GetReturnValue().Set(args[0].As<String>()->Utf8Length());
}

// String::Utf8Value

EXPORT_TO_JS(Utf8Value) {
    String::Utf8Value utf8(args[0]);
    args.GetReturnValue().Set(utf8.length());
}

EXPORT_TO_JS(Utf8ValueEmpty) {
    Local<String> empty;
    String::Utf8Value utf8(empty);
    bool result = utf8.length() == 0 && *utf8 == nullptr;
    args.GetReturnValue().Set(result);
}

// String::Write

EXPORT_TO_JS(CheckWrite) {
    Local<String> str = args[0].As<String>();
    uint16_t buffer[20];
    int writtenBytes = str->Write(buffer);
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
    Local<String> str = args[0].As<String>();
    char buffer[20];
    int writtenBytes = str->WriteOneByte((uint8_t*) buffer);
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
    Local<String> str = args[0].As<String>();
    char buffer[20];
    int writtenBytes = str->WriteUtf8(buffer);
    if (writtenBytes != 13) { //only WriteUtf8 counts the NULL terminator
        Fail("length not as expected");
    }
    if (strcmp(buffer, "abcABC123!$@") != 0) {
        Fail("content not as expected");
    }
    args.GetReturnValue().Set(true);
}

#undef SUITE
