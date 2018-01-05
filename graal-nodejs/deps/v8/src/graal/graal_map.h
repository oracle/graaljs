/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_MAP_H_
#define GRAAL_MAP_H_

#include "graal_object.h"

class GraalMap : public GraalObject {
public:
    GraalMap(GraalIsolate* isolate, jobject java_map);
    bool IsMap() const override;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_MAP_H_ */
