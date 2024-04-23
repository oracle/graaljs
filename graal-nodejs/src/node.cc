// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

#include "node.h"

// ========== local headers ==========

#include "debug_utils-inl.h"
#include "env-inl.h"
#include "histogram-inl.h"
#include "memory_tracker-inl.h"
#include "node_binding.h"
#include "node_builtins.h"
#include "node_errors.h"
#include "node_internals.h"
#include "node_main_instance.h"
#include "node_metadata.h"
#include "node_options-inl.h"
#include "node_perf.h"
#include "node_process-inl.h"
#include "node_realm-inl.h"
#include "node_report.h"
#include "node_revert.h"
#include "node_sea.h"
#include "node_snapshot_builder.h"
#include "node_v8_platform-inl.h"
#include "node_version.h"

#if HAVE_OPENSSL
#include "node_crypto.h"
#endif

#if defined(NODE_HAVE_I18N_SUPPORT)
#include "node_i18n.h"
#endif

#if HAVE_INSPECTOR
#include "inspector_agent.h"
#include "inspector_io.h"
#endif

#if defined HAVE_DTRACE || defined HAVE_ETW
#include "node_dtrace.h"
#endif

#if NODE_USE_V8_PLATFORM
#include "libplatform/libplatform.h"
#endif  // NODE_USE_V8_PLATFORM
#include "v8-profiler.h"

#if HAVE_INSPECTOR
#include "inspector/worker_inspector.h"  // ParentInspectorHandle
#endif

#ifdef NODE_ENABLE_VTUNE_PROFILING
#include "../deps/v8/src/third_party/vtune/v8-vtune.h"
#endif

#include "large_pages/node_large_page.h"

#if defined(__APPLE__) || defined(__linux__) || defined(_WIN32)
#define NODE_USE_V8_WASM_TRAP_HANDLER 1
#else
#define NODE_USE_V8_WASM_TRAP_HANDLER 0
#endif

#if NODE_USE_V8_WASM_TRAP_HANDLER
#if defined(_WIN32)
#include "v8-wasm-trap-handler-win.h"
#else
#include <atomic>
#include "v8-wasm-trap-handler-posix.h"
#endif
#endif  // NODE_USE_V8_WASM_TRAP_HANDLER

// ========== global C headers ==========

#include <fcntl.h>  // _O_RDWR
#include <sys/types.h>

#if defined(NODE_HAVE_I18N_SUPPORT)
#include <unicode/uvernum.h>
#include <unicode/utypes.h>
#endif


#if defined(LEAK_SANITIZER)
#include <sanitizer/lsan_interface.h>
#endif

#if defined(_MSC_VER)
#include <direct.h>
#include <io.h>
#define STDIN_FILENO 0
#else
#include <pthread.h>
#include <sys/resource.h>  // getrlimit, setrlimit
#include <termios.h>       // tcgetattr, tcsetattr
#include <unistd.h>        // STDIN_FILENO, STDERR_FILENO
#endif

// ========== global C++ headers ==========

#include <cerrno>
#include <climits>  // PATH_MAX
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <string>
#include <tuple>
#include <vector>

namespace node {

using v8::EscapableHandleScope;
using v8::Isolate;
using v8::Local;
using v8::MaybeLocal;
using v8::Object;
using v8::V8;
using v8::Value;

namespace per_process {

// node_revert.h
// Bit flag used to track security reverts.
unsigned int reverted_cve = 0;

// util.h
// Tells whether the per-process V8::Initialize() is called and
// if it is safe to call v8::Isolate::TryGetCurrent().
bool v8_initialized = false;

// node_internals.h
// process-relative uptime base in nanoseconds, initialized in node::Start()
uint64_t node_start_time;

#if NODE_USE_V8_WASM_TRAP_HANDLER && defined(_WIN32)
PVOID old_vectored_exception_handler;
#endif

// node_v8_platform-inl.h
struct V8Platform v8_platform;
}  // namespace per_process

// The section in the OpenSSL configuration file to be loaded.
const char* conf_section_name = STRINGIFY(NODE_OPENSSL_CONF_NAME);

#ifdef __POSIX__
void SignalExit(int signo, siginfo_t* info, void* ucontext) {
  ResetStdio();
  raise(signo);
}
#endif  // __POSIX__

#if HAVE_INSPECTOR
int Environment::InitializeInspector(
    std::unique_ptr<inspector::ParentInspectorHandle> parent_handle) {
  std::string inspector_path;
  bool is_main = !parent_handle;
  if (parent_handle) {
    inspector_path = parent_handle->url();
    inspector_agent_->SetParentHandle(std::move(parent_handle));
  } else {
    inspector_path = argv_.size() > 1 ? argv_[1].c_str() : "";
  }

  CHECK(!inspector_agent_->IsListening());
  // Inspector agent can't fail to start, but if it was configured to listen
  // right away on the websocket port and fails to bind/etc, this will return
  // false.
  inspector_agent_->Start(inspector_path,
                          options_->debug_options(),
                          inspector_host_port(),
                          is_main);
  if (options_->debug_options().inspector_enabled &&
      !inspector_agent_->IsListening()) {
    return 12;  // Signal internal error
  }

  profiler::StartProfilers(this);

  if (inspector_agent_->options().break_node_first_line) {
    inspector_agent_->PauseOnNextJavascriptStatement("Break at bootstrap");
  }

  return 0;
}
#endif  // HAVE_INSPECTOR

#define ATOMIC_WAIT_EVENTS(V)                                               \
  V(kStartWait,           "started")                                        \
  V(kWokenUp,             "was woken up by another thread")                 \
  V(kTimedOut,            "timed out")                                      \
  V(kTerminatedExecution, "was stopped by terminated execution")            \
  V(kAPIStopped,          "was stopped through the embedder API")           \
  V(kNotEqual,            "did not wait because the values mismatched")     \

static void AtomicsWaitCallback(Isolate::AtomicsWaitEvent event,
                                Local<v8::SharedArrayBuffer> array_buffer,
                                size_t offset_in_bytes, int64_t value,
                                double timeout_in_ms,
                                Isolate::AtomicsWaitWakeHandle* stop_handle,
                                void* data) {
  Environment* env = static_cast<Environment*>(data);

  const char* message = "(unknown event)";
  switch (event) {
#define V(key, msg)                         \
    case Isolate::AtomicsWaitEvent::key:    \
      message = msg;                        \
      break;
    ATOMIC_WAIT_EVENTS(V)
#undef V
  }

  fprintf(stderr,
          "(node:%d) [Thread %" PRIu64 "] Atomics.wait(%p + %zx, %" PRId64
          ", %.f) %s\n",
          static_cast<int>(uv_os_getpid()),
          env->thread_id(),
          array_buffer->Data(),
          offset_in_bytes,
          value,
          timeout_in_ms,
          message);
}

void Environment::InitializeDiagnostics() {
  isolate_->GetHeapProfiler()->AddBuildEmbedderGraphCallback(
      Environment::BuildEmbedderGraph, this);
  if (heap_snapshot_near_heap_limit_ > 0) {
    AddHeapSnapshotNearHeapLimitCallback();
  }
  if (options_->trace_uncaught)
    isolate_->SetCaptureStackTraceForUncaughtExceptions(true);
  if (options_->trace_atomics_wait) {
    isolate_->SetAtomicsWaitCallback(AtomicsWaitCallback, this);
    AddCleanupHook([](void* data) {
      Environment* env = static_cast<Environment*>(data);
      env->isolate()->SetAtomicsWaitCallback(nullptr, nullptr);
    }, this);
  }

#if defined HAVE_DTRACE || defined HAVE_ETW
  InitDTrace(this);
#endif
}

static
MaybeLocal<Value> StartExecution(Environment* env, const char* main_script_id) {
  EscapableHandleScope scope(env->isolate());
  CHECK_NOT_NULL(main_script_id);
  Realm* realm = env->principal_realm();

  return scope.EscapeMaybe(realm->ExecuteBootstrapper(main_script_id));
}

MaybeLocal<Value> StartExecution(Environment* env, StartExecutionCallback cb) {
  InternalCallbackScope callback_scope(
      env,
      Object::New(env->isolate()),
      { 1, 0 },
      InternalCallbackScope::kSkipAsyncHooks);

  if (cb != nullptr) {
    EscapableHandleScope scope(env->isolate());

    if (StartExecution(env, "internal/main/environment").IsEmpty()) return {};

    StartExecutionCallbackInfo info = {
        env->process_object(),
        env->builtin_module_require(),
    };

    return scope.EscapeMaybe(cb(info));
  }

  // TODO(joyeecheung): move these conditions into JS land and let the
  // deserialize main function take precedence. For workers, we need to
  // move the pre-execution part into a different file that can be
  // reused when dealing with user-defined main functions.
  if (!env->snapshot_deserialize_main().IsEmpty()) {
    return env->RunSnapshotDeserializeMain();
  }

  if (env->worker_context() != nullptr) {
    return StartExecution(env, "internal/main/worker_thread");
  }

  std::string first_argv;
  if (env->argv().size() > 1) {
    first_argv = env->argv()[1];
  }

#ifndef DISABLE_SINGLE_EXECUTABLE_APPLICATION
  if (sea::IsSingleExecutable()) {
    // TODO(addaleax): Find a way to reuse:
    //
    // LoadEnvironment(Environment*, const char*)
    //
    // instead and not add yet another main entry point here because this
    // already duplicates existing code.
    return StartExecution(env, "internal/main/single_executable_application");
  }
#endif

  if (first_argv == "inspect") {
    return StartExecution(env, "internal/main/inspect");
  }

  if (per_process::cli_options->build_snapshot) {
    return StartExecution(env, "internal/main/mksnapshot");
  }

  if (per_process::cli_options->print_help) {
    return StartExecution(env, "internal/main/print_help");
  }


  if (env->options()->prof_process) {
    return StartExecution(env, "internal/main/prof_process");
  }

  // -e/--eval without -i/--interactive
  if (env->options()->has_eval_string && !env->options()->force_repl) {
    return StartExecution(env, "internal/main/eval_string");
  }

  if (env->options()->syntax_check_only) {
    return StartExecution(env, "internal/main/check_syntax");
  }

  if (env->options()->test_runner) {
    return StartExecution(env, "internal/main/test_runner");
  }

  if (env->options()->watch_mode) {
    return StartExecution(env, "internal/main/watch_mode");
  }

  if (!first_argv.empty() && first_argv != "-") {
    return StartExecution(env, "internal/main/run_main_module");
  }

  if (env->options()->force_repl || uv_guess_handle(STDIN_FILENO) == UV_TTY) {
    return StartExecution(env, "internal/main/repl");
  }

  return StartExecution(env, "internal/main/eval_stdin");
}

#ifdef __POSIX__
typedef void (*sigaction_cb)(int signo, siginfo_t* info, void* ucontext);
#endif
#if NODE_USE_V8_WASM_TRAP_HANDLER
#if defined(_WIN32)
static LONG TrapWebAssemblyOrContinue(EXCEPTION_POINTERS* exception) {
  if (v8::TryHandleWebAssemblyTrapWindows(exception)) {
    return EXCEPTION_CONTINUE_EXECUTION;
  }
  return EXCEPTION_CONTINUE_SEARCH;
}
#else
static std::atomic<sigaction_cb> previous_sigsegv_action;
// TODO(align behavior between macos and other in next major version)
#if defined(__APPLE__)
static std::atomic<sigaction_cb> previous_sigbus_action;
#endif  // __APPLE__

void TrapWebAssemblyOrContinue(int signo, siginfo_t* info, void* ucontext) {
  if (!v8::TryHandleWebAssemblyTrapPosix(signo, info, ucontext)) {
#if defined(__APPLE__)
    sigaction_cb prev = signo == SIGBUS ? previous_sigbus_action.load()
                                        : previous_sigsegv_action.load();
#else
    sigaction_cb prev = previous_sigsegv_action.load();
#endif  // __APPLE__
    if (prev != nullptr) {
      prev(signo, info, ucontext);
    } else {
      // Reset to the default signal handler, i.e. cause a hard crash.
      struct sigaction sa;
      memset(&sa, 0, sizeof(sa));
      sa.sa_handler = SIG_DFL;
      CHECK_EQ(sigaction(signo, &sa, nullptr), 0);

      ResetStdio();
      raise(signo);
    }
  }
}
#endif  // defined(_WIN32)
#endif  // NODE_USE_V8_WASM_TRAP_HANDLER

#ifdef __POSIX__
void RegisterSignalHandler(int signal,
                           sigaction_cb handler,
                           bool reset_handler) {
  CHECK_NOT_NULL(handler);
#if NODE_USE_V8_WASM_TRAP_HANDLER
  if (signal == SIGSEGV) {
    CHECK(previous_sigsegv_action.is_lock_free());
    CHECK(!reset_handler);
    previous_sigsegv_action.store(handler);
    return;
  }
// TODO(align behavior between macos and other in next major version)
#if defined(__APPLE__)
  if (signal == SIGBUS) {
    CHECK(previous_sigbus_action.is_lock_free());
    CHECK(!reset_handler);
    previous_sigbus_action.store(handler);
    return;
  }
#endif  // __APPLE__
#endif  // NODE_USE_V8_WASM_TRAP_HANDLER
  struct sigaction sa;
  memset(&sa, 0, sizeof(sa));
  sa.sa_sigaction = handler;
  sa.sa_flags = reset_handler ? SA_RESETHAND : 0;
  sigfillset(&sa.sa_mask);
  CHECK_EQ(sigaction(signal, &sa, nullptr), 0);
}
#endif  // __POSIX__

#ifdef __POSIX__
static struct {
  int flags;
  bool isatty;
  struct stat stat;
  struct termios termios;
} stdio[1 + STDERR_FILENO];
#endif  // __POSIX__

void ResetSignalHandlers() {
#ifdef __POSIX__
  // Restore signal dispositions, the parent process may have changed them.
  struct sigaction act;
  memset(&act, 0, sizeof(act));

  // The hard-coded upper limit is because NSIG is not very reliable; on Linux,
  // it evaluates to 32, 34 or 64, depending on whether RT signals are enabled.
  // Counting up to SIGRTMIN doesn't work for the same reason.
  for (unsigned nr = 1; nr < kMaxSignal; nr += 1) {
    if (nr == SIGKILL || nr == SIGSTOP)
      continue;
    act.sa_handler = (nr == SIGPIPE || nr == SIGXFSZ) ? SIG_IGN : SIG_DFL;
    if (act.sa_handler == SIG_DFL) {
      // The only bad handler value we can inhert from before exec is SIG_IGN
      // (any actual function pointer is reset to SIG_DFL during exec).
      // If that's the case, we want to reset it back to SIG_DFL.
      // However, it's also possible that an embeder (or an LD_PRELOAD-ed
      // library) has set up own signal handler for own purposes
      // (e.g. profiling). If that's the case, we want to keep it intact.
      struct sigaction old;
      CHECK_EQ(0, sigaction(nr, nullptr, &old));
      if ((old.sa_flags & SA_SIGINFO) || old.sa_handler != SIG_IGN) continue;
    }
    CHECK_EQ(0, sigaction(nr, &act, nullptr));
  }
#endif  // __POSIX__
}

static std::atomic<uint32_t> init_process_flags = 0;

static void PlatformInit(ProcessInitializationFlags::Flags flags) {
  // init_process_flags is accessed in ResetStdio(),
  // which can be called from signal handlers.
  CHECK(init_process_flags.is_lock_free());
  init_process_flags.store(flags);

  if (!(flags & ProcessInitializationFlags::kNoStdioInitialization)) {
    atexit(ResetStdio);
  }

#ifdef __POSIX__
  if (!(flags & ProcessInitializationFlags::kNoStdioInitialization)) {
    // Disable stdio buffering, it interacts poorly with printf()
    // calls elsewhere in the program (e.g., any logging from V8.)
    setvbuf(stdout, nullptr, _IONBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    // Make sure file descriptors 0-2 are valid before we start logging
    // anything.
    for (auto& s : stdio) {
      const int fd = &s - stdio;
      if (fstat(fd, &s.stat) == 0) continue;

      // Anything but EBADF means something is seriously wrong.  We don't
      // have to special-case EINTR, fstat() is not interruptible.
      if (errno != EBADF) ABORT();

      // If EBADF (file descriptor doesn't exist), open /dev/null and duplicate
      // its file descriptor to the invalid file descriptor.  Make sure *that*
      // file descriptor is valid.  POSIX doesn't guarantee the next file
      // descriptor open(2) gives us is the lowest available number anymore in
      // POSIX.1-2017, which is why dup2(2) is needed.
      int null_fd;

      do {
        null_fd = open("/dev/null", O_RDWR);
      } while (null_fd < 0 && errno == EINTR);

      if (null_fd != fd) {
        int err;

        do {
          err = dup2(null_fd, fd);
        } while (err < 0 && errno == EINTR);
        CHECK_EQ(err, 0);
      }

      if (fstat(fd, &s.stat) < 0) ABORT();
    }
  }

  if (!(flags & ProcessInitializationFlags::kNoDefaultSignalHandling)) {
#if HAVE_INSPECTOR
    sigset_t sigmask;
    sigemptyset(&sigmask);
    sigaddset(&sigmask, SIGUSR1);
    const int err = pthread_sigmask(SIG_SETMASK, &sigmask, nullptr);
    CHECK_EQ(err, 0);
#endif  // HAVE_INSPECTOR

    ResetSignalHandlers();
  }

  if (!(flags & ProcessInitializationFlags::kNoStdioInitialization)) {
    // Record the state of the stdio file descriptors so we can restore it
    // on exit.  Needs to happen before installing signal handlers because
    // they make use of that information.
    for (auto& s : stdio) {
      const int fd = &s - stdio;
      int err;

      do {
        s.flags = fcntl(fd, F_GETFL);
      } while (s.flags == -1 && errno == EINTR);  // NOLINT
      CHECK_NE(s.flags, -1);

      if (uv_guess_handle(fd) != UV_TTY) continue;
      s.isatty = true;

      do {
        err = tcgetattr(fd, &s.termios);
      } while (err == -1 && errno == EINTR);  // NOLINT
      CHECK_EQ(err, 0);
    }
  }

  if (!(flags & ProcessInitializationFlags::kNoDefaultSignalHandling)) {
    RegisterSignalHandler(SIGINT, SignalExit, true);
    RegisterSignalHandler(SIGTERM, SignalExit, true);

#if NODE_USE_V8_WASM_TRAP_HANDLER
#if defined(_WIN32)
    {
      constexpr ULONG first = TRUE;
      per_process::old_vectored_exception_handler =
          AddVectoredExceptionHandler(first, TrapWebAssemblyOrContinue);
    }
#else
    // Tell V8 to disable emitting WebAssembly
    // memory bounds checks. This means that we have
    // to catch the SIGSEGV/SIGBUS in TrapWebAssemblyOrContinue
    // and pass the signal context to V8.
    {
      struct sigaction sa;
      memset(&sa, 0, sizeof(sa));
      sa.sa_sigaction = TrapWebAssemblyOrContinue;
      sa.sa_flags = SA_SIGINFO;
      CHECK_EQ(sigaction(SIGSEGV, &sa, nullptr), 0);
// TODO(align behavior between macos and other in next major version)
#if defined(__APPLE__)
      CHECK_EQ(sigaction(SIGBUS, &sa, nullptr), 0);
#endif
    }
#endif  // defined(_WIN32)
    V8::EnableWebAssemblyTrapHandler(false);
#endif  // NODE_USE_V8_WASM_TRAP_HANDLER
  }

  if (!(flags & ProcessInitializationFlags::kNoAdjustResourceLimits)) {
    // Raise the open file descriptor limit.
    struct rlimit lim;
    if (getrlimit(RLIMIT_NOFILE, &lim) == 0 && lim.rlim_cur != lim.rlim_max) {
      // Do a binary search for the limit.
      rlim_t min = lim.rlim_cur;
      rlim_t max = 1 << 20;
      // But if there's a defined upper bound, don't search, just set it.
      if (lim.rlim_max != RLIM_INFINITY) {
        min = lim.rlim_max;
        max = lim.rlim_max;
      }
      do {
        lim.rlim_cur = min + (max - min) / 2;
        if (setrlimit(RLIMIT_NOFILE, &lim)) {
          max = lim.rlim_cur;
        } else {
          min = lim.rlim_cur;
        }
      } while (min + 1 < max);
    }
  }
#endif  // __POSIX__
#ifdef _WIN32
  if (!(flags & ProcessInitializationFlags::kNoStdioInitialization)) {
    for (int fd = 0; fd <= 2; ++fd) {
      auto handle = reinterpret_cast<HANDLE>(_get_osfhandle(fd));
      if (handle == INVALID_HANDLE_VALUE ||
          GetFileType(handle) == FILE_TYPE_UNKNOWN) {
        // Ignore _close result. If it fails or not depends on used Windows
        // version. We will just check _open result.
        _close(fd);
        if (fd != _open("nul", _O_RDWR)) ABORT();
      }
    }
  }
#endif  // _WIN32
}

// Safe to call more than once and from signal handlers.
void ResetStdio() {
  if (init_process_flags.load() &
      ProcessInitializationFlags::kNoStdioInitialization) {
    return;
  }

  uv_tty_reset_mode();
#ifdef __POSIX__
  for (auto& s : stdio) {
    const int fd = &s - stdio;

    struct stat tmp;
    if (-1 == fstat(fd, &tmp)) {
      CHECK_EQ(errno, EBADF);  // Program closed file descriptor.
      continue;
    }

    bool is_same_file =
        (s.stat.st_dev == tmp.st_dev && s.stat.st_ino == tmp.st_ino);
    if (!is_same_file) continue;  // Program reopened file descriptor.

    int flags;
    do
      flags = fcntl(fd, F_GETFL);
    while (flags == -1 && errno == EINTR);  // NOLINT
    CHECK_NE(flags, -1);

    // Restore the O_NONBLOCK flag if it changed.
    if (O_NONBLOCK & (flags ^ s.flags)) {
      flags &= ~O_NONBLOCK;
      flags |= s.flags & O_NONBLOCK;

      int err;
      do
        err = fcntl(fd, F_SETFL, flags);
      while (err == -1 && errno == EINTR);  // NOLINT
      CHECK_NE(err, -1);
    }

    if (s.isatty) {
      sigset_t sa;
      int err;

      // We might be a background job that doesn't own the TTY so block SIGTTOU
      // before making the tcsetattr() call, otherwise that signal suspends us.
      sigemptyset(&sa);
      sigaddset(&sa, SIGTTOU);

      CHECK_EQ(0, pthread_sigmask(SIG_BLOCK, &sa, nullptr));
      do
        err = tcsetattr(fd, TCSANOW, &s.termios);
      while (err == -1 && errno == EINTR);  // NOLINT
      CHECK_EQ(0, pthread_sigmask(SIG_UNBLOCK, &sa, nullptr));

      // Normally we expect err == 0. But if macOS App Sandbox is enabled,
      // tcsetattr will fail with err == -1 and errno == EPERM.
      CHECK_IMPLIES(err != 0, err == -1 && errno == EPERM);
    }
  }
#endif  // __POSIX__
}


int ProcessGlobalArgs(std::vector<std::string>* args,
                      std::vector<std::string>* exec_args,
                      std::vector<std::string>* errors,
                      OptionEnvvarSettings settings) {
  // Parse a few arguments which are specific to Node.
  std::vector<std::string> v8_args;

  Mutex::ScopedLock lock(per_process::cli_options_mutex);
  options_parser::Parse(
      args,
      exec_args,
      &v8_args,
      per_process::cli_options.get(),
      settings,
      errors);

  if (!errors->empty()) return 9;

  std::string revert_error;
  for (const std::string& cve : per_process::cli_options->security_reverts) {
    Revert(cve.c_str(), &revert_error);
    if (!revert_error.empty()) {
      errors->emplace_back(std::move(revert_error));
      return 12;
    }
  }

  if (per_process::cli_options->disable_proto != "delete" &&
      per_process::cli_options->disable_proto != "throw" &&
      per_process::cli_options->disable_proto != "") {
    errors->emplace_back("invalid mode passed to --disable-proto");
    return 12;
  }

  // TODO(aduh95): remove this when the harmony-import-assertions flag
  // is removed in V8.
  if (std::find(v8_args.begin(), v8_args.end(),
                "--no-harmony-import-assertions") == v8_args.end()) {
    v8_args.emplace_back("--harmony-import-assertions");
  }
  // TODO(aduh95): remove this when the harmony-import-attributes flag
  // is removed in V8.
  if (std::find(v8_args.begin(),
                v8_args.end(),
                "--no-harmony-import-attributes") == v8_args.end()) {
    v8_args.emplace_back("--harmony-import-attributes");
  }

  auto env_opts = per_process::cli_options->per_isolate->per_env;
  if (std::find(v8_args.begin(), v8_args.end(),
                "--abort-on-uncaught-exception") != v8_args.end() ||
      std::find(v8_args.begin(), v8_args.end(),
                "--abort_on_uncaught_exception") != v8_args.end()) {
    env_opts->abort_on_uncaught_exception = true;
  }

  const DebugOptions& debug_options = env_opts->debug_options();
  auto full_option = [&debug_options](std::string option){
      option += debug_options.host_port.host();
      option += ":";
      option += std::to_string(debug_options.host_port.port());
      return option;
  };
  if (debug_options.inspector_enabled) {
      v8_args.push_back(full_option("--inspect="));
  }
  if (debug_options.break_first_line) {
      v8_args.push_back(full_option("--inspect-brk="));
  }
  if (debug_options.break_node_first_line) {
      v8_args.push_back(full_option("--inspect-brk-node="));
  }

#ifdef __POSIX__
  // Block SIGPROF signals when sleeping in epoll_wait/kevent/etc.  Avoids the
  // performance penalty of frequent EINTR wakeups when the profiler is running.
  // Only do this for v8.log profiling, as it breaks v8::CpuProfiler users.
  if (std::find(v8_args.begin(), v8_args.end(), "--prof") != v8_args.end()) {
    uv_loop_configure(uv_default_loop(), UV_LOOP_BLOCK_SIGNAL, SIGPROF);
  }
#endif

  std::vector<char*> v8_args_as_char_ptr(v8_args.size());
  if (v8_args.size() > 0) {
    for (size_t i = 0; i < v8_args.size(); ++i)
      v8_args_as_char_ptr[i] = v8_args[i].data();
    int argc = v8_args.size();
    V8::SetFlagsFromCommandLine(&argc, v8_args_as_char_ptr.data(), true);
    v8_args_as_char_ptr.resize(argc);
  }

  // Anything that's still in v8_argv is not a V8 or a node option.
  for (size_t i = 1; i < v8_args_as_char_ptr.size(); i++)
    errors->push_back("bad option: " + std::string(v8_args_as_char_ptr[i]));

  if (v8_args_as_char_ptr.size() > 1) return 9;

  return 0;
}

static std::atomic_bool init_called{false};

// TODO(addaleax): Turn this into a wrapper around InitializeOncePerProcess()
// (with the corresponding additional flags set), then eventually remove this.
int InitializeNodeWithArgs(std::vector<std::string>* argv,
                           std::vector<std::string>* exec_argv,
                           std::vector<std::string>* errors) {
  return InitializeNodeWithArgs(argv, exec_argv, errors,
                                ProcessFlags::kNoFlags);
}

int InitializeNodeWithArgs(std::vector<std::string>* argv,
                           std::vector<std::string>* exec_argv,
                           std::vector<std::string>* errors,
                           ProcessInitializationFlags::Flags flags) {
  // Make sure InitializeNodeWithArgs() is called only once.
  CHECK(!init_called.exchange(true));

  // Initialize node_start_time to get relative uptime.
  per_process::node_start_time = uv_hrtime();

  // Register built-in bindings
  binding::RegisterBuiltinBindings();

  // Make inherited handles noninheritable.
  if (!(flags & ProcessInitializationFlags::kEnableStdioInheritance) &&
      !(flags & ProcessInitializationFlags::kNoStdioInitialization)) {
    uv_disable_stdio_inheritance();
  }

  // Cache the original command line to be
  // used in diagnostic reports.
  per_process::cli_options->cmdline = *argv;

#if defined(NODE_V8_OPTIONS)
  // Should come before the call to V8::SetFlagsFromCommandLine()
  // so the user can disable a flag --foo at run-time by passing
  // --no_foo from the command line.
  V8::SetFlagsFromString(NODE_V8_OPTIONS, sizeof(NODE_V8_OPTIONS) - 1);
#endif

  HandleEnvOptions(per_process::cli_options->per_isolate->per_env);

#if !defined(NODE_WITHOUT_NODE_OPTIONS)
  if (!(flags & ProcessInitializationFlags::kDisableNodeOptionsEnv)) {
    std::string node_options;

    if (credentials::SafeGetenv("NODE_OPTIONS", &node_options)) {
      std::vector<std::string> env_argv =
          ParseNodeOptionsEnvVar(node_options, errors);

      if (!errors->empty()) return 9;

      // [0] is expected to be the program name, fill it in from the real argv.
      env_argv.insert(env_argv.begin(), argv->at(0));

      const int exit_code = ProcessGlobalArgs(&env_argv,
                                              nullptr,
                                              errors,
                                              kAllowedInEnvvar);
      if (exit_code != 0) return exit_code;
    }
  }
#endif

  if (!(flags & ProcessInitializationFlags::kDisableCLIOptions)) {
    const int exit_code = ProcessGlobalArgs(argv,
                                            exec_argv,
                                            errors,
                                            kDisallowedInEnvvar);
    if (exit_code != 0) return exit_code;
  }

  // Set the process.title immediately after processing argv if --title is set.
  if (!per_process::cli_options->title.empty())
    uv_set_process_title(per_process::cli_options->title.c_str());

#if defined(NODE_HAVE_I18N_SUPPORT)
  if (!(flags & ProcessInitializationFlags::kNoICU)) {
    // If the parameter isn't given, use the env variable.
    if (per_process::cli_options->icu_data_dir.empty())
      credentials::SafeGetenv("NODE_ICU_DATA",
                              &per_process::cli_options->icu_data_dir);

#ifdef NODE_ICU_DEFAULT_DATA_DIR
    // If neither the CLI option nor the environment variable was specified,
    // fall back to the configured default
    if (per_process::cli_options->icu_data_dir.empty()) {
      // Check whether the NODE_ICU_DEFAULT_DATA_DIR contains the right data
      // file and can be read.
      static const char full_path[] =
          NODE_ICU_DEFAULT_DATA_DIR "/" U_ICUDATA_NAME ".dat";

      FILE* f = fopen(full_path, "rb");

      if (f != nullptr) {
        fclose(f);
        per_process::cli_options->icu_data_dir = NODE_ICU_DEFAULT_DATA_DIR;
      }
    }
#endif  // NODE_ICU_DEFAULT_DATA_DIR

    // Initialize ICU.
    // If icu_data_dir is empty here, it will load the 'minimal' data.
    if (!i18n::InitializeICUDirectory(per_process::cli_options->icu_data_dir)) {
      errors->push_back("could not initialize ICU "
                        "(check NODE_ICU_DATA or --icu-data-dir parameters)\n");
      return 9;
    }
    per_process::metadata.versions.InitializeIntlVersions();
  }

# ifndef __POSIX__
  std::string tz;
  if (credentials::SafeGetenv("TZ", &tz) && !tz.empty()) {
    i18n::SetDefaultTimeZone(tz.c_str());
  }
# endif

#endif  // defined(NODE_HAVE_I18N_SUPPORT)

  // We should set node_is_initialized here instead of in node::Start,
  // otherwise embedders using node::Init to initialize everything will not be
  // able to set it and native addons will not load for them.
  node_is_initialized = true;
  return 0;
}

std::unique_ptr<InitializationResult> InitializeOncePerProcess(
    const std::vector<std::string>& args,
    ProcessInitializationFlags::Flags flags) {
  auto result = std::make_unique<InitializationResultImpl>();
  result->args_ = args;

  if (!(flags & ProcessInitializationFlags::kNoParseGlobalDebugVariables)) {
    // Initialized the enabled list for Debug() calls with system
    // environment variables.
    per_process::enabled_debug_list.Parse();
  }

  PlatformInit(flags);

  // This needs to run *before* V8::Initialize().
  {
    result->exit_code_ = InitializeNodeWithArgs(
        &result->args_, &result->exec_args_, &result->errors_, flags);
    if (result->exit_code() != 0) {
      result->early_return_ = true;
      return result;
    }
  }

  if (!(flags & ProcessInitializationFlags::kNoUseLargePages) &&
      (per_process::cli_options->use_largepages == "on" ||
       per_process::cli_options->use_largepages == "silent")) {
    int lp_result = node::MapStaticCodeToLargePages();
    if (per_process::cli_options->use_largepages == "on" && lp_result != 0) {
      result->errors_.emplace_back(node::LargePagesError(lp_result));
    }
  }

  if (!(flags & ProcessInitializationFlags::kNoPrintHelpOrVersionOutput)) {
    if (per_process::cli_options->print_version) {
      printf("%s\n", NODE_VERSION);
      result->exit_code_ = 0;
      result->early_return_ = true;
      return result;
    }

    if (per_process::cli_options->print_bash_completion) {
      std::string completion = options_parser::GetBashCompletion();
      printf("%s\n", completion.c_str());
      result->exit_code_ = 0;
      result->early_return_ = true;
      return result;
    }

    if (per_process::cli_options->print_v8_help) {
      V8::SetFlagsFromString("--help", static_cast<size_t>(6));
      result->exit_code_ = 0;
      result->early_return_ = true;
      return result;
    }
  }

  if (!(flags & ProcessInitializationFlags::kNoInitOpenSSL)) {
#if HAVE_OPENSSL && !defined(OPENSSL_IS_BORINGSSL)
    auto GetOpenSSLErrorString = []() -> std::string {
      std::string ret;
      ERR_print_errors_cb(
          [](const char* str, size_t len, void* opaque) {
            std::string* ret = static_cast<std::string*>(opaque);
            ret->append(str, len);
            ret->append("\n");
            return 0;
          },
          static_cast<void*>(&ret));
      return ret;
    };

    // In the case of FIPS builds we should make sure
    // the random source is properly initialized first.
#if OPENSSL_VERSION_MAJOR >= 3
    // Call OPENSSL_init_crypto to initialize OPENSSL_INIT_LOAD_CONFIG to
    // avoid the default behavior where errors raised during the parsing of the
    // OpenSSL configuration file are not propagated and cannot be detected.
    //
    // If FIPS is configured the OpenSSL configuration file will have an
    // .include pointing to the fipsmodule.cnf file generated by the openssl
    // fipsinstall command. If the path to this file is incorrect no error
    // will be reported.
    //
    // For Node.js this will mean that CSPRNG() will be called by V8 as
    // part of its initialization process, and CSPRNG() will in turn call
    // call RAND_status which will now always return 0, leading to an endless
    // loop and the node process will appear to hang/freeze.

    // Passing NULL as the config file will allow the default openssl.cnf file
    // to be loaded, but the default section in that file will not be used,
    // instead only the section that matches the value of conf_section_name
    // will be read from the default configuration file.
    const char* conf_file = nullptr;
    // To allow for using the previous default where the 'openssl_conf' appname
    // was used, the command line option 'openssl-shared-config' can be used to
    // force the old behavior.
    if (per_process::cli_options->openssl_shared_config) {
      conf_section_name = "openssl_conf";
    }
    // Use OPENSSL_CONF environment variable is set.
    std::string env_openssl_conf;
    credentials::SafeGetenv("OPENSSL_CONF", &env_openssl_conf);
    if (!env_openssl_conf.empty()) {
      conf_file = env_openssl_conf.c_str();
    }
    // Use --openssl-conf command line option if specified.
    if (!per_process::cli_options->openssl_config.empty()) {
      conf_file = per_process::cli_options->openssl_config.c_str();
    }

    OPENSSL_INIT_SETTINGS* settings = OPENSSL_INIT_new();
    OPENSSL_INIT_set_config_filename(settings, conf_file);
    OPENSSL_INIT_set_config_appname(settings, conf_section_name);
    OPENSSL_INIT_set_config_file_flags(settings,
                                       CONF_MFLAGS_IGNORE_MISSING_FILE);

    OPENSSL_init_crypto(OPENSSL_INIT_LOAD_CONFIG, settings);
    OPENSSL_INIT_free(settings);

    if (ERR_peek_error() != 0) {
      // XXX: ERR_GET_REASON does not return something that is
      // useful as an exit code at all.
      result->exit_code_ = ERR_GET_REASON(ERR_peek_error());
      result->early_return_ = true;
      result->errors_.emplace_back("OpenSSL configuration error:\n" +
                                   GetOpenSSLErrorString());
      return result;
    }
#else  // OPENSSL_VERSION_MAJOR < 3
    if (FIPS_mode()) {
      OPENSSL_init();
    }
#endif
    if (!crypto::ProcessFipsOptions()) {
      // XXX: ERR_GET_REASON does not return something that is
      // useful as an exit code at all.
      result->exit_code_ = ERR_GET_REASON(ERR_peek_error());
      result->early_return_ = true;
      result->errors_.emplace_back(
          "OpenSSL error when trying to enable FIPS:\n" +
          GetOpenSSLErrorString());
      return result;
    }

    // Ensure CSPRNG is properly seeded.
    CHECK(crypto::CSPRNG(nullptr, 0).is_ok());

    V8::SetEntropySource([](unsigned char* buffer, size_t length) {
      // V8 falls back to very weak entropy when this function fails
      // and /dev/urandom isn't available. That wouldn't be so bad if
      // the entropy was only used for Math.random() but it's also used for
      // hash table and address space layout randomization. Better to abort.
      CHECK(crypto::CSPRNG(buffer, length).is_ok());
      return true;
    });

    {
      std::string extra_ca_certs;
      if (credentials::SafeGetenv("NODE_EXTRA_CA_CERTS", &extra_ca_certs))
        crypto::UseExtraCaCerts(extra_ca_certs);
    }
#endif  // HAVE_OPENSSL && !defined(OPENSSL_IS_BORINGSSL)
  }

  if (!(flags & ProcessInitializationFlags::kNoInitializeNodeV8Platform)) {
    per_process::v8_platform.Initialize(
        static_cast<int>(per_process::cli_options->v8_thread_pool_size));
    result->platform_ = per_process::v8_platform.Platform();
  }

  if (!(flags & ProcessInitializationFlags::kNoInitializeV8)) {
    V8::Initialize();
  }

  performance::performance_v8_start = PERFORMANCE_NOW();
  per_process::v8_initialized = true;

  return result;
}

void TearDownOncePerProcess() {
  const uint64_t flags = init_process_flags.load();
  ResetStdio();
  if (!(flags & ProcessInitializationFlags::kNoDefaultSignalHandling)) {
    ResetSignalHandlers();
  }

  per_process::v8_initialized = false;
  if (!(flags & ProcessInitializationFlags::kNoInitializeV8)) {
    V8::Dispose();
  }

#if NODE_USE_V8_WASM_TRAP_HANDLER && defined(_WIN32)
  if (!(flags & ProcessInitializationFlags::kNoDefaultSignalHandling)) {
    RemoveVectoredExceptionHandler(per_process::old_vectored_exception_handler);
  }
#endif

  if (!(flags & ProcessInitializationFlags::kNoInitializeNodeV8Platform)) {
    V8::DisposePlatform();
    // uv_run cannot be called from the time before the beforeExit callback
    // runs until the program exits unless the event loop has any referenced
    // handles after beforeExit terminates. This prevents unrefed timers
    // that happen to terminate during shutdown from being run unsafely.
    // Since uv_run cannot be called, uv_async handles held by the platform
    // will never be fully cleaned up.
    per_process::v8_platform.Dispose();
  }
}

InitializationResult::~InitializationResult() {}
InitializationResultImpl::~InitializationResultImpl() {}

int GenerateAndWriteSnapshotData(const SnapshotData** snapshot_data_ptr,
                                 const InitializationResult* result) {
  int exit_code = result->exit_code();
  // nullptr indicates there's no snapshot data.
  DCHECK_NULL(*snapshot_data_ptr);

  // node:embedded_snapshot_main indicates that we are using the
  // embedded snapshot and we are not supposed to clean it up.
  if (result->args()[1] == "node:embedded_snapshot_main") {
    *snapshot_data_ptr = SnapshotBuilder::GetEmbeddedSnapshotData();
    if (*snapshot_data_ptr == nullptr) {
      // The Node.js binary is built without embedded snapshot
      fprintf(stderr,
              "node:embedded_snapshot_main was specified as snapshot "
              "entry point but Node.js was built without embedded "
              "snapshot.\n");
      exit_code = 1;
      return exit_code;
    }
  } else {
    // Otherwise, load and run the specified main script.
    std::unique_ptr<SnapshotData> generated_data =
        std::make_unique<SnapshotData>();
    exit_code = node::SnapshotBuilder::Generate(
        generated_data.get(), result->args(), result->exec_args());
    if (exit_code == 0) {
      *snapshot_data_ptr = generated_data.release();
    } else {
      return exit_code;
    }
  }

  // Get the path to write the snapshot blob to.
  std::string snapshot_blob_path;
  if (!per_process::cli_options->snapshot_blob.empty()) {
    snapshot_blob_path = per_process::cli_options->snapshot_blob;
  } else {
    // Defaults to snapshot.blob in the current working directory.
    snapshot_blob_path = std::string("snapshot.blob");
  }

  FILE* fp = fopen(snapshot_blob_path.c_str(), "wb");
  if (fp != nullptr) {
    (*snapshot_data_ptr)->ToBlob(fp);
    fclose(fp);
  } else {
    fprintf(stderr,
            "Cannot open %s for writing a snapshot.\n",
            snapshot_blob_path.c_str());
    exit_code = 1;
  }
  return exit_code;
}

int LoadSnapshotDataAndRun(const SnapshotData** snapshot_data_ptr,
                           const InitializationResult* result) {
  int exit_code = result->exit_code();
  // nullptr indicates there's no snapshot data.
  DCHECK_NULL(*snapshot_data_ptr);
  // --snapshot-blob indicates that we are reading a customized snapshot.
  if (!per_process::cli_options->snapshot_blob.empty()) {
    std::string filename = per_process::cli_options->snapshot_blob;
    FILE* fp = fopen(filename.c_str(), "rb");
    if (fp == nullptr) {
      fprintf(stderr, "Cannot open %s", filename.c_str());
      exit_code = 1;
      return exit_code;
    }
    std::unique_ptr<SnapshotData> read_data = std::make_unique<SnapshotData>();
    bool ok = SnapshotData::FromBlob(read_data.get(), fp);
    fclose(fp);
    if (!ok) {
      // If we fail to read the customized snapshot, simply exit with 1.
      exit_code = 1;
      return exit_code;
    }
    *snapshot_data_ptr = read_data.release();
  } else if (per_process::cli_options->node_snapshot) {
    // If --snapshot-blob is not specified, we are reading the embedded
    // snapshot, but we will skip it if --no-node-snapshot is specified.
    const node::SnapshotData* read_data =
        SnapshotBuilder::GetEmbeddedSnapshotData();
    if (read_data != nullptr && read_data->Check()) {
      // If we fail to read the embedded snapshot, treat it as if Node.js
      // was built without one.
      *snapshot_data_ptr = read_data;
    }
  }

  NodeMainInstance main_instance(*snapshot_data_ptr,
                                 uv_default_loop(),
                                 per_process::v8_platform.Platform(),
                                 result->args(),
                                 result->exec_args());
  exit_code = main_instance.Run();
  return exit_code;
}

int Start(int argc, char** argv) {
#ifndef DISABLE_SINGLE_EXECUTABLE_APPLICATION
  std::tie(argc, argv) = sea::FixupArgsForSEA(argc, argv);
#endif

  CHECK_GT(argc, 0);

  // Hack around with the argv pointer. Used for process.title = "blah".
  argv = uv_setup_args(argc, argv);

  std::unique_ptr<InitializationResult> result =
      InitializeOncePerProcess(std::vector<std::string>(argv, argv + argc));
  for (const std::string& error : result->errors()) {
    FPrintF(stderr, "%s: %s\n", result->args().at(0), error);
  }
  if (result->early_return()) {
    return result->exit_code();
  }

  DCHECK_EQ(result->exit_code(), 0);
  const SnapshotData* snapshot_data = nullptr;

  auto cleanup_process = OnScopeLeave([&]() {
    TearDownOncePerProcess();

    if (snapshot_data != nullptr &&
        snapshot_data->data_ownership == SnapshotData::DataOwnership::kOwned) {
      delete snapshot_data;
    }
  });

  uv_loop_configure(uv_default_loop(), UV_METRICS_IDLE_TIME);

  // --build-snapshot indicates that we are in snapshot building mode.
  if (per_process::cli_options->build_snapshot) {
    if (result->args().size() < 2) {
      fprintf(stderr,
              "--build-snapshot must be used with an entry point script.\n"
              "Usage: node --build-snapshot /path/to/entry.js\n");
      return 9;
    }
    return GenerateAndWriteSnapshotData(&snapshot_data, result.get());
  }

  // Without --build-snapshot, we are in snapshot loading mode.
  return LoadSnapshotDataAndRun(&snapshot_data, result.get());
}

int Stop(Environment* env) {
  return Stop(env, StopFlags::kNoFlags);
}

int Stop(Environment* env, StopFlags::Flags flags) {
  env->ExitEnv(flags);
  return 0;
}

// GRAAL EXTENSIONS

static void os_setenv(const char * name, const char * value) {
#ifdef __POSIX__
    setenv(name, value, 1);
#else
    _putenv_s(name, value);
#endif
}

long GraalArgumentsPreprocessing(int argc, char *argv[]) {
  long result = -1;
  for (int i = 1; i < argc; i++) {
    const char *arg = argv[i];
    const char *nptr = nullptr;
    const char *classpath = nullptr;
    if (!strncmp(arg, "--vm.Xss", sizeof("--vm.Xss") - 1)) {
      nptr = arg + sizeof("--vm.Xss") - 1;
    } else if (!strncmp(arg, "--jvm.Xss", sizeof("--jvm.Xss") - 1)) {
      nptr = arg + sizeof("--jvm.Xss") - 1;
    } else if(!strncmp(arg, "--native.Xss", sizeof("--native.Xss") - 1)) {
      nptr = arg + sizeof("--native.Xss") - 1;
    } else if (!strncmp(arg, "--vm.classpath", sizeof ("--vm.classpath") - 1)) {
      classpath = arg + sizeof ("--vm.classpath") - 1;
    } else if (!strncmp(arg, "--vm.cp", sizeof ("--vm.cp") - 1)) {
      classpath = arg + sizeof ("--vm.cp") - 1;
    } else if (!strncmp(arg, "--jvm.classpath", sizeof ("--jvm.classpath") - 1)) {
      classpath = arg + sizeof ("--jvm.classpath") - 1;
    } else if (!strncmp(arg, "--jvm.cp", sizeof ("--jvm.cp") - 1)) {
      classpath = arg + sizeof ("--jvm.cp") - 1;
    } else if (arg[0] != '-') { // stop at first non-option argument
        break;
    }
    if (classpath != nullptr) {
      if (classpath[0] == '=') {
        os_setenv("NODE_JVM_CLASSPATH", classpath + 1);
      } else if (classpath[0] == 0 && i + 1 < argc) {
        os_setenv("NODE_JVM_CLASSPATH", argv[++i]);
        // Hack: replace the argument with the classpath by a string full of '-'.
        // This ensures that it looks like an option (i.e. is passed to JS engine
        // options, i.e., is not considered to be a name of the script to execute).
        memset(argv[i], '-', strlen(argv[i]));
      } else {
        fprintf(stderr, "the --vm.classpath and --vm.cp arguments must be of the form --vm.[classpath|cp]=<classpath>");
        exit(9);
      }
    }
    if (nptr != nullptr) {
      // NOTE: if more than one value is specified, return the last one. If
      // --native and --jvm are mixed, let the parsing code for all arguments
      // handle this.
      char *endptr;
      long value = strtol(nptr, &endptr, 10);
      if (endptr != nptr) {
        long multiplier;
        if (*endptr == '\0') {
          multiplier = 1;
        } else if(!strcmp("k", endptr) || !strcmp("K", endptr)) {
          multiplier = 1000;
        } else if(!strcmp("m", endptr) || !strcmp("M", endptr)) {
          multiplier = 1000000;
        } else if(!strcmp("g", endptr) || !strcmp("G", endptr)) {
          multiplier = 1000000000;
        } else {
          multiplier = -1; // garbage
        }
        if (multiplier > -1) {
          result = value * multiplier;
        }
      }
    }
  }
  return result;
}

}  // namespace node

#if !HAVE_INSPECTOR
void Initialize() {}

NODE_BINDING_CONTEXT_AWARE_INTERNAL(inspector, Initialize)
#endif  // !HAVE_INSPECTOR
