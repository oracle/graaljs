/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_EXTERNAL_H_
#define GRAAL_EXTERNAL_H_

#include "graal_value.h"

class GraalExternal : public GraalValue {
public:
    GraalExternal(GraalIsolate* isolate, void* value, jobject java_external);
    static v8::Local<v8::External> New(v8::Isolate* isolate, void* value);
    bool IsExternal() const;
    bool IsObject() const;

    inline void* Value() const {
        return value_;
    }
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    void* value_;
};

#endif /* GRAAL_EXTERNAL_H_ */

