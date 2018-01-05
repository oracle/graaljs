/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "graal_isolate.h"
#include "graal_string.h"
#include "graal_symbol.h"

GraalSymbol::GraalSymbol(GraalIsolate* isolate, jobject java_symbol) : GraalName(isolate, java_symbol) {
}

GraalHandleContent* GraalSymbol::CopyImpl(jobject java_object_copy) {
    return new GraalSymbol(Isolate(), java_object_copy);
}

v8::Local<v8::Symbol> GraalSymbol::New(v8::Isolate* isolate, v8::Local<v8::String> name) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_name = reinterpret_cast<GraalString*> (*name);
    jobject java_name = graal_name->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_new, Object, java_name);
    GraalSymbol* graal_symbol = new GraalSymbol(graal_isolate, java_symbol);
    return reinterpret_cast<v8::Symbol*> (graal_symbol);
}

bool GraalSymbol::IsSymbol() const {
    return true;
}

bool GraalSymbol::IsName() const {
    return true;
}
