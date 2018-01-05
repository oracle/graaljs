/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_PROXY_H_
#define GRAAL_PROXY_H_

#include "graal_object.h"

class GraalProxy : public GraalObject {
public:
    GraalProxy(GraalIsolate* isolate, jobject java_proxy);
    bool IsProxy() const override;
    bool IsFunction() const override;
    v8::Local<v8::Object> GetTarget();
    v8::Local<v8::Value> GetHandler();

protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
};

#endif /* GRAAL_PROXY_H_ */
