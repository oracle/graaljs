/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <string.h>

#define SUITE V8Obj

#ifdef SUITE_INTERNALS

bool EntropySourceFunction(unsigned char* buffer, unsigned long size) {
    for (unsigned long i = 0; i < size; i++) {
        buffer[i] = 'a';
    }
    return true;
}

#endif

// V8::Initialize

EXPORT_TO_JS(Initialize) {
    args.GetReturnValue().Set(V8::Initialize());
}

// V8::Dispose

EXPORT_TO_JS(Dispose) {
    args.GetReturnValue().Set(V8::Dispose());
}

// V8::SetFlagsFromString

EXPORT_TO_JS(SetFlagsFromString) {
    const char* str = "hello";
    V8::SetFlagsFromString(str, strlen(str));
    args.GetReturnValue().Set(true);
}

// V8::GetVersion

EXPORT_TO_JS(GetVersion) {
    args.GetReturnValue().Set(String::NewFromUtf8(args.GetIsolate(), V8::GetVersion()));
}

// V8::SetEntropySource

EXPORT_TO_JS(SetEntropySource) {
    V8::SetEntropySource(EntropySourceFunction);
    args.GetReturnValue().Set(true);
}

// V8::SetFlagsFromCommandLine

EXPORT_TO_JS(SetFlagsFromCommandLine) {
    char const *argv[3] = {"one", "two", "three"};
    int argc = 3;
    V8::SetFlagsFromCommandLine(&argc, (char**) argv, false);
    args.GetReturnValue().Set(true);
}

// ResourceConstraints::ResourceConstraints

EXPORT_TO_JS(ResourceConstraints) {
    ResourceConstraints* rc = new ResourceConstraints();

    rc->set_max_semi_space_size(1234);
    rc->set_max_old_space_size(2345);
    rc->set_max_executable_size(3456);

    if (rc->max_semi_space_size() != 1234) {
        Fail("get max_semi_space_size");
    }
    if (rc->max_old_space_size() != 2345) {
        Fail("get max_old_space_size");
    }
    if (rc->max_executable_size() != 3456) {
        Fail("get max_executable_size");
    }

    delete rc;
    args.GetReturnValue().Set(true);
}


#undef SUITE
