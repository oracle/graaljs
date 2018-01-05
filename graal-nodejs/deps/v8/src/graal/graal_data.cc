/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_data.h"

GraalData::GraalData(GraalIsolate* isolate, jobject java_object) : GraalHandleContent(isolate, java_object) {
}
