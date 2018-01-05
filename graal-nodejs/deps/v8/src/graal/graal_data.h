/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_DATA_H_
#define GRAAL_DATA_H_

#include "graal_handle_content.h"

class GraalIsolate;

class GraalData : public GraalHandleContent {
public:
    GraalData(GraalIsolate* isolate, jobject java_object);
};

#endif /* GRAAL_DATA_H_ */
