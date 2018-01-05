/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_missing_primitive.h"

GraalMissingPrimitive::GraalMissingPrimitive(GraalIsolate* isolate, jobject java_object, bool undefined) : GraalPrimitive(isolate, java_object), undefined_(undefined) {
}

GraalHandleContent* GraalMissingPrimitive::CopyImpl(jobject java_object_copy) {
    return new GraalMissingPrimitive(Isolate(), java_object_copy, undefined_);
}

bool GraalMissingPrimitive::IsNull() const {
    return !undefined_;
}

bool GraalMissingPrimitive::IsUndefined() const {
    return undefined_;
}
