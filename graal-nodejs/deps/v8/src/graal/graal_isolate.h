/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GRAAL_ISOLATE_H_
#define GRAAL_ISOLATE_H_

#include "current_isolate.h"
#include "graal_handle_content.h"
#include "include/v8.h"
#include "include/v8-platform.h"
#include "jni.h"
#include "microtask_queue.h"
#include <string.h>
#include <vector>
#ifdef DEBUG
#include <algorithm>
#endif

#ifdef __POSIX__
#include <pthread.h>
#endif

#define JNI_CALL_HELPER(semicolon, equals, return_type, variable, isolate, id, type, ...) \
    return_type variable semicolon \
    { \
        GraalIsolate* graal_isolate_tmp = reinterpret_cast<GraalIsolate*> (isolate); \
        jmethodID method_id_tmp = graal_isolate_tmp->GetJNIMethod(id); \
        jobject access_tmp = graal_isolate_tmp->GetGraalAccess(); \
        JNIEnv *env_tmp = graal_isolate_tmp->GetJNIEnv(); \
        variable equals env_tmp->functions->Call ## type ## Method(env_tmp, access_tmp, method_id_tmp, ##__VA_ARGS__); \
        if (GraalIsolate::InternalErrorCheckEnabled()) { \
            graal_isolate_tmp->InternalErrorCheck(); \
        } \
    }

#define JNI_CALL(return_type, variable, isolate, id, type, ...) \
    JNI_CALL_HELPER(;, =, return_type, variable, isolate, id, type, ##__VA_ARGS__)

#define JNI_CALL_VOID(isolate, id, ...) \
    JNI_CALL_HELPER(/*;*/,/*=*/,/*return_type*/,/*variable*/, isolate, id, Void, ##__VA_ARGS__)

#define EXCEPTION_CHECK(jni_env, T) if (jni_env->ExceptionCheck()) return v8::Local<T>();

class GraalArray;
class GraalBoolean;
class GraalContext;
class GraalExternal;
class GraalFunction;
class GraalNumber;
class GraalObject;
class GraalPrimitive;
class GraalString;
class GraalValue;

enum GraalAccessMethod {
    undefined_instance,
    null_instance,
    value_type,
    value_double,
    value_string,
    value_external,
    value_unknown,
    value_to_object,
    value_to_string,
    value_to_integer,
    value_to_boolean,
    value_to_int32,
    value_to_uint32,
    value_to_number,
    value_to_array_index,
    value_int32_value,
    value_uint32_value,
    value_integer_value,
    value_is_native_error,
    value_is_set_iterator,
    value_is_map_iterator,
    value_is_shared_array_buffer,
    value_is_arguments_object,
    value_is_boolean_object,
    value_is_number_object,
    value_is_string_object,
    value_is_symbol_object,
    value_is_big_int_object,
    value_is_weak_map,
    value_is_weak_set,
    value_is_async_function,
    value_is_generator_function,
    value_is_generator_object,
    value_is_module_namespace_object,
    value_is_wasm_memory_object,
    value_equals,
    value_strict_equals,
    value_instance_of,
    value_type_of,
    value_to_detail_string,
    object_new,
    object_set,
    object_set_index,
    object_set_private,
    object_force_set,
    object_get,
    object_get_index,
    object_get_private,
    object_get_real_named_property,
    object_get_real_named_property_attributes,
    object_get_own_property_descriptor,
    object_has,
    object_has_own_property,
    object_has_private,
    object_has_real_named_property,
    object_delete,
    object_delete_index,
    object_delete_private,
    object_set_accessor,
    object_clone,
    object_get_prototype,
    object_set_prototype,
    object_get_constructor_name,
    object_get_property_names,
    object_get_own_property_names,
    object_creation_context,
    object_create_data_property,
    object_create_data_property_index,
    object_define_property,
    object_preview_entries,
    object_set_integrity_level,
    object_is_constructor,
    array_new,
    array_new_from_elements,
    array_length,
    array_buffer_new,
    array_buffer_new_buffer,
    array_buffer_new_backing_store,
    array_buffer_byte_length,
    array_buffer_get_contents,
    array_buffer_view_buffer,
    array_buffer_view_byte_length,
    array_buffer_view_byte_offset,
    array_buffer_detach,
    array_buffer_was_detached,
    typed_array_length,
    uint8_array_new,
    uint8_clamped_array_new,
    int8_array_new,
    uint16_array_new,
    int16_array_new,
    uint32_array_new,
    int32_array_new,
    float32_array_new,
    float64_array_new,
    big_int64_array_new,
    big_uint64_array_new,
    data_view_new,
    external_new,
    integer_new,
    number_new,
    date_time_configuration_change_notification,
    date_new,
    date_value_of,
    exception_error,
    exception_type_error,
    exception_syntax_error,
    exception_range_error,
    exception_reference_error,
    exception_create_message,
    isolate_throw_exception,
    isolate_run_microtasks,
    isolate_internal_error_check,
    isolate_throw_stack_overflow_error,
    isolate_get_heap_statistics,
    isolate_terminate_execution,
    isolate_cancel_terminate_execution,
    isolate_is_execution_terminating,
    isolate_request_interrupt,
    isolate_get_int_placeholder,
    isolate_get_safe_int_placeholder,
    isolate_get_double_placeholder,
    isolate_dispose,
    isolate_enter_polyglot_engine,
    isolate_perform_gc,
    isolate_enable_promise_hook,
    isolate_enable_promise_reject_callback,
    isolate_enable_import_meta_initializer,
    isolate_enable_import_module_dynamically,
    isolate_enable_prepare_stack_trace_callback,
    isolate_enter,
    isolate_exit,
    isolate_enqueue_microtask,
    isolate_schedule_pause_on_next_statement,
    isolate_measure_memory,
    isolate_set_task_runner,
    isolate_execute_runnable,
    template_set,
    template_set_accessor_property,
    object_template_new,
    object_template_new_instance,
    object_template_set_accessor,
    object_template_set_handler,
    object_template_set_call_as_function_handler,
    object_template_set_internal_field_count,
    function_new_instance,
    function_set_name,
    function_get_name,
    function_call,
    function_call0,
    function_call1,
    function_call2,
    function_call3,
    function_call4,
    function_call5,
    function_call6,
    function_call7,
    function_call8,
    function_call9,
    function_resource_name,
    function_get_script_line_number,
    function_get_script_column_number,
    function_template_new,
    function_template_set_class_name,
    function_template_instance_template,
    function_template_prototype_template,
    function_template_get_function,
    function_template_has_instance,
    function_template_set_call_handler,
    function_template_inherit,
    function_template_read_only_prototype,
    script_compile,
    script_run,
    script_get_unbound_script,
    unbound_script_compile,
    unbound_script_bind_to_context,
    unbound_script_get_id,
    unbound_script_get_content,
    context_global,
    context_set_pointer_in_embedder_data,
    context_get_pointer_in_embedder_data,
    context_set_embedder_data,
    context_get_embedder_data,
    context_get_extras_binding_object,
    context_set_promise_hooks,
    context_is_code_generation_from_strings_allowed,
    try_catch_exception,
    try_catch_has_terminated,
    message_get_script_resource_name,
    message_get_line_number,
    message_get_source_line,
    message_get_start_column,
    message_get_stack_trace,
    message_get_start_position,
    message_get_end_position,
    message_get,
    stack_trace_current_stack_trace,
    stack_frame_get_line_number,
    stack_frame_get_column,
    stack_frame_get_script_name,
    stack_frame_get_function_name,
    stack_frame_is_eval,
    make_weak,
    clear_weak,
    string_external_resource_callback,
    context_new,
    context_set_security_token,
    context_get_security_token,
    find_dynamic_object_fields,
    proxy_get_handler,
    proxy_get_target,
    proxy_is_function,
    boolean_object_new,
    boolean_object_value_of,
    regexp_new,
    regexp_get_source,
    regexp_get_flags,
    string_empty,
    string_new,
    string_new_from_two_byte,
    string_length,
    string_equals,
    string_concat,
    string_utf8_length,
    string_utf8_write,
    string_write_one_byte,
    string_write,
    string_contains_only_one_byte,
    string_object_new,
    string_object_value_of,
    number_object_new,
    object_internal_field_count,
    object_slow_get_aligned_pointer_from_internal_field,
    object_set_aligned_pointer_in_internal_field,
    object_slow_get_internal_field,
    object_set_internal_field,
    json_parse,
    json_stringify,
    symbol_new,
    symbol_name,
    symbol_for,
    symbol_for_api,
    symbol_get_async_iterator,
    symbol_get_has_instance,
    symbol_get_is_concat_spreadable,
    symbol_get_iterator,
    symbol_get_match,
    symbol_get_replace,
    symbol_get_search,
    symbol_get_split,
    symbol_get_to_primitive,
    symbol_get_to_string_tag,
    symbol_get_unscopables,
    symbol_private_for_api,
    symbol_private_new,
    promise_result,
    promise_state,
    promise_resolver_new,
    promise_resolver_resolve,
    promise_resolver_reject,
    module_compile,
    module_instantiate,
    module_evaluate,
    module_get_status,
    module_get_namespace,
    module_get_identity_hash,
    module_get_exception,
    module_get_unbound_module_script,
    module_create_synthetic_module,
    module_set_synthetic_module_export,
    module_get_module_requests,
    module_request_get_specifier,
    module_request_get_import_assertions,
    script_or_module_get_resource_name,
    script_or_module_get_host_defined_options,
    value_serializer_new,
    value_serializer_release,
    value_serializer_size,
    value_serializer_write_header,
    value_serializer_write_value,
    value_serializer_write_uint32,
    value_serializer_write_uint64,
    value_serializer_write_double,
    value_serializer_write_raw_bytes,
    value_serializer_set_treat_array_buffer_views_as_host_objects,
    value_serializer_transfer_array_buffer,
    value_deserializer_new,
    value_deserializer_read_header,
    value_deserializer_read_value,
    value_deserializer_read_uint32,
    value_deserializer_read_uint64,
    value_deserializer_read_double,
    value_deserializer_read_raw_bytes,
    value_deserializer_transfer_array_buffer,
    value_deserializer_get_wire_format_version,
    big_int_int64_value,
    big_int_uint64_value,
    big_int_new,
    big_int_new_from_unsigned,
    big_int_new_from_words,
    big_int_word_count,
    big_int_to_words_array,
    map_new,
    map_set,
    set_new,
    set_add,
    shared_array_buffer_new,
    shared_array_buffer_get_contents,
    shared_array_buffer_externalize,
    shared_array_buffer_byte_length,
    script_compiler_compile_function_in_context,
    backing_store_register_callback,
    fixed_array_length,
    fixed_array_get,
    wasm_module_object_get_compiled_module,
    wasm_module_object_from_compiled_module,

    count // Should be the last item of GraalAccessMethod
};

enum class GraalAccessField {
    array_buffer_byte_buffer,
    array_buffer_view_buffer,

    count // Should be the last item of GraalAccessField
};

template <class T, size_t kCapacity = 1024> class GraalObjectPool {
public:
    inline bool IsEmpty() {
        return size_ == 0;
    }
    inline bool IsFull() {
        return size_ == kCapacity;
    }
    inline T* Pop() {
        return pool_[--size_];
    }
    inline void Push(T* array_object) {
        pool_[size_++] = array_object;
    }
private:
    T* pool_[kCapacity];
    int size_ = 0;
};

class GraalIsolate {
public:
    GraalIsolate(JavaVM* jvm, JNIEnv* env, v8::Isolate::CreateParams const& params);
    v8::Local<v8::Value> ThrowException(v8::Local<v8::Value> exception);
    bool AddMessageListener(v8::MessageCallback callback, v8::Local<v8::Value> data);
    void NotifyMessageListener(v8::Local<v8::Message> message, v8::Local<v8::Value> error, jthrowable java_error);
    void SetAbortOnUncaughtExceptionCallback(v8::Isolate::AbortOnUncaughtExceptionCallback callback);
    bool AbortOnUncaughtExceptionCallbackValue();
    void Dispose();
    void Dispose(bool exit, int status);
    inline double ReadDoubleFromSharedBuffer();
    inline int32_t ReadInt32FromSharedBuffer();
    inline int64_t ReadInt64FromSharedBuffer();
    inline void WriteInt32ToSharedBuffer(int32_t number);
    inline void WriteInt64ToSharedBuffer(int64_t number);
    inline void WriteDoubleToSharedBuffer(double number);
    void InternalErrorCheck();
    static v8::Isolate* New(v8::Isolate::CreateParams const& params, v8::Isolate* placement = nullptr);
    void SetPromiseHook(v8::PromiseHook promise_hook);
    void NotifyPromiseHook(v8::PromiseHookType, v8::Local<v8::Promise> promise, v8::Local<v8::Value> parent);
    void SetPromiseRejectCallback(v8::PromiseRejectCallback callback);
    void NotifyPromiseRejectCallback(v8::PromiseRejectMessage message);
    void SetImportMetaInitializer(v8::HostInitializeImportMetaObjectCallback callback);
    void NotifyImportMetaInitializer(v8::Local<v8::Object> import_meta, v8::Local<v8::Module> module);
    void SetImportModuleDynamicallyCallback(v8::HostImportModuleDynamicallyCallback callback);
    v8::MaybeLocal<v8::Promise> NotifyImportModuleDynamically(v8::Local<v8::Context> context, v8::Local<v8::Data> host_defined_options, v8::Local<v8::Value> resource_name, v8::Local<v8::String> specifier, v8::Local<v8::FixedArray> import_assertions);
    void SetPrepareStackTraceCallback(v8::PrepareStackTraceCallback callback);
    v8::MaybeLocal<v8::Value> NotifyPrepareStackTraceCallback(v8::Local<v8::Context> context, v8::Local<v8::Value> error, v8::Local<v8::Array> sites);
    void SetWasmStreamingCallback(v8::WasmStreamingCallback callback);
    v8::WasmStreamingCallback GetWasmStreamingCallback();
    void EnqueueMicrotask(v8::MicrotaskCallback microtask, void* data);
    void RunMicrotasks();
    void Enter();
    void Exit();
    void HandleEmptyCallResult();
    void EnqueueMicrotask(v8::Local<v8::Function> microtask);
    void ExecuteRunnable(jobject runnable);

    enum GCCallbackType {
        kIsolateGCCallbackType = 0,
        kIsolateGCCallbackWithDataType = 1,
        kV8GCCallbackType = 2,
    };
    void AddGCPrologueCallback(GCCallbackType type, void* callback, void* data = nullptr);
    void RemoveGCPrologueCallback(void* callback);
    void AddGCEpilogueCallback(GCCallbackType type, void* callback, void* data = nullptr);
    void RemoveGCEpilogueCallback(void* callback);
    void NotifyGCCallbacks(bool prolog);

    void TerminateExecution();
    void CancelTerminateExecution();
    bool IsExecutionTerminating();
    void RequestInterrupt(v8::InterruptCallback callback, void* data);

    inline JNIEnv* GetJNIEnv() {
        return jni_env_;
    }

    inline jobject GetGraalAccess() {
        return access_;
    }

    inline jclass GetGraalAccessClass() {
        return access_class_;
    }

    inline jclass GetObjectClass() {
        return object_class_;
    }

    inline GraalPrimitive* GetUndefined() {
        return undefined_instance_;
    }

    inline GraalPrimitive* GetNull() {
        return null_instance_;
    }

    inline jobject GetJavaTrue() {
        return boolean_true_;
    }

    inline jobject GetJavaFalse() {
        return boolean_false_;
    }

    inline GraalBoolean* GetTrue() {
        return true_instance_;
    }

    inline GraalBoolean* GetFalse() {
        return false_instance_;
    }

    inline jmethodID GetJNIMethod(GraalAccessMethod id) {
        return jni_methods_[id];
    }

    inline jfieldID GetJNIField(GraalAccessField id) {
        return jni_fields_[static_cast<int>(id)];
    }

    jobject JNIGetObjectFieldOrCall(jobject java_object, GraalAccessField graal_field_id, GraalAccessMethod graal_method_id);

    inline void SetEternal(v8::Value* value, int* index) {
        if (*index == -1) {
            *index = eternals.size();
            eternals.push_back(value);
        } else {
            eternals[*index] = value;
        }
    }

    inline v8::Value* GetEternal(int index) {
        return eternals[index];
    }

    inline void ContextEnter(v8::Context* context) {
        GraalHandleContent* global_context = reinterpret_cast<GraalHandleContent*> (context)->Copy(true);
        contexts.push_back(reinterpret_cast<v8::Context*> (global_context));
    }

    inline void ContextExit(v8::Context* context) {
        GraalHandleContent* graal_context = reinterpret_cast<GraalHandleContent*> (context);
        GraalHandleContent* top_context = reinterpret_cast<GraalHandleContent*> (contexts.back());
        if (GraalHandleContent::SameData(top_context, graal_context)) {
            contexts.pop_back();
            top_context->ReferenceRemoved();
        } else {
            fprintf(stderr, "Attempt to exit a context that is not on the top of the stack!\n");
        }
    }

    inline v8::Local<v8::Context> GetCurrentContext() {
        return ContextEntered() ? contexts.back() : nullptr;
    }

    inline bool ContextEntered() {
        return !contexts.empty();
    }

    inline jobject CurrentJavaContext() {
        return reinterpret_cast<GraalHandleContent*> (contexts.back())->GetJavaObject();
    }

    inline void HandleScopeEnter() {
        handles.push_back(nullptr); // using nullptr as a separator of HandleScopes
    }

    inline void HandleScopeExit() {
        while (true) {
            GraalHandleContent* handle = handles.back();
            handles.pop_back();
            if (handle == nullptr) {
                break;
            } else {
                handle->ReferenceRemoved();
            }
        }
    }

    inline void HandleScopeReference(GraalHandleContent* handle) {
#ifdef DEBUG
        if (handle == nullptr) {
            fprintf(stderr, "NULL handle passed to HandleScopeReference!\n");
        }
        if (handles.empty()) {
            fprintf(stderr, "No HandleScope defined for a handle!\n");
        }
        if (std::find(handles.begin(), handles.end(), handle) != handles.end()) {
            fprintf(stderr, "A handle registered twice through HandleScopeReference!\n");
        }
#endif
        handles.push_back(handle);
        handle->ReferenceAdded();
    }

    static v8::Isolate* TryGetCurrent();
    static inline v8::Isolate* GetCurrent() {
        return reinterpret_cast<v8::Isolate*> (CurrentIsolate());
    }

    static inline void SetAbortOnUncaughtException(bool value) {
        abort_on_uncaught_exception_ = value;
    }

    static inline bool GetAbortOnUncaughtException() {
        return abort_on_uncaught_exception_;
    }

    static inline bool InternalErrorCheckEnabled() {
        return internal_error_check_;
    }

    inline void TryCatchEnter() {
        try_catch_count_++;
    }

    inline void TryCatchExit() {
        try_catch_count_--;
    }

    inline bool TryCatchExists() {
        return (try_catch_count_ != 0);
    }

    inline void ResetSharedBuffer() {
        shared_buffer_pos_ = 0;
    }

    inline bool StackOverflowCheckEnabled() {
        return stack_check_enabled_;
    }

    V8_INLINE bool StackOverflowCheck(intptr_t stack_top);

    void ThrowStackOverflowError();

    void FindDynamicObjectFields(jobject context);

    inline int NextFunctionTemplateID() {
        return ++function_template_count_;
    }

    inline GraalValue* GetFunctionTemplateData(unsigned id) {
        return function_template_data[id];
    }

    inline v8::FunctionCallback GetFunctionTemplateCallback(unsigned id) {
        return function_template_callbacks[id];
    }

    void SetFunctionTemplateFunction(unsigned id, GraalValue* function);
    void SetFunctionTemplateData(unsigned id, GraalValue* data);
    void SetFunctionTemplateCallback(unsigned id, v8::FunctionCallback callback);
    void ReportAPIFailure(const char* location, const char* message);

    inline void SetFatalErrorHandler(v8::FatalErrorCallback callback) {
        fatal_error_handler_ = callback;
    }

    inline void SaveReturnValue(double value) {
        return_value_ = value;
    }

    jobject CorrectReturnValue(GraalValue* value, jobject null_replacement);
    v8::ArrayBuffer::Allocator* GetArrayBufferAllocator();
    void SchedulePauseOnNextStatement();

    inline GraalObjectPool<GraalObject>* GetGraalObjectPool() {
        return object_pool_;
    }

    inline GraalObjectPool<GraalString>* GetGraalStringPool() {
        return string_pool_;
    }

    inline GraalObjectPool<GraalContext>* GetGraalContextPool() {
        return context_pool_;
    }

    inline GraalObjectPool<GraalFunction>* GetGraalFunctionPool() {
        return function_pool_;
    }

    inline GraalObjectPool<GraalArray>* GetGraalArrayPool() {
        return array_pool_;
    }

    inline GraalObjectPool<GraalNumber>* GetGraalNumberPool() {
        return number_pool_;
    }

    inline GraalObjectPool<GraalExternal>* GetGraalExternalPool() {
        return external_pool_;
    }

    inline v8::MicrotaskQueue* GetMicrotaskQueue() {
        return &microtask_queue_;
    }

    enum JSExecutionAction {
        kJSExecutionAllowed,
        kJSExecutionThrow,
        kJSExecutionCrash,
    };

    inline JSExecutionAction SetJSExecutionAction(JSExecutionAction action) {
        JSExecutionAction old = js_execution_action_;
        js_execution_action_ = action;
        return old;
    }

    inline bool CheckJSExecutionAllowed() {
        if (js_execution_action_ == kJSExecutionAllowed) {
            return true;
        } else {
            JSExecutionViolation(js_execution_action_);
            return false;
        }
    }

    void JSExecutionViolation(JSExecutionAction action);

    static void SetFlags(int argc, char** argv) {
        char** old_argv = GraalIsolate::argv;
        int old_argc = GraalIsolate::argc;
        int new_argc = argc + old_argc;
        GraalIsolate::argc = new_argc;
        GraalIsolate::argv = new char*[new_argc];
        memcpy(GraalIsolate::argv, old_argv, old_argc * sizeof (char*));
        for (int i = 0; i < argc; i++) {
            int len = strlen(argv[i]) + 1;
            GraalIsolate::argv[i + old_argc] = new char[len];
            strncpy(GraalIsolate::argv[i + old_argc], argv[i], len);
        }
        if (old_argv != nullptr) {
            delete[] old_argv;
        }
    }

    static void SetMode(int mode, bool polyglot) {
        if (mode != kModeDefault) {
            GraalIsolate::mode = mode;
        }
        if (polyglot) {
            GraalIsolate::polyglot = true;
        }
    }

    static void InitThreadLocals();
    static void SetEnv(const char * name, const char * value);
    static void UnsetEnv(const char * name);

    // Valid values of mode
    static const int kModeDefault = 0;
    static const int kModeNative = 1;
    static const int kModeJVM = 2;
private:
    // Slots accessed by v8::Isolate::Get/SetData
    // They must be the first field of GraalIsolate
    void* slot[v8::internal::Internals::kIsolateRootsOffset / v8::internal::kApiSystemPointerSize + v8::internal::Internals::kDoubleReturnValuePlaceholderIndex + 1] = {};
    std::vector<v8::Value*> eternals;
    std::vector<v8::Context*> contexts;
    std::vector<GraalHandleContent*> handles;
    std::vector<std::tuple<GCCallbackType, void*, void*>> prolog_callbacks;
    std::vector<std::tuple<GCCallbackType, void*, void*>> epilog_callbacks;
    std::vector<std::pair<v8::MicrotaskCallback, void*>> microtasks;
    std::vector<GraalValue*> function_template_data;
    std::vector<v8::FunctionCallback> function_template_callbacks;
    JavaVM* jvm_;
    JNIEnv* jni_env_;
    jclass object_class_;
    jclass access_class_;
    jobject access_;
    jobject boolean_true_;
    jobject boolean_false_;
    jobject int32_placeholder_;
    jobject uint32_placeholder_;
    jobject double_placeholder_;
    jmethodID jni_methods_[GraalAccessMethod::count];
    jfieldID jni_fields_[static_cast<int>(GraalAccessField::count)];
    GraalPrimitive* undefined_instance_;
    GraalPrimitive* null_instance_;
    GraalBoolean* true_instance_;
    GraalBoolean* false_instance_;
    static const int number_cache_low_ = -128;
    static const int number_cache_high_ = 127;
    GraalNumber* number_cache_[number_cache_high_ - number_cache_low_ + 1] = {};
    void* shared_buffer_;
    int shared_buffer_pos_;
    v8::MessageCallback message_listener_;
    bool sending_message_;
    v8::Isolate::AbortOnUncaughtExceptionCallback abort_on_uncaught_exception_callback_;
    std::shared_ptr<v8::ArrayBuffer::Allocator> array_buffer_allocator_;
    int try_catch_count_;
    int function_template_count_;
    bool stack_check_enabled_;
    intptr_t stack_bottom_;
    ptrdiff_t stack_size_limit_;
    bool main_;
    JSExecutionAction js_execution_action_ = kJSExecutionAllowed;
    double return_value_;
    static bool abort_on_uncaught_exception_;
    static bool internal_error_check_;
    static void EnsureValidWorkingDir();

    inline void SetJNIField(GraalAccessField id, jfieldID field) {
        jni_fields_[static_cast<int>(id)] = field;
    }

    void SetJNIField(GraalAccessField id, jobject holder_class, jobject field_name, const char* sig);
    void InitStackOverflowCheck(intptr_t stack_bottom);
    void RemoveCallback(std::vector<std::tuple<GCCallbackType, void*, void*>>&vector, void* callback);
    void SetTaskRunner(std::shared_ptr<v8::TaskRunner> task_runner);

    GraalNumber* CachedNumber(int value);
    friend class GraalNumber;

    jobject error_to_ignore_;
    int calls_on_stack_;
    std::shared_ptr<v8::TaskRunner> task_runner_;
    friend class GraalFunction;
    friend class v8::Isolate;

#ifdef __POSIX__
    pthread_mutex_t lock_;
#else
    void* lock_;
#endif
    v8::Locker* lock_owner_;
    friend class v8::Locker;
    friend class v8::Unlocker;

    static int argc;
    static char** argv;
    static int mode;
    static bool polyglot;
    static bool use_classpath_env_var;
    friend v8::V8;

    v8::PromiseHook promise_hook_;
    v8::PromiseRejectCallback promise_reject_callback_;
    v8::HostInitializeImportMetaObjectCallback import_meta_initializer;
    v8::HostImportModuleDynamicallyCallback import_module_dynamically;
    v8::FatalErrorCallback fatal_error_handler_;
    v8::PrepareStackTraceCallback prepare_stack_trace_callback_;
    v8::WasmStreamingCallback wasm_streaming_callback_;
    v8::internal::MicrotaskQueue microtask_queue_;

    GraalObjectPool<GraalObject>* object_pool_;
    GraalObjectPool<GraalString>* string_pool_;
    GraalObjectPool<GraalContext>* context_pool_;
    GraalObjectPool<GraalFunction>* function_pool_;
    GraalObjectPool<GraalArray>* array_pool_;
    GraalObjectPool<GraalNumber>* number_pool_;
    GraalObjectPool<GraalExternal>* external_pool_;
};

// This is a poor-man's check that attempts to avoid stack-overflow
// during invocation of an average native JavaScript function.
// It's main purpose is to avoid stack-overflow during JNI calls
// back to Graal.js engine, it does not handle possible large stack
// demands of the user-implemented parts of the native function.
// It is an experimental feature with a very naive implementation.
// It should be replaced by more sophisticated techniques if it
// turns out to be useful.
bool GraalIsolate::StackOverflowCheck(intptr_t stack_top) {
    if (labs(stack_top - stack_bottom_) > stack_size_limit_) {
        ThrowStackOverflowError();
        return true;
    }
    return false;
}

double GraalIsolate::ReadDoubleFromSharedBuffer() {
    double* result = (double*)((char*)shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof(double);
    return *result;
}

int32_t GraalIsolate::ReadInt32FromSharedBuffer() {
    int32_t* result = (int32_t*)((char*)shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof(int32_t);
    return *result;
}

int64_t GraalIsolate::ReadInt64FromSharedBuffer() {
    int64_t* result = (int64_t*)((char*)shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof(int64_t);
    return *result;
}

void GraalIsolate::WriteInt32ToSharedBuffer(int32_t number) {
    int32_t* result = (int32_t*) ((char*) shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof (int32_t);
    *result = number;
}

void GraalIsolate::WriteInt64ToSharedBuffer(int64_t number) {
    int64_t* result = (int64_t*) ((char*) shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof (int64_t);
    *result = number;
}

void GraalIsolate::WriteDoubleToSharedBuffer(double number) {
    double* result = (double*) ((char*) shared_buffer_ + shared_buffer_pos_);
    shared_buffer_pos_ += sizeof (double);
    *result = number;
}

#endif /* GRAAL_ISOLATE_H_ */
