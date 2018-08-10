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
