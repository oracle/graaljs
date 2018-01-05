/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_map.h"

GraalMap::GraalMap(GraalIsolate* isolate, jobject java_map) : GraalObject(isolate, java_map) {
}

GraalHandleContent* GraalMap::CopyImpl(jobject java_object_copy) {
    return new GraalMap(Isolate(), java_object_copy);
}

bool GraalMap::IsMap() const {
    return true;
}
