/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include "graal_isolate.h"
#include "graal_string.h"
#include "graal_symbol.h"

#include "graal_symbol-inl.h"

GraalHandleContent* GraalSymbol::CopyImpl(jobject java_object_copy) {
    return GraalSymbol::Allocate(Isolate(), java_object_copy);
}

v8::Local<v8::Symbol> GraalSymbol::New(v8::Isolate* isolate, v8::Local<v8::String> name) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_name = reinterpret_cast<GraalString*> (*name);
    jobject java_name = graal_name->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_new, Object, java_name);
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol);
    v8::Symbol* v8_symbol = reinterpret_cast<v8::Symbol*> (graal_symbol);
    return v8::Local<v8::Symbol>::New(isolate, v8_symbol);
}

v8::Local<v8::Private> GraalSymbol::NewPrivate(v8::Isolate* isolate, v8::Local<v8::String> description) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_description = reinterpret_cast<GraalString*> (*description);
    jobject java_description = graal_description->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_private_new, Object, java_description);
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol);
    v8::Private* v8_symbol = reinterpret_cast<v8::Private*> (graal_symbol);
    return v8::Local<v8::Private>::New(isolate, v8_symbol);
}

#define SYMBOL_GETTER(getter, method_id) \
v8::Local<v8::Symbol> GraalSymbol::getter(v8::Isolate* isolate) { \
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate); \
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::method_id, Object); \
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol); \
    v8::Symbol* v8_symbol = reinterpret_cast<v8::Symbol*> (graal_symbol); \
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate); \
    return v8::Local<v8::Symbol>::New(v8_isolate, v8_symbol); \
}

SYMBOL_GETTER(GetAsyncIterator, symbol_get_async_iterator)
SYMBOL_GETTER(GetHasInstance, symbol_get_has_instance)
SYMBOL_GETTER(GetIsConcatSpreadable, symbol_get_is_concat_spreadable)
SYMBOL_GETTER(GetIterator, symbol_get_iterator)
SYMBOL_GETTER(GetMatch, symbol_get_match)
SYMBOL_GETTER(GetReplace, symbol_get_replace)
SYMBOL_GETTER(GetSearch, symbol_get_search)
SYMBOL_GETTER(GetSplit, symbol_get_split)
SYMBOL_GETTER(GetToPrimitive, symbol_get_to_primitive)
SYMBOL_GETTER(GetToStringTag, symbol_get_to_string_tag)
SYMBOL_GETTER(GetUnscopables, symbol_get_unscopables)

v8::Local<v8::Symbol> GraalSymbol::For(v8::Isolate* isolate, v8::Local<v8::String> description) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_description = reinterpret_cast<GraalString*> (*description);
    jobject java_description = graal_description->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_for, Object, java_description);
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol);
    v8::Symbol* v8_symbol = reinterpret_cast<v8::Symbol*> (graal_symbol);
    return v8::Local<v8::Symbol>::New(isolate, v8_symbol);
}

v8::Local<v8::Symbol> GraalSymbol::ForApi(v8::Isolate* isolate, v8::Local<v8::String> description) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_description = reinterpret_cast<GraalString*> (*description);
    jobject java_description = graal_description->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_for_api, Object, java_description);
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol);
    v8::Symbol* v8_symbol = reinterpret_cast<v8::Symbol*> (graal_symbol);
    return v8::Local<v8::Symbol>::New(isolate, v8_symbol);
}

v8::Local<v8::Private> GraalSymbol::PrivateForApi(v8::Isolate* isolate, v8::Local<v8::String> description) {
    GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
    GraalString* graal_description = reinterpret_cast<GraalString*> (*description);
    jobject java_description = graal_description->GetJavaObject();
    JNI_CALL(jobject, java_symbol, graal_isolate, GraalAccessMethod::symbol_private_for_api, Object, java_description);
    GraalSymbol* graal_symbol = GraalSymbol::Allocate(graal_isolate, java_symbol);
    v8::Private* v8_symbol = reinterpret_cast<v8::Private*> (graal_symbol);
    return v8::Local<v8::Private>::New(isolate, v8_symbol);
}

v8::Local<v8::Value> GraalSymbol::Name() const {
    GraalIsolate* graal_isolate = Isolate();
    jobject java_symbol = GetJavaObject();
    JNI_CALL(jobject, java_name, graal_isolate, GraalAccessMethod::symbol_name, Object, java_symbol);
    GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_name);
    v8::Value* v8_value = reinterpret_cast<v8::Value*> (graal_value);
    v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (graal_isolate);
    return v8::Local<v8::Value>::New(v8_isolate, v8_value);
}

bool GraalSymbol::IsSymbol() const {
    return true;
}

bool GraalSymbol::IsName() const {
    return true;
}
