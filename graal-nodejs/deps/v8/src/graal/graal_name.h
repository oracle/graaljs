/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_NAME_H_
#define GRAAL_NAME_H_

#include "graal_primitive.h"

class GraalName : public GraalPrimitive {
public:
    GraalName(GraalIsolate* isolate, jobject java_name);
};

#endif /* GRAAL_NAME_H_ */

