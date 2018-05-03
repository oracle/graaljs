/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "graal_boolean.h"
#include "graal_context.h"
#include "graal_date.h"
#include "graal_external.h"
#include "graal_function.h"
#include "graal_function_template.h"
#include "graal_isolate.h"
#include "graal_message.h"
#include "graal_module.h"
#include "graal_number.h"
#include "graal_object_template.h"
#include "graal_promise.h"
#include "graal_proxy.h"
#include "graal_regexp.h"
#include "graal_script.h"
#include "graal_stack_frame.h"
#include "graal_stack_trace.h"
#include "graal_string.h"
#include "graal_unbound_script.h"
#include "graal_value.h"
#include "include/v8.h"
#include "include/v8-debug.h"
#include "include/v8-profiler.h"
#include "include/libplatform/v8-tracing.h"
#include "src/base/once.h"
#include "src/base/platform/mutex.h"
#include "stdlib.h"
#include "graal_symbol.h"
#include <string.h>
#include <string>

#define TRACE
//#define TRACE printf("at %s line %d\n", __func__, __LINE__);

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
        jobject java_buffer = graal_isolate->JNIGetObjectFieldOrCall(java_array_buffer, GraalAccessField::array_buffer_byte_buffer, GraalAccessMethod::array_buffer_get_contents);
        JNIEnv* env = graal_isolate->GetJNIEnv();
        ArrayBuffer::Contents contents;
        contents.data_ = env->GetDirectBufferAddress(java_buffer);
        contents.byte_length_ = env->GetDirectBufferCapacity(java_buffer);
        env->DeleteLocalRef(java_buffer);
        return contents;
    }

    void ArrayBuffer::Neuter() {
        TRACE
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

    void base::CallOnceImpl(OnceType* once, PointerArgFunction init_func, void* arg) {
        TRACE
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
            MaybeLocal<Value> value) {
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
        GraalContext* ctx = new GraalContext(graal_isolate, java_context);
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

    void CpuProfiler::SetIdle(bool) {
        TRACE
    }

    void Debug::DebugBreak(Isolate*) {
        TRACE
    }

    void Debug::SendCommand(Isolate*, unsigned short const*, int, Debug::ClientData*) {
        TRACE
    }

    bool Debug::SetDebugEventListener(Isolate* isolate, Debug::EventCallback that, Local<Value> data) {
        TRACE
        return false;
    }

    EscapableHandleScope::EscapableHandleScope(Isolate* isolate) {
    }

    internal::Object** EscapableHandleScope::Escape(internal::Object** obj) {
        return obj;
    }

#define EXCEPTION_ERROR(error_type) \
    GraalString* graal_message = reinterpret_cast<GraalString*> (*message); \
    GraalIsolate* isolate = graal_message->Isolate(); \
    jobject java_context = isolate->CurrentJavaContext(); \
    jobject java_message = graal_message->GetJavaObject(); \
    JNI_CALL(jobject, java_error, isolate, GraalAccessMethod::error_type, Object, java_context, java_message); \
    GraalObject* graal_object = new GraalObject(isolate, java_error); \
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

    Local<Value> Function::Call(Local<Value> recv, int argc, Local<Value> argv[]) {
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

    Local<Function> FunctionTemplate::GetFunction() {
        Local<Context> context = Isolate::GetCurrent()->GetCurrentContext();
        return reinterpret_cast<GraalFunctionTemplate*> (this)->GetFunction(context);
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
            Isolate* isolate, void (*callback)(FunctionCallbackInfo<Value> const&),
            Local<Value> data, Local<Signature> signature, int length,
            ConstructorBehavior behavior) {
        return GraalFunctionTemplate::New(isolate, callback, data, signature, length);
    }

    Local<ObjectTemplate> FunctionTemplate::PrototypeTemplate() {
        return reinterpret_cast<GraalFunctionTemplate*> (this)->PrototypeTemplate();
    }

    void FunctionTemplate::SetClassName(Local<String> name) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->SetClassName(name);
    }

    void FunctionTemplate::SetHiddenPrototype(bool) {
        TRACE
    }

    void FunctionTemplate::SetCallHandler(FunctionCallback callback, Local<Value> data) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->SetCallHandler(callback, data);
    }

    void FunctionTemplate::Inherit(Local<FunctionTemplate> parent) {
        reinterpret_cast<GraalFunctionTemplate*> (this)->Inherit(parent);
    }

    internal::Object** HandleScope::CreateHandle(internal::Isolate* isolate, internal::Object* value) {
        GraalHandleContent* graal_original = reinterpret_cast<GraalHandleContent*> (value);
        GraalHandleContent* graal_copy = graal_original->Copy(false);
        return reinterpret_cast<internal::Object**> (graal_copy);
    }

    HandleScope::~HandleScope() {
    }

    HandleScope::HandleScope(Isolate* isolate) {
    }

    void HeapProfiler::SetWrapperClassInfoProvider(unsigned short, RetainedObjectInfo* (*)(unsigned short, Local<Value>)) {
        TRACE
    }

    void HeapProfiler::StartTrackingHeapObjects(bool) {
        TRACE
    }

    HeapStatistics::HeapStatistics() {
        TRACE
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

    bool Isolate::IsDead() {
        return false;
    }

    void Isolate::Enter() {
        TRACE
    }

    void Isolate::Exit() {
        TRACE
    }

    CpuProfiler* Isolate::GetCpuProfiler() {
        TRACE
        return nullptr;
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

    const HeapSnapshot* HeapProfiler::TakeHeapSnapshot(ActivityControl* control, ObjectNameResolver* global_object_name_resolver) {
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
    }

    Isolate* Isolate::New(Isolate::CreateParams const& params) {
        return GraalIsolate::New(params);
    }

    void Isolate::RunMicrotasks() {
        reinterpret_cast<GraalIsolate*> (this)->RunMicrotasks();
    }

    void Isolate::SetAbortOnUncaughtExceptionCallback(AbortOnUncaughtExceptionCallback callback) {
        reinterpret_cast<GraalIsolate*> (this)->SetAbortOnUncaughtExceptionCallback(callback);
    }

    void Isolate::SetAutorunMicrotasks(bool) {
        TRACE
    }

    void Isolate::SetFatalErrorHandler(void (*)(char const*, char const*)) {
        TRACE
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
        pthread_mutex_lock(&graal_isolate->lock_);
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
            pthread_mutex_unlock(&graal_isolate->lock_);
        }
    }

    void Unlocker::Initialize(Isolate* isolate) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        if (graal_isolate->lock_owner_ != nullptr) {
            graal_isolate->lock_owner_ = nullptr;
            pthread_mutex_unlock(&graal_isolate->lock_);
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
        return Just(Delete(key));
    }

    bool Object::Delete(Local<Value> key) {
        return reinterpret_cast<GraalObject*> (this)->Delete(key);
    }

    bool Object::Delete(uint32_t index) {
        return reinterpret_cast<GraalObject*> (this)->Delete(index);
    }

    Maybe<bool> Object::Delete(Local<Context> context, uint32_t index) {
        return Just(Delete(index));
    }

    Maybe<bool> Object::ForceSet(Local<Context> context, Local<Value> key, Local<Value> value, PropertyAttribute attribs) {
        return Just(ForceSet(key, value, attribs));
    }

    bool Object::ForceSet(
            Local<Value> key,
            Local<Value> value,
            PropertyAttribute attribs
            ) {
        return reinterpret_cast<GraalObject*> (this)->ForceSet(key, value, attribs);
    }

    Local<String> Object::GetConstructorName() {
        return reinterpret_cast<GraalObject*> (this)->GetConstructorName();
    }

    Isolate* Object::GetIsolate() {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalObject*> (this)->Isolate();
        return reinterpret_cast<Isolate*> (graal_isolate);
    }

    Local<Array> Object::GetOwnPropertyNames() {
        return reinterpret_cast<GraalObject*> (this)->GetOwnPropertyNames();
    }

    MaybeLocal<Array> Object::GetOwnPropertyNames(Local<Context> context) {
        return reinterpret_cast<GraalObject*> (this)->GetOwnPropertyNames();
    }

    Local<Array> Object::GetPropertyNames() {
        return reinterpret_cast<GraalObject*> (this)->GetPropertyNames();
    }

    MaybeLocal<Array> Object::GetPropertyNames(Local<Context> context) {
        return reinterpret_cast<GraalObject*> (this)->GetPropertyNames();
    }

    Local<Value> Object::GetPrototype() {
        return reinterpret_cast<GraalObject*> (this)->GetPrototype();
    }

    Maybe<PropertyAttribute> Object::GetRealNamedPropertyAttributes(Local<Context> context, Local<Name> key) {
        return reinterpret_cast<GraalObject*> (this)->GetRealNamedPropertyAttributes(context, key);
    }

    Local<Value> Object::GetRealNamedProperty(Local<String> key) {
        return reinterpret_cast<GraalObject*> (this)->GetRealNamedProperty(GetIsolate()->GetCurrentContext(), key);
    }

    MaybeLocal<Value> Object::GetRealNamedProperty(Local<Context> context, Local<Name> key) {
        return reinterpret_cast<GraalObject*> (this)->GetRealNamedProperty(context, key);
    }

    bool Object::HasRealNamedProperty(Local<String> key) {
        return reinterpret_cast<GraalObject*> (this)->HasRealNamedProperty(key);
    }

    Maybe<bool> Object::HasRealNamedProperty(Local<Context> context, Local<Name> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->HasRealNamedProperty(key));
    }

    Local<Value> Object::Get(uint32_t index) {
        return reinterpret_cast<GraalObject*> (this)->Get(index);
    }

    MaybeLocal<Value> Object::Get(Local<Context> context, uint32_t index) {
        return reinterpret_cast<GraalObject*> (this)->Get(index);
    }

    Local<Value> Object::Get(Local<Value> key) {
        return reinterpret_cast<GraalObject*> (this)->Get(key);
    }

    MaybeLocal<Value> Object::Get(Local<Context> context, Local<Value> key) {
        return reinterpret_cast<GraalObject*> (this)->Get(key);
    }

    bool Object::HasOwnProperty(Local<String> key) {
        return reinterpret_cast<GraalObject*> (this)->HasOwnProperty(key);
    }

    Maybe<bool> Object::HasOwnProperty(Local<Context> context, Local<Name> key) {
        return Just(reinterpret_cast<GraalObject*> (this)->HasOwnProperty(key));
    }

    bool Object::HasRealIndexedProperty(uint32_t index) {
        return HasOwnProperty(Integer::NewFromUnsigned(GetIsolate(), index)->ToString());
    }

    bool Object::Has(Local<Value> key) {
        return reinterpret_cast<GraalObject*> (this)->Has(key);
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

    bool Object::SetAccessor(
            Local<String> name,
            void (*getter)(Local<String>, PropertyCallbackInfo<Value> const&),
            void (*setter)(Local<String>, Local<Value>, PropertyCallbackInfo<void> const&),
            Local<Value> data,
            AccessControl settings,
            PropertyAttribute attributes
            ) {
        return reinterpret_cast<GraalObject*> (this)->SetAccessor(name, getter, setter, data, settings, attributes);
    }

    void Object::SetAlignedPointerInInternalField(int index, void* value) {
        reinterpret_cast<GraalObject*> (this)->SetAlignedPointerInInternalField(index, value);
    }

    bool Object::SetPrototype(Local<Value> prototype) {
        return reinterpret_cast<GraalObject*> (this)->SetPrototype(prototype);
    }

    Maybe<bool> Object::SetPrototype(Local<Context> context, Local<Value> prototype) {
        return Just(reinterpret_cast<GraalObject*> (this)->SetPrototype(prototype));
    }

    Maybe<bool> Object::Set(Local<Context> context, uint32_t index, Local<Value> value) {
        return Just(reinterpret_cast<GraalObject*> (this)->Set(index, value));
    }

    bool Object::Set(uint32_t index, Local<Value> value) {
        return reinterpret_cast<GraalObject*> (this)->Set(index, value);
    }

    Maybe<bool> Object::Set(Local<Context> context, Local<Value> key, Local<Value> value) {
        return Just(reinterpret_cast<GraalObject*> (this)->Set(key, value));
    }

    bool Object::Set(Local<Value> key, Local<Value> value) {
        return reinterpret_cast<GraalObject*> (this)->Set(key, value);
    }

    void* Object::SlowGetAlignedPointerFromInternalField(int index) {
        return reinterpret_cast<GraalObject*> (this)->SlowGetAlignedPointerFromInternalField(index);
    }

    MaybeLocal<Value> Object::CallAsFunction(Local<Context> context, Local<Value> recv, int argc, Local<Value> argv[]) {
        return reinterpret_cast<Function*> (this)->Call(context, recv, argc, argv);
    }

    Local<Object> ObjectTemplate::NewInstance() {
        Local<Context> context = Isolate::GetCurrent()->GetCurrentContext();
        return reinterpret_cast<GraalObjectTemplate*> (this)->NewInstance(context);
    }

    MaybeLocal<Object> ObjectTemplate::NewInstance(Local<Context> context) {
        return reinterpret_cast<GraalObjectTemplate*> (this)->NewInstance(context);
    }

    Local<ObjectTemplate> ObjectTemplate::New() {
        return New(Isolate::GetCurrent());
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
            Local<AccessorSignature> signature) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetAccessor(name, getter, setter, data, settings, attribute, signature);
    }

    void ObjectTemplate::SetAccessor(
            Local<Name> name,
            AccessorNameGetterCallback getter,
            AccessorNameSetterCallback setter,
            Local<Value> data,
            AccessControl settings,
            PropertyAttribute attribute,
            Local<AccessorSignature> signature) {
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

    void ObjectTemplate::SetNamedPropertyHandler(NamedPropertyGetterCallback getter,
            NamedPropertySetterCallback setter,
            NamedPropertyQueryCallback query,
            NamedPropertyDeleterCallback deleter,
            NamedPropertyEnumeratorCallback enumerator,
            Local<Value> data) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetNamedPropertyHandler(
                getter, setter, query, deleter, enumerator, data);
    }

    void ObjectTemplate::SetHandler(const IndexedPropertyHandlerConfiguration& configuration) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetHandler(configuration);
    }

    void ObjectTemplate::SetCallAsFunctionHandler(FunctionCallback callback, Local<Value> data) {
        reinterpret_cast<GraalObjectTemplate*> (this)->SetCallAsFunctionHandler(callback, data);
    }

    ResourceConstraints::ResourceConstraints() {
        TRACE
    }

    ScriptCompiler::CachedData::~CachedData() {
        if (buffer_policy == BufferOwned) {
            delete[] data;
        }
    }

    Local<UnboundScript> ScriptCompiler::CompileUnbound(Isolate* isolate, ScriptCompiler::Source* source, ScriptCompiler::CompileOptions options) {
        Local<Value> resource_name = source->resource_name;
        Local<String> file_name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate);
        return GraalUnboundScript::Compile(source->source_string, file_name);
    }

    Local<Script> ScriptCompiler::Compile(Isolate* isolate, Source* source, CompileOptions options) {
        Local<Value> resource_name = source->resource_name;
        Local<String> file_name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate);
        return GraalScript::Compile(source->source_string, file_name);
    }

    MaybeLocal<Script> ScriptCompiler::Compile(Local<Context> context, Source* source, CompileOptions options) {
        Local<Value> resource_name = source->resource_name;
        Local<String> file_name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(context->GetIsolate());
        return GraalScript::Compile(source->source_string, file_name);
    }

    Local<Script> Script::Compile(Local<String> source, Local<String> filename) {
        return GraalScript::Compile(source, filename);
    }

    Local<Script> Script::Compile(Local<String> source, ScriptOrigin* origin) {
        return GraalScript::Compile(source, origin == nullptr ? Local<String>() : origin->ResourceName().As<String>());
    }

    Local<Value> Script::Run() {
        return reinterpret_cast<GraalScript*> (this)->Run();
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

    Local<String> String::Concat(Local<String> left, Local<String> right) {
        GraalString* graal_left = reinterpret_cast<GraalString*> (*left);
        GraalString* graal_right = reinterpret_cast<GraalString*> (*right);
        jstring java_left = (jstring) graal_left->GetJavaObject();
        jstring java_right = (jstring) graal_right->GetJavaObject();
        GraalIsolate* isolate = graal_left->Isolate();
        JNIEnv* env = isolate->GetJNIEnv();
        int left_length = env->GetStringLength(java_left);
        int right_length = env->GetStringLength(java_right);
        int length = left_length + right_length;
        jchar* str = new jchar[length];
        env->GetStringRegion(java_left, 0, left_length, str);
        env->GetStringRegion(java_right, 0, right_length, str + left_length);
        GraalString* graal_concat = new GraalString(isolate, env->NewString(str, length));
        delete[] str;
        return reinterpret_cast<String*> (graal_concat);
    }

    const String::ExternalOneByteStringResource* String::GetExternalOneByteStringResource() const {
        TRACE
        return NULL;
    }

    Local<String> String::NewExternal(Isolate* isolate, ExternalOneByteStringResource* resource) {
        return GraalString::NewExternal(isolate, resource);
    }

    Local<String> String::NewExternal(Isolate* isolate, ExternalStringResource* resource) {
        return GraalString::NewExternal(isolate, resource);
    }

    MaybeLocal<String> String::NewFromOneByte(
            Isolate* isolate,
            unsigned char const* data,
            v8::NewStringType type,
            int length) {
        return GraalString::NewFromOneByte(isolate, data, (String::NewStringType)type, length);
    }

    Local<String> String::NewFromOneByte(
            Isolate* isolate,
            unsigned char const* data,
            String::NewStringType type,
            int length
            ) {
        return GraalString::NewFromOneByte(isolate, data, type, length);
    }

    MaybeLocal<String> String::NewFromTwoByte(
            Isolate* isolate,
            const uint16_t* data,
            v8::NewStringType type,
            int length) {
        return GraalString::NewFromTwoByte(isolate, data, (String::NewStringType)type, length);
    }

    Local<String> String::NewFromTwoByte(Isolate* isolate, const uint16_t* data, String::NewStringType type, int length) {
        return GraalString::NewFromTwoByte(isolate, data, type, length);
    }

    MaybeLocal<String> String::NewFromUtf8(Isolate* isolate, char const* data, v8::NewStringType type, int length) {
        return GraalString::NewFromUtf8(isolate, data, (String::NewStringType)type, length);
    }

    Local<String> String::NewFromUtf8(
            Isolate* isolate,
            char const* data,
            String::NewStringType type,
            int length
            ) {
        return GraalString::NewFromUtf8(isolate, data, type, length);
    }

    String::Utf8Value::~Utf8Value() {
        if (java_string_ != nullptr) {
            JNIEnv* env = reinterpret_cast<GraalIsolate*> (isolate_)->GetJNIEnv();
            env->DeleteLocalRef((jobject) java_string_);
            delete[] str_;
        }
    }

    String::Utf8Value::Utf8Value(Local<v8::Value> obj) {
        JNIEnv* env;
        if (obj.IsEmpty()) {
            java_string_ = nullptr;
            env = nullptr;
        } else {
            GraalValue* graal_obj = reinterpret_cast<GraalValue*> (*obj);
            GraalIsolate* graal_isolate = graal_obj->Isolate();
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

    String::Value::Value(Local<v8::Value> obj) {
        GraalValue* graal_obj = reinterpret_cast<GraalValue*> (*obj);
        GraalIsolate* graal_isolate = graal_obj->Isolate();
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
        if (!rethrow_ && HasCaught()) {
            JNIEnv* env = graal_isolate->GetJNIEnv();
            if (is_verbose_) {
                jthrowable java_exception = env->ExceptionOccurred();
                Local<Value> exception = Exception();
                Local<v8::Message> message = Message();
                env->ExceptionClear();
                graal_isolate->SendMessage(message, exception, java_exception);
            } else if (REPORT_CAUGHT_EXCEPTIONS) {
                env->ExceptionDescribe();
            } else {
                env->ExceptionClear();
            }
        }
    }

    TryCatch::TryCatch() : TryCatch(reinterpret_cast<v8::Isolate*> (CurrentIsolate())) {
    }

    TryCatch::TryCatch(Isolate* isolate) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        isolate_ = reinterpret_cast<v8::internal::Isolate*> (isolate);
        rethrow_ = graal_isolate->GetJNIEnv()->ExceptionCheck(); // Do not catch exceptions thrown before
        exception_ = rethrow_ ? graal_isolate->GetJNIEnv()->ExceptionOccurred() : nullptr;
        is_verbose_ = false;
        graal_isolate->TryCatchEnter();
    }
    
#define ArrayBufferViewNew(view_class, view_type, graal_access_method) \
    Local<view_class> view_class::New(Local<ArrayBuffer> array_buffer, size_t byte_offset, size_t length) { \
        GraalArrayBuffer* graal_array_buffer = reinterpret_cast<GraalArrayBuffer*> (*array_buffer); \
        jobject java_array_buffer = graal_array_buffer->GetJavaObject(); \
        GraalIsolate* graal_isolate = graal_array_buffer->Isolate(); \
        JNI_CALL(jobject, java_array_buffer_view, graal_isolate, GraalAccessMethod::graal_access_method, Object, java_array_buffer, (jint) byte_offset, (jint) length); \
        return reinterpret_cast<view_class*> (new GraalArrayBufferView(graal_isolate, java_array_buffer_view, GraalArrayBufferView::view_type)); \
    }

    ArrayBufferViewNew(Uint8Array, kUint8Array, uint8_array_new)
    ArrayBufferViewNew(Uint8ClampedArray, kUint8ClampedArray, uint8_clamped_array_new)
    ArrayBufferViewNew(Int8Array, kInt8Array, int8_array_new)
    ArrayBufferViewNew(Uint16Array, kUint16Array, uint16_array_new)
    ArrayBufferViewNew(Int16Array, kInt16Array, int16_array_new)
    ArrayBufferViewNew(Uint32Array, kUint32Array, uint32_array_new)
    ArrayBufferViewNew(Int32Array, kInt32Array, int32_array_new)
    ArrayBufferViewNew(Float32Array, kFloat32Array, float32_array_new)
    ArrayBufferViewNew(Float64Array, kFloat64Array, float64_array_new)
    ArrayBufferViewNew(DataView, kDataView, data_view_new)

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

    void* V8::ClearWeak(internal::Object** global_handle) {
        GraalHandleContent* handle = reinterpret_cast<GraalHandleContent*> (global_handle);
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

    void V8::DisposeGlobal(internal::Object** global_handle) {
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
        TRACE
    }

    const char* V8::GetVersion() {
        TRACE
        return "5.0.0";
    }

    internal::Object** V8::GlobalizeReference(internal::Isolate* isolate, internal::Object** obj) {
        GraalHandleContent* graal_original = reinterpret_cast<GraalHandleContent*> (obj);
        GraalHandleContent* graal_copy = graal_original->Copy(true);
        return reinterpret_cast<internal::Object**> (graal_copy);
    }

    internal::Object** V8::CopyPersistent(internal::Object** handle) {
        GraalHandleContent* graal_handle = reinterpret_cast<GraalHandleContent*> (handle);
        graal_handle->ReferenceAdded();
        return handle;
    }

    bool V8::Initialize() {
        TRACE
        return true;
    }

    void V8::InitializePlatform(Platform*) {
        TRACE
    }

    void V8::MakeWeak(internal::Object** global_handle, void* data,
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

    void V8::SetEntropySource(EntropySource source) {
        TRACE
    }

    void V8::AddGCPrologueCallback(GCCallback callback, GCType gc_type_filter) {
        CurrentIsolate()->AddGCPrologueCallback(GraalIsolate::kV8GCCallbackType, (void*) callback);
    }

    void V8::AddGCEpilogueCallback(GCCallback callback, GCType gc_type_filter) {
        CurrentIsolate()->AddGCEpilogueCallback(GraalIsolate::kV8GCCallbackType, (void*) callback);
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
        bool use_jvm = false;
        bool use_native = false;
        std::string vm_args;

        int unprocessed = 0;
        for (int index = 1; index < *argc; index++) {
            char* const arg = argv[index];
            const char *classpath = nullptr;
            if (!strcmp(arg, "--jvm")) {
                use_jvm = true;
            } else if (!strcmp(arg, "--native")) {
                use_native = true;
            } else if (!strncmp(arg, "--jvm.classpath", sizeof ("--jvm.classpath") - 1)) {
                classpath = arg + sizeof ("--jvm.classpath") - 1;
            } else if (!strncmp(arg, "--jvm.cp", sizeof ("--jvm.cp") - 1)) {
                classpath = arg + sizeof ("--jvm.cp") - 1;
            } else if (!strncmp(arg, "--jvm.", sizeof ("--jvm.") - 1) || (!strncmp(arg, "--native.", sizeof ("--native.") - 1) && strcmp(arg, "--native.help"))) {
                use_jvm = use_jvm || arg[2] == 'j';
                use_native = use_native || arg[2] == 'n';
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
                    exit(10);
                }
            } else if (!strcmp(arg, "--use-classpath-env-var")) {
                GraalIsolate::use_classpath_env_var = true;
            } else {
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
                exit(9);
            }
        }

        if (!vm_args.empty()) {
            char *existing = getenv("NODE_JVM_OPTIONS");
            if (existing != NULL) {
                vm_args.append(" ").append(existing);
            }
            setenv("NODE_JVM_OPTIONS", vm_args.c_str(), 1);
        }

        GraalIsolate::SetMode(use_jvm ? GraalIsolate::kModeJVM : (use_native ? GraalIsolate::kModeNative : GraalIsolate::kModeDefault));
        GraalIsolate::SetFlags(unprocessed, argv + 1);
        if (remove_flags) {
            // claim that we understood and processed all command line options
            // (we have termined already if we encountered an unknown option)
            *argc = 1;
        }
    }

    void V8::SetFlagsFromString(char const*, int) {
        TRACE
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

    Local<String> Value::ToString(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToString(isolate);
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

    int32_t Value::Int32Value() const {
        return reinterpret_cast<const GraalValue*> (this)->Int32Value();
    }

    Maybe<int32_t> Value::Int32Value(Local<Context> context) const {
        return Just<int32_t>(reinterpret_cast<const GraalValue*> (this)->Int32Value());
    }

    uint32_t Value::Uint32Value() const {
        return reinterpret_cast<const GraalValue*> (this)->Uint32Value();
    }

    Maybe<uint32_t> Value::Uint32Value(Local<Context> context) const {
        return Just<uint32_t>(reinterpret_cast<const GraalValue*> (this)->Uint32Value());
    }

    int64_t Value::IntegerValue() const {
        return reinterpret_cast<const GraalValue*> (this)->IntegerValue();
    }

    Maybe<int64_t> Value::IntegerValue(Local<Context> context) const {
        return Just<int64_t>(reinterpret_cast<const GraalValue*> (this)->IntegerValue());
    }

    bool Value::BooleanValue() const {
        return reinterpret_cast<const GraalValue*> (this)->BooleanValue();
    }

    Maybe<bool> Value::BooleanValue(Local<Context> context) const {
        return Just<bool>(reinterpret_cast<const GraalValue*> (this)->BooleanValue());
    }

    double Value::NumberValue() const {
        return reinterpret_cast<const GraalValue*> (this)->NumberValue();
    }

    Maybe<double> Value::NumberValue(Local<Context> context) const {
        return Just<double>(reinterpret_cast<const GraalValue*> (this)->NumberValue());
    }

    void* External::Value() const {
        const GraalExternal* external = reinterpret_cast<const GraalExternal*> (this);
        return external->Value();
    }

    Local<Object> Function::NewInstance() const {
        return NewInstance(0, nullptr);
    }

    Local<Object> Function::NewInstance(int argc, Local<Value>* argv) const {
        return reinterpret_cast<const GraalFunction*> (this)->NewInstance(argc, argv);
    }

    MaybeLocal<Object> Function::NewInstance(Local<Context> context, int argc, Local<Value> argv[]) const {
        return reinterpret_cast<const GraalFunction*> (this)->NewInstance(argc, argv);
    }

    Local<Function> Function::New(Isolate* isolate, FunctionCallback callback, Local<Value> data, int length) {
        return FunctionTemplate::New(isolate, callback, data, Local<Signature>(), length)->GetFunction();
    }

    MaybeLocal<Function> Function::New(Local<Context> context, FunctionCallback callback, Local<Value> data, int length, ConstructorBehavior behavior) {
        return FunctionTemplate::New(context->GetIsolate(), callback, data, Local<Signature>(), length)->GetFunction();
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

    int Message::GetLineNumber() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetLineNumber();
    }

    Local<Value> Message::GetScriptResourceName() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetScriptResourceName();
    }

    Local<String> Message::GetSourceLine() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetSourceLine();
    }

    int Message::GetStartColumn() const {
        return reinterpret_cast<const GraalMessage*> (this)->GetStartColumn();
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
        TRACE
        return false;
    }

    int StackTrace::GetFrameCount() const {
        return reinterpret_cast<const GraalStackTrace*> (this)->GetFrameCount();
    }

    Local<StackFrame> StackTrace::GetFrame(uint32_t index) const {
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
        TRACE
        return false;
    }

    int String::Length() const {
        return reinterpret_cast<const GraalString*> (this)->Length();
    }

    int String::Utf8Length() const {
        return reinterpret_cast<const GraalString*> (this)->Utf8Length();
    }

    int String::WriteOneByte(uint8_t* buffer, int start, int length, int options) const {
        return reinterpret_cast<const GraalString*> (this)->WriteOneByte(buffer, start, length, options);
    }

    int String::Write(uint16_t* buffer, int start, int length, int options) const {
        return reinterpret_cast<const GraalString*> (this)->Write(buffer, start, length, options);
    }

    int String::WriteUtf8(char* buffer, int length, int* nchars_ref, int options) const {
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
            GraalValue* graal_exception = GraalValue::FromJavaObject(graal_isolate, exception_object);

            // Restore the original pending exception (unless we managed
            // to generate a new one from the call above already)
            if (!env->ExceptionCheck()) {
                env->Throw(java_exception);
            }
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
            GraalMessage* graal_message = new GraalMessage(graal_isolate, java_exception);
            return reinterpret_cast<v8::Message*> (graal_message);
        } else {
            return Local<v8::Message>();
        }
    }

    bool Value::Equals(Local<Value> that) const {
        return reinterpret_cast<const GraalValue*> (this)->Equals(that);
    }

    Maybe<bool> Value::Equals(Local<Context> context, Local<Value> that) const {
        return Just(reinterpret_cast<const GraalValue*> (this)->Equals(that));
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

    bool Value::StrictEquals(Local<Value> that) const {
        return reinterpret_cast<const GraalValue*> (this)->StrictEquals(that);
    }

    Local<Boolean> Value::ToBoolean(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToBoolean(isolate);
    }

    MaybeLocal<Boolean> Value::ToBoolean(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToBoolean(isolate);
    }

    Local<Integer> Value::ToInteger(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToInteger(isolate);
    }

    MaybeLocal<Integer> Value::ToInteger(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToInteger(isolate);
    }

    Local<Int32> Value::ToInt32(v8::Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToInt32(isolate);
    }

    MaybeLocal<Int32> Value::ToInt32(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToInt32(isolate);
    }

    Local<Uint32> Value::ToUint32(v8::Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToUint32(isolate);
    }

    MaybeLocal<Uint32> Value::ToUint32(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToUint32(isolate);
    }

    Local<Object> Value::ToObject(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToObject(isolate);
    }

    MaybeLocal<Object> Value::ToObject(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToObject(isolate);
    }

    Local<Number> Value::ToNumber(Isolate* isolate) const {
        return reinterpret_cast<const GraalValue*> (this)->ToNumber(isolate);
    }

    MaybeLocal<Number> Value::ToNumber(Local<Context> context) const {
        const GraalValue* graal_value = reinterpret_cast<const GraalValue*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_value->Isolate());
        return graal_value->ToNumber(isolate);
    }

    Local<Uint32> Value::ToArrayIndex() const {
        return reinterpret_cast<const GraalValue*> (this)->ToArrayIndex();
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

    Local<Value> Date::New(Isolate* isolate, double time) {
        return GraalDate::New(isolate, time);
    }

    MaybeLocal<Value> Date::New(Local<Context> context, double time) {
        return GraalDate::New(context->GetIsolate(), time);
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

    Local<Message> Exception::CreateMessage(Local<Value> exception) {
        GraalValue* graal_exception = reinterpret_cast<GraalValue*> (*exception);
        jobject exception_object = graal_exception->GetJavaObject();
        GraalIsolate* graal_isolate = graal_exception->Isolate();
        JNI_CALL(jobject, java_exception, graal_isolate, GraalAccessMethod::exception_create_message, Object, exception_object);
        return reinterpret_cast<v8::Message*> (new GraalMessage(graal_isolate, java_exception));
    }

    Maybe<bool> Object::Has(Local<Context> context, Local<Value> key) {
        return Just(Has(key));
    }

    Maybe<bool> Object::Has(Local<Context> context, uint32_t index) {
        return Just(Has(Integer::NewFromUnsigned(context->GetIsolate(), index)));
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

    void Debug::ProcessDebugMessages(Isolate* isolate) {
        TRACE
    }

    Maybe<bool> Object::SetAccessor(
            Local<Context> context,
            Local<Name> name,
            AccessorNameGetterCallback getter,
            AccessorNameSetterCallback setter,
            MaybeLocal<Value> data,
            AccessControl settings,
            PropertyAttribute attribute) {
        return v8::Just(reinterpret_cast<GraalObject*> (this)->SetAccessor(
                reinterpret_cast<String*> (*name),
                reinterpret_cast<AccessorGetterCallback> (getter),
                reinterpret_cast<AccessorSetterCallback> (setter),
                data.ToLocalChecked(),
                settings,
                attribute));
    }

    bool Object::SetAccessor(Local<Name> name,
            AccessorNameGetterCallback getter,
            AccessorNameSetterCallback setter,
            Local<Value> data,
            AccessControl settings,
            PropertyAttribute attribute) {
        return reinterpret_cast<GraalObject*> (this)->SetAccessor(
                reinterpret_cast<String*> (*name),
                reinterpret_cast<AccessorGetterCallback> (getter),
                reinterpret_cast<AccessorSetterCallback> (setter),
                data,
                settings,
                attribute);
    }

    Maybe<bool> Object::DefineOwnProperty(Local<Context> context, Local<Name> key, Local<Value> value, PropertyAttribute attributes) {
        return ForceSet(context, key, value, attributes);
    }

    MaybeLocal<Script> Script::Compile(Local<Context> context, Local<String> source, ScriptOrigin* origin) {
        return Script::Compile(source, origin);
    }

    Local<Private> Private::ForApi(Isolate* isolate, Local<String> name) {
        return reinterpret_cast<Private*> (*name);
    }

    void Isolate::ReportExternalAllocationLimitReached() {
        TRACE
    }

    Local<Context> Debug::GetDebugContext(Isolate* isolate) {
        return reinterpret_cast<GraalIsolate*> (isolate)->GetDebugContext();
    }

    ScriptCompiler::CachedData::CachedData(const uint8_t* data, int length, BufferPolicy buffer_policy): data(data), length(length), rejected(false), buffer_policy(buffer_policy) {
    }

    MaybeLocal<UnboundScript> ScriptCompiler::CompileUnboundScript(Isolate* isolate, Source* source, CompileOptions options) {
        if (options == ScriptCompiler::kProduceCodeCache) {
            String::Utf8Value text(source->source_string);
            uint8_t* copy = new uint8_t[text.length()];
            memcpy(copy, *text, text.length());
            source->cached_data = new ScriptCompiler::CachedData((const uint8_t*) copy, text.length());
        } else if (options == ScriptCompiler::kConsumeCodeCache) {
            String::Utf8Value text(source->source_string);
            CachedData* data = source->cached_data;
            if (data->length != text.length() || memcmp(data->data, (const uint8_t*) *text, text.length())) {
                data->rejected = true;
            }
        }
        return CompileUnbound(isolate, source, options);
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
                IsFloat64Array();
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

    Local<Object> Proxy::GetTarget() {
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
            "new_space",
            "old_space",
            "code_space",
            "map_space",
            "large_object_space"
        };
        space_statistics->space_name_ = names[index];
        space_statistics->space_size_ = 0;
        space_statistics->space_used_size_ = 0;
        space_statistics->space_available_size_ = 0;
        space_statistics->physical_space_size_ = 0;
        return true;
    }

    size_t Isolate::NumberOfHeapSpaces() {
        return 5;
    }

    Local<AccessorSignature> AccessorSignature::New(Isolate* isolate, Local<FunctionTemplate> receiver) {
        return reinterpret_cast<AccessorSignature*> (*receiver);
    }

    void Debug::SetMessageHandler(Isolate* isolate, MessageHandler handler) {
        TRACE
    }

    MaybeLocal<String> String::NewExternalOneByte(Isolate* isolate, ExternalOneByteStringResource* resource) {
        return String::NewExternal(isolate, resource);
    }

    MaybeLocal<String> String::NewExternalTwoByte(Isolate* isolate, ExternalStringResource* resource) {
        return String::NewExternal(isolate, resource);
    }

    Local<Value> BooleanObject::New(Isolate* isolate, bool value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jboolean java_value = value;
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::boolean_object_new, Object, java_context, java_value);
        GraalObject* graal_object = new GraalObject(graal_isolate, java_object);
        return reinterpret_cast<BooleanObject*> (graal_object);
    }

    bool BooleanObject::ValueOf() const {
        const GraalObject* graal_object = reinterpret_cast<const GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jboolean, value, graal_isolate, GraalAccessMethod::boolean_object_value_of, Boolean, java_object);
        return value;
    }

    Local<Value> StringObject::New(Local<String> value) {
        GraalIsolate* graal_isolate = CurrentIsolate();
        jobject java_context = graal_isolate->CurrentJavaContext();
        GraalString* graal_value = reinterpret_cast<GraalString*> (*value);
        jobject java_value = graal_value->GetJavaObject();
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::string_object_new, Object, java_context, java_value);
        GraalObject* graal_object = new GraalObject(graal_isolate, java_object);
        return reinterpret_cast<StringObject*> (graal_object);
    }

    Local<String> StringObject::ValueOf() const {
        const GraalObject* graal_object = reinterpret_cast<const GraalObject*> (this);
        GraalIsolate* graal_isolate = graal_object->Isolate();
        jobject java_object = graal_object->GetJavaObject();
        JNI_CALL(jobject, value, graal_isolate, GraalAccessMethod::string_object_value_of, Object, java_object);
        GraalString* graal_string = new GraalString(graal_isolate, (jstring) value);
        return reinterpret_cast<v8::String*> (graal_string);
    }

    Local<Value> NumberObject::New(Isolate* isolate, double value) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jdouble java_value = value;
        JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::number_object_new, Object, java_context, java_value);
        GraalObject* graal_object = new GraalObject(graal_isolate, java_object);
        return reinterpret_cast<NumberObject*> (graal_object);
    }

    Local<RegExp> RegExp::New(Local<String> pattern, Flags flags) {
        return GraalRegExp::New(CurrentIsolate()->GetCurrentContext(), pattern, flags);
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

    Local<Value> Isolate::CorrectReturnValue(internal::Object* value) {
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

    void Isolate::EnterPolyglotEngine(void* param1, void* param2, int argc, void* argv, int exec_argc, void* exec_argv, void (*callback) (void* isolate, void* param1, void* param2, int argc, void* argv, int exec_argc, void* exec_argv)) {
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enter_polyglot_engine, (jlong) callback, (jlong) this, (jlong) param1, (jlong) param2, (jint) argc, (jlong) argv, (jint) exec_argc, (jlong) exec_argv);
    }

    MaybeLocal<Value> JSON::Parse(Isolate* isolate, Local<String> json_string) {
        GraalIsolate* graal_isolate = reinterpret_cast<GraalIsolate*> (isolate);
        jobject java_context = graal_isolate->CurrentJavaContext();
        jobject java_string = reinterpret_cast<GraalString*> (*json_string)->GetJavaObject();
        JNI_CALL(jobject, java_value, graal_isolate, GraalAccessMethod::json_parse, Object, java_context, java_string);
        if (java_value == nullptr) {
            return Local<Value>();
        } else {
            GraalValue* graal_value = GraalValue::FromJavaObject(graal_isolate, java_value);
            return Local<Value>(reinterpret_cast<Value*> (graal_value));
        }
    }

    MaybeLocal<Value> JSON::Parse(Local<Context> context, Local<String> json_string) {
        return Parse(context->GetIsolate(), json_string);
    }

    MaybeLocal<String> JSON::Stringify(Local<Context> context, Local<Object> json_object, Local<String> gap) {
        GraalContext* graal_context = reinterpret_cast<GraalContext*> (*context);
        GraalIsolate* graal_isolate = graal_context->Isolate();
        jobject java_context = graal_context->GetJavaObject();
        jobject java_object = reinterpret_cast<GraalObject*> (*json_object)->GetJavaObject();
        jstring java_gap = gap.IsEmpty() ? nullptr : (jstring) reinterpret_cast<GraalString*> (*gap)->GetJavaObject();
        JNI_CALL(jobject, java_result, graal_isolate, GraalAccessMethod::json_stringify, Object, java_context, java_object, java_gap);
        if (java_result == nullptr) {
            return Local<String>();
        } else {
            GraalString* graal_string = new GraalString(graal_isolate, (jstring) java_result);
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
        TRACE
        return false;
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

    Local<Promise::Resolver> Promise::Resolver::New(Isolate* isolate) {
        return Promise::Resolver::New(isolate->GetCurrentContext()).ToLocalChecked();
    }

    Maybe<bool> Promise::Resolver::Resolve(Local<Context> context, Local<Value> value) {
        return GraalPromise::ResolverResolve(this, value);
    }

    void Promise::Resolver::Resolve(Local<Value> value) {
        GraalPromise::ResolverResolve(this, value);
    }

    Maybe<bool> Promise::Resolver::Reject(Local<Context> context, Local<Value> value) {
        return GraalPromise::ResolverReject(this, value);
    }

    void Promise::Resolver::Reject(Local<Value> value) {
        GraalPromise::ResolverReject(this, value);
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

    MaybeLocal<Module> ScriptCompiler::CompileModule(Isolate* isolate, Source* source) {
        Local<Value> resource_name = source->resource_name;
        Local<String> name = resource_name.IsEmpty() ? resource_name.As<String>() : resource_name->ToString(isolate);
        return GraalModule::Compile(source->source_string, name);
    }

    uint32_t ScriptCompiler::CachedDataVersionTag() {
        TRACE
        return 0;
    }

    Maybe<bool> Object::SetIntegrityLevel(Local<Context> context, IntegrityLevel level) {
        TRACE
        return Nothing<bool>();
    }

    ScriptOrigin Message::GetScriptOrigin() const {
        TRACE
        const GraalMessage* graal_message = reinterpret_cast<const GraalMessage*> (this);
        Isolate* isolate = reinterpret_cast<Isolate*> (graal_message->Isolate());
        Local<Integer> zero = Integer::New(isolate, 0);
        return ScriptOrigin(Local<Value>(), zero, zero);
    }

    void ArrayBuffer::Allocator::SetProtection(void* data, size_t length, Protection protection) {
        TRACE
    }

    void* ArrayBuffer::Allocator::Reserve(size_t length) {
        TRACE
        return nullptr;
    }

    void ArrayBuffer::Allocator::Free(void* data, size_t length, AllocationMode mode) {
        TRACE
    }

    ArrayBuffer::Allocator* ArrayBuffer::Allocator::NewDefaultAllocator() {
        TRACE
        return nullptr;
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

    ValueSerializer::~ValueSerializer() {
        TRACE
    }

    ValueSerializer::ValueSerializer(Isolate* isolate, Delegate* delegate) {
        TRACE
    }

    void ValueSerializer::WriteUint32(uint32_t value) {
        TRACE
    }

    void ValueSerializer::WriteUint64(uint64_t value) {
        TRACE
    }

    void ValueSerializer::WriteDouble(double value) {
        TRACE
    }

    void ValueSerializer::WriteRawBytes(const void* source, size_t length) {
        TRACE
    }

    Maybe<bool> ValueSerializer::WriteValue(Local<Context> context, Local<Value> value) {
        TRACE
        return Nothing<bool>();
    }

    void ValueSerializer::SetTreatArrayBufferViewsAsHostObjects(bool mode) {
        TRACE
    }

    void ValueSerializer::TransferArrayBuffer(uint32_t transfer_id, Local<ArrayBuffer> array_buffer) {
        TRACE
    }

    std::pair<uint8_t*, size_t> ValueSerializer::Release() {
        TRACE
        return {nullptr, 0};
    }

    void ValueSerializer::WriteHeader() {
        TRACE
    }

    Maybe<bool> ValueSerializer::Delegate::WriteHostObject(Isolate* isolate, Local<Object> object) {
        TRACE
        return Nothing<bool>();
    }

    Maybe<uint32_t> ValueSerializer::Delegate::GetSharedArrayBufferId(Isolate* isolate, Local<SharedArrayBuffer> shared_array_buffer) {
        TRACE
        return Nothing<uint32_t>();
    }

    Maybe<uint32_t> ValueSerializer::Delegate::GetWasmModuleTransferId(Isolate* isolate, Local<WasmCompiledModule> module) {
        TRACE
        return Nothing<uint32_t>();
    }

    void* ValueSerializer::Delegate::ReallocateBufferMemory(void* old_buffer, size_t size, size_t* actual_size) {
        TRACE
        return nullptr;
    }

    void ValueSerializer::Delegate::FreeBufferMemory(void* buffer) {
        TRACE
    }

    ValueDeserializer::ValueDeserializer(Isolate* isolate, const uint8_t* data, size_t size, Delegate* delegate) {
        TRACE
    }

    ValueDeserializer::~ValueDeserializer() {
        TRACE
    }

    Maybe<bool> ValueDeserializer::ReadHeader(Local<Context> context) {
        TRACE
        return Nothing<bool>();
    }

    MaybeLocal<Value> ValueDeserializer::ReadValue(Local<Context> context) {
        TRACE
        return MaybeLocal<Value>();
    }

    bool ValueDeserializer::ReadDouble(double* value) {
        TRACE
        return false;
    }

    bool ValueDeserializer::ReadUint32(uint32_t* value) {
        TRACE
        return false;
    }

    bool ValueDeserializer::ReadUint64(uint64_t* value) {
        TRACE
        return false;
    }

    bool ValueDeserializer::ReadRawBytes(size_t length, const void** data) {
        TRACE
        return false;
    }

    MaybeLocal<Object> ValueDeserializer::Delegate::ReadHostObject(Isolate* isolate) {
        TRACE
        return MaybeLocal<Object>();
    }

    MaybeLocal<WasmCompiledModule> ValueDeserializer::Delegate::GetWasmModuleFromId(Isolate* isolate, uint32_t transfer_id) {
        TRACE
        return MaybeLocal<WasmCompiledModule>();
    }

    uint32_t ValueDeserializer::GetWireFormatVersion() const {
        TRACE
        return 0;
    }

    void ValueDeserializer::TransferArrayBuffer(uint32_t transfer_id, Local<ArrayBuffer> array_buffer) {
        TRACE
    }

    void ValueDeserializer::TransferSharedArrayBuffer(uint32_t id, Local<SharedArrayBuffer> shared_array_buffer) {
        TRACE
    }

    v8::platform::tracing::TraceObject::~TraceObject() {
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

    v8::platform::tracing::TracingController::TracingController() {
        TRACE
    }

    v8::platform::tracing::TracingController::~TracingController() {
        TRACE
    }

    const uint8_t* v8::platform::tracing::TracingController::GetCategoryGroupEnabled(const char* category_group) {
        TRACE
        return nullptr;
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

    void V8::ShutdownPlatform() {
        TRACE
    }

    void String::VerifyExternalStringResource(ExternalStringResource* val) const {
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

}
