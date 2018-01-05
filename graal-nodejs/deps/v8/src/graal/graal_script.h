/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_SCRIPT_H_
#define GRAAL_SCRIPT_H_

#include "graal_handle_content.h"
#include "include/v8.h"

class GraalScript : GraalHandleContent {
public:
    GraalScript(GraalIsolate* isolate, jobject java_script);
    static v8::Local<v8::Script> Compile(v8::Local<v8::String> source, v8::Local<v8::String> file_name);
    v8::Local<v8::Value> Run();
    v8::Local<v8::UnboundScript> GetUnboundScript();
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_SCRIPT_H_ */
