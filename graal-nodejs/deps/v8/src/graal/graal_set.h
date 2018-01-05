/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_SET_H_
#define GRAAL_SET_H_

#include "graal_object.h"

class GraalSet : public GraalObject {
public:
    GraalSet(GraalIsolate* isolate, jobject java_set);
    bool IsSet() const override;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_SET_H_ */
