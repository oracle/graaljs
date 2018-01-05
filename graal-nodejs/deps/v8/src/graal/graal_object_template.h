/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_OBJECT_TEMPLATE_H_
#define GRAAL_OBJECT_TEMPLATE_H_

#include "graal_template.h"

class GraalObjectTemplate : public GraalTemplate {
public:
    static v8::Local<v8::ObjectTemplate> New(v8::Isolate* isolate, v8::Local<v8::FunctionTemplate> constructor);
    GraalObjectTemplate(GraalIsolate* isolate, jobject java_template);
    v8::Local<v8::Object> NewInstance(v8::Local<v8::Context> context);
    void SetInternalFieldCount(int);
    void SetAccessor(
            v8::Local<v8::String> name,
            v8::AccessorGetterCallback getter,
            v8::AccessorSetterCallback setter,
            v8::Local<v8::Value> data,
            v8::AccessControl settings,
            v8::PropertyAttribute attribute,
            v8::Local<v8::AccessorSignature> signature);
    void SetNamedPropertyHandler(v8::NamedPropertyGetterCallback getter,
            v8::NamedPropertySetterCallback setter,
            v8::NamedPropertyQueryCallback query,
            v8::NamedPropertyDeleterCallback deleter,
            v8::NamedPropertyEnumeratorCallback enumerator,
            v8::Local<v8::Value> data);
    void SetHandler(const v8::NamedPropertyHandlerConfiguration& configuration);
    void SetHandler(const v8::IndexedPropertyHandlerConfiguration& configuration);
    void SetCallAsFunctionHandler(v8::FunctionCallback callback, v8::Local<v8::Value> data);

    inline int InternalFieldCount() {
        return internal_field_count_;
    }
protected:
    GraalHandleContent* CopyImpl(jobject java_object_copy);
private:
    int internal_field_count_;
};

#endif /* GRAAL_OBJECT_TEMPLATE_H_ */

