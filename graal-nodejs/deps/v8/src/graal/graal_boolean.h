/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_BOOLEAN_H_
#define GRAAL_BOOLEAN_H_

#include "graal_primitive.h"

class GraalIsolate;

class GraalBoolean : public GraalPrimitive {
public:
    GraalBoolean(GraalIsolate* isolate, bool value);
    GraalBoolean(GraalIsolate* isolate, bool value, jobject java_value);
    bool IsBoolean() const;
    bool IsTrue() const;
    bool IsFalse() const;
    bool Value() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    bool value_;
};

#endif /* GRAAL_BOOLEAN_H_ */

