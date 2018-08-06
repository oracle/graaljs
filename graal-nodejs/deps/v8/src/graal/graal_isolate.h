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

#ifndef GRAAL_ISOLATE_H_
#define GRAAL_ISOLATE_H_

#include "current_isolate.h"
#include "graal_handle_content.h"
#include "include/v8.h"
#include "jni.h"
#include <pthread.h>
#include <vector>

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

class GraalBoolean;
class GraalNumber;
class GraalPrimitive;
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
    value_equals,
    value_strict_equals,
    value_instance_of,
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
    object_define_property,
    array_new,
    array_length,
    array_buffer_new,
    array_buffer_new_buffer,
    array_buffer_get_contents,
    array_buffer_view_buffer,
    array_buffer_view_byte_length,
    array_buffer_view_byte_offset,
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
    data_view_new,
    external_new,
    integer_new,
    number_new,
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
    isolate_create_internal_field_count_key,
    isolate_create_internal_field_key,
    isolate_internal_error_check,
    isolate_throw_stack_overflow_error,
    isolate_get_heap_statistics,
    isolate_terminate_execution,
    isolate_cancel_terminate_execution,
    isolate_get_int_placeholder,
    isolate_get_large_int_placeholder,
    isolate_get_double_placeholder,
    isolate_dispose,
    isolate_enter_polyglot_engine,
    isolate_perform_gc,
    isolate_get_debug_context,
    isolate_enable_promise_hook,
    isolate_enable_promise_reject_callback,
    template_set,
    object_template_new,
    object_template_new_instance,
    object_template_set_accessor,
    object_template_set_named_property_handler,
    object_template_set_handler,
    object_template_set_call_as_function_handler,
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
    function_template_get_function_to_cache,
    function_template_has_instance,
    function_template_set_call_handler,
    function_template_inherit,
    script_compile,
    script_run,
    script_get_unbound_script,
    unbound_script_compile,
    unbound_script_bind_to_context,
    unbound_script_get_id,
    context_global,
    context_set_pointer_in_embedder_data,
    context_get_pointer_in_embedder_data,
    context_set_embedder_data,
    context_get_embedder_data,
    try_catch_exception,
    try_catch_has_terminated,
    message_get_script_resource_name,
    message_get_line_number,
    message_get_source_line,
    message_get_start_column,
    message_get_stack_trace,
    stack_trace_current_stack_trace,
    stack_frame_get_line_number,
    stack_frame_get_column,
    stack_frame_get_script_name,
    stack_frame_get_function_name,
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
    string_object_new,
    string_object_value_of,
    number_object_new,
    object_internal_field_count,
    object_slow_get_aligned_pointer_from_internal_field,
    object_set_aligned_pointer_in_internal_field,
    json_parse,
    json_stringify,
    symbol_new,
    promise_result,
    promise_state,
    promise_resolver_new,
    promise_resolver_resolve,
    promise_resolver_reject,
    module_compile,
    module_instantiate,
    module_evaluate,
    module_get_status,
    module_get_requests_length,
    module_get_request,
    module_get_namespace,
    module_get_identity_hash,
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

    count // Should be the last item of GraalAccessMethod
};

enum class GraalAccessField {
    array_buffer_byte_buffer,
    array_buffer_view_buffer,

    count // Should be the last item of GraalAccessField
};

class GraalIsolate {
public:
    GraalIsolate(JavaVM* jvm, JNIEnv* env);
    v8::Local<v8::Value> ThrowException(v8::Local<v8::Value> exception);
    bool AddMessageListener(v8::MessageCallback callback, v8::Local<v8::Value> data);
    void SendMessage(v8::Local<v8::Message> message, v8::Local<v8::Value> error, jthrowable java_error);
    void SetAbortOnUncaughtExceptionCallback(v8::Isolate::AbortOnUncaughtExceptionCallback callback);
    bool AbortOnUncaughtExceptionCallbackValue();
    v8::Local<v8::Value> InternalFieldKey(int index);
    void Dispose();
    void Dispose(bool exit, int status);
    double ReadDoubleFromSharedBuffer();
    int32_t ReadInt32FromSharedBuffer();
    int64_t ReadInt64FromSharedBuffer();
    void WriteInt32ToSharedBuffer(int32_t number);
    void WriteInt64ToSharedBuffer(int64_t number);
    void WriteDoubleToSharedBuffer(double number);
    void InternalErrorCheck();
    static v8::Isolate* New(v8::Isolate::CreateParams const& params);
    v8::Local<v8::Context> GetDebugContext();
    void SetPromiseHook(v8::PromiseHook promise_hook);
    void NotifyPromiseHook(v8::PromiseHookType, v8::Local<v8::Promise> promise, v8::Local<v8::Value> parent);
    void SetPromiseRejectCallback(v8::PromiseRejectCallback callback);
    void NotifyPromiseRejectCallback(v8::PromiseRejectMessage message);
    void EnqueueMicrotask(v8::MicrotaskCallback microtask, void* data);
    void RunMicrotasks();

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
        return contexts.back();
    }

    inline jobject CurrentJavaContext() {
        return reinterpret_cast<GraalHandleContent*> (contexts.back())->GetJavaObject();
    }

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

    inline v8::Local<v8::Value> InternalFieldCountKey() {
        return internal_field_count_key_;
    }

    inline void ResetSharedBuffer() {
        shared_buffer_pos_ = 0;
    }

    inline bool StackOverflowCheckEnabled() {
        return stack_check_enabled_;
    }

    bool StackOverflowCheck(long stack_top);

    void FindDynamicObjectFields(jobject context);

    inline int NextFunctionTemplateID() {
        return ++function_template_count_;
    }

    inline GraalValue* GetFunctionTemplateFunction(unsigned id) {
        GraalValue* result;
        if (function_template_functions.size() <= id) {
            result = nullptr;
        } else {
            result = function_template_functions[id];
        }
        if (result == nullptr) {
            result = CacheFunctionTemplateFunction(id);
        }
        return result;
    }

    inline GraalValue* GetFunctionTemplateData(unsigned id) {
        return function_template_data[id];
    }

    inline v8::FunctionCallback GetFunctionTemplateCallback(unsigned id) {
        return function_template_callbacks[id];
    }

    GraalValue* CacheFunctionTemplateFunction(unsigned id);
    void SetFunctionTemplateFunction(unsigned id, GraalValue* function);
    void SetFunctionTemplateData(unsigned id, GraalValue* data);
    void SetFunctionTemplateCallback(unsigned id, v8::FunctionCallback callback);

    inline void SaveReturnValue(double value) {
        return_value_ = value;
    }

    jobject CorrectReturnValue(GraalValue* value, jobject null_replacement);

    static void SetFlags(int argc, char** argv) {
        GraalIsolate::argc = argc;
        GraalIsolate::argv = argv;
    }

    static void SetMode(int mode) {
        GraalIsolate::mode = mode;
    }

    static void InitThreadLocals();

    // Valid values of mode
    static const int kModeDefault = 0;
    static const int kModeNative = 1;
    static const int kModeJVM = 2;
private:
    // Slots accessed by v8::Isolate::Get/SetData
    // They must be the first field of GraalIsolate
    void* slot[22] = {};
    std::vector<v8::Value*> eternals;
    std::vector<v8::Context*> contexts;
    std::vector<v8::Value*> internal_field_keys;
    std::vector<std::tuple<GCCallbackType, void*, void*>> prolog_callbacks;
    std::vector<std::tuple<GCCallbackType, void*, void*>> epilog_callbacks;
    std::vector<std::pair<v8::MicrotaskCallback, void*>> microtasks;
    std::vector<GraalValue*> function_template_functions;
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
    v8::Value* internal_field_count_key_;
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
    int try_catch_count_;
    int function_template_count_;
    bool stack_check_enabled_;
    long stack_bottom_;
    long stack_size_limit_;
    bool main_;
    double return_value_;
    static bool abort_on_uncaught_exception_;
    static bool internal_error_check_;
    static void EnsureValidWorkingDir();

    inline void SetJNIField(GraalAccessField id, jfieldID field) {
        jni_fields_[static_cast<int>(id)] = field;
    }

    void SetJNIField(GraalAccessField id, jobject holder_class, jobject field_name, const char* sig);
    void InitStackOverflowCheck(long stack_bottom);
    void RemoveCallback(std::vector<std::tuple<GCCallbackType, void*, void*>>&vector, void* callback);

    GraalNumber* CachedNumber(int value);
    friend class GraalNumber;

    jobject error_to_ignore_;
    int calls_on_stack_;
    friend class GraalFunction;
    friend class v8::Isolate;

    pthread_mutex_t lock_;
    v8::Locker* lock_owner_;
    friend class v8::Locker;
    friend class v8::Unlocker;

    static int argc;
    static char** argv;
    static int mode;
    static bool use_classpath_env_var;
    friend v8::V8;

    v8::PromiseHook promise_hook_;
    v8::PromiseRejectCallback promise_reject_callback_;
};

#endif /* GRAAL_ISOLATE_H_ */

