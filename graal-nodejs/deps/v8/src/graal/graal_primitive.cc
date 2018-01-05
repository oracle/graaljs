/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_primitive.h"

GraalPrimitive::GraalPrimitive(GraalIsolate* isolate, jobject java_object) : GraalValue(isolate, java_object) {
}

