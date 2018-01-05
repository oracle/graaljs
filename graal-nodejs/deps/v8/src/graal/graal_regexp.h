/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_REGEXP_H_
#define GRAAL_REGEXP_H_

#include "graal_object.h"

class GraalRegExp : public GraalObject {
public:
    GraalRegExp(GraalIsolate* isolate, jobject java_regexp);
    bool IsRegExp() const;
    v8::Local<v8::String> GetSource() const;
    v8::RegExp::Flags GetFlags() const;
    static v8::Local<v8::RegExp> New(v8::Local<v8::Context> context, v8::Local<v8::String> pattern, v8::RegExp::Flags flags);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_REGEXP_H_ */
