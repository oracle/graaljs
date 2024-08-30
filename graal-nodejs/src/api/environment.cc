#include "node.h"
#include "node_builtins.h"
#include "node_context_data.h"
#include "node_errors.h"
#include "node_internals.h"
#include "node_options-inl.h"
#include "node_platform.h"
#include "node_realm-inl.h"
#include "node_shadow_realm.h"
#include "node_v8_platform-inl.h"
#include "node_wasm_web_api.h"
#include "uv.h"
#ifdef NODE_ENABLE_VTUNE_PROFILING
#include "../deps/v8/src/third_party/vtune/v8-vtune.h"
#endif
#if HAVE_INSPECTOR
#include "inspector/worker_inspector.h"  // ParentInspectorHandle
#endif

namespace node {
using errors::TryCatchScope;
using v8::Array;
using v8::Boolean;
using v8::Context;
using v8::EscapableHandleScope;
using v8::Function;
using v8::FunctionCallbackInfo;
using v8::HandleScope;
using v8::Isolate;
using v8::Just;
using v8::Local;
using v8::Maybe;
using v8::MaybeLocal;
using v8::Nothing;
using v8::Null;
using v8::Object;
using v8::ObjectTemplate;
using v8::Private;
using v8::PropertyDescriptor;
using v8::SealHandleScope;
using v8::String;
using v8::Value;

bool AllowWasmCodeGenerationCallback(Local<Context> context,
                                     Local<String>) {
  Local<Value> wasm_code_gen =
      context->GetEmbedderData(ContextEmbedderIndex::kAllowWasmCodeGeneration);
  return wasm_code_gen->IsUndefined() || wasm_code_gen->IsTrue();
}

bool ShouldAbortOnUncaughtException(Isolate* isolate) {
  DebugSealHandleScope scope(isolate);
  Environment* env = Environment::GetCurrent(isolate);
  return env != nullptr &&
         (env->is_main_thread() || !env->is_stopping()) &&
         env->abort_on_uncaught_exception() &&
         env->should_abort_on_uncaught_toggle()[0] &&
         !env->inside_should_not_abort_on_uncaught_scope();
}

MaybeLocal<Value> PrepareStackTraceCallback(Local<Context> context,
                                            Local<Value> exception,
                                            Local<Array> trace) {
  Environment* env = Environment::GetCurrent(context);
  if (env == nullptr) {
    return exception->ToString(context).FromMaybe(Local<Value>());
  }
  Realm* realm = Realm::GetCurrent(context);
  Local<Function> prepare;
  if (realm != nullptr) {
    // If we are in a Realm, call the realm specific prepareStackTrace callback
    // to avoid passing the JS objects (the exception and trace) across the
    // realm boundary with the `Error.prepareStackTrace` override.
    prepare = realm->prepare_stack_trace_callback();
  } else {
    // The context is created with ContextifyContext, call the principal
    // realm's prepareStackTrace callback.
    prepare = env->principal_realm()->prepare_stack_trace_callback();
  }
  if (prepare.IsEmpty()) {
    return exception->ToString(context).FromMaybe(Local<Value>());
  }
  Local<Value> args[] = {
      context->Global(),
      exception,
      trace,
  };
  // This TryCatch + Rethrow is required by V8 due to details around exception
  // handling there. For C++ callbacks, V8 expects a scheduled exception (which
  // is what ReThrow gives us). Just returning the empty MaybeLocal would leave
  // us with a pending exception.
  TryCatchScope try_catch(env);
  MaybeLocal<Value> result = prepare->Call(
      context, Undefined(env->isolate()), arraysize(args), args);
  if (try_catch.HasCaught() && !try_catch.HasTerminated()) {
    try_catch.ReThrow();
  }
  return result;
}

void* NodeArrayBufferAllocator::Allocate(size_t size) {
  void* ret;
  if (zero_fill_field_ || per_process::cli_options->zero_fill_all_buffers)
    ret = allocator_->Allocate(size);
  else
    ret = allocator_->AllocateUninitialized(size);
  if (LIKELY(ret != nullptr))
    total_mem_usage_.fetch_add(size, std::memory_order_relaxed);
  return ret;
}

void* NodeArrayBufferAllocator::AllocateUninitialized(size_t size) {
  void* ret = allocator_->AllocateUninitialized(size);
  if (LIKELY(ret != nullptr))
    total_mem_usage_.fetch_add(size, std::memory_order_relaxed);
  return ret;
}

void* NodeArrayBufferAllocator::Reallocate(
    void* data, size_t old_size, size_t size) {
  void* ret = allocator_->Reallocate(data, old_size, size);
  if (LIKELY(ret != nullptr) || UNLIKELY(size == 0))
    total_mem_usage_.fetch_add(size - old_size, std::memory_order_relaxed);
  return ret;
}

void NodeArrayBufferAllocator::Free(void* data, size_t size) {
  total_mem_usage_.fetch_sub(size, std::memory_order_relaxed);
  allocator_->Free(data, size);
}

DebuggingArrayBufferAllocator::~DebuggingArrayBufferAllocator() {
  CHECK(allocations_.empty());
}

void* DebuggingArrayBufferAllocator::Allocate(size_t size) {
  Mutex::ScopedLock lock(mutex_);
  void* data = NodeArrayBufferAllocator::Allocate(size);
  RegisterPointerInternal(data, size);
  return data;
}

void* DebuggingArrayBufferAllocator::AllocateUninitialized(size_t size) {
  Mutex::ScopedLock lock(mutex_);
  void* data = NodeArrayBufferAllocator::AllocateUninitialized(size);
  RegisterPointerInternal(data, size);
  return data;
}

void DebuggingArrayBufferAllocator::Free(void* data, size_t size) {
  Mutex::ScopedLock lock(mutex_);
  UnregisterPointerInternal(data, size);
  NodeArrayBufferAllocator::Free(data, size);
}

void* DebuggingArrayBufferAllocator::Reallocate(void* data,
                                                size_t old_size,
                                                size_t size) {
  Mutex::ScopedLock lock(mutex_);
  void* ret = NodeArrayBufferAllocator::Reallocate(data, old_size, size);
  if (ret == nullptr) {
    if (size == 0) {  // i.e. equivalent to free().
      // suppress coverity warning as data is used as key versus as pointer
      // in UnregisterPointerInternal
      // coverity[pass_freed_arg]
      UnregisterPointerInternal(data, old_size);
    }
    return nullptr;
  }

  if (data != nullptr) {
    auto it = allocations_.find(data);
    CHECK_NE(it, allocations_.end());
    allocations_.erase(it);
  }

  RegisterPointerInternal(ret, size);
  return ret;
}

void DebuggingArrayBufferAllocator::RegisterPointer(void* data, size_t size) {
  Mutex::ScopedLock lock(mutex_);
  NodeArrayBufferAllocator::RegisterPointer(data, size);
  RegisterPointerInternal(data, size);
}

void DebuggingArrayBufferAllocator::UnregisterPointer(void* data, size_t size) {
  Mutex::ScopedLock lock(mutex_);
  NodeArrayBufferAllocator::UnregisterPointer(data, size);
  UnregisterPointerInternal(data, size);
}

void DebuggingArrayBufferAllocator::UnregisterPointerInternal(void* data,
                                                              size_t size) {
  if (data == nullptr) return;
  auto it = allocations_.find(data);
  CHECK_NE(it, allocations_.end());
  if (size > 0) {
    // We allow allocations with size 1 for 0-length buffers to avoid having
    // to deal with nullptr values.
    CHECK_EQ(it->second, size);
  }
  allocations_.erase(it);
}

void DebuggingArrayBufferAllocator::RegisterPointerInternal(void* data,
                                                            size_t size) {
  if (data == nullptr) return;
  CHECK_EQ(allocations_.count(data), 0);
  allocations_[data] = size;
}

std::unique_ptr<ArrayBufferAllocator> ArrayBufferAllocator::Create(bool debug) {
  if (debug || per_process::cli_options->debug_arraybuffer_allocations)
    return std::make_unique<DebuggingArrayBufferAllocator>();
  else
    return std::make_unique<NodeArrayBufferAllocator>();
}

ArrayBufferAllocator* CreateArrayBufferAllocator() {
  return ArrayBufferAllocator::Create().release();
}

void FreeArrayBufferAllocator(ArrayBufferAllocator* allocator) {
  delete allocator;
}

void SetIsolateCreateParamsForNode(Isolate::CreateParams* params) {
  const uint64_t constrained_memory = uv_get_constrained_memory();
  const uint64_t total_memory = constrained_memory > 0 ?
      std::min(uv_get_total_memory(), constrained_memory) :
      uv_get_total_memory();
  if (total_memory > 0 &&
      params->constraints.max_old_generation_size_in_bytes() == 0) {
    // V8 defaults to 700MB or 1.4GB on 32 and 64 bit platforms respectively.
    // This default is based on browser use-cases. Tell V8 to configure the
    // heap based on the actual physical memory.
    params->constraints.ConfigureDefaults(total_memory, 0);
  }
  params->embedder_wrapper_object_index = BaseObject::InternalFields::kSlot;
  params->embedder_wrapper_type_index = std::numeric_limits<int>::max();

#ifdef NODE_ENABLE_VTUNE_PROFILING
  params->code_event_handler = vTune::GetVtuneCodeEventHandler();
#endif
}

void SetIsolateErrorHandlers(v8::Isolate* isolate, const IsolateSettings& s) {
  if (s.flags & MESSAGE_LISTENER_WITH_ERROR_LEVEL)
    isolate->AddMessageListenerWithErrorLevel(
            errors::PerIsolateMessageListener,
            Isolate::MessageErrorLevel::kMessageError |
                Isolate::MessageErrorLevel::kMessageWarning);

  auto* abort_callback = s.should_abort_on_uncaught_exception_callback ?
      s.should_abort_on_uncaught_exception_callback :
      ShouldAbortOnUncaughtException;
  isolate->SetAbortOnUncaughtExceptionCallback(abort_callback);

  auto* fatal_error_cb = s.fatal_error_callback ?
      s.fatal_error_callback : OnFatalError;
  isolate->SetFatalErrorHandler(fatal_error_cb);
  isolate->SetOOMErrorHandler(OOMErrorHandler);

  if ((s.flags & SHOULD_NOT_SET_PREPARE_STACK_TRACE_CALLBACK) == 0) {
    auto* prepare_stack_trace_cb = s.prepare_stack_trace_callback ?
        s.prepare_stack_trace_callback : PrepareStackTraceCallback;
    isolate->SetPrepareStackTraceCallback(prepare_stack_trace_cb);
  }
}

void SetIsolateMiscHandlers(v8::Isolate* isolate, const IsolateSettings& s) {
  isolate->SetMicrotasksPolicy(s.policy);

  auto* allow_wasm_codegen_cb = s.allow_wasm_code_generation_callback ?
    s.allow_wasm_code_generation_callback : AllowWasmCodeGenerationCallback;
  isolate->SetAllowWasmCodeGenerationCallback(allow_wasm_codegen_cb);

  auto* modify_code_generation_from_strings_callback =
      ModifyCodeGenerationFromStrings;
  if (s.flags & ALLOW_MODIFY_CODE_GENERATION_FROM_STRINGS_CALLBACK &&
      s.modify_code_generation_from_strings_callback) {
    modify_code_generation_from_strings_callback =
        s.modify_code_generation_from_strings_callback;
  }
  isolate->SetModifyCodeGenerationFromStringsCallback(
      modify_code_generation_from_strings_callback);

  Mutex::ScopedLock lock(node::per_process::cli_options_mutex);
  if (per_process::cli_options->get_per_isolate_options()
          ->get_per_env_options()
          ->experimental_fetch) {
    isolate->SetWasmStreamingCallback(wasm_web_api::StartStreamingCompilation);
  }

  if (per_process::cli_options->get_per_isolate_options()
          ->experimental_shadow_realm) {
    isolate->SetHostCreateShadowRealmContextCallback(
        shadow_realm::HostCreateShadowRealmContextCallback);
  }

  if ((s.flags & SHOULD_NOT_SET_PROMISE_REJECTION_CALLBACK) == 0) {
    auto* promise_reject_cb = s.promise_reject_callback ?
      s.promise_reject_callback : PromiseRejectCallback;
    isolate->SetPromiseRejectCallback(promise_reject_cb);
  }

  if (s.flags & DETAILED_SOURCE_POSITIONS_FOR_PROFILING)
    v8::CpuProfiler::UseDetailedSourcePositionsForProfiling(isolate);
}

void SetIsolateUpForNode(v8::Isolate* isolate,
                         const IsolateSettings& settings) {
  SetIsolateErrorHandlers(isolate, settings);
  SetIsolateMiscHandlers(isolate, settings);
}

void SetIsolateUpForNode(v8::Isolate* isolate) {
  IsolateSettings settings;
  SetIsolateUpForNode(isolate, settings);
}

// TODO(joyeecheung): we may want to expose this, but then we need to be
// careful about what we override in the params.
Isolate* NewIsolate(Isolate::CreateParams* params,
                    uv_loop_t* event_loop,
                    MultiIsolatePlatform* platform,
                    bool has_snapshot_data) {
  Isolate* isolate = Isolate::Allocate();
  if (isolate == nullptr) return nullptr;
#ifdef NODE_V8_SHARED_RO_HEAP
  {
    // In shared-readonly-heap mode, V8 requires all snapshots used for
    // creating Isolates to be identical. This isn't really memory-safe
    // but also otherwise just doesn't work, and the only real alternative
    // is disabling shared-readonly-heap mode altogether.
    static Isolate::CreateParams first_params = *params;
    params->snapshot_blob = first_params.snapshot_blob;
    params->external_references = first_params.external_references;
  }
#endif

  // Register the isolate on the platform before the isolate gets initialized,
  // so that the isolate can access the platform during initialization.
  platform->RegisterIsolate(isolate, event_loop);

  SetIsolateCreateParamsForNode(params);
  Isolate::Initialize(isolate, *params);
  if (!has_snapshot_data) {
    // If in deserialize mode, delay until after the deserialization is
    // complete.
    SetIsolateUpForNode(isolate);
  } else {
    SetIsolateMiscHandlers(isolate, {});
  }

  return isolate;
}

Isolate* NewIsolate(ArrayBufferAllocator* allocator,
                    uv_loop_t* event_loop,
                    MultiIsolatePlatform* platform) {
  Isolate::CreateParams params;
  if (allocator != nullptr) params.array_buffer_allocator = allocator;
  return NewIsolate(&params, event_loop, platform);
}

Isolate* NewIsolate(std::shared_ptr<ArrayBufferAllocator> allocator,
                    uv_loop_t* event_loop,
                    MultiIsolatePlatform* platform) {
  Isolate::CreateParams params;
  if (allocator) params.array_buffer_allocator_shared = allocator;
  return NewIsolate(&params, event_loop, platform);
}

IsolateData* CreateIsolateData(Isolate* isolate,
                               uv_loop_t* loop,
                               MultiIsolatePlatform* platform,
                               ArrayBufferAllocator* allocator) {
  return new IsolateData(isolate, loop, platform, allocator);
}

void FreeIsolateData(IsolateData* isolate_data) {
  delete isolate_data;
}

InspectorParentHandle::~InspectorParentHandle() {}

// Hide the internal handle class from the public API.
#if HAVE_INSPECTOR
struct InspectorParentHandleImpl : public InspectorParentHandle {
  std::unique_ptr<inspector::ParentInspectorHandle> impl;

  explicit InspectorParentHandleImpl(
      std::unique_ptr<inspector::ParentInspectorHandle>&& impl)
    : impl(std::move(impl)) {}
};
#endif

Environment* CreateEnvironment(
    IsolateData* isolate_data,
    Local<Context> context,
    const std::vector<std::string>& args,
    const std::vector<std::string>& exec_args,
    EnvironmentFlags::Flags flags,
    ThreadId thread_id,
    std::unique_ptr<InspectorParentHandle> inspector_parent_handle) {
  Isolate* isolate = context->GetIsolate();
  HandleScope handle_scope(isolate);
  Context::Scope context_scope(context);
  // TODO(addaleax): This is a much better place for parsing per-Environment
  // options than the global parse call.
  Environment* env = new Environment(
      isolate_data, context, args, exec_args, nullptr, flags, thread_id);

#if HAVE_INSPECTOR
  if (env->should_create_inspector()) {
    if (inspector_parent_handle) {
      env->InitializeInspector(std::move(
          static_cast<InspectorParentHandleImpl*>(inspector_parent_handle.get())
              ->impl));
    } else {
      env->InitializeInspector({});
    }
  }
#endif
  if (env->options()->debug_options().break_node_first_line) {
    isolate->SchedulePauseOnNextStatement();
  }

  if (env->principal_realm()->RunBootstrapping().IsEmpty()) {
    FreeEnvironment(env);
    return nullptr;
  }

  return env;
}

void FreeEnvironment(Environment* env) {
  Isolate* isolate = env->isolate();
  Isolate::DisallowJavascriptExecutionScope disallow_js(isolate,
      Isolate::DisallowJavascriptExecutionScope::THROW_ON_FAILURE);
  {
    HandleScope handle_scope(isolate);  // For env->context().
    Context::Scope context_scope(env->context());
    SealHandleScope seal_handle_scope(isolate);

    // Set the flag in accordance with the DisallowJavascriptExecutionScope
    // above.
    env->set_can_call_into_js(false);
    env->set_stopping(true);
    env->stop_sub_worker_contexts();
    env->RunCleanup();
    RunAtExit(env);
  }

  delete env;
}

NODE_EXTERN std::unique_ptr<InspectorParentHandle> GetInspectorParentHandle(
    Environment* env,
    ThreadId thread_id,
    const char* url) {
  return GetInspectorParentHandle(env, thread_id, url, "");
}

NODE_EXTERN std::unique_ptr<InspectorParentHandle> GetInspectorParentHandle(
    Environment* env, ThreadId thread_id, const char* url, const char* name) {
  CHECK_NOT_NULL(env);
  if (name == nullptr) name = "";
  CHECK_NE(thread_id.id, static_cast<uint64_t>(-1));
  if (!env->should_create_inspector()) {
    return nullptr;
  }
#if HAVE_INSPECTOR
  return std::make_unique<InspectorParentHandleImpl>(
      env->inspector_agent()->GetParentHandle(thread_id.id, url, name));
#else
  return {};
#endif
}

MaybeLocal<Value> LoadEnvironment(
    Environment* env,
    StartExecutionCallback cb) {
  env->InitializeLibuv();
  env->InitializeDiagnostics();

  return StartExecution(env, cb);
}

MaybeLocal<Value> LoadEnvironment(
    Environment* env,
    const char* main_script_source_utf8) {
  CHECK_NOT_NULL(main_script_source_utf8);
  return LoadEnvironment(
      env, [&](const StartExecutionCallbackInfo& info) -> MaybeLocal<Value> {
        std::string name = "embedder_main_" + std::to_string(env->thread_id());
        env->builtin_loader()->Add(name.c_str(), main_script_source_utf8);
        Realm* realm = env->principal_realm();

        return realm->ExecuteBootstrapper(name.c_str());
      });
}

Environment* GetCurrentEnvironment(Local<Context> context) {
  return Environment::GetCurrent(context);
}

IsolateData* GetEnvironmentIsolateData(Environment* env) {
  return env->isolate_data();
}

ArrayBufferAllocator* GetArrayBufferAllocator(IsolateData* isolate_data) {
  return isolate_data->node_allocator();
}

MultiIsolatePlatform* GetMultiIsolatePlatform(Environment* env) {
  return GetMultiIsolatePlatform(env->isolate_data());
}

MultiIsolatePlatform* GetMultiIsolatePlatform(IsolateData* env) {
  return env->platform();
}

MultiIsolatePlatform* CreatePlatform(
    int thread_pool_size,
    node::tracing::TracingController* tracing_controller) {
  return CreatePlatform(
      thread_pool_size,
      static_cast<v8::TracingController*>(tracing_controller));
}

MultiIsolatePlatform* CreatePlatform(
    int thread_pool_size,
    v8::TracingController* tracing_controller) {
  return MultiIsolatePlatform::Create(thread_pool_size,
                                      tracing_controller)
      .release();
}

void FreePlatform(MultiIsolatePlatform* platform) {
  delete platform;
}

std::unique_ptr<MultiIsolatePlatform> MultiIsolatePlatform::Create(
    int thread_pool_size,
    v8::TracingController* tracing_controller,
    v8::PageAllocator* page_allocator) {
  return std::make_unique<NodePlatform>(thread_pool_size,
                                        tracing_controller,
                                        page_allocator);
}

MaybeLocal<Object> GetPerContextExports(Local<Context> context) {
  Isolate* isolate = context->GetIsolate();
  EscapableHandleScope handle_scope(isolate);

  Local<Object> global = context->Global();
  Local<Private> key = Private::ForApi(isolate,
      FIXED_ONE_BYTE_STRING(isolate, "node:per_context_binding_exports"));

  Local<Value> existing_value;
  if (!global->GetPrivate(context, key).ToLocal(&existing_value))
    return MaybeLocal<Object>();
  if (existing_value->IsObject())
    return handle_scope.Escape(existing_value.As<Object>());

  Local<Object> exports = Object::New(isolate);
  if (context->Global()->SetPrivate(context, key, exports).IsNothing() ||
      InitializePrimordials(context).IsNothing())
    return MaybeLocal<Object>();
  return handle_scope.Escape(exports);
}

// Any initialization logic should be performed in
// InitializeContext, because embedders don't necessarily
// call NewContext and so they will experience breakages.
Local<Context> NewContext(Isolate* isolate,
                          Local<ObjectTemplate> object_template) {
  auto context = Context::New(isolate, nullptr, object_template);
  if (context.IsEmpty()) return context;

  if (InitializeContext(context).IsNothing()) {
    return Local<Context>();
  }

  return context;
}

void ProtoThrower(const FunctionCallbackInfo<Value>& info) {
  THROW_ERR_PROTO_ACCESS(info.GetIsolate());
}

// This runs at runtime, regardless of whether the context
// is created from a snapshot.
Maybe<bool> InitializeContextRuntime(Local<Context> context) {
  Isolate* isolate = context->GetIsolate();
  HandleScope handle_scope(isolate);

  // When `IsCodeGenerationFromStringsAllowed` is true, V8 takes the fast path
  // and ignores the ModifyCodeGenerationFromStrings callback. Set it to false
  // to delegate the code generation validation to
  // node::ModifyCodeGenerationFromStrings.
  // The `IsCodeGenerationFromStringsAllowed` can be refreshed by V8 according
  // to the runtime flags, propagate the value to the embedder data.
  bool is_code_generation_from_strings_allowed =
      context->IsCodeGenerationFromStringsAllowed();
  context->AllowCodeGenerationFromStrings(false);
  context->SetEmbedderData(
      ContextEmbedderIndex::kAllowCodeGenerationFromStrings,
      Boolean::New(isolate, is_code_generation_from_strings_allowed));

  if (per_process::cli_options->disable_proto == "") {
    return Just(true);
  }

  // Remove __proto__
  // https://github.com/nodejs/node/issues/31951
  Local<Object> prototype;
  {
    Local<String> object_string =
      FIXED_ONE_BYTE_STRING(isolate, "Object");
    Local<String> prototype_string =
      FIXED_ONE_BYTE_STRING(isolate, "prototype");

    Local<Value> object_v;
    if (!context->Global()
        ->Get(context, object_string)
        .ToLocal(&object_v)) {
      return Nothing<bool>();
    }

    Local<Value> prototype_v;
    if (!object_v.As<Object>()
        ->Get(context, prototype_string)
        .ToLocal(&prototype_v)) {
      return Nothing<bool>();
    }

    prototype = prototype_v.As<Object>();
  }

  Local<String> proto_string =
    FIXED_ONE_BYTE_STRING(isolate, "__proto__");

  if (per_process::cli_options->disable_proto == "delete") {
    if (prototype
        ->Delete(context, proto_string)
        .IsNothing()) {
      return Nothing<bool>();
    }
  } else if (per_process::cli_options->disable_proto == "throw") {
    Local<Value> thrower;
    if (!Function::New(context, ProtoThrower)
        .ToLocal(&thrower)) {
      return Nothing<bool>();
    }

    PropertyDescriptor descriptor(thrower, thrower);
    descriptor.set_enumerable(false);
    descriptor.set_configurable(true);
    if (prototype
        ->DefineProperty(context, proto_string, descriptor)
        .IsNothing()) {
      return Nothing<bool>();
    }
  } else if (per_process::cli_options->disable_proto != "") {
    // Validated in ProcessGlobalArgs
    OnFatalError("InitializeContextRuntime()", "invalid --disable-proto mode");
  }

  return Just(true);
}

Maybe<bool> InitializeBaseContextForSnapshot(Local<Context> context) {
  Isolate* isolate = context->GetIsolate();
  HandleScope handle_scope(isolate);

  // Delete `Intl.v8BreakIterator`
  // https://github.com/nodejs/node/issues/14909
  {
    Context::Scope context_scope(context);
    Local<String> intl_string = FIXED_ONE_BYTE_STRING(isolate, "Intl");
    Local<String> break_iter_string =
        FIXED_ONE_BYTE_STRING(isolate, "v8BreakIterator");

    Local<Value> intl_v;
    if (!context->Global()->Get(context, intl_string).ToLocal(&intl_v)) {
      return Nothing<bool>();
    }

    if (intl_v->IsObject() &&
        intl_v.As<Object>()->Delete(context, break_iter_string).IsNothing()) {
      return Nothing<bool>();
    }
  }
  return Just(true);
}

Maybe<bool> InitializeMainContextForSnapshot(Local<Context> context) {
  Isolate* isolate = context->GetIsolate();
  HandleScope handle_scope(isolate);

  // Initialize the default values.
  context->SetEmbedderData(ContextEmbedderIndex::kAllowWasmCodeGeneration,
                           True(isolate));
  context->SetEmbedderData(
      ContextEmbedderIndex::kAllowCodeGenerationFromStrings, True(isolate));

  if (InitializeBaseContextForSnapshot(context).IsNothing()) {
    return Nothing<bool>();
  }
  return InitializePrimordials(context);
}

Maybe<bool> InitializePrimordials(Local<Context> context) {
  // Run per-context JS files.
  Isolate* isolate = context->GetIsolate();
  Context::Scope context_scope(context);
  Local<Object> exports;

  Local<String> primordials_string =
      FIXED_ONE_BYTE_STRING(isolate, "primordials");

  // Create primordials first and make it available to per-context scripts.
  Local<Object> primordials = Object::New(isolate);
  if (primordials->SetPrototype(context, Null(isolate)).IsNothing() ||
      !GetPerContextExports(context).ToLocal(&exports) ||
      exports->Set(context, primordials_string, primordials).IsNothing()) {
    return Nothing<bool>();
  }

  static const char* context_files[] = {"internal/per_context/primordials",
                                        "internal/per_context/domexception",
                                        "internal/per_context/messageport",
                                        nullptr};

  // We do not have access to a per-Environment BuiltinLoader instance
  // at this point, because this code runs before an Environment exists
  // in the first place. However, creating BuiltinLoader instances is
  // relatively cheap and all the scripts that we may want to run at
  // startup are always present in it.
  thread_local builtins::BuiltinLoader builtin_loader;
  for (const char** module = context_files; *module != nullptr; module++) {
    Local<Value> arguments[] = {exports, primordials};
    if (builtin_loader
            .CompileAndCall(
                context, *module, arraysize(arguments), arguments, nullptr)
            .IsEmpty()) {
      // Execution failed during context creation.
      return Nothing<bool>();
    }
  }

  return Just(true);
}

// This initializes the main context (i.e. vm contexts are not included).
Maybe<bool> InitializeContext(Local<Context> context) {
  if (InitializeMainContextForSnapshot(context).IsNothing()) {
    return Nothing<bool>();
  }

  return InitializeContextRuntime(context);
}

uv_loop_t* GetCurrentEventLoop(Isolate* isolate) {
  HandleScope handle_scope(isolate);
  Local<Context> context = isolate->GetCurrentContext();
  if (context.IsEmpty()) return nullptr;
  Environment* env = Environment::GetCurrent(context);
  if (env == nullptr) return nullptr;
  return env->event_loop();
}

void AddLinkedBinding(Environment* env, const node_module& mod) {
  CHECK_NOT_NULL(env);
  Mutex::ScopedLock lock(env->extra_linked_bindings_mutex());

  node_module* prev_tail = env->extra_linked_bindings_tail();
  env->extra_linked_bindings()->push_back(mod);
  if (prev_tail != nullptr)
    prev_tail->nm_link = &env->extra_linked_bindings()->back();
}

void AddLinkedBinding(Environment* env, const napi_module& mod) {
  node_module node_mod = napi_module_to_node_module(&mod);
  node_mod.nm_flags = NM_F_LINKED;
  AddLinkedBinding(env, node_mod);
}

void AddLinkedBinding(Environment* env,
                      const char* name,
                      addon_context_register_func fn,
                      void* priv) {
  node_module mod = {
    NODE_MODULE_VERSION,
    NM_F_LINKED,
    nullptr,  // nm_dso_handle
    nullptr,  // nm_filename
    nullptr,  // nm_register_func
    fn,
    name,
    priv,
    nullptr   // nm_link
  };
  AddLinkedBinding(env, mod);
}

void AddLinkedBinding(Environment* env,
                      const char* name,
                      napi_addon_register_func fn,
                      int32_t module_api_version) {
  node_module mod = {
      -1,           // nm_version for Node-API
      NM_F_LINKED,  // nm_flags
      nullptr,      // nm_dso_handle
      nullptr,      // nm_filename
      nullptr,      // nm_register_func
      get_node_api_context_register_func(env, name, module_api_version),
      name,                         // nm_modname
      reinterpret_cast<void*>(fn),  // nm_priv
      nullptr                       // nm_link
  };
  AddLinkedBinding(env, mod);
}

static std::atomic<uint64_t> next_thread_id{0};

ThreadId AllocateEnvironmentThreadId() {
  return ThreadId { next_thread_id++ };
}

void DefaultProcessExitHandler(Environment* env, int exit_code) {
  env->set_stopping(true);
  env->set_can_call_into_js(false);
  env->stop_sub_worker_contexts();
  env->isolate()->DumpAndResetStats();
  // The tracing agent could be in the process of writing data using the
  // threadpool. Stop it before shutting down libuv. The rest of the tracing
  // agent disposal will be performed in DisposePlatform().
  per_process::v8_platform.StopTracingAgent();
  // When the process exits, the tasks in the thread pool may also need to
  // access the data of V8Platform, such as trace agent, or a field
  // added in the future. So make sure the thread pool exits first.
  // And make sure V8Platform don not call into Libuv threadpool, see Dispose
  // in node_v8_platform-inl.h
  uv_library_shutdown();
  DisposePlatform();
  exit(exit_code);
}


void SetProcessExitHandler(Environment* env,
                           std::function<void(Environment*, int)>&& handler) {
  env->set_process_exit_handler(std::move(handler));
}

}  // namespace node
