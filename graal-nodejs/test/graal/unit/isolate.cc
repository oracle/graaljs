/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#define SUITE Isolate

// Isolate::GetCurrent
// Isolate::Enter
// Isolate::Exit
// Isolate::GetData

EXPORT_TO_JS(BasicTest) {
    Isolate* isolate = Isolate::GetCurrent();
    if (isolate->GetData(0) != NULL) {
        Fail("Isolate->GetData() expected to return NULL");
    }

    isolate->Enter();
    isolate->Exit();

    if (isolate->GetData(0) != NULL) {
        Fail("Isolate->GetData() expected to return NULL");
    }
    args.GetReturnValue().Set(true);
}

#undef SUITE
