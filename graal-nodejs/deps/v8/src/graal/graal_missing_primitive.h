/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_MISSING_PRIMITIVE_H_
#define GRAAL_MISSING_PRIMITIVE_H_

#include "graal_primitive.h"

class GraalMissingPrimitive : public GraalPrimitive {
public:
    GraalMissingPrimitive(GraalIsolate* isolate, jobject java_object, bool undefined);
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
    bool IsNull() const override;
    bool IsUndefined() const override;
private:
    bool undefined_;
};

#endif /* GRAAL_MISSING_PRIMITIVE_H_ */
