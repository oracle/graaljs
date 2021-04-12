/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_array.h"
#include "graal_array_buffer.h"
#include "graal_array_buffer_view.h"
#include "graal_backing_store.h"
#include "graal_big_int.h"
#include "graal_boolean.h"
#include "graal_context.h"
#include "graal_date.h"
#include "graal_external.h"
#include "graal_function.h"
#include "graal_function_template.h"
#include "graal_isolate.h"
#include "graal_map.h"
#include "graal_message.h"
#include "graal_module.h"
#include "graal_number.h"
#include "graal_object_template.h"
#include "graal_primitive_array.h"
#include "graal_promise.h"
#include "graal_proxy.h"
#include "graal_regexp.h"
#include "graal_script.h"
#include "graal_script_or_module.h"
#include "graal_set.h"
#include "graal_stack_frame.h"
#include "graal_stack_trace.h"
#include "graal_string.h"
#include "graal_symbol.h"
#include "graal_unbound_script.h"
#include "graal_value.h"
#include "v8.h"
#include "v8-profiler.h"
#include "v8-version-string.h"
#ifdef __POSIX__
#include "v8-wasm-trap-handler-posix.h"
#endif
#include "libplatform/v8-tracing.h"
#include "src/base/once.h"
#include "src/base/platform/mutex.h"
#include "stdlib.h"
#include <string.h>
#include <string>

#include "graal_array-inl.h"
#include "graal_array_buffer-inl.h"
#include "graal_array_buffer_view-inl.h"
#include "graal_big_int-inl.h"
#include "graal_boolean-inl.h"
#include "graal_context-inl.h"
#include "graal_date-inl.h"
#include "graal_external-inl.h"
#include "graal_function-inl.h"
#include "graal_function_template-inl.h"
#include "graal_map-inl.h"
#include "graal_message-inl.h"
#include "graal_module-inl.h"
#include "graal_number-inl.h"
#include "graal_object_template-inl.h"
#include "graal_primitive_array-inl.h"
#include "graal_promise-inl.h"
#include "graal_proxy-inl.h"
#include "graal_regexp-inl.h"
#include "graal_script-inl.h"
#include "graal_script_or_module-inl.h"
#include "graal_set-inl.h"
#include "graal_stack_frame-inl.h"
#include "graal_stack_trace-inl.h"
#include "graal_string-inl.h"
#include "graal_symbol-inl.h"
#include "graal_unbound_script-inl.h"
#include "graal_value-inl.h"

#define TRACE
//#define TRACE fprintf(stderr, "at %s line %d\n", __func__, __LINE__);

#define REPORT_CAUGHT_EXCEPTIONS false

namespace v8 {
    namespace internal {

        class Object : public v8::Object {
        };

        class Isolate : public v8::Isolate {
        };
    }

    ArrayBuffer::Contents ArrayBuffer::GetContents() {
        GraalArrayBuffer* graal_array_buffer = reinterpret_cast<GraalArrayBuffer*> (this);
        GraalIsolate* graal_isolate = graal_array_buffer->Isolate();
        jobject java_array_buffer = graal_array_buffer->GetJavaObject();
        jobject java_buffer;
        if (graal_array_buffer->IsDirect()) {
            java_buffer = graal_isolate->JNIGetObjectFieldOrCall(java_array_buffer, GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
        } else {
            JNI_CALL(jobject, java_not_direct_buffer, graal_isolate, GraalAccessMethod::array_buffer_get_contents, Object, java_array_buffer);
            java_buffer = java_not_direct_buffer;
        }
        JNIEnv* env = graal_isolate->GetJNIEnv();
        ArrayBuffer::Contents contents;
        if (java_buffer == nullptr) {
            contents.data_ = nullptr;
            contents.byte_length_ = 0;
        } else {
            contents.data_ = env->GetDirectBufferAddress(java_buffer);
            contents.byte_length_ = env->GetDirectBufferCapacity(java_buffer);
            env->DeleteLocalRef(java_buffer);
        }
        return contents;
    }

    void ArrayBuffer::Detach() {
        reinterpret_cast<GraalArrayBuffer*> (this)->Detach();
    }

    Local<ArrayBuffer> ArrayBuffer::New(Isolate* isolate, size_t byte_length) {
        return GraalArrayBuffer::New(isolate, byte_length);
    }

    Local<ArrayBuffer> ArrayBuffer::New(Isolate* isolate, void* data, size_t byte_length, ArrayBufferCreationMode mode) {
        return GraalArrayBuffer::New(isolate, data, byte_length, mode);
    }

    Local<ArrayBuffer> ArrayBufferView::Buffer() {
        return reinterpret_cast<GraalArrayBufferView*> (this)->Buffer();
    }

    size_t ArrayBufferView::ByteOffset() {
        return reinterpret_cast<GraalArrayBufferView*> (this)->ByteOffset();
    }

    bool ArrayBufferView::HasBuffer() const {
        return true;
    }

    Local<Array> Array::New(Isolate* isolate, int length) {
        if (length < 0) {
            length = 0;
        }
        return GraalArray::New(isolate, length);
    }

    void Context::Enter() {
        reinterpret_cast<GraalContext*> (this)->Enter();
    }

    void Context::Exit() {
        reinterpret_cast<GraalContext*> (this)->Exit();
    }

    Isolate* Context::GetIsolate() {
        return reinterpret_cast<GraalContext*> (this)->GetIsolate();
    }

    Local<Value> Context::GetSecurityToken() {
        return reinterpret_cast<GraalContext*> (this)->GetSecurityToken();
    }

    void Context::UseDefaultSecurityToken() {
        return reinterpret_cast<GraalContext*> (this)->UseDefaultSecurityToken();
    }

    Local<Object> Context::Global() {
        return reinterpret_cast<GraalContext*> (this)->Global();
    }

    Local<Context> Context::New(
            Isolate* isolate,
            ExtensionConfiguration* ext,
            MaybeLocal<ObjectTemplate> templ,
            MaybeLocal<Value> value,
            DeserializeInternalFieldsCallback internal_fields_deserializer,
            MicrotaskQueue* microtask_queue) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_template;
        if (templ.IsEmpty()) {
            java_template = NULL;
        } else {
            GraalObjectTemplate* graal_template = reinterpret_cast<GraalObjectTemplate*> (*(templ.ToLocalChecked()));
            java_template = graal_template->GetJavaObject();
        }
        JNI_CALL(jobject, java_context, isolate, GraalAccessMethod::context_new, Object, java_template);
        if (java_context == NULL) {
            graal_isolate->GetJNIEnv()->ExceptionDescribe();
            exit(1);
        }
        graal_isolate->FindDynamicObjectFields(java_context);
        GraalContext* ctx = GraalContext::Allocate(graal_isolate, java_context);
        return reinterpret_cast<Context*> (ctx);
    }

    void Context::SetAlignedPointerInEmbedderData(int index, void* value) {
        reinterpret_cast<GraalContext*> (this)->SetAlignedPointerInEmbedderData(index, value);
    }

    void Context::SetSecurityToken(Local<Value> token) {
        reinterpret_cast<GraalContext*> (this)->SetSecurityToken(token);
    }

    void Context::SetEmbedderData(int index, Local<Value> value) {
        reinterpret_cast<GraalContext*> (this)->SetEmbedderData(index, value);
    }

    v8::Local<v8::Value> Context::SlowGetEmbedderData(int index) {
        return reinterpret_cast<GraalContext*> (this)->SlowGetEmbedderData(index);
    }

    uint32_t Context::GetNumberOfEmbedderDataFields() {
        return 64;
    }

    EscapableHandleScope::EscapableHandleScope(Isolate* isolate) : HandleScope(isolate) {
    }

    internal::Address* EscapableHandleScope::Escape(internal::Address* obj) {
        return obj;
    }

#define EXCEPTION_ERROR(error_type) \
    GraalString* graal_message = reinterpret_cast<GraalString*> (*message); \
    GraalIsolate* isolate = graal_message->Isolate(); \
    jobject java_context = isolate->CurrentJavaContext(); \
    jobject java_message = graal_message->GetJavaObject(); \
    JNI_CALL(jobject, java_error, isolate, GraalAccessMethod::error_type, Object, java_context, java_message); \
    GraalObject* graal_object = GraalObject::Allocate(isolate, java_error); \
    return reinterpret_cast<Value*> (graal_object);

    Local<Value> Exception::Error(Local<String> message) {
        EXCEPTION_ERROR(exception_error)
    }

    Local<Value> Exception::RangeError(Local<String> message) {
        EXCEPTION_ERROR(exception_range_error)
    }

    Local<Value> Exception::TypeError(Local<String> message) {
        EXCEPTION_ERROR(exception_type_error)
    }

    Local<Value> Exception::ReferenceError(Local<String> message) {
        EXCEPTION_ERROR(exception_reference_error)
    }

    Local<Value> Exception::SyntaxError(Local<String> message) {
        EXCEPTION_ERROR(exception_syntax_error)
    }

    Local<External> External::New(Isolate* isolate, void* value) {
        return GraalExternal::New(isolate, value);
    }

    MaybeLocal<Value> Function::Call(Local<Context> context, Local<Value> recv, int argc, Local<Value> argv[]) {
        return reinterpret_cast<GraalFunction*> (this)->Call(recv, argc, argv);
    }

    void Function::SetName(Local<String> name) {
        reinterpret_cast<GraalFunction*> (this)->SetName(name);
    }

    Local<Value> Function::GetName() const {
        return reinterpret_cast<const GraalFunction*> (this)->GetName();
    }

    Local<Value> Function::GetInferredName() const {
        return String::Empty(const_cast<Function*> (this)->GetIsolate());
    }

    const int Function::kLineOffsetNotFound = -1;

    int Function::GetScriptLineNumber() const {
        return reinterpret_cast<const GraalFunction*> (this)->GetScriptLineNumber();
    }

    int Function::GetScriptColumnNumber() const {
        return reinterpret_cast<const GraalFunction*> (this)->GetScriptColumnNumber();
    }

    ScriptOrigin Function::GetScriptOrigin() const {
        return reinterpret_cast<const GraalFunction*> (this)->GetScriptOrigin();
    }

    MaybeLocal<Function> FunctionTemplate::GetFunction(Local<Context> context) {
        return reinterpret_cast<GraalFunctionTemplate*> (this)->GetFunction(context);
    }

    bool FunctionTemplate::HasInstance(Local<Value> object) {
        return reinterpret_cast<GraalFunctionTemplate*> (this)->HasInstance(object);
    }

    Local<ObjectTemplate> FunctionTemplate::InstanceTemplate() {
        return reinterpret_cast<GraalFunctionTemplate*> (this)->InstanceTemplate();
    }

    Local<FunctionTemplate> FunctionTemplate::New(
            Isolate* isolate, FunctionCallback callback,
            Local<Value> data, Local<Signature> signature, int length,
            ConstructorBehavior behavior, SideEffectType side_effect_type,
            const CFunction* c_function) {
        return GraalFunctionTemplate::New(isolate, callback, data, signature, length, behavior, false);
    }

    Local<ObjectTemplate> FunctionTemplate::PrototypeTemplate() {
        return reinterpret_cast<GraalFunctionTemplate*> (this)->PrototypeTemplate();
    }

    void FunctionTemplate::SetClassName(Local<String> name) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->SetClassName(name);
    }

    void FunctionTemplate::SetCallHandler(FunctionCallback callback, Local<Value> data, SideEffectType side_effect_type, const CFunction* c_function) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->SetCallHandler(callback, data);
    }

    void FunctionTemplate::Inherit(Local<FunctionTemplate> parent) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->Inherit(parent);
    }

    internal::Address* HandleScope::CreateHandle(internal::Isolate* isolate, internal::Address value) {
        GraalHandleContent* graal_original = reinterpret_cast<GraalHandleContent*> (value);
        GraalHandleContent* graal_copy = graal_original->Copy(false);
        return reinterpret_cast<internal::Address*> (graal_copy);
    }

    HandleScope::~HandleScope() {
        reinterpret_cast<GraalIsolate*> (isolate_)->HandleScopeExit();
    }

    HandleScope::HandleScope(Isolate* isolate) {
        this->isolate_ = reinterpret_cast<internal::Isolate*> (isolate);
        reinterpret_cast<GraalIsolate*> (isolate)->HandleScopeEnter();
    }

    void HeapProfiler::StartTrackingHeapObjects(bool) {
        TRACE
    }

    HeapStatistics::HeapStatistics() :
        total_heap_size_(0),
        total_heap_size_executable_(0),
        total_physical_size_(0),
        total_available_size_(0),
        used_heap_size_(0),
        heap_size_limit_(0),
        malloced_memory_(0),
        external_memory_(0),
        peak_malloced_memory_(0),
        does_zap_garbage_(false),
        number_of_native_contexts_(0),
        number_of_detached_contexts_(0) {
    }

    Local<Integer> Integer::NewFromUnsigned(Isolate* isolate, uint32_t value) {
        return GraalNumber::NewFromUnsigned(isolate, value);
    }

    Local<Integer> Integer::New(Isolate* isolate, int value) {
        return GraalNumber::New(isolate, value);
    }

    bool Isolate::AddMessageListener(MessageCallback callback, Local<Value> data) {
        return reinterpret_cast<GraalIsolate*> (this)->AddMessageListener(callback, data);
    }

    void Isolate::CancelTerminateExecution() {
        reinterpret_cast<GraalIsolate*> (this)->CancelTerminateExecution();
    }

    void Isolate::Dispose() {
        reinterpret_cast<GraalIsolate*> (this)->Dispose();
    }

    void Isolate::Dispose(bool exit, int status) {
        reinterpret_cast<GraalIsolate*> (this)->Dispose(exit, status);
    }

    void Isolate::SchedulePauseOnNextStatement() {
        reinterpret_cast<GraalIsolate*> (this)->SchedulePauseOnNextStatement();
    }

    bool Isolate::IsDead() {
        return false;
    }

    void Isolate::Enter() {
        reinterpret_cast<GraalIsolate*> (this)->Enter();
    }

    void Isolate::Exit() {
        reinterpret_cast<GraalIsolate*> (this)->Exit();
    }

    Isolate* Isolate::GetCurrent() {
        return GraalIsolate::GetCurrent();
    }

    Local<Context> Isolate::GetCurrentContext() {
        return reinterpret_cast<GraalIsolate*> (this)->GetCurrentContext();
    }

    int Isolate::ContextDisposedNotification(bool dependant_context) {
        return 0;
    }

    HeapProfiler* Isolate::GetHeapProfiler() {
        TRACE
        return nullptr;
    }

    const HeapSnapshot* HeapProfiler::TakeHeapSnapshot(ActivityControl* control, ObjectNameResolver* global_object_name_resolver, bool treat_global_objects_as_roots) {
        TRACE
        return nullptr;
    }

    void HeapSnapshot::Serialize(OutputStream* stream, SerializationFormat format) const {
        TRACE
    }

    void HeapSnapshot::Delete() {
        TRACE
    }

    int HeapSnapshot::GetNodesCount() const {
        TRACE
        return 0;
    }

    const HeapGraphNode* HeapSnapshot::GetRoot() const {
        TRACE
        return nullptr;
    }

    SnapshotObjectId HeapGraphNode::GetId() const {
        TRACE
        return 0;
    }

    HeapGraphNode::Type HeapGraphNode::GetType() const {
        TRACE
        return HeapGraphNode::Type::kHidden;
    }

    size_t HeapGraphNode::GetShallowSize() const {
        TRACE
        return 0;
    }

    int HeapGraphNode::GetChildrenCount() const {
        TRACE
        return 0;
    }

    void Isolate::GetHeapStatistics(HeapStatistics* heap_statistics) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (this);
        graal_isolate->ResetSharedBuffer();
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_get_heap_statistics);
        heap_statistics->total_heap_size_ = graal_isolate->ReadInt64FromSharedBuffer();
        heap_statistics->total_heap_size_executable_ = 0;
        heap_statistics->total_physical_size_ = 0;
        heap_statistics->total_available_size_ = graal_isolate->ReadInt64FromSharedBuffer();
        heap_statistics->used_heap_size_ = graal_isolate->ReadInt64FromSharedBuffer();
        heap_statistics->heap_size_limit_ = 0;
        heap_statistics->does_zap_garbage_ = false;
        heap_statistics->external_memory_ = 4096; // dummy value
    }

    Isolate* Isolate::New(Isolate::CreateParams const& params) {
        return GraalIsolate::New(params);
    }

    void Isolate::PerformMicrotaskCheckpoint() {
        reinterpret_cast<GraalIsolate*> (this)->RunMicrotasks();
    }

    void Isolate::SetAbortOnUncaughtExceptionCallback(AbortOnUncaughtExceptionCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetAbortOnUncaughtExceptionCallback(callback);
    }

    void Isolate::SetFatalErrorHandler(FatalErrorCallback that) {
        reinterpret_cast<GraalIsolate*> (this)->SetFatalErrorHandler(that);
    }

    void Isolate::SetPromiseRejectCallback(PromiseRejectCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetPromiseRejectCallback(callback);
    }

    void Isolate::TerminateExecution() {
        reinterpret_cast<GraalIsolate*> (this)->TerminateExecution();
    }

    Local<Value> Isolate::ThrowException(Local<Value> exception) {
        return reinterpret_cast<GraalIsolate*> (this)->ThrowException(exception);
    }

    void Locker::Initialize(Isolate* isolate) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
#ifdef __POSIX__
        pthread_mutex_lock(&graal_isolate->lock_);
#else
        WaitForSingleObject(graal_isolate->lock_, INFINITE);
#endif
        graal_isolate->lock_owner_ = this;
        isolate_ = reinterpret_cast<internal::Isolate*> (isolate);
    }

    bool Locker::IsActive() {
        return true;
    }

    Locker::~Locker() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
        if (graal_isolate->lock_owner_ == this) {
            graal_isolate->lock_owner_ = nullptr;
#ifdef __POSIX__
            pthread_mutex_unlock(&graal_isolate->lock_);
#else
            ReleaseMutex(graal_isolate->lock_);
#endif
        }
    }

    void Unlocker::Initialize(Isolate* isolate) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        if (graal_isolate->lock_owner_ != nullptr) {
            graal_isolate->lock_owner_ = nullptr;
#ifdef __POSIX__
            pthread_mutex_unlock(&graal_isolate->lock_);
#else
            ReleaseMutex(graal_isolate->lock_);
#endif
        }
    }

    Unlocker::~Unlocker() {
    }

    Local<Number> Number::New(Isolate* isolate, double value) {
        return GraalNumber::New(isolate, value);
    }

    Local<Object> Object::Clone() {
        return reinterpret_cast<GraalObject*> (this)->Clone();
    }

    Local<Context> Object::CreationContext() {
        return reinterpret_cast<GraalObject*> (this)->CreationContext();
    }

    Maybe<bool> Object::Delete(Local<Context> context, Local<Value> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->Delete(key));
    }

    Maybe<bool> Object::Delete(Local<Context> context, uint32_t index) {
        return Just(reinterpret_cast<GraalObject*> (this)->Delete(index));
    }

    Local<String> Object::GetConstructorName() {
        return reinterpret_cast<GraalObject*> (this)->GetConstructorName();
    }

    Isolate* Object::GetIsolate() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalObject*> (this)->Isolate();
        return reinterpret_cast<Isolate*> (graal_isolate);
    }

    MaybeLocal<Array> Object::GetOwnPropertyNames(Local<Context> context) {
        return reinterpret_cast<GraalObject*> (this)->GetOwnPropertyNames();
    }

    MaybeLocal<Array> Object::GetPropertyNames(Local<Context> context) {
        return GetPropertyNames(context, KeyCollectionMode::kIncludePrototypes,
                static_cast<v8::PropertyFilter> (ONLY_ENUMERABLE | SKIP_SYMBOLS),
                v8::IndexFilter::kIncludeIndices);
    }

    MaybeLocal<Array> Object::GetPropertyNames(Local<Context> context, KeyCollectionMode mode,
            PropertyFilter property_filter, IndexFilter index_filter,
            KeyConversionMode key_conversion) {
        return reinterpret_cast<GraalObject*> (this)->GetPropertyNames(context, mode, property_filter, index_filter, key_conversion);
    }

    Local<Value> Object::GetPrototype() {
        return reinterpret_cast<GraalObject*> (this)->GetPrototype();
    }

    Maybe<PropertyAttribute> Object::GetRealNamedPropertyAttributes(Local<Context> context, Local<Name> key) {
        return reinterpret_cast<GraalObject*> (this)->GetRealNamedPropertyAttributes(context, key);
    }

    MaybeLocal<Value> Object::GetRealNamedProperty(Local<Context> context, Local<Name> key) {
        return reinterpret_cast<GraalObject*> (this)->GetRealNamedProperty(context, key);
    }

    Maybe<bool> Object::HasRealNamedProperty(Local<Context> context, Local<Name> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->HasRealNamedProperty(key));
    }

    MaybeLocal<Value> Object::Get(Local<Context> context, uint32_t index) {
        return reinterpret_cast<GraalObject*> (this)->Get(index);
    }

    MaybeLocal<Value> Object::Get(Local<Context> context, Local<Value> key) {
        return reinterpret_cast<GraalObject*> (this)->Get(key);
    }

    Maybe<bool> Object::HasOwnProperty(Local<Context> context, Local<Name> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->HasOwnProperty(key));
    }

    Maybe<bool> Object::HasRealIndexedProperty(Local<Context> context, uint32_t index) {
        return HasOwnProperty(context, Integer::NewFromUnsigned(GetIsolate(), index)->ToString(context).ToLocalChecked());
    }

    int Object::InternalFieldCount() {
        return reinterpret_cast<GraalObject*> (this)->InternalFieldCount();
    }

    void Object::SetInternalField(int index, Local<Value> value) {
        reinterpret_cast<GraalObject*> (this)->SetInternalField(index, value);
    }

    Local<Value> Object::SlowGetInternalField(int index) {
        return reinterpret_cast<GraalObject*> (this)->SlowGetInternalField(index);
    }

    Local<Object> Object::New(Isolate* isolate) {
        return GraalObject::New(isolate);
    }

    Maybe<bool> Object::SetAccessor(
            Local<Context> context,
            Local<Name> name,
            AccessorNameGetterCallback getter,
            AccessorNameSetterCallback setter,
            MaybeLocal<Value> data,
            AccessControl settings,
            PropertyAttribute attributes,
            SideEffectType getter_side_effect_type,
            SideEffectType setter_side_effect_type) {
        return reinterpret_cast<GraalObject*> (this)->SetAccessor(name, getter, setter, data, settings, attributes);
    }

    void Object::SetAlignedPointerInInternalField(int index, void* value) {
        reinterpret_cast<GraalObject*> (this)->SetAlignedPointerInInternalField(index, value);
    }

    Maybe<bool> Object::SetPrototype(Local<Context> context, Local<Value> prototype) {
        return Just(reinterpret_cast<GraalObject*> (this)->SetPrototype(prototype));
    }

    Maybe<bool> Object::Set(Local<Context> context, uint32_t index, Local<Value> value) {
        return Just(reinterpret_cast<GraalObject*> (this)->Set(index, value));
    }

    Maybe<bool> Object::Set(Local<Context> context, Local<Value> key, Local<Value> value) {
        return Just(reinterpret_cast<GraalObject*> (this)->Set(key, value));
    }

    void* Object::SlowGetAlignedPointerFromInternalField(int index) {
        return reinterpret_cast<GraalObject*> (this)->SlowGetAlignedPointerFromInternalField(index);
    }

    MaybeLocal<Value> Object::CallAsFunction(Local<Context> context, Local<Value> recv, int argc, Local<Value> argv[]) {
        return reinterpret_cast<Function*> (this)->Call(context, recv, argc, argv);
    }

    MaybeLocal<Object> ObjectTemplate::NewInstance(Local<Context> context) {
        return reinterpret_cast<GraalObjectTemplate*> (this)->NewInstance(context);
    }

    Local<ObjectTemplate> ObjectTemplate::New(Isolate* isolate, Local<FunctionTemplate> constructor) {
        return GraalObjectTemplate::New(isolate, constructor);
    }

    void ObjectTemplate::SetAccessor(
            Local<String> name,
            AccessorGetterCallback getter,
            AccessorSetterCallback setter,
            Local<Value> data,
            AccessControl settings,
            PropertyAttribute attribute,
            Local<AccessorSignature> signature,
            SideEffectType getter_side_effect_type,
            SideEffectType setter_side_effect_type) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetAccessor(name, getter, setter, data, settings, attribute, signature);
    }

    void ObjectTemplate::SetAccessor(
            Local<Name> name,
            AccessorNameGetterCallback getter,
            AccessorNameSetterCallback setter,
            Local<Value> data,
            AccessControl settings,
            PropertyAttribute attribute,
            Local<AccessorSignature> signature,
            SideEffectType getter_side_effect_type,
            SideEffectType setter_side_effect_type) {
        SetAccessor(name.As<String>(), (AccessorGetterCallback) getter, (AccessorSetterCallback) setter, data, settings, attribute, signature);
    };

    void ObjectTemplate::SetHandler(const NamedPropertyHandlerConfiguration& configuration) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetHandler(configuration);
    }

    void ObjectTemplate::SetInternalFieldCount(int value) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetInternalFieldCount(value);
    }

    int ObjectTemplate::InternalFieldCount() {
        return reinterpret_cast<GraalObjectTemplate*> (this)->InternalFieldCount();
    }

    void ObjectTemplate::SetHandler(const IndexedPropertyHandlerConfiguration& configuration) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetHandler(configuration);
    }

    void ObjectTemplate::SetCallAsFunctionHandler(FunctionCallback callback, Local<Value> data) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetCallAsFunctionHandler(callback, data);
    }

    ScriptCompiler::CachedData::~CachedData() {
        if (buffer_policy == BufferOwned) {
            delete[] data;
        }
    }

    MaybeLocal<Script> ScriptCompiler::Compile(
            Local<Context> context,
            Source* source,
            CompileOptions options,
            NoCacheReason no_cache_reason) {
        ScriptOrigin origin(source->resource_name);
        return GraalScript::Compile(source->source_string, &origin);
    }

    MaybeLocal<Value> Script::Run(v8::Local<v8::Context> context) {
        return reinterpret_cast<GraalScript*> (this)->Run();
    }

    SealHandleScope::~SealHandleScope() {
        TRACE
    }

    SealHandleScope::SealHandleScope(Isolate* isolate) : isolate_(reinterpret_cast<internal::Isolate*> (isolate)) {
        TRACE
    }

    Local<Signature> Signature::New(Isolate* isolate, Local<FunctionTemplate> receiver) {
        return reinterpret_cast<Signature*> (*receiver);
    }

    Local<StackTrace> StackTrace::CurrentStackTrace(v8::Isolate* isolate, int frame_limit, StackTraceOptions options) {
        return GraalStackTrace::CurrentStackTrace(isolate, frame_limit, options);
    }

    Local<String> String::Concat(Isolate* isolate, Local<String> left, Local<String> right) {
        GraalString* graal_left = reinterpret_cast<GraalString*> (*left);
        GraalString* graal_right = reinterpret_cast<GraalString*> (*right);
        jstring java_left = (jstring) graal_left->GetJavaObject();
        jstring java_right = (jstring) graal_right->GetJavaObject();
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        int left_length = env->GetStringLength(java_left);
        int right_length = env->GetStringLength(java_right);
        int length = left_length + right_length;
        jchar* str = new jchar[length];
        env->GetStringRegion(java_left, 0, left_length, str);
        env->GetStringRegion(java_right, 0, right_length, str + left_length);
        GraalString* graal_concat = GraalString::Allocate(graal_isolate, env->NewString(str, length));
        delete[] str;
        return reinterpret_cast<String*> (graal_concat);
    }

    const String::ExternalOneByteStringResource* String::GetExternalOneByteStringResource() const {
        TRACE
        return NULL;
    }

    MaybeLocal<String> String::NewFromOneByte(
            Isolate* isolate,
            unsigned char const* data,
            v8::NewStringType type,
            int length) {
        return GraalString::NewFromOneByte(isolate, data, type, length);
    }

    MaybeLocal<String> String::NewFromTwoByte(
            Isolate* isolate,
            const uint16_t* data,
            v8::NewStringType type,
            int length) {
        return GraalString::NewFromTwoByte(isolate, data, type, length);
    }

    MaybeLocal<String> String::NewFromUtf8(Isolate* isolate, char const* data, v8::NewStringType type, int length) {
        return GraalString::NewFromUtf8(isolate, data, type, length);
    }

    Local<v8::String> String::NewFromUtf8Literal(Isolate* isolate, const char* literal, NewStringType type, int length) {
        return GraalString::NewFromUtf8(isolate, literal, type, length);
    }

    String::Utf8Value::~Utf8Value() {
        if (java_string_ != nullptr) {
            JNIEnv* env = reinterpret_cast<GraalIsolate*> (isolate_)->GetJNIEnv();
            env->DeleteLocalRef((jobject) java_string_);
            delete[] str_;
        }
    }

    String::Utf8Value::Utf8Value(Isolate* isolate, Local<v8::Value> obj) {
        JNIEnv* env;
        if (obj.IsEmpty()) {
            java_string_ = nullptr;
            env = nullptr;
        } else {
            GraalValue* graal_obj = reinterpret_cast<GraalValue*> (*obj);
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
            env = graal_isolate->GetJNIEnv();
            if (graal_obj->IsString()) {
                java_string_ = env->NewLocalRef(graal_obj->GetJavaObject());
            } else {
                JNI_CALL(jobject, java_string, graal_isolate, GraalAccessMethod::value_to_string, Object, graal_obj->GetJavaObject());
                if (java_string == nullptr) {
                    env->ExceptionClear();
                }
                java_string_ = java_string;
            }
            isolate_ = reinterpret_cast<v8::Isolate*> (graal_isolate);
        }
        if (java_string_ == nullptr) {
            str_ = nullptr;
            length_ = 0;
        } else {
            jstring java_string = (jstring) java_string_;
            int utf16length = env->GetStringLength(java_string);
            const jchar* utf16chars = env->GetStringCritical(java_string, nullptr);
            length_ = GraalString::Utf8Length(utf16chars, utf16length);
            str_ = new char[length_ + 1];
            GraalString::Utf8Write(utf16chars, utf16length, str_, length_ + 1, nullptr, 0);
            env->ReleaseStringCritical(java_string, utf16chars);
        }
    }

    String::Value::~Value() {
        if (java_string_ != nullptr) {
            JNIEnv* env = reinterpret_cast<GraalIsolate*> (isolate_)->GetJNIEnv();
            env->ReleaseStringChars((jstring) java_string_, (const jchar*) str_);
            env->DeleteLocalRef((jobject) java_string_);
        }
    }

    String::Value::Value(Isolate* isolate, Local<v8::Value> obj) {
        GraalValue* graal_obj = reinterpret_cast<GraalValue*> (*obj);
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        if (graal_obj->IsString()) {
            java_string_ = env->NewLocalRef(graal_obj->GetJavaObject());
        } else {
            JNI_CALL(jobject, java_string, graal_isolate, GraalAccessMethod::value_to_string, Object, graal_obj->GetJavaObject());
            java_string_ = java_string;
        }
        isolate_ = reinterpret_cast<v8::Isolate*> (graal_isolate);
        str_ = (uint16_t*) env->GetStringChars((jstring) java_string_, nullptr);
        length_ = env->GetStringLength((jstring) java_string_);
    }

    void Template::Set(Local<Name> name, Local<Data> value, PropertyAttribute attributes) {
        reinterpret_cast<GraalTemplate*> (this)->Set(name, value, attributes);
    }

    void Template::SetAccessorProperty(
            Local<Name> name,
            Local<FunctionTemplate> getter,
            Local<FunctionTemplate> setter,
            PropertyAttribute attributes,
            AccessControl settings) {
        reinterpret_cast<GraalTemplate*> (this)->SetAccessorProperty(name, getter, setter, attributes);
    }

    Local<Value> TryCatch::ReThrow() {
        rethrow_ = true;
        exception_ = nullptr;
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
        if (!graal_isolate->GetJNIEnv()->ExceptionCheck()) {
            graal_isolate->ThrowException(Null(reinterpret_cast<v8::Isolate*> (isolate_)));
        }
        return Undefined(reinterpret_cast<v8::Isolate*> (isolate_));
    }

    void TryCatch::SetVerbose(bool value) {
        is_verbose_ = value;
    }

    bool TryCatch::IsVerbose() const {
        return is_verbose_;
    }

    TryCatch::~TryCatch() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
        graal_isolate->TryCatchExit();
        if (!rethrow_ && HasCaught() && !HasTerminated()) {
            JNIEnv* env = graal_isolate->GetJNIEnv();
            if (is_verbose_) {
                jthrowable java_exception = env->ExceptionOccurred();
                Local<Value> exception = Exception();
                Local<v8::Message> message = Message();
                env->ExceptionClear();
                graal_isolate->NotifyMessageListener(message, exception, java_exception);
            } else if (REPORT_CAUGHT_EXCEPTIONS) {
                env->ExceptionDescribe();
            } else {
                env->ExceptionClear();
            }
        }
    }

    TryCatch::TryCatch(Isolate* isolate) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        isolate_ = reinterpret_cast<v8::internal::Isolate*> (isolate);
        rethrow_ = graal_isolate->GetJNIEnv()->ExceptionCheck(); // Do not catch exceptions thrown before
        exception_ = rethrow_ ? graal_isolate->GetJNIEnv()->ExceptionOccurred() : nullptr;
        is_verbose_ = false;
        graal_isolate->TryCatchEnter();
    }

#define ArrayBufferViewNew(view_class, direct_view_type, interop_view_type, graal_access_method) \
    Local<view_class> view_class::New(Local<ArrayBuffer> array_buffer, size_t byte_offset, size_t length) { \
        GraalArrayBuffer* graal_array_buffer = reinterpret_cast<GraalArrayBuffer*> (*array_buffer); \
        jobject java_array_buffer = graal_array_buffer->GetJavaObject(); \
        GraalIsolate* graal_isolate = graal_array_buffer->Isolate(); \
        JNI_CALL(jobject, java_array_buffer_view, graal_isolate, GraalAccessMethod::graal_access_method, Object, java_array_buffer, (jint) byte_offset, (jint) length); \
        int view_type = graal_array_buffer->IsDirect() ? GraalArrayBufferView::direct_view_type : GraalArrayBufferView::interop_view_type; \
        return reinterpret_cast<view_class*> (GraalArrayBufferView::Allocate(graal_isolate, java_array_buffer_view, view_type)); \
    }

    ArrayBufferViewNew(Uint8Array, kDirectUint8Array, kInteropUint8Array, uint8_array_new)
    ArrayBufferViewNew(Uint8ClampedArray, kDirectUint8ClampedArray, kInteropUint8ClampedArray, uint8_clamped_array_new)
    ArrayBufferViewNew(Int8Array, kDirectInt8Array, kInteropInt8Array, int8_array_new)
    ArrayBufferViewNew(Uint16Array, kDirectUint16Array, kInteropUint16Array, uint16_array_new)
    ArrayBufferViewNew(Int16Array, kDirectInt16Array, kInteropInt16Array, int16_array_new)
    ArrayBufferViewNew(Uint32Array, kDirectUint32Array, kInteropUint32Array, uint32_array_new)
    ArrayBufferViewNew(Int32Array, kDirectInt32Array, kInteropInt32Array, int32_array_new)
    ArrayBufferViewNew(Float32Array, kDirectFloat32Array, kInteropFloat32Array, float32_array_new)
    ArrayBufferViewNew(Float64Array, kDirectFloat64Array, kInteropFloat64Array, float64_array_new)
    ArrayBufferViewNew(DataView, kDataView, kDataView, data_view_new)
    ArrayBufferViewNew(BigInt64Array, kDirectBigInt64Array, kInteropBigInt64Array, big_int64_array_new)
    ArrayBufferViewNew(BigUint64Array, kDirectBigUint64Array, kInteropBigUint64Array, big_uint64_array_new)

    size_t TypedArray::Length() {
        GraalArrayBufferView* graal_typed_array = reinterpret_cast<GraalArrayBufferView*> (this);
        jobject java_typed_array = graal_typed_array->GetJavaObject();
        GraalIsolate* graal_isolate = graal_typed_array->Isolate();
        JNI_CALL(jint, length, graal_isolate, GraalAccessMethod::typed_array_length, Int, java_typed_array);
        return length;
    }

    Local<Script> UnboundScript::BindToCurrentContext() {
        return reinterpret_cast<GraalUnboundScript*> (this)->BindToCurrentContext();
    }

    void* V8::ClearWeak(internal::Address* global_handle) {
        GraalHandleContent* handle = reinterpret_cast<GraalHandleContent*> (global_handle);
        if (!handle->IsWeak()) {
            return nullptr;
        }
        GraalIsolate* graal_isolate = handle->Isolate();
        handle->ClearWeak();
        jlong reference = (jlong) handle;
        JNI_CALL(jlong, result, graal_isolate, GraalAccessMethod::clear_weak, Long, handle->GetJavaObject(), reference);
        return (void*) result;
    }

    bool V8::Dispose() {
        TRACE
        return false;
    }

    void V8::DisposeGlobal(internal::Address* global_handle) {
        GraalHandleContent* graal_handle = reinterpret_cast<GraalHandleContent*> (global_handle);
        if (graal_handle->IsWeak()) {
            // Disable potential weak callback
            GraalIsolate* graal_isolate = graal_handle->Isolate();
            jlong reference = (jlong) graal_handle;
            jobject java_handle = graal_handle->GetJavaObject();
            JNI_CALL(jlong, result, graal_isolate, GraalAccessMethod::clear_weak, Long, java_handle, reference);
        }
        graal_handle->ReferenceRemoved();
    }

    Value* V8::Eternalize(Isolate* isolate, Value* value) {
        GraalHandleContent* graal_original = reinterpret_cast<GraalHandleContent*> (value);
        GraalHandleContent* graal_copy = graal_original->Copy(true);
        Value* value_copy = reinterpret_cast<Value*> (graal_copy);
        int index = -1;
        reinterpret_cast<GraalIsolate*> (isolate)->SetEternal(value_copy, &index);
        return value_copy;
    }

    void V8::FromJustIsNothing() {
        reinterpret_cast<GraalIsolate*> (GraalIsolate::GetCurrent())->ReportAPIFailure("v8::FromJust", "Maybe value is Nothing.");
    }

    const char* V8::GetVersion() {
        return V8_VERSION_STRING;
    }

    internal::Address* V8::GlobalizeReference(internal::Isolate* isolate, internal::Address* obj) {
        GraalHandleContent* graal_original = reinterpret_cast<GraalHandleContent*> (obj);
        GraalHandleContent* graal_copy = graal_original->Copy(true);
        return reinterpret_cast<internal::Address*> (graal_copy);
    }

    bool V8::Initialize(int build_config) {
        TRACE
        return true;
    }

    static v8::Platform* platform_ = nullptr;
    void V8::InitializePlatform(Platform* platform) {
        platform_ = platform;
    }

    void V8::ShutdownPlatform() {
        platform_ = nullptr;
    }

    void V8::MakeWeak(internal::Address* global_handle, void* data,
            WeakCallbackInfo<void>::Callback weak_callback,
            WeakCallbackType type) {
        jint graal_type;
        GraalObject* graal_object = reinterpret_cast<GraalObject*> (global_handle);
        if (type == WeakCallbackType::kInternalFields) {
            void** wrapper = new void*[kInternalFieldsInWeakCallback + 1]; // deleted in GraalWeakCallback
            for (int i = 0; i < kInternalFieldsInWeakCallback; i++) {
                wrapper[i] = graal_object->SlowGetAlignedPointerFromInternalField(i);
            }
            wrapper[kInternalFieldsInWeakCallback] = data;
            data = (void*) wrapper;
            graal_type = 2;
        } else {
            graal_type = 1;
        }
        GraalIsolate* isolate = graal_object->Isolate();
        jlong reference = (jlong) graal_object;
        jlong java_data = (jlong) data;
        jlong callback = (jlong) weak_callback;
        JNI_CALL_VOID(isolate, GraalAccessMethod::make_weak, graal_object->GetJavaObject(), reference, java_data, callback, graal_type);
        graal_object->MakeWeak();
    }

    void V8::MakeWeak(internal::Address** location_addr) {
        GraalObject* graal_object = reinterpret_cast<GraalObject*> (*location_addr);
        graal_object->MakeWeak();
    }

    void V8::SetEntropySource(EntropySource source) {
        TRACE
    }

    bool EqualOptions(char* name, const char* normalized_name) {
        while (*name && *normalized_name) {
            char c = *name;
            if (c == '_') {
                c = '-';
            }
            if (c != *normalized_name) {
                return false;
            }
            name++;
            normalized_name++;
        }
        return (*name == *normalized_name // option without a value
                || (!*normalized_name && *name == '=')); // option with a value
    }

    void V8::SetFlagsFromCommandLine(int* argc, char** argv, bool remove_flags) {
        bool show_help = false;
        bool use_jvm = false;
        bool use_native = false;
        bool use_polyglot = false;
        bool show_jvm_warning = false;
        bool show_native_warning = false;
        std::string vm_args;

        int unprocessed = 0;
        for (int index = 1; index < *argc; index++) {
            char* const arg = argv[index];
            const char *classpath = nullptr;
            if (!strcmp(arg, "--jvm")) {
                use_jvm = true;
            } else if (!strcmp(arg, "--native")) {
                use_native = true;
            } else if (!strncmp(arg, "--vm.classpath", sizeof ("--vm.classpath") - 1)) {
                classpath = arg + sizeof ("--vm.classpath") - 1;
            } else if (!strncmp(arg, "--vm.cp", sizeof ("--vm.cp") - 1)) {
                classpath = arg + sizeof ("--vm.cp") - 1;
            } else if (!strncmp(arg, "--jvm.classpath", sizeof ("--jvm.classpath") - 1)) {
                show_jvm_warning = true;
                classpath = arg + sizeof ("--jvm.classpath") - 1;
            } else if (!strncmp(arg, "--jvm.cp", sizeof ("--jvm.cp") - 1)) {
                show_jvm_warning = true;
                classpath = arg + sizeof ("--jvm.cp") - 1;
            } else if (!strncmp(arg, "--vm.", sizeof ("--vm.") - 1) || !strncmp(arg, "--jvm.", sizeof ("--jvm.") - 1) || (!strncmp(arg, "--native.", sizeof ("--native.") - 1) && strcmp(arg, "--native.help"))) {
                if (arg[2] == 'j') {
                    use_jvm = true;
                    show_jvm_warning = true;
                } else if (arg[2] == 'n') {
                    use_native = true;
                    show_native_warning = true;
                }
                const char *trailing = strchr(arg, '.') + 1;
                if (!vm_args.empty()) {
                    vm_args.append(" ");
                }
                vm_args.append("-").append(trailing);
            } else if (EqualOptions(arg, "--abort-on-uncaught-exception")) {
                GraalIsolate::SetAbortOnUncaughtException(true);
            } else if (EqualOptions(arg, "--max-old-space-size")) {
                char* start = arg + sizeof ("--max-old-space-size");
                char* end = nullptr;
                long value = strtol(start, &end, 10);
                if (value > 0) {
                    if (!vm_args.empty()) {
                        vm_args.append(" ");
                    }
                    char strvalue[20];
                    snprintf(strvalue, 20, "-Xmx%ldm", value);
                    vm_args.append(strvalue);
                } else if (value != 0 || start == end) {
                    fprintf(stderr, "Invalid value of --max-old-space-size option!\n");
                    argv[1] = arg;
                    *argc = 2;
                    return;
                }
            } else if (!strcmp(arg, "--use-classpath-env-var")) {
                GraalIsolate::use_classpath_env_var = true;
            } else {
                if (!strcmp(arg, "--help")) {
                    show_help = true;
                } else if (!strcmp(arg, "--polyglot")) {
                    use_polyglot = true;
                }
                argv[++unprocessed] = arg;
            }
            if (classpath != nullptr) {
                use_jvm = true;
                if (classpath[0] == 0) {
                    index++; // skip next argument - it contains classpath that has been processed already
                }
            }
            if (use_jvm && use_native) {
                fprintf(stderr, "`--jvm` and `--native` options can not be used together.\n");
                argv[1] = arg;
                *argc = 2;
                return;
            }
        }

        if (!vm_args.empty()) {
            char *existing = getenv("NODE_JVM_OPTIONS");
            if (existing != NULL) {
                vm_args.append(" ").append(existing);
            }
            GraalIsolate::SetEnv("NODE_JVM_OPTIONS", vm_args.c_str());
        }

        GraalIsolate::SetMode(use_jvm ? GraalIsolate::kModeJVM : (use_native ? GraalIsolate::kModeNative : GraalIsolate::kModeDefault), use_polyglot);
        GraalIsolate::SetFlags(unprocessed, argv + 1);
        if (remove_flags) {
            // claim that we understood and processed all command line options
            // (we have termined already if we encountered an unknown option)
            *argc = 1;
        }
        if (show_jvm_warning) {
            fprintf(stderr, "'--jvm.*' options are deprecated, use '--vm.*' instead.\n");
        }
        if (show_native_warning) {
            fprintf(stderr, "'--native.*' options are deprecated, use '--vm.*' instead.\n");
        }
        if (show_help) {
            // show help and terminate
            v8::Isolate::CreateParams params;
            GraalIsolate::New(params);
        }
    }

    static char* SkipWhiteSpace(char* p) {
        while (*p != '\0' && isspace(*p) != 0) p++;
        return p;
    }

    static char* SkipBlackSpace(char* p) {
        while (*p != '\0' && isspace(*p) == 0) p++;
        return p;
    }

    void V8::SetFlagsFromString(const char* str) {
        TRACE
        V8::SetFlagsFromString(str, strlen(str));
    }

    void V8::SetFlagsFromString(const char* str, size_t length) {
        // ensure 0-termination
        char* args = new char[length + 1];
        memcpy(args, str, length);
        args[length] = 0;

        // count arguments
        char* p = SkipWhiteSpace(args);
        int argc = 1;
        while (*p != '\0') {
            p = SkipBlackSpace(p);
            p = SkipWhiteSpace(p);
            argc++;
        }

        // fill arguments
        char** argv = new char*[argc];
        p = SkipWhiteSpace(args);
        argc = 1;
        while (*p != '\0') {
            argv[argc] = p;
            p = SkipBlackSpace(p);
            if (*p != '\0') *p++ = '\0'; // 0-terminate argument
            p = SkipWhiteSpace(p);
            argc++;
        }
        SetFlagsFromCommandLine(&argc, argv, false);

        delete[] argv;
        delete[] args;
    }

    void V8::ToLocalEmpty() {
        TRACE
    }

    bool Value::IsExternal() const {
        return reinterpret_cast<const GraalValue*> (this)->IsExternal();
    }

    bool Value::IsObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsObject();
    }

    bool Value::IsBoolean() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBoolean();
    }

    bool Value::IsTrue() const {
        return reinterpret_cast<const GraalValue*> (this)->IsTrue();
    }

    bool Value::IsFalse() const {
        return reinterpret_cast<const GraalValue*> (this)->IsFalse();
    }

    bool Value::IsInt32() const {
        return reinterpret_cast<const GraalValue*> (this)->IsInt32();
    }

    bool Value::IsUint32() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUint32();
    }

    bool Value::IsNumber() const {
        return reinterpret_cast<const GraalValue*> (this)->IsNumber();
    }

    bool Value::IsFunction() const {
        return reinterpret_cast<const GraalValue*> (this)->IsFunction();
    }

    bool Value::IsSymbol() const {
        return reinterpret_cast<const GraalValue*> (this)->IsSymbol();
    }

    bool Value::IsName() const {
        return reinterpret_cast<const GraalValue*> (this)->IsName();
    }

    MaybeLocal<String> Value::ToString(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToString(isolate);
    }

    MaybeLocal<String> Value::ToDetailString(Local<Context> context) const {
        // TODO: Temporary, should be fixed soon
        if (IsSymbol()) {
            return String::NewFromUtf8(context->GetIsolate(), "Symbol()");
        } else if (IsProxy()) {
            return String::NewFromUtf8(context->GetIsolate(), "[object Object]");
        }
        return ToString(context);
    }

    Maybe<int32_t> Value::Int32Value(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        JNIEnv* env = graal_value->Isolate()->GetJNIEnv();
        jthrowable pending = env->ExceptionOccurred();
        if (pending) env->ExceptionClear();
        int32_t result = graal_value->Int32Value();
        if (env->ExceptionCheck()) {
            return Nothing<int32_t>();
        } else {
            if (pending) env->Throw(pending);
            return Just<int32_t>(result);
        }
    }

    Maybe<uint32_t> Value::Uint32Value(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        JNIEnv* env = graal_value->Isolate()->GetJNIEnv();
        jthrowable pending = env->ExceptionOccurred();
        if (pending) env->ExceptionClear();
        uint32_t result = graal_value->Uint32Value();
        if (env->ExceptionCheck()) {
            return Nothing<uint32_t>();
        } else {
            if (pending) env->Throw(pending);
            return Just<uint32_t>(result);
        }
    }

    Maybe<int64_t> Value::IntegerValue(Local<Context> context) const {
        return reinterpret_cast<const GraalValue*> (this)->IntegerValue(context);
    }

    bool Value::BooleanValue(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->BooleanValue();
    }

    Maybe<double> Value::NumberValue(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        JNIEnv* env = graal_value->Isolate()->GetJNIEnv();
        jthrowable pending = env->ExceptionOccurred();
        if (pending) env->ExceptionClear();
        double result = graal_value->NumberValue();
        if (env->ExceptionCheck()) {
            return Nothing<double>();
        } else {
            if (pending) env->Throw(pending);
            return Just<double>(result);
        }
    }

    Local<String> Value::TypeOf(Isolate* isolate) {
        return reinterpret_cast<GraalValue*> (this)->TypeOf(isolate);
    }

    void* External::Value() const {
        const GraalExternal* external = reinterpret_cast<const GraalExternal*> (this);
        return external->Value();
    }

    MaybeLocal<Object> Function::NewInstance(Local<Context> context, int argc, Local<Value> argv[]) const {
        return reinterpret_cast<const GraalFunction*> (this)->NewInstance(argc, argv);
    }

    MaybeLocal<Function> Function::New(
            Local<Context> context,
            FunctionCallback callback,
            Local<Value> data,
            int length,
            ConstructorBehavior behavior,
            SideEffectType side_effect_type) {
        return GraalFunctionTemplate::New(context->GetIsolate(), callback, data, Local<Signature>(), length, behavior, true)->GetFunction(context);
    }

    size_t ArrayBufferView::ByteLength() {
        return reinterpret_cast<GraalArrayBufferView*> (this)->ByteLength();
    }

    uint32_t Array::Length() const {
        return reinterpret_cast<const GraalArray*> (this)->Length();
    }

    size_t ArrayBuffer::ByteLength() const {
        return reinterpret_cast<const GraalArrayBuffer*> (this)->ByteLength();
    }

    int Message::GetEndColumn() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetEndColumn();
    }

    Maybe<int> Message::GetLineNumber(Local<Context> context) const {
        return reinterpret_cast<const GraalMessage*> (this)->GetLineNumber();
    }

    Local<Value> Message::GetScriptResourceName() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetScriptResourceName();
    }

    MaybeLocal<String> Message::GetSourceLine(Local<Context> context) const {
        return reinterpret_cast<const GraalMessage*> (this)->GetSourceLine();
    }

    int Message::GetStartColumn() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetStartColumn();
    }

    Local<String> Message::Get() const {
        return reinterpret_cast<const GraalMessage*> (this)->Get();
    }

    int StackFrame::GetColumn() const {
        return reinterpret_cast<const GraalStackFrame*> (this)->GetColumn();
    }

    Local<String> StackFrame::GetFunctionName() const {
        return reinterpret_cast<const GraalStackFrame*> (this)->GetFunctionName();
    }

    int StackFrame::GetLineNumber() const {
        return reinterpret_cast<const GraalStackFrame*> (this)->GetLineNumber();
    }

    int StackFrame::GetScriptId() const {
        TRACE
        return 0;
    }

    Local<String> StackFrame::GetScriptName() const {
        return reinterpret_cast<const GraalStackFrame*> (this)->GetScriptName();
    }

    bool StackFrame::IsEval() const {
        return reinterpret_cast<const GraalStackFrame*> (this)->IsEval();
    }

    int StackTrace::GetFrameCount() const {
        return reinterpret_cast<const GraalStackTrace*> (this)->GetFrameCount();
    }

    Local<StackFrame> StackTrace::GetFrame(Isolate* isolate, uint32_t index) const {
        return reinterpret_cast<const GraalStackTrace*> (this)->GetFrame(index);
    }

    bool String::IsExternal() const {
        TRACE
        return false;
    }

    bool String::IsExternalOneByte() const {
        TRACE
        return false;
    }

    bool String::IsOneByte() const {
        return reinterpret_cast<const GraalString*> (this)->ContainsOnlyOneByte();
    }

    bool String::ContainsOnlyOneByte() const {
        return reinterpret_cast<const GraalString*> (this)->ContainsOnlyOneByte();
    }

    int String::Length() const {
        return reinterpret_cast<const GraalString*> (this)->Length();
    }

    int String::Utf8Length(Isolate* isolate) const {
        return reinterpret_cast<const GraalString*> (this)->Utf8Length();
    }

    int String::WriteOneByte(Isolate* isolate, uint8_t* buffer, int start, int length, int options) const {
        return reinterpret_cast<const GraalString*> (this)->WriteOneByte(buffer, start, length, options);
    }

    int String::Write(Isolate* isolate, uint16_t* buffer, int start, int length, int options) const {
        return reinterpret_cast<const GraalString*> (this)->Write(buffer, start, length, options);
    }

    int String::WriteUtf8(Isolate* isolate, char* buffer, int length, int* nchars_ref, int options) const {
        return reinterpret_cast<const GraalString*> (this)->WriteUtf8(buffer, length, nchars_ref, options);
    }

    Local<Value> TryCatch::Exception() const {
        if (HasCaught()) {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
            JNIEnv* env = graal_isolate->GetJNIEnv();
            jthrowable java_exception = env->ExceptionOccurred();
            jobject java_context = graal_isolate->CurrentJavaContext();

            // We should not perform the following Java call with a pending exception
            env->ExceptionClear();

            JNI_CALL(jobject, exception_object, graal_isolate, GraalAccessMethod::try_catch_exception, Object, java_context, java_exception);

            // Restore the original pending exception (unless we managed
            // to generate a new one from the call above already)
            if (env->ExceptionCheck()) {
                exception_object = env->ExceptionOccurred();
            } else {
                env->Throw(java_exception);
            }
            GraalValue* graal_exception = GraalValue::FromJavaObject(graal_isolate, exception_object);
            return reinterpret_cast<Value*> (graal_exception);
        } else {
            return Local<Value>();
        }
    }

    bool TryCatch::HasCaught() const {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        if (GraalIsolate::GetAbortOnUncaughtException()
                && env->ExceptionCheck()
                && graal_isolate->AbortOnUncaughtExceptionCallbackValue()) {
            // Yes, this is a bit strange place to perform this check.
            // V8 performs this check when the exception is thrown (which
            // we are unable to do). Hence, we perform the check when
            // fatal_exception_function in FatalException() throws
            // (and hope that the value returned by the callback
            // is the same as if we asked when the exception was thrown).
            // See fatal_try_catch.HasCaught() in node.cc
            abort();
        }
        if (rethrow_) {
            return !env->IsSameObject((jobject) exception_, env->ExceptionOccurred());
        } else {
            return env->ExceptionCheck();
        }
    }

    bool TryCatch::HasTerminated() const {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
        jobject java_exception = graal_isolate->GetJNIEnv()->ExceptionOccurred();
        JNI_CALL(jboolean, terminated, graal_isolate, GraalAccessMethod::try_catch_has_terminated, Boolean, java_exception);
        return terminated;
    }

    Local<Message> TryCatch::Message() const {
        if (HasCaught()) {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
            jobject java_exception = graal_isolate->GetJNIEnv()->ExceptionOccurred();
            GraalMessage* graal_message = GraalMessage::Allocate(graal_isolate, java_exception);
            return reinterpret_cast<v8::Message*> (graal_message);
        } else {
            return Local<v8::Message>();
        }
    }

    void TryCatch::Reset() {
        if (!rethrow_) {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate_);
            graal_isolate->GetJNIEnv()->ExceptionClear();
        }
    }

    Maybe<bool> Value::Equals(Local<Context> context, Local<Value> that) const {
        return reinterpret_cast<const GraalValue*> (this)->Equals(that);
    }

    bool Value::IsArrayBuffer() const {
        return reinterpret_cast<const GraalValue*> (this)->IsArrayBuffer();
    }

    bool Value::IsSharedArrayBuffer() const {
        return reinterpret_cast<const GraalValue*> (this)->IsSharedArrayBuffer();
    }

    bool Value::IsArrayBufferView() const {
        return reinterpret_cast<const GraalValue*> (this)->IsArrayBufferView();
    }

    bool Value::IsArray() const {
        return reinterpret_cast<const GraalValue*> (this)->IsArray();
    }

    bool Value::IsMapIterator() const {
        return reinterpret_cast<const GraalValue*> (this)->IsMapIterator();
    }

    bool Value::IsNativeError() const {
        return reinterpret_cast<const GraalValue*> (this)->IsNativeError();
    }

    bool Value::IsPromise() const {
        return reinterpret_cast<const GraalValue*> (this)->IsPromise();
    }

    bool Value::IsSetIterator() const {
        return reinterpret_cast<const GraalValue*> (this)->IsSetIterator();
    }

    bool Value::IsUint8Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUint8Array();
    }

    bool Value::IsUint8ClampedArray() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUint8ClampedArray();
    }

    bool Value::IsInt8Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsInt8Array();
    }

    bool Value::IsUint16Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUint16Array();
    }

    bool Value::IsInt16Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsInt16Array();
    }

    bool Value::IsUint32Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUint32Array();
    }

    bool Value::IsInt32Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsInt32Array();
    }

    bool Value::IsFloat32Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsFloat32Array();
    }

    bool Value::IsFloat64Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsFloat64Array();
    }

    bool Value::IsBigInt64Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBigInt64Array();
    }

    bool Value::IsBigUint64Array() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBigUint64Array();
    }

    bool Value::StrictEquals(Local<Value> that) const {
        return reinterpret_cast<const GraalValue*> (this)->StrictEquals(that);
    }

    Local<Boolean> Value::ToBoolean(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToBoolean(isolate);
    }

    MaybeLocal<Integer> Value::ToInteger(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToInteger(isolate);
    }

    MaybeLocal<Int32> Value::ToInt32(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToInt32(isolate);
    }

    MaybeLocal<Uint32> Value::ToUint32(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToUint32(isolate);
    }

    MaybeLocal<Object> Value::ToObject(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToObject(isolate);
    }

    MaybeLocal<Number> Value::ToNumber(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToNumber(isolate);
    }

    MaybeLocal<Uint32> Value::ToArrayIndex(Local<Context> context) const {
        return reinterpret_cast<const GraalValue*> (this)->ToArrayIndex();
    }

    void internal::Internals::CheckInitializedImpl(v8::Isolate* external_isolate) {
    }

    bool Value::FullIsUndefined() const {
        return reinterpret_cast<const GraalValue*> (this)->IsUndefined();
    }

    bool Value::FullIsNull() const {
        return reinterpret_cast<const GraalValue*> (this)->IsNull();
    }

    bool Value::FullIsString() const {
        return reinterpret_cast<const GraalValue*> (this)->IsString();
    }

    void* Context::SlowGetAlignedPointerFromEmbedderData(int index) {
        return reinterpret_cast<GraalContext*> (this)->SlowGetAlignedPointerFromEmbedderData(index);
    }

    double Number::Value() const {
        return reinterpret_cast<const GraalNumber*> (this)->Value();
    }

    int64_t Integer::Value() const {
        return reinterpret_cast<const GraalNumber*> (this)->Value();
    }

    int32_t Int32::Value() const {
        return reinterpret_cast<const GraalNumber*> (this)->Value();
    }

    uint32_t Uint32::Value() const {
        return reinterpret_cast<const GraalNumber*> (this)->Value();
    }

    bool Boolean::Value() const {
        return reinterpret_cast<const GraalBoolean*> (this)->Value();
    }

    bool Value::IsDate() const {
        return reinterpret_cast<const GraalValue*> (this)->IsDate();
    }

    MaybeLocal<Value> Date::New(Local<Context> context, double time) {
        return GraalDate::New(context, time);
    }

    Local<UnboundScript> Script::GetUnboundScript() {
        return reinterpret_cast<GraalScript*> (this)->GetUnboundScript();
    }

    int UnboundScript::GetId() {
        return reinterpret_cast<GraalUnboundScript*> (this)->GetId();
    }

    bool Value::IsRegExp() const {
        return reinterpret_cast<const GraalValue*> (this)->IsRegExp();
    }

    void Isolate::SetCaptureStackTraceForUncaughtExceptions(bool capture, int frame_limit, StackTrace::StackTraceOptions options) {
        TRACE
    }

    void Isolate::AddGCPrologueCallback(GCCallback callback, GCType gc_type_filter) {
        reinterpret_cast<GraalIsolate*> (this)->AddGCPrologueCallback(GraalIsolate::kIsolateGCCallbackType, (void*) callback);
    }

    void Isolate::AddGCEpilogueCallback(GCCallback callback, GCType gc_type_filter) {
        reinterpret_cast<GraalIsolate*> (this)->AddGCEpilogueCallback(GraalIsolate::kIsolateGCCallbackType, (void*) callback);
    }

    void Isolate::RemoveGCPrologueCallback(GCCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->RemoveGCPrologueCallback((void*) callback);
    }

    void Isolate::RemoveGCEpilogueCallback(GCCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->RemoveGCEpilogueCallback((void*) callback);
    }

    void Isolate::RequestGarbageCollectionForTesting(GarbageCollectionType type) {
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_perform_gc);
    }

    bool Isolate::IdleNotificationDeadline(double deadline_in_seconds) {
        RequestGarbageCollectionForTesting(kFullGarbageCollection);
        return true;
    }

    void Isolate::LowMemoryNotification() {
        RequestGarbageCollectionForTesting(kFullGarbageCollection);
    }

    void Isolate::SetStackLimit(uintptr_t stack_limit) {
        TRACE
    }

    Local<StackTrace> Message::GetStackTrace() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetStackTrace();
    }

    Local<Message> Exception::CreateMessage(Isolate* isolate, Local<Value> exception) {
        GraalValue* graal_exception = reinterpret_cast<GraalValue*> (*exception);
        jobject exception_object = graal_exception->GetJavaObject();
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        JNI_CALL(jobject, java_exception, graal_isolate, GraalAccessMethod::exception_create_message, Object, exception_object);
        return reinterpret_cast<v8::Message*> (GraalMessage::Allocate(graal_isolate, java_exception));
    }

    Maybe<bool> Object::Has(Local<Context> context, Local<Value> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->Has(key));
    }

    Maybe<bool> Object::Has(Local<Context> context, uint32_t index) {
        return Has(context, Integer::NewFromUnsigned(context->GetIsolate(), index));
    }

    MaybeLocal<Value> Object::GetPrivate(Local<Context> context, Local<Private> key) {
        return reinterpret_cast<GraalObject*> (this)->GetPrivate(context, key);
    }

    Maybe<bool> Object::HasPrivate(Local<Context> context, Local<Private> key) {
        return reinterpret_cast<GraalObject*> (this)->HasPrivate(context, key);
    }

    Maybe<bool> Object::SetPrivate(Local<Context> context, Local<Private> key, Local<Value> value) {
        return reinterpret_cast<GraalObject*> (this)->SetPrivate(context, key, value);
    }

    Maybe<bool> Object::DeletePrivate(Local<Context> context, Local<Private> key) {
        return reinterpret_cast<GraalObject*> (this)->DeletePrivate(context, key);
    }

    Maybe<int> Message::GetStartColumn(Local<Context> context) const {
        return Just(GetStartColumn());
    }

    Maybe<int> Message::GetEndColumn(Local<Context> context) const {
        return Just(GetEndColumn());
    }

    Maybe<bool> Object::DefineOwnProperty(Local<Context> context, Local<Name> key, Local<Value> value, PropertyAttribute attributes) {
        return Just(reinterpret_cast<GraalObject*> (this)->ForceSet(key, value, attributes));
    }

    MaybeLocal<Script> Script::Compile(Local<Context> context, Local<String> source, ScriptOrigin* origin) {
        return GraalScript::Compile(source, origin);
    }

    Local<Private> Private::ForApi(Isolate* isolate, Local<String> name) {
        return reinterpret_cast<Private*> (*name);
    }

    void Isolate::ReportExternalAllocationLimitReached() {
        TRACE
    }

    ScriptCompiler::CachedData::CachedData(const uint8_t* data, int length, BufferPolicy buffer_policy): data(data), length(length), rejected(false), buffer_policy(buffer_policy) {
    }

    MaybeLocal<UnboundScript> ScriptCompiler::CompileUnboundScript(
            Isolate* isolate,
            Source* source,
            CompileOptions options,
            NoCacheReason no_cache_reason) {
        if (options == ScriptCompiler::kConsumeCodeCache) {
            String::Utf8Value text(isolate, source->source_string);
            CachedData* data = source->cached_data;
            if (data->length != text.length() || memcmp(data->data, (const uint8_t*) *text, text.length())) {
                data->rejected = true;
            }
        }
        Local<Value> resource_name = source->resource_name;
        Local<String> file_name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate->GetCurrentContext()).ToLocalChecked();
        return GraalUnboundScript::Compile(source->source_string, file_name, source->host_defined_options);
    }

    bool Value::IsDataView() const {
        return reinterpret_cast<const GraalValue*> (this)->IsDataView();
    }

    bool Value::IsTypedArray() const {
        return IsUint8Array() ||
                IsUint8ClampedArray() ||
                IsInt8Array() ||
                IsUint16Array() ||
                IsInt16Array() ||
                IsUint32Array() ||
                IsInt32Array() ||
                IsFloat32Array() ||
                IsFloat64Array() ||
                IsBigInt64Array() ||
                IsBigUint64Array();
    }

    bool Value::IsMap() const {
        return reinterpret_cast<const GraalValue*> (this)->IsMap();
    }

    bool Value::IsSet() const {
        return reinterpret_cast<const GraalValue*> (this)->IsSet();
    }

    bool Value::IsProxy() const {
        return reinterpret_cast<const GraalValue*> (this)->IsProxy();
    }

    Local<Value> Proxy::GetTarget() {
        return reinterpret_cast<GraalProxy*> (this)->GetTarget();
    }

    Local<Value> Proxy::GetHandler() {
        return reinterpret_cast<GraalProxy*> (this)->GetHandler();
    }

    HeapSpaceStatistics::HeapSpaceStatistics() {
    }

    bool Isolate::GetHeapSpaceStatistics(HeapSpaceStatistics* space_statistics, size_t index) {
        if (index >= NumberOfHeapSpaces()) {
            return false;
        }
        const char* names[] = {
            "read_only_space",
            "new_space",
            "old_space",
            "code_space",
            "map_space",
            "large_object_space",
            "code_large_object_space",
            "new_large_object_space"
        };
        space_statistics->space_name_ = names[index];
        space_statistics->space_size_ = 0;
        space_statistics->space_used_size_ = 0;
        space_statistics->space_available_size_ = 0;
        space_statistics->physical_space_size_ = 0;
        return true;
    }

    size_t Isolate::NumberOfHeapSpaces() {
        return 8;
    }

    Local<AccessorSignature> AccessorSignature::New(Isolate* isolate, Local<FunctionTemplate> receiver) {
        return reinterpret_cast<AccessorSignature*> (*receiver);
    }

    MaybeLocal<String> String::NewExternalOneByte(Isolate* isolate, ExternalOneByteStringResource* resource) {
        return GraalString::NewExternal(isolate, resource);
    }

    MaybeLocal<String> String::NewExternalTwoByte(Isolate* isolate, ExternalStringResource* resource) {
        return GraalString::NewExternal(isolate, resource);
    }

    Local<Value> BooleanObject::New(Isolate* isolate, bool value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jboolean java_value = value;
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::boolean_object_new, Object, java_context, java_value);
        GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
        return reinterpret_cast<BooleanObject*> (graal_object);
    }

    bool BooleanObject::ValueOf() const {
        const GraalObject* graal_object = reinterpret_cast<const GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jboolean, value, graal_isolate, GraalAccessMethod::boolean_object_value_of, Boolean, java_object);
        return value;
    }

    Local<Value> StringObject::New(Isolate* isolate, Local<String> value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        GraalString* graal_value = reinterpret_cast<GraalString*> (*value);
        jobject java_value = graal_value->GetJavaObject();
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::string_object_new, Object, java_context, java_value);
        GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
        return reinterpret_cast<StringObject*> (graal_object);
    }

    Local<String> StringObject::ValueOf() const {
        const GraalObject* graal_object = reinterpret_cast<const GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jobject, value, graal_isolate, GraalAccessMethod::string_object_value_of, Object, java_object);
        GraalString* graal_string = GraalString::Allocate(graal_isolate, (jstring) value);
        return reinterpret_cast<v8::String*> (graal_string);
    }

    Local<Value> NumberObject::New(Isolate* isolate, double value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jdouble java_value = value;
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::number_object_new, Object, java_context, java_value);
        GraalObject* graal_object = GraalObject::Allocate(graal_isolate, java_object);
        return reinterpret_cast<NumberObject*> (graal_object);
    }

    MaybeLocal<RegExp> RegExp::New(Local<Context> context, Local<String> pattern, Flags flags) {
        return GraalRegExp::New(context, pattern, flags);
    }

    Local<String> RegExp::GetSource() const {
        return reinterpret_cast<const GraalRegExp*> (this)->GetSource();
    }

    RegExp::Flags RegExp::GetFlags() const {
        return reinterpret_cast<const GraalRegExp*> (this)->GetFlags();
    }

    void Isolate::SaveReturnValue(double value) {
        reinterpret_cast<GraalIsolate*> (this)->SaveReturnValue(value);
    }

    Local<Value> Isolate::CorrectReturnValue(internal::Address value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (this);
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (value);
        Local<Value> v8_value;
        if (graal_value == nullptr) {
            v8_value = Undefined(this);
        } else {
            jobject java_value = graal_value->GetJavaObject();
            if (java_value == graal_isolate->int32_placeholder_) {
                v8_value = GraalNumber::New(this, (int32_t) graal_isolate->return_value_);
            } else if (java_value == graal_isolate->uint32_placeholder_) {
                v8_value = GraalNumber::NewFromUnsigned(this, (uint32_t) graal_isolate->return_value_);
            } else if (java_value == graal_isolate->double_placeholder_) {
                v8_value = GraalNumber::New(this, graal_isolate->return_value_);
            } else {
                v8_value = reinterpret_cast<Value*> (graal_value);
            }
        }
        return v8_value;
    }

    void Isolate::EnterPolyglotEngine(void* param1, void* param2, void* args, void* exec_args, void (*callback) (void* isolate, void* param1, void* param2, void* args, void* exec_args)) {
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enter_polyglot_engine, (jlong) callback, (jlong) this, (jlong) param1, (jlong) param2, (jlong) args, (jlong) exec_args);
    }

    MaybeLocal<Value> JSON::Parse(Local<Context> context, Local<String> json_string) {
        GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
        GraalIsolate* graal_isolate = graal_context->Isolate();
        jobject java_context = graal_context->GetJavaObject();
        jobject java_string = reinterpret_cast<GraalString*> (*json_string)->GetJavaObject();
        JNI_CALL(jobject, java_value, graal_isolate, GraalAccessMethod::json_parse, Object, java_context, java_string);
        if (java_value == nullptr) {
            return Local<Value>();
        } else {
            GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_value);
            return Local<Value>(reinterpret_cast<Value*> (graal_value));
        }
    }

    MaybeLocal<String> JSON::Stringify(Local<Context> context, Local<Value> json_object, Local<String> gap) {
        GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
        GraalIsolate* graal_isolate = graal_context->Isolate();
        jobject java_context = graal_context->GetJavaObject();
        jobject java_object = reinterpret_cast<GraalObject*> (*json_object)->GetJavaObject();
        jstring java_gap = gap.IsEmpty() ? nullptr : (jstring) reinterpret_cast<GraalString*> (*gap)->GetJavaObject();
        JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::json_stringify, Object, java_context, java_object, java_gap);
        if (java_result == nullptr) {
            return Local<String>();
        } else {
            GraalString* graal_string = GraalString::Allocate(graal_isolate, (jstring) java_result);
            return Local<String>(reinterpret_cast<String*> (graal_string));
        }
    }

    void Isolate::AddGCEpilogueCallback(GCCallbackWithData callback, void* data, GCType gc_type_filter) {
        reinterpret_cast<GraalIsolate*> (this)->AddGCEpilogueCallback(GraalIsolate::kIsolateGCCallbackWithDataType, (void*) callback, data);
    }

    void Isolate::RemoveGCEpilogueCallback(GCCallbackWithData callback, void* data) {
        reinterpret_cast<GraalIsolate*> (this)->RemoveGCEpilogueCallback((void*) callback);
    }

    void Isolate::AddGCPrologueCallback(GCCallbackWithData callback, void* data, GCType gc_type_filter) {
        reinterpret_cast<GraalIsolate*> (this)->AddGCPrologueCallback(GraalIsolate::kIsolateGCCallbackWithDataType, (void*) callback, data);
    }

    void Isolate::RemoveGCPrologueCallback(GCCallbackWithData callback, void* data) {
        reinterpret_cast<GraalIsolate*> (this)->RemoveGCPrologueCallback((void*) callback);
    }

    void Isolate::SetPromiseHook(PromiseHook hook) {
        reinterpret_cast<GraalIsolate*> (this)->SetPromiseHook(hook);
    }

    bool Value::IsAsyncFunction() const {
        return reinterpret_cast<const GraalValue*> (this)->IsAsyncFunction();
    }

    Maybe<bool> Value::InstanceOf(Local<Context> context, Local<Object> object) {
        return Just(reinterpret_cast<GraalValue*> (this)->InstanceOf(object));
    }

    Local<Value> Function::GetDebugName() const {
        return GetName();
    }

    Local<Value> Function::GetBoundFunction() const {
        TRACE
        return Local<Value>();
    }

    Local<Symbol> Symbol::New(Isolate* isolate, Local<String> name) {
        return GraalSymbol::New(isolate, name.IsEmpty() ? String::Empty(isolate) : name);
    }

    Local<Value> Symbol::Description() const {
        return reinterpret_cast<const GraalSymbol*> (this)->Name();
    }

    Local<Symbol> Symbol::GetIterator(Isolate* isolate) {
        return GraalSymbol::GetIterator(isolate);
    }

    Local<Private> Private::New(Isolate* isolate, Local<String> name) {
        return reinterpret_cast<Private*> (*name);
    }

    Local<Value> Promise::Result() {
        return reinterpret_cast<GraalPromise*> (this)->Result();
    }

    Promise::PromiseState Promise::State() {
        return reinterpret_cast<GraalPromise*> (this)->State();
    }

    MaybeLocal<Promise::Resolver> Promise::Resolver::New(Local<Context> context) {
        return GraalPromise::ResolverNew(context);
    }

    Maybe<bool> Promise::Resolver::Resolve(Local<Context> context, Local<Value> value) {
        return GraalPromise::ResolverResolve(this, value);
    }

    Maybe<bool> Promise::Resolver::Reject(Local<Context> context, Local<Value> value) {
        return GraalPromise::ResolverReject(this, value);
    }

    Local<Promise> Promise::Resolver::GetPromise() {
        return GraalPromise::ResolverGetPromise(this);
    }

    void Isolate::EnqueueMicrotask(MicrotaskCallback microtask, void* data) {
        reinterpret_cast<GraalIsolate*> (this)->EnqueueMicrotask(microtask, data);
    }

    Maybe<bool> Object::DefineProperty(Local<Context> context, Local<Name> key, PropertyDescriptor& descriptor) {
        return reinterpret_cast<GraalObject*> (this)->DefineProperty(context, key, descriptor);
    }

    MaybeLocal<Value> Object::GetOwnPropertyDescriptor(Local<Context> context, Local<Name> key) {
        return reinterpret_cast<GraalObject*> (this)->GetOwnPropertyDescriptor(context, key);
    }

    MaybeLocal<Value> Module::Evaluate(Local<Context> context) {
        return reinterpret_cast<GraalModule*> (this)->Evaluate(context);
    }

    Maybe<bool> Module::InstantiateModule(Local<Context> context, ResolveCallback callback) {
        return reinterpret_cast<GraalModule*> (this)->InstantiateModule(context, callback);
    }

    Module::Status Module::GetStatus() const {
        return reinterpret_cast<const GraalModule*> (this)->GetStatus();
    }

    Local<Value> Module::GetModuleNamespace() {
        return reinterpret_cast<GraalModule*> (this)->GetModuleNamespace();
    }

    int Module::GetIdentityHash() const {
        return reinterpret_cast<const GraalModule*> (this)->GetIdentityHash();
    }

    int Module::GetModuleRequestsLength() const {
        return reinterpret_cast<const GraalModule*> (this)->GetModuleRequestsLength();
    }

    Local<String> Module::GetModuleRequest(int i) const {
        return reinterpret_cast<const GraalModule*> (this)->GetModuleRequest(i);
    }

    Local<Module> Module::CreateSyntheticModule(
            Isolate* isolate, Local<String> module_name,
            const std::vector<Local<String>>&export_names,
            SyntheticModuleEvaluationSteps evaluation_steps) {
        return GraalModule::CreateSyntheticModule(isolate, module_name, export_names, evaluation_steps);
    }

    void Module::SetSyntheticModuleExport(Local<String> export_name, Local<Value> export_value) {
        reinterpret_cast<GraalModule*> (this)->SetSyntheticModuleExport(export_name, export_value);
    }

    Maybe<bool> Module::SetSyntheticModuleExport(Isolate* isolate, Local<String> export_name, Local<Value> export_value) {
        reinterpret_cast<GraalModule*> (this)->SetSyntheticModuleExport(export_name, export_value);
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        return graal_isolate->GetJNIEnv()->ExceptionCheck() ? Nothing<bool>() : Just<bool>(true);
    }

    MaybeLocal<Module> ScriptCompiler::CompileModule(Isolate* isolate, Source* source,
            CompileOptions options, NoCacheReason no_cache_reason) {
        if (options == ScriptCompiler::kConsumeCodeCache) {
            String::Utf8Value text(isolate, source->source_string);
            CachedData* data = source->cached_data;
            if (data->length != text.length() || memcmp(data->data, (const uint8_t*) *text, text.length())) {
                data->rejected = true;
            }
        }
        Local<Value> resource_name = source->resource_name;
        Local<String> name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate->GetCurrentContext()).ToLocalChecked();
        return GraalModule::Compile(source->source_string, name, source->host_defined_options);
    }

    Local<UnboundModuleScript> Module::GetUnboundModuleScript() {
        return reinterpret_cast<GraalModule*> (this)->GetUnboundModuleScript();
    }

    uint32_t ScriptCompiler::CachedDataVersionTag() {
        TRACE
        return 0;
    }

    Maybe<bool> Object::SetIntegrityLevel(Local<Context> context, IntegrityLevel level) {
        return reinterpret_cast<GraalObject*> (this)->SetIntegrityLevel(context, level);
    }

    ScriptOrigin Message::GetScriptOrigin() const {
        TRACE
        const GraalMessage* graal_message = reinterpret_cast<const GraalMessage*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_message->Isolate());
        Local<Integer> zero = Integer::New(isolate, 0);
        return ScriptOrigin(String::NewFromUtf8(isolate, "unknown").ToLocalChecked(), zero, zero);
    }

    class DefaultArrayBufferAllocator : public v8::ArrayBuffer::Allocator {
    public:

        void* Allocate(size_t length) override {
            return calloc(length, 1);
        }

        void* AllocateUninitialized(size_t length) override {
            return malloc(length);
        }

        void Free(void* data, size_t) override {
            free(data);
        }
    };

    void ArrayBuffer::Allocator::Free(void* data, size_t length) {
        free(data);
    }

    ArrayBuffer::Allocator* ArrayBuffer::Allocator::NewDefaultAllocator() {
        return new DefaultArrayBufferAllocator();
    }

    namespace internal {

    class PropertyDescriptor {
    public:
        PropertyDescriptor();
    private:
        bool enumerable_ : 1;
        bool has_enumerable_ : 1;
        bool configurable_ : 1;
        bool has_configurable_ : 1;
        bool writable_ : 1;
        bool has_writable_ : 1;
        Local<Value> value_;
        Local<Value> get_;
        Local<Value> set_;
        friend v8::PropertyDescriptor;
    };

    PropertyDescriptor::PropertyDescriptor() :
        enumerable_(false),
        has_enumerable_(false),
        configurable_(false),
        has_configurable_(false),
        writable_(false),
        has_writable_(false) {
    }

    }

    struct v8::PropertyDescriptor::PrivateData {
        PrivateData() : desc() {}
        internal::PropertyDescriptor desc;
    };

    PropertyDescriptor::PropertyDescriptor() : private_(new PrivateData()) {}

    PropertyDescriptor::PropertyDescriptor(Local<Value> value) : private_(new PrivateData()) {
        private_->desc.value_ = value;
    }

    PropertyDescriptor::PropertyDescriptor(Local<Value> value, bool writable) : private_(new PrivateData()) {
        private_->desc.value_ = value;
        private_->desc.has_writable_ = true;
        private_->desc.writable_ = writable;
    }

    PropertyDescriptor::PropertyDescriptor(Local<Value> get, Local<Value> set) : private_(new PrivateData()) {
        private_->desc.get_ = get;
        private_->desc.set_ = set;
    }

    PropertyDescriptor::~PropertyDescriptor() {
        delete private_;
    }

    Local<Value> PropertyDescriptor::value() const {
        return private_->desc.value_;
    }

    bool PropertyDescriptor::has_value() const {
        return !private_->desc.value_.IsEmpty();
    }

    Local<Value> PropertyDescriptor::get() const {
        return private_->desc.get_;
    }

    bool PropertyDescriptor::has_get() const {
        return !private_->desc.get_.IsEmpty();
    }

    Local<Value> PropertyDescriptor::set() const {
        return private_->desc.set_;
    }

    bool PropertyDescriptor::has_set() const {
        return !private_->desc.set_.IsEmpty();
    }

    void PropertyDescriptor::set_enumerable(bool enumerable) {
        private_->desc.has_enumerable_ = true;
        private_->desc.enumerable_ = enumerable;
    }

    bool PropertyDescriptor::enumerable() const {
        return private_->desc.enumerable_;
    }

    bool PropertyDescriptor::has_enumerable() const {
        return private_->desc.has_enumerable_;
    }

    void PropertyDescriptor::set_configurable(bool configurable) {
        private_->desc.has_configurable_ = true;
        private_->desc.configurable_ = configurable;
    }

    bool PropertyDescriptor::configurable() const {
        return private_->desc.configurable_;
    }

    bool PropertyDescriptor::has_configurable() const {
        return private_->desc.has_configurable_;
    }

    bool PropertyDescriptor::writable() const {
        return private_->desc.writable_;
    }

    bool PropertyDescriptor::has_writable() const {
        return private_->desc.has_writable_;
    }

    struct ValueSerializer::PrivateData {

        PrivateData(Isolate* i, Delegate* d) : isolate(i), delegate(d) {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (i);
            JNI_CALL(jobject, result, graal_isolate, GraalAccessMethod::value_serializer_new, Object, (jlong) d);
            JNIEnv* env = graal_isolate->GetJNIEnv();
            serializer = env->NewGlobalRef(result);
            env->DeleteLocalRef(result);
        }

        ~PrivateData() {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
            graal_isolate->GetJNIEnv()->DeleteGlobalRef(serializer);
        }

        Isolate* isolate;
        Delegate* delegate;
        jobject serializer;
    };

    ValueSerializer::ValueSerializer(Isolate* isolate, Delegate* delegate) : private_(new PrivateData(isolate, delegate)) {
    }

    ValueSerializer::~ValueSerializer() {
        delete private_;
    }

    void ValueSerializer::WriteUint32(uint32_t value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_uint32, private_->serializer, (jint) value);
    }

    void ValueSerializer::WriteUint64(uint64_t value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_uint64, private_->serializer, (jlong) value);
    }

    void ValueSerializer::WriteDouble(double value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_double, private_->serializer, (jdouble) value);
    }

    void ValueSerializer::WriteRawBytes(const void* source, size_t length) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        jobject java_buffer = env->NewDirectByteBuffer(const_cast<void*> (source), length);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_raw_bytes, private_->serializer, java_buffer);
        env->DeleteLocalRef(java_buffer);
    }

    Maybe<bool> ValueSerializer::WriteValue(Local<Context> context, Local<Value> value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*value);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_value, private_->serializer, graal_value->GetJavaObject());
        return graal_isolate->GetJNIEnv()->ExceptionCheck() ? Nothing<bool>() : Just<bool>(true);
    }

    void ValueSerializer::SetTreatArrayBufferViewsAsHostObjects(bool mode) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_set_treat_array_buffer_views_as_host_objects, private_->serializer, (jboolean) mode);
    }

    void ValueSerializer::TransferArrayBuffer(uint32_t transfer_id, Local<ArrayBuffer> array_buffer) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*array_buffer);
        jobject java_value = graal_value->GetJavaObject();
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_transfer_array_buffer, private_->serializer, (jint) transfer_id, java_value);
    }

    std::pair<uint8_t*, size_t> ValueSerializer::Release() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jint, size, graal_isolate, GraalAccessMethod::value_serializer_size, Int, private_->serializer);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        void* address = private_->delegate->ReallocateBufferMemory(nullptr, size, nullptr);
        jobject buffer = env->NewDirectByteBuffer(address, size);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_release, private_->serializer, buffer);
        return {(uint8_t*) address, (size_t) size};
    }

    void ValueSerializer::WriteHeader() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_serializer_write_header, private_->serializer);
    }

    Maybe<bool> ValueSerializer::Delegate::WriteHostObject(Isolate* isolate, Local<Object> object) {
        isolate->ThrowException(Exception::Error(String::NewFromUtf8(isolate, "Host object could not be cloned.").ToLocalChecked()));
        return Nothing<bool>();
    }

    Maybe<uint32_t> ValueSerializer::Delegate::GetSharedArrayBufferId(Isolate* isolate, Local<SharedArrayBuffer> shared_array_buffer) {
        isolate->ThrowException(Exception::Error(String::NewFromUtf8(isolate, "#<SharedArrayBuffer> could not be cloned.").ToLocalChecked()));
        return Nothing<uint32_t>();
    }

    Maybe<uint32_t> ValueSerializer::Delegate::GetWasmModuleTransferId(Isolate* isolate, Local<WasmModuleObject> module) {
        isolate->ThrowException(Exception::Error(String::NewFromUtf8(isolate, "Wasm module could not be transferred.").ToLocalChecked()));
        return Nothing<uint32_t>();
    }

    void* ValueSerializer::Delegate::ReallocateBufferMemory(void* old_buffer, size_t size, size_t* actual_size) {
        if (actual_size) {
            *actual_size = size;
        }
        void* address = realloc(old_buffer, size);
        return address;
    }

    void ValueSerializer::Delegate::FreeBufferMemory(void* buffer) {
        return free(buffer);
    }

    struct ValueDeserializer::PrivateData {

        PrivateData(Isolate* i, const uint8_t* data, size_t size, Delegate* d) : isolate(i), delegate(d) {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (i);
            JNIEnv* env = graal_isolate->GetJNIEnv();
            jobject buffer = env->NewDirectByteBuffer(const_cast<uint8_t*> (data), size);
            JNI_CALL(jobject, result, graal_isolate, GraalAccessMethod::value_deserializer_new, Object, (jlong) d, buffer);
            env->DeleteLocalRef(buffer);
            deserializer = env->NewGlobalRef(result);
            env->DeleteLocalRef(result);
            this->data = data;
        }

        ~PrivateData() {
            GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
            graal_isolate->GetJNIEnv()->DeleteGlobalRef(deserializer);
        }

        Isolate* isolate;
        Delegate* delegate;
        jobject deserializer;
        const uint8_t* data;
    };

    ValueDeserializer::ValueDeserializer(Isolate* isolate, const uint8_t* data, size_t size, Delegate* delegate) : private_(new PrivateData(isolate, data, size, delegate)) {
    }

    ValueDeserializer::~ValueDeserializer() {
        delete private_;
    }

    Maybe<bool> ValueDeserializer::ReadHeader(Local<Context> context) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_deserializer_read_header, private_->deserializer);
        return graal_isolate->GetJNIEnv()->ExceptionCheck() ? Nothing<bool>() : Just<bool>(true);
    }

    MaybeLocal<Value> ValueDeserializer::ReadValue(Local<Context> context) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
        JNI_CALL(jobject, java_value, graal_isolate, GraalAccessMethod::value_deserializer_read_value, Object, graal_context->GetJavaObject(), private_->deserializer);
        GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_value);
        Local<Value> v8_value = reinterpret_cast<Value*> (graal_value);
        return v8_value;
    }

    bool ValueDeserializer::ReadDouble(double* value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jdouble, result, graal_isolate, GraalAccessMethod::value_deserializer_read_double, Double, private_->deserializer);
        *value = result;
        return true;
    }

    bool ValueDeserializer::ReadUint32(uint32_t* value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jint, result, graal_isolate, GraalAccessMethod::value_deserializer_read_uint32, Int, private_->deserializer);
        *value = result;
        return true;
    }

    bool ValueDeserializer::ReadUint64(uint64_t* value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jlong, result, graal_isolate, GraalAccessMethod::value_deserializer_read_uint64, Long, private_->deserializer);
        *value = result;
        return true;
    }

    bool ValueDeserializer::ReadRawBytes(size_t length, const void** data) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jint, position, graal_isolate, GraalAccessMethod::value_deserializer_read_raw_bytes, Int, private_->deserializer, (jint) length);
        *data = private_->data + position;
        return true;
    }

    MaybeLocal<Object> ValueDeserializer::Delegate::ReadHostObject(Isolate* isolate) {
        isolate->ThrowException(Exception::Error(String::NewFromUtf8(isolate, "Host object could not be cloned.").ToLocalChecked()));
        return MaybeLocal<Object>();
    }

    MaybeLocal<WasmModuleObject> ValueDeserializer::Delegate::GetWasmModuleFromId(Isolate* isolate, uint32_t transfer_id) {
        isolate->ThrowException(Exception::Error(String::NewFromUtf8(isolate, "Wasm module could not be transferred.").ToLocalChecked()));
        return MaybeLocal<WasmModuleObject>();
    }

    uint32_t ValueDeserializer::GetWireFormatVersion() const {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        JNI_CALL(jint, version, graal_isolate, GraalAccessMethod::value_deserializer_get_wire_format_version, Int, private_->deserializer);
        return version;
    }

    void ValueDeserializer::TransferArrayBuffer(uint32_t transfer_id, Local<ArrayBuffer> array_buffer) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*array_buffer);
        jobject java_value = graal_value->GetJavaObject();
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_deserializer_transfer_array_buffer, private_->deserializer, (jint) transfer_id, java_value);
    }

    void ValueDeserializer::TransferSharedArrayBuffer(uint32_t id, Local<SharedArrayBuffer> shared_array_buffer) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (private_->isolate);
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*shared_array_buffer);
        jobject java_value = graal_value->GetJavaObject();
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::value_deserializer_transfer_array_buffer, private_->deserializer, (jint) id, java_value);
    }

    v8::platform::tracing::TraceObject::~TraceObject() {
        TRACE
    }

    void v8::platform::tracing::TraceObject::Initialize(
            char phase, const uint8_t* category_enabled_flag, const char* name,
            const char* scope, uint64_t id, uint64_t bind_id, int num_args,
            const char** arg_names, const uint8_t* arg_types,
            const uint64_t* arg_values,
            std::unique_ptr<v8::ConvertableToTraceFormat>* arg_convertables,
            unsigned int flags, int64_t timestamp, int64_t cpu_timestamp) {
        TRACE
    }

    v8::platform::tracing::TraceBufferChunk::TraceBufferChunk(uint32_t seq) {
        TRACE
    }

    void v8::platform::tracing::TraceBufferChunk::Reset(uint32_t new_seq) {
        TRACE
    }

    v8::platform::tracing::TraceObject* v8::platform::tracing::TraceBufferChunk::AddTraceEvent(size_t* event_index) {
        TRACE
        return nullptr;
    }

    v8::platform::tracing::TraceWriter* v8::platform::tracing::TraceWriter::CreateJSONTraceWriter(std::ostream&) {
        TRACE
        return nullptr;
    }

    void v8::platform::tracing::TraceConfig::AddIncludedCategory(const char* included_category) {
        TRACE
    }

    void v8::platform::tracing::TracingController::StartTracing(TraceConfig* trace_config) {
        TRACE
    }

    void v8::platform::tracing::TracingController::StopTracing() {
        TRACE
    }

    void v8::platform::tracing::TracingController::Initialize(TraceBuffer* trace_buffer) {
        TRACE
    }

    int64_t v8::platform::tracing::TracingController::CurrentTimestampMicroseconds() {
        TRACE
        return 0;
    }

    int64_t v8::platform::tracing::TracingController::CurrentCpuTimestampMicroseconds() {
        TRACE
        return 0;
    }

    v8::platform::tracing::TracingController::TracingController() {
        TRACE
    }

    v8::platform::tracing::TracingController::~TracingController() {
        TRACE
    }

    // Tracing is disabled
    static uint8_t CategoryGroupEnabled = 0;
    const uint8_t* v8::platform::tracing::TracingController::GetCategoryGroupEnabled(const char* category_group) {
        return &CategoryGroupEnabled;
    }

    uint64_t v8::platform::tracing::TracingController::AddTraceEvent(
            char phase, const uint8_t* category_enabled_flag, const char* name,
            const char* scope, uint64_t id, uint64_t bind_id, int32_t num_args,
            const char** arg_names, const uint8_t* arg_types,
            const uint64_t* arg_values,
            std::unique_ptr<v8::ConvertableToTraceFormat>* arg_convertables,
            unsigned int flags) {
        TRACE
        return 0;
    }

    uint64_t v8::platform::tracing::TracingController::AddTraceEventWithTimestamp(
            char phase, const uint8_t* category_enabled_flag, const char* name,
            const char* scope, uint64_t id, uint64_t bind_id, int32_t num_args,
            const char** arg_names, const uint8_t* arg_types,
            const uint64_t* arg_values,
            std::unique_ptr<v8::ConvertableToTraceFormat>* arg_convertables,
            unsigned int flags, int64_t timestamp) {
        TRACE
        return 0;
    }

    void v8::platform::tracing::TracingController::UpdateTraceEventDuration(
            const uint8_t* category_enabled_flag, const char* name, uint64_t handle) {
        TRACE
    }

    void v8::platform::tracing::TracingController::AddTraceStateObserver(
            v8::TracingController::TraceStateObserver* observer) {
        TRACE
    }

    void v8::platform::tracing::TracingController::RemoveTraceStateObserver(v8::TracingController::TraceStateObserver* observer) {
        TRACE
    }

    v8::base::Mutex::~Mutex() {
        TRACE
    }

    void String::VerifyExternalStringResource(ExternalStringResource* val) const {
        TRACE
    }

    void Isolate::SetMicrotasksPolicy(MicrotasksPolicy policy) {
        TRACE
    }

    void Isolate::SetAllowWasmCodeGenerationCallback(AllowWasmCodeGenerationCallback callback) {
        TRACE
    }

    Local<Object> Context::GetExtrasBindingObject() {
        return reinterpret_cast<GraalContext*> (this)->GetExtrasBindingObject();
    }

    void Isolate::DiscardThreadSpecificMetadata() {
        TRACE
    }

    Isolate::DisallowJavascriptExecutionScope::DisallowJavascriptExecutionScope(Isolate* isolate, OnFailure on_failure) {
        TRACE
    }

    Isolate::DisallowJavascriptExecutionScope::~DisallowJavascriptExecutionScope() {
        TRACE
    }

    void Isolate::CheckMemoryPressure() {
        TRACE
    }

    void HeapProfiler::RemoveBuildEmbedderGraphCallback(BuildEmbedderGraphCallback callback, void* data) {
        TRACE
    }

    void HeapProfiler::AddBuildEmbedderGraphCallback(BuildEmbedderGraphCallback callback, void* data) {
        TRACE
    }

    void Isolate::SetIdle(bool is_idle) {
        TRACE
    }

    bool Value::SameValue(Local<Value> that) const {
        TRACE
        return false;
    }

    int Object::GetIdentityHash() {
        TRACE
        return 0;
    }

    int Name::GetIdentityHash() {
        TRACE
        return 0;
    }

    Local<Value> Module::GetException() const {
        return reinterpret_cast<const GraalModule*> (this)->GetException();
    }

    void Isolate::SetHostImportModuleDynamicallyCallback(HostImportModuleDynamicallyCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetImportModuleDynamicallyCallback(callback);
    }

    void Isolate::SetHostInitializeImportMetaObjectCallback(HostInitializeImportMetaObjectCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetImportMetaInitializer(callback);
    }

    Local<Value> ScriptOrModule::GetResourceName() {
        return reinterpret_cast<GraalScriptOrModule*> (this)->GetResourceName();
    }

    Local<PrimitiveArray> ScriptOrModule::GetHostDefinedOptions() {
        return reinterpret_cast<GraalScriptOrModule*> (this)->GetHostDefinedOptions();
    }

    Local<BigInt> BigInt::New(Isolate* isolate, int64_t value) {
        return GraalBigInt::New(isolate, value);
    }

    Local<BigInt> BigInt::NewFromUnsigned(Isolate* isolate, uint64_t value) {
        return GraalBigInt::NewFromUnsigned(isolate, value);
    }

    MaybeLocal<BigInt> BigInt::NewFromWords(Local<Context> context, int sign_bit, int word_count, const uint64_t* words) {
        return GraalBigInt::NewFromWords(context, sign_bit, word_count, words);
    }

    uint64_t BigInt::Uint64Value(bool* lossless) const {
        return reinterpret_cast<const GraalBigInt*> (this)->Uint64Value(lossless);
    }

    int64_t BigInt::Int64Value(bool* lossless) const {
        return reinterpret_cast<const GraalBigInt*> (this)->Int64Value(lossless);
    }

    int BigInt::WordCount() const {
        return reinterpret_cast<const GraalBigInt*> (this)->WordCount();
    }

    void BigInt::ToWordsArray(int* sign_bit, int* word_count, uint64_t* words) const {
        return reinterpret_cast<const GraalBigInt*> (this)->ToWordsArray(sign_bit, word_count, words);
    }

    bool Value::IsBigInt() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBigInt();
    }

    bool Value::IsGeneratorFunction() const {
        return reinterpret_cast<const GraalValue*> (this)->IsGeneratorFunction();
    }

    bool Value::IsGeneratorObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsGeneratorObject();
    }

    bool Value::IsModuleNamespaceObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsModuleNamespaceObject();
    }

    bool Value::IsWeakMap() const {
        return reinterpret_cast<const GraalValue*> (this)->IsWeakMap();
    }

    bool Value::IsWeakSet() const {
        return reinterpret_cast<const GraalValue*> (this)->IsWeakSet();
    }

    bool Value::IsNumberObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsNumberObject();
    }

    bool Value::IsStringObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsStringObject();
    }

    bool Value::IsSymbolObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsSymbolObject();
    }

    bool Value::IsBooleanObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBooleanObject();
    }

    bool Value::IsBigIntObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsBigIntObject();
    }

    bool Value::IsArgumentsObject() const {
        return reinterpret_cast<const GraalValue*> (this)->IsArgumentsObject();
    }

    void Context::AllowCodeGenerationFromStrings(bool allow) {
        TRACE
    }

    ScriptCompiler::CachedData* ScriptCompiler::CreateCodeCache(Local<UnboundScript> unbound_script) {
        GraalUnboundScript* graal_script = reinterpret_cast<GraalUnboundScript*> (*unbound_script);
        String::Utf8Value text(reinterpret_cast<Isolate*> (graal_script->Isolate()), graal_script->GetContent());
        uint8_t* copy = new uint8_t[text.length()];
        memcpy(copy, *text, text.length());
        return new ScriptCompiler::CachedData((const uint8_t*) copy, text.length());
    }

    ScriptCompiler::CachedData* ScriptCompiler::CreateCodeCache(Local<UnboundModuleScript> unbound_module_script) {
        return CreateCodeCache(reinterpret_cast<UnboundScript*> (*unbound_module_script));
    }

    bool ArrayBuffer::IsDetachable() const {
        return true;
    }

    bool ArrayBuffer::IsExternal() const {
        return reinterpret_cast<const GraalArrayBuffer*> (this)->IsExternal();
    }

    ArrayBuffer::Contents ArrayBuffer::Externalize() {
        GraalArrayBuffer* graal_array_buffer = reinterpret_cast<GraalArrayBuffer*> (this);
        GraalIsolate* graal_isolate = graal_array_buffer->Isolate();
        jobject java_array_buffer = graal_array_buffer->GetJavaObject();
        jobject java_buffer;
        if (graal_array_buffer->IsDirect()) {
            java_buffer = graal_isolate->JNIGetObjectFieldOrCall(java_array_buffer, GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
        } else {
            JNI_CALL(jobject, java_not_direct_buffer, graal_isolate, GraalAccessMethod::array_buffer_get_contents, Object, java_array_buffer);
            java_buffer = java_not_direct_buffer;
        }
        graal_isolate->Externalize(java_buffer);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        ArrayBuffer::Contents contents;
        contents.data_ = env->GetDirectBufferAddress(java_buffer);
        contents.byte_length_ = env->GetDirectBufferCapacity(java_buffer);
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::array_buffer_externalize, java_array_buffer);
        env->DeleteLocalRef(java_buffer);
        return contents;
    }

    bool SharedArrayBuffer::IsExternal() const {
        const GraalObject* graal_object = reinterpret_cast<const GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jboolean, result, graal_isolate, GraalAccessMethod::shared_array_buffer_is_external, Boolean, java_object);
        return result;
    }

    SharedArrayBuffer::Contents SharedArrayBuffer::Externalize() {
        GraalObject* graal_object = reinterpret_cast<GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jobject, java_buffer, graal_isolate, GraalAccessMethod::shared_array_buffer_get_contents, Object, java_object);
        graal_isolate->Externalize(java_buffer);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        SharedArrayBuffer::Contents contents;
        void* pointer = env->GetDirectBufferAddress(java_buffer);
        contents.data_ = pointer;
        contents.byte_length_ = env->GetDirectBufferCapacity(java_buffer);
        contents.deleter_ = [](void* buffer, size_t length, void* info) {
            free(buffer);
        };
        JNI_CALL_VOID(graal_isolate, GraalAccessMethod::shared_array_buffer_externalize, java_object, (jlong) pointer);
        env->DeleteLocalRef(java_buffer);
        return contents;
    }

    Local<SharedArrayBuffer> SharedArrayBuffer::New(
            Isolate* isolate,
            void* data,
            size_t byte_length,
            ArrayBufferCreationMode mode) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_byte_buffer = graal_isolate->GetJNIEnv()->NewDirectByteBuffer(data, byte_length);
        jboolean externalized = (mode == v8::ArrayBufferCreationMode::kExternalized);
        jobject java_context = graal_isolate->CurrentJavaContext();
        JNI_CALL(jobject, java_array_buffer, graal_isolate, GraalAccessMethod::shared_array_buffer_new, Object, java_context, java_byte_buffer, (jlong) data, externalized);
        graal_isolate->GetJNIEnv()->DeleteLocalRef(java_byte_buffer);
        return reinterpret_cast<v8::SharedArrayBuffer*> (GraalObject::Allocate(graal_isolate, java_array_buffer));
    }

    double Platform::SystemClockTimeMillis() {
        TRACE
        return 0;
    }

    MaybeLocal<Array> Object::PreviewEntries(bool* is_key_value) {
        return reinterpret_cast<GraalObject*> (this)->PreviewEntries(is_key_value);
    }

    MaybeLocal<SharedArrayBuffer> ValueDeserializer::Delegate::GetSharedArrayBufferFromId(Isolate* isolate, uint32_t clone_id) {
        TRACE
        return MaybeLocal<SharedArrayBuffer>();
    }

    Local<Map> Map::New(Isolate* isolate) {
        return GraalMap::New(isolate);
    }

    MaybeLocal<Map> Map::Set(Local<Context> context, Local<Value> key, Local<Value> value) {
        return reinterpret_cast<GraalMap*> (this)->Set(context, key, value);
    }

    MaybeLocal<Function> ScriptCompiler::CompileFunctionInContext(
            Local<Context> context, Source* source,
            size_t arguments_count,
            Local<String> arguments[],
            size_t context_extension_count,
            Local<Object> context_extensions[],
            CompileOptions options,
            NoCacheReason no_cache_reason,
            Local<ScriptOrModule>* script_or_module_out) {
        Isolate* isolate = context->GetIsolate();
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        JNIEnv* env = graal_isolate->GetJNIEnv();

        jobject java_context = reinterpret_cast<GraalContext*> (*context)->GetJavaObject();

        Local<Value> resource_name = source->resource_name;
        Local<String> file_name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate->GetCurrentContext()).ToLocalChecked();
        jobject java_source_name = file_name.IsEmpty() ? nullptr : reinterpret_cast<GraalString*> (*file_name)->GetJavaObject();

        jobject java_body = reinterpret_cast<GraalString*> (*source->source_string)->GetJavaObject();

        jobjectArray java_arguments = env->NewObjectArray(arguments_count, graal_isolate->GetObjectClass(), nullptr);
        for (size_t i = 0; i < arguments_count; i++) {
            jobject java_argument = reinterpret_cast<GraalString*> (*arguments[i])->GetJavaObject();
            env->SetObjectArrayElement(java_arguments, i, java_argument);
        }

        jobjectArray java_context_extensions = env->NewObjectArray(context_extension_count, graal_isolate->GetObjectClass(), nullptr);
        for (size_t i = 0; i < context_extension_count; i++) {
            jobject java_context_extension = reinterpret_cast<GraalObject*> (*context_extensions[i])->GetJavaObject();
            env->SetObjectArrayElement(java_context_extensions, i, java_context_extension);
        }

        Local<PrimitiveArray> host_options = source->host_defined_options;
        jobject java_options = host_options.IsEmpty() ? NULL : reinterpret_cast<GraalPrimitiveArray*> (*host_options)->GetJavaObject();

        JNI_CALL(jobject, java_array, graal_isolate, GraalAccessMethod::script_compiler_compile_function_in_context, Object, java_context, java_source_name, java_body, java_arguments, java_context_extensions, java_options);
        env->DeleteLocalRef(java_arguments);
        env->DeleteLocalRef(java_context_extensions);

        if (java_array == nullptr) {
            return MaybeLocal<Function>();
        }

        if (script_or_module_out != nullptr) {
            jobject java_script = graal_isolate->GetJNIEnv()->GetObjectArrayElement((jobjectArray) java_array, 1);
            GraalScriptOrModule* graal_script = GraalScriptOrModule::Allocate(graal_isolate, java_script);
            *script_or_module_out = reinterpret_cast<ScriptOrModule*>(graal_script);
        }

        jobject java_function = graal_isolate->GetJNIEnv()->GetObjectArrayElement((jobjectArray) java_array, 0);
        GraalFunction* graal_function = GraalFunction::Allocate(graal_isolate, java_function);
        Local<Function> v8_function = reinterpret_cast<Function*> (graal_function);
        return v8_function;
    }

    ScriptCompiler::CachedData* ScriptCompiler::CreateCodeCacheForFunction(Local<Function> function) {
        TRACE
        return new ScriptCompiler::CachedData((const uint8_t*) nullptr, 0);
    }

    bool ScriptCompiler::ExternalSourceStream::SetBookmark() {
        TRACE
        return false;
    }

    void ScriptCompiler::ExternalSourceStream::ResetToBookmark() {
        TRACE
    }

    SnapshotCreator::SnapshotCreator(const intptr_t* external_references, StartupData* existing_blob) {
        TRACE
    }

    Local<PrimitiveArray> PrimitiveArray::New(Isolate* isolate, int length) {
        return GraalPrimitiveArray::New(isolate, length);
    }

    int PrimitiveArray::Length() const {
        return reinterpret_cast<const GraalPrimitiveArray*> (this)->Length();
    }

    void PrimitiveArray::Set(Isolate* isolate, int index, Local<Primitive> item) {
        reinterpret_cast<GraalPrimitiveArray*> (this)->Set(isolate, index, item);
    }

    Local<Primitive> PrimitiveArray::Get(Isolate* isolate, int index) {
        return reinterpret_cast<GraalPrimitiveArray*> (this)->Get(isolate, index);
    }

    Isolate* Isolate::Allocate() {
        TRACE
        return reinterpret_cast<Isolate*> (malloc(sizeof(GraalIsolate)));
    }

    void Isolate::EnqueueMicrotask(Local<Function> microtask) {
        reinterpret_cast<GraalIsolate*> (this)->EnqueueMicrotask(microtask);
    }

    bool Isolate::GetHeapCodeAndMetadataStatistics(HeapCodeStatistics* object_statistics) {
        TRACE
        return false;
    }

    bool Isolate::InContext() {
        TRACE
        return true;
    }

    void Isolate::Initialize(Isolate* isolate, const CreateParams& params) {
        GraalIsolate::New(params, isolate);
        // We don't need this reference (i.e. we don't use it) by now.
        // Unfortunately, PerIsolatePlatformData::Shutdown()
        // assumes that someone holds the reference (V8 does so).
        reinterpret_cast<GraalIsolate*> (isolate)->task_runner_ = platform_->GetForegroundTaskRunner(isolate);
    }

    internal::Address* Isolate::GetDataFromSnapshotOnce(size_t index) {
        TRACE
        return nullptr;
    }

    ArrayBuffer::Allocator* Isolate::GetArrayBufferAllocator() {
        return reinterpret_cast<GraalIsolate*> (this)->GetArrayBufferAllocator();
    }

    void Isolate::SetPrepareStackTraceCallback(PrepareStackTraceCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetPrepareStackTraceCallback(callback);
    }

    bool Isolate::AddMessageListenerWithErrorLevel(MessageCallback callback, int message_levels, Local<Value> data) {
        return reinterpret_cast<GraalIsolate*> (this)->AddMessageListener(callback, data);
    }

    void V8::MoveGlobalReference(internal::Address** from, internal::Address** to) {
        *to = *from;
    }

    internal::Address* V8::CopyGlobalReference(internal::Address* from) {
        reinterpret_cast<GraalHandleContent*> (from)->ReferenceAdded();
        return from;
    }

    double Date::ValueOf() const {
        return reinterpret_cast<const GraalDate*> (this)->ValueOf();
    }

    size_t ArrayBufferView::CopyContents(void* dest, size_t byte_length) {
        size_t offset = ByteOffset();
        ArrayBuffer::Contents contents = Buffer()->GetContents();
        size_t content_length = contents.ByteLength() - offset;
        if (content_length < byte_length) {
            byte_length = content_length;
        }
        memcpy(dest, reinterpret_cast<char*> (contents.Data()) + offset, byte_length);
        return byte_length;
    }

    Local<Array> Array::New(Isolate* isolate, Local<Value>* elements, size_t length) {
        return GraalArray::New(isolate, elements, length);
    }

    Local<Set> Set::New(Isolate* isolate) {
        return GraalSet::New(isolate);
    }

    MaybeLocal<Set> Set::Add(Local<Context> context, Local<Value> key) {
        return reinterpret_cast<GraalSet*> (this)->Add(context, key);
    }

    bool TryCatch::CanContinue() const {
        TRACE
        return !HasTerminated();
    }

    HeapCodeStatistics::HeapCodeStatistics() {
        TRACE
    }

    Isolate::AllowJavascriptExecutionScope::AllowJavascriptExecutionScope(Isolate* isolate) {
        TRACE
    }

    Isolate::AllowJavascriptExecutionScope::~AllowJavascriptExecutionScope() {
        TRACE
    }

    MaybeLocal<Context> Context::FromSnapshot(
            Isolate* isolate, size_t context_snapshot_index,
            DeserializeInternalFieldsCallback embedder_fields_deserializer,
            ExtensionConfiguration* extensions,
            MaybeLocal<Value> global_object,
            MicrotaskQueue* microtask_queue) {
        TRACE
        return Local<Context>();
    }

    String::ExternalStringResource* String::GetExternalStringResourceSlow() const {
        TRACE
        return nullptr;
    }

    int Message::ErrorLevel() const {
        return Isolate::MessageErrorLevel::kMessageError;
    }

    Isolate* Message::GetIsolate() const {
        GraalIsolate* graal_isolate = reinterpret_cast<const GraalMessage*> (this)->Isolate();
        return reinterpret_cast<Isolate*> (graal_isolate);
    }

    bool v8::internal::ShouldThrowOnError(v8::internal::Isolate* isolate) {
        TRACE
        return true;
    }

    size_t SnapshotCreator::AddData(internal::Address object) {
        TRACE
        return 0;
    }

    Isolate* SnapshotCreator::GetIsolate() {
        TRACE
        return nullptr;
    }

    void ResourceConstraints::ConfigureDefaults(uint64_t physical_memory, uint64_t virtual_memory_limit) {
        TRACE
    }

    void CpuProfiler::UseDetailedSourcePositionsForProfiling(Isolate* isolate) {
        TRACE
    }

    SnapshotCreator::SnapshotCreator(Isolate* isolate, const intptr_t* external_references, StartupData* existing_blob) {
        TRACE
    }

    void SnapshotCreator::SetDefaultContext(Local<Context> context, SerializeInternalFieldsCallback callback) {
        TRACE
    }

    StartupData SnapshotCreator::CreateBlob(FunctionCodeHandling function_code_handling) {
        TRACE
        return { nullptr, 0 };
    }

    size_t SnapshotCreator::AddContext(Local<Context> context, SerializeInternalFieldsCallback callback) {
        TRACE
        return 0;
    }

    SnapshotCreator::~SnapshotCreator() {
        TRACE
    }

    bool StartupData::CanBeRehashed() const {
        TRACE
        return false;
    }

    void MicrotasksScope::PerformCheckpoint(Isolate* isolate) {
        isolate->RunMicrotasks();
    }

    String::ExternalStringResourceBase* String::GetExternalStringResourceBaseSlow(String::Encoding* encoding_out) const {
        TRACE
        return nullptr;
    }

    void Isolate::AddNearHeapLimitCallback(NearHeapLimitCallback callback, void* data) {
        TRACE
    }

    void Isolate::RequestInterrupt(InterruptCallback callback, void* data) {
        TRACE
    }

    void Isolate::ClearKeptObjects() {
        TRACE
    }

    void Isolate::SetHostCleanupFinalizationGroupCallback(HostCleanupFinalizationGroupCallback callback) {
        TRACE
    }

    Maybe<bool> FinalizationGroup::Cleanup(Local<FinalizationGroup> finalization_group) {
        TRACE
        return Just(true);
    }

    Local<ArrayBuffer> ArrayBuffer::New(Isolate* isolate, std::shared_ptr<BackingStore> backing_store) {
        return GraalArrayBuffer::New(isolate, backing_store);
    }

    std::unique_ptr<BackingStore> ArrayBuffer::NewBackingStore(Isolate* isolate, size_t byte_length) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        JNI_CALL(jobject, java_buffer, graal_isolate, GraalAccessMethod::array_buffer_new_backing_store, Object, (jint) byte_length);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        jobject java_store = env->NewGlobalRef(java_buffer);
        env->DeleteLocalRef(java_buffer);
        return std::unique_ptr<v8::BackingStore>(reinterpret_cast<v8::BackingStore*>(new GraalBackingStore(java_store)));
    }

    std::unique_ptr<BackingStore> ArrayBuffer::NewBackingStore(void* data, size_t byte_length, v8::BackingStore::DeleterCallback deleter, void* deleter_data) {
        GraalIsolate* graal_isolate = CurrentIsolate();
        jobject java_store;
        if (data == nullptr) {
            java_store = nullptr;
        } else {
            JNIEnv* env = graal_isolate->GetJNIEnv();
            jobject java_buffer = env->NewDirectByteBuffer(data, byte_length);
            java_store = env->NewGlobalRef(java_buffer);
            env->DeleteLocalRef(java_buffer);

            JNI_CALL_VOID(graal_isolate, GraalAccessMethod::backing_store_register_callback, java_store, (jlong) data, (jint) byte_length, (jlong) deleter_data, (jlong) deleter);
        }

        return std::unique_ptr<v8::BackingStore>(reinterpret_cast<v8::BackingStore*>(new GraalBackingStore(java_store)));
    }

    std::shared_ptr<BackingStore> ArrayBuffer::GetBackingStore() {
        return reinterpret_cast<GraalArrayBuffer*> (this)->GetBackingStore();
    }

    void* ArrayBuffer::Allocator::Reallocate(void* data, size_t old_length, size_t new_length) {
        TRACE
        return nullptr;
    }

    size_t BackingStore::ByteLength() const {
        return reinterpret_cast<const GraalBackingStore*> (this)->ByteLength();
    }

    void* BackingStore::Data() const {
        return reinterpret_cast<const GraalBackingStore*> (this)->Data();
    }

    std::unique_ptr<BackingStore> BackingStore::Reallocate(Isolate* isolate, std::unique_ptr<BackingStore> backing_store, size_t byte_length) {
        std::unique_ptr<BackingStore> new_store = ArrayBuffer::NewBackingStore(isolate, byte_length);
        memcpy(new_store->Data(), backing_store->Data(), std::min(byte_length, backing_store->ByteLength()));
        return new_store;
    }

    BackingStore::~BackingStore() {
        const GraalBackingStore* graal_store = reinterpret_cast<const GraalBackingStore*> (this);
        jobject java_store = graal_store->GetJavaStore();
        if (java_store != nullptr) {
            CurrentIsolate()->GetJNIEnv()->DeleteGlobalRef(java_store);
        }
    }

    Local<SharedArrayBuffer> SharedArrayBuffer::New(Isolate* isolate, std::shared_ptr<BackingStore> backing_store) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jobject java_store = reinterpret_cast<GraalBackingStore*> (backing_store.get())->GetJavaStore();
        void* data = backing_store->Data();
        JNI_CALL(jobject, java_array_buffer, isolate, GraalAccessMethod::shared_array_buffer_new, Object, java_context, java_store, (jlong) data, true);
        return reinterpret_cast<v8::SharedArrayBuffer*> (GraalObject::Allocate(graal_isolate, java_array_buffer));
    }

    std::shared_ptr<BackingStore> SharedArrayBuffer::GetBackingStore() {
        GraalObject* graal_array_buffer = reinterpret_cast<GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_array_buffer->Isolate();
        jobject java_array_buffer = graal_array_buffer->GetJavaObject();
        JNI_CALL(jobject, java_buffer, graal_isolate, GraalAccessMethod::shared_array_buffer_get_contents, Object, java_array_buffer);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        if (java_buffer != nullptr) {
            void* pointer = env->GetDirectBufferAddress(java_buffer);
            JNI_CALL_VOID(graal_isolate, GraalAccessMethod::shared_array_buffer_externalize, java_array_buffer, (jlong) pointer);
        }
        jobject java_store = env->NewGlobalRef(java_buffer);
        env->DeleteLocalRef(java_buffer);
        return std::shared_ptr<v8::BackingStore>(reinterpret_cast<v8::BackingStore*>(new GraalBackingStore(java_store)));
    }

    CompiledWasmModule WasmModuleObject::GetCompiledModule() {
        TRACE
        return CompiledWasmModule(nullptr, nullptr, 0);
    }

    CompiledWasmModule::CompiledWasmModule(std::shared_ptr<internal::wasm::NativeModule>, const char* source_url, size_t url_length) {
        TRACE
    }

    MaybeLocal<WasmModuleObject> WasmModuleObject::FromCompiledModule(Isolate* isolate, const CompiledWasmModule&) {
        TRACE
        return MaybeLocal<WasmModuleObject>();
    }

    void Isolate::DateTimeConfigurationChangeNotification(TimeZoneDetection time_zone_detection) {
        TRACE
    }

    std::unique_ptr<MicrotaskQueue> MicrotaskQueue::New(Isolate* isolate, MicrotasksPolicy policy) {
        return nullptr;
        TRACE
    }

    class DefaultMeasureMemoryDelegate : public MeasureMemoryDelegate {
    public:
        DefaultMeasureMemoryDelegate(Local<Promise::Resolver> promise_resolver, MeasureMemoryMode mode) : promise_resolver_(promise_resolver), mode_(mode) {
        }

        virtual bool ShouldMeasure(Local<Context> context) {
            return true;
        }

        virtual void MeasurementComplete(const std::vector<std::pair<Local<Context>, size_t>>& context_sizes_in_bytes, size_t unattributed_size_in_bytes) {
            TRACE
        }

    private:
        Local<Promise::Resolver> promise_resolver_;
        MeasureMemoryMode mode_;
        friend Isolate;
    };

    bool Isolate::MeasureMemory(std::unique_ptr<MeasureMemoryDelegate> delegate, MeasureMemoryExecution execution) {
        DefaultMeasureMemoryDelegate* d = reinterpret_cast<DefaultMeasureMemoryDelegate*> (delegate.get());
        GraalHandleContent* graal_resolver = reinterpret_cast<GraalHandleContent*> (*(d->promise_resolver_));
        jobject java_resolver = graal_resolver->GetJavaObject();
        jboolean detailed = (d->mode_ == MeasureMemoryMode::kDetailed);
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_measure_memory, java_resolver, detailed);
        return false;
    }

    std::unique_ptr<MeasureMemoryDelegate> MeasureMemoryDelegate::Default(
            Isolate* isolate, Local<Context> context,
            Local<Promise::Resolver> promise_resolver, MeasureMemoryMode mode) {
        return std::unique_ptr<MeasureMemoryDelegate> (new DefaultMeasureMemoryDelegate(promise_resolver, mode));
    }

    void Isolate::SetAtomicsWaitCallback(AtomicsWaitCallback callback, void* data) {
        TRACE
    }

    bool V8::EnableWebAssemblyTrapHandler(bool use_v8_signal_handler) {
        TRACE
        return false;
    }

#ifdef __POSIX__
    bool TryHandleWebAssemblyTrapPosix(int sig_code, siginfo_t* info, void* context) {
        TRACE
        return false;
    }
#endif

    bool EmbedderHeapTracer::IsRootForNonTracingGC(const v8::TracedReference<v8::Value>& handle) {
        TRACE
        return true;
    }

    bool EmbedderHeapTracer::IsRootForNonTracingGC(const v8::TracedGlobal<v8::Value>& handle) {
        TRACE
        return true;
    }

    void EmbedderHeapTracer::ResetHandleInNonTracingGC(const v8::TracedReference<v8::Value>& handle) {
        TRACE
    }

    CpuProfilingOptions::CpuProfilingOptions(CpuProfilingMode mode, unsigned max_samples, int sampling_interval_us, MaybeLocal<Context> filter_context) {
        TRACE
    }

    void Object::CheckCast(v8::Value* obj) {}
    void Promise::CheckCast(v8::Value* obj) {}
    void Function::CheckCast(v8::Value* obj) {}
    void Array::CheckCast(v8::Value* obj) {}
    void Uint32Array::CheckCast(v8::Value* obj) {}
    void Float64Array::CheckCast(v8::Value* obj) {}
    void Boolean::CheckCast(v8::Value* obj) {}
    void Name::CheckCast(v8::Value* obj) {}
    void Number::CheckCast(v8::Value* obj) {}
    void Int32::CheckCast(v8::Value* obj) {}
    void Uint32::CheckCast(v8::Value* obj) {}
    void Promise::Resolver::CheckCast(v8::Value* obj) {}
    void ArrayBuffer::CheckCast(v8::Value* obj) {}
    void TypedArray::CheckCast(v8::Value* obj) {}
    void DataView::CheckCast(v8::Value* obj) {}
    void ArrayBufferView::CheckCast(v8::Value* obj) {}
    void Uint8Array::CheckCast(v8::Value* obj) {}
    void SharedArrayBuffer::CheckCast(v8::Value* obj) {}
    void Proxy::CheckCast(v8::Value* obj) {}
    void Date::CheckCast(v8::Value* obj) {}
    void Integer::CheckCast(v8::Value* that) {}
    void RegExp::CheckCast(v8::Value* that) {}
    void External::CheckCast(v8::Value* that) {}
    void BigInt::CheckCast(v8::Value* that) {}
    void BigInt64Array::CheckCast(v8::Value* that) {}
    void BigUint64Array::CheckCast(v8::Value* that) {}
    void Symbol::CheckCast(v8::Value* that) {}
    void Private::CheckCast(v8::Data* that) {}
    void Map::CheckCast(v8::Value* that) {}
    void Set::CheckCast(v8::Value* that) {}

}

void V8_Fatal(const char* format, ...) {
    TRACE
}
