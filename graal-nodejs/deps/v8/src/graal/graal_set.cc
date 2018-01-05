/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_set.h"

GraalSet::GraalSet(GraalIsolate* isolate, jobject java_set) : GraalObject(isolate, java_set) {
}

GraalHandleContent* GraalSet::CopyImpl(jobject java_object_copy) {
    return new GraalSet(Isolate(), java_object_copy);
}

bool GraalSet::IsSet() const {
    return true;
}
