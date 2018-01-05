/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_DATE_H_
#define GRAAL_DATE_H_

#include "graal_object.h"

class GraalIsolate;

class GraalDate : public GraalObject {
public:
    GraalDate(GraalIsolate* isolate, double time, jobject java_date);
    static v8::Local<v8::Value> New(v8::Isolate* isolate, double time);
    double ValueOf() const;
    bool IsDate() const;
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy) override;
private:
    double time_;
};

#endif /* GRAAL_DATE_H_ */
