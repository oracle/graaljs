/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_SYMBOL_H_
#define GRAAL_SYMBOL_H_

#include "graal_name.h"

class GraalSymbol : public GraalName {
public:
    GraalSymbol(GraalIsolate* isolate, jobject java_symbol);
    static v8::Local<v8::Symbol> New(v8::Isolate* isolate, v8::Local<v8::String> name);
    bool IsSymbol() const override;
    bool IsName() const override;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_SYMBOL_H_ */

