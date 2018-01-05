/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_PRIMITIVE_H_
#define GRAAL_PRIMITIVE_H_

#include "graal_value.h"

class GraalIsolate;

class GraalPrimitive : public GraalValue {
public:
    GraalPrimitive(GraalIsolate* isolate, jobject java_object);
};

#endif /* GRAAL_PRIMITIVE_H_ */

