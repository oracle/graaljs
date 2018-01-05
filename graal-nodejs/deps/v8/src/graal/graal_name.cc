/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_name.h"

GraalName::GraalName(GraalIsolate* isolate, jobject java_name) : GraalPrimitive(isolate, java_name) {
}
