#include "node_options.h"  // NOLINT(build/include_inline)
#include "node_options-inl.h"

#include "env-inl.h"
#include "node_binding.h"

#include <cstdlib>  // strtoul, errno

using v8::Boolean;
using v8::Context;
using v8::FunctionCallbackInfo;
using v8::Integer;
using v8::Isolate;
using v8::Local;
using v8::Map;
using v8::Number;
using v8::Object;
using v8::String;
using v8::Undefined;
using v8::Value;

namespace node {

namespace per_process {
Mutex cli_options_mutex(false); // It might be destroyed in spite of holding lock (e.g. --help)
std::shared_ptr<PerProcessOptions> cli_options{new PerProcessOptions()};
}  // namespace per_process

void DebugOptions::CheckOptions(std::vector<std::string>* errors) {
#if !NODE_USE_V8_PLATFORM && !HAVE_INSPECTOR
  if (inspector_enabled) {
    errors->push_back("Inspector is not available when Node is compiled "
                      "--without-v8-platform and --without-inspector.");
  }
#endif

  if (deprecated_debug) {
    errors->push_back("[DEP0062]: `node --debug` and `node --debug-brk` "
                      "are invalid. Please use `node --inspect` and "
                      "`node --inspect-brk` instead.");
  }

  std::vector<std::string> destinations =
      SplitString(inspect_publish_uid_string, ',');
  inspect_publish_uid.console = false;
  inspect_publish_uid.http = false;
  for (const std::string& destination : destinations) {
    if (destination == "stderr") {
      inspect_publish_uid.console = true;
    } else if (destination == "http") {
      inspect_publish_uid.http = true;
    } else {
      errors->push_back("--inspect-publish-uid destination can be "
                        "stderr or http");
    }
  }
}

void PerProcessOptions::CheckOptions(std::vector<std::string>* errors) {
#if HAVE_OPENSSL
  if (use_openssl_ca && use_bundled_ca) {
    errors->push_back("either --use-openssl-ca or --use-bundled-ca can be "
                      "used, not both");
  }
#endif
  per_isolate->CheckOptions(errors);
}

void PerIsolateOptions::CheckOptions(std::vector<std::string>* errors) {
  per_env->CheckOptions(errors);
#ifdef NODE_REPORT
  if (per_env->experimental_report) {
    // Assign the report_signal default value here. Once the
    // --experimental-report flag is dropped, move this initialization to
    // node_options.h, where report_signal is declared.
    if (report_signal.empty())
      report_signal = "SIGUSR2";
    return;
  }

  if (!report_directory.empty()) {
    errors->push_back("--report-directory option is valid only when "
                      "--experimental-report is set");
  }

  if (!report_filename.empty()) {
    errors->push_back("--report-filename option is valid only when "
                      "--experimental-report is set");
  }

  if (!report_signal.empty()) {
    errors->push_back("--report-signal option is valid only when "
                      "--experimental-report is set");
  }

  if (report_on_fatalerror) {
    errors->push_back(
        "--report-on-fatalerror option is valid only when "
        "--experimental-report is set");
  }

  if (report_on_signal) {
    errors->push_back("--report-on-signal option is valid only when "
                      "--experimental-report is set");
  }

  if (report_uncaught_exception) {
    errors->push_back(
        "--report-uncaught-exception option is valid only when "
        "--experimental-report is set");
  }
#endif  // NODE_REPORT
}

void EnvironmentOptions::CheckOptions(std::vector<std::string>* errors) {
  if (!userland_loader.empty() && !experimental_modules) {
    errors->push_back("--experimental-loader requires "
                      "--experimental-modules be enabled");
  }
  if (has_policy_integrity_string && experimental_policy.empty()) {
    errors->push_back("--policy-integrity requires "
                      "--experimental-policy be enabled");
  }
  if (has_policy_integrity_string && experimental_policy_integrity.empty()) {
    errors->push_back("--policy-integrity cannot be empty");
  }

  if (!module_type.empty()) {
    if (!experimental_modules) {
      errors->push_back("--input-type requires "
                        "--experimental-modules to be enabled");
    }
    if (module_type != "commonjs" && module_type != "module") {
      errors->push_back("--input-type must be \"module\" or \"commonjs\"");
    }
  }

  if (experimental_json_modules && !experimental_modules) {
    errors->push_back("--experimental-json-modules requires "
                      "--experimental-modules be enabled");
  }

  if (experimental_wasm_modules && !experimental_modules) {
    errors->push_back("--experimental-wasm-modules requires "
                      "--experimental-modules be enabled");
  }

  if (!es_module_specifier_resolution.empty()) {
    if (!experimental_modules) {
      errors->push_back("--es-module-specifier-resolution requires "
                        "--experimental-modules be enabled");
    }
    if (es_module_specifier_resolution != "node" &&
        es_module_specifier_resolution != "explicit") {
      errors->push_back("invalid value for --es-module-specifier-resolution");
    }
  }

  if (syntax_check_only && has_eval_string) {
    errors->push_back("either --check or --eval can be used, not both");
  }

  if (http_parser != "legacy" && http_parser != "llhttp") {
    errors->push_back("invalid value for --http-parser");
  }

  if (!unhandled_rejections.empty() &&
      unhandled_rejections != "strict" &&
      unhandled_rejections != "warn" &&
      unhandled_rejections != "none") {
    errors->push_back("invalid value for --unhandled-rejections");
  }

  if (tls_min_v1_3 && tls_max_v1_2) {
    errors->push_back("either --tls-min-v1.3 or --tls-max-v1.2 can be "
                      "used, not both");
  }

#if HAVE_INSPECTOR
  if (!cpu_prof) {
    if (!cpu_prof_name.empty()) {
      errors->push_back("--cpu-prof-name must be used with --cpu-prof");
    }
    if (!cpu_prof_dir.empty()) {
      errors->push_back("--cpu-prof-dir must be used with --cpu-prof");
    }
    // We can't catch the case where the value passed is the default value,
    // then the option just becomes a noop which is fine.
    if (cpu_prof_interval != kDefaultCpuProfInterval) {
      errors->push_back("--cpu-prof-interval must be used with --cpu-prof");
    }
  }

  if (!heap_prof) {
    if (!heap_prof_name.empty()) {
      errors->push_back("--heap-prof-name must be used with --heap-prof");
    }
    if (!heap_prof_dir.empty()) {
      errors->push_back("--heap-prof-dir must be used with --heap-prof");
    }
    // We can't catch the case where the value passed is the default value,
    // then the option just becomes a noop which is fine.
    if (heap_prof_interval != kDefaultHeapProfInterval) {
      errors->push_back("--heap-prof-interval must be used with --heap-prof");
    }
  }
  debug_options_.CheckOptions(errors);
#endif  // HAVE_INSPECTOR
}

namespace options_parser {

class DebugOptionsParser : public OptionsParser<DebugOptions> {
 public:
  DebugOptionsParser();
};

class EnvironmentOptionsParser : public OptionsParser<EnvironmentOptions> {
 public:
  EnvironmentOptionsParser();
  explicit EnvironmentOptionsParser(const DebugOptionsParser& dop)
    : EnvironmentOptionsParser() {
    Insert(dop, &EnvironmentOptions::get_debug_options);
  }
};

class PerIsolateOptionsParser : public OptionsParser<PerIsolateOptions> {
 public:
  PerIsolateOptionsParser() = delete;
  explicit PerIsolateOptionsParser(const EnvironmentOptionsParser& eop);
};

class PerProcessOptionsParser : public OptionsParser<PerProcessOptions> {
 public:
  PerProcessOptionsParser() = delete;
  explicit PerProcessOptionsParser(const PerIsolateOptionsParser& iop);
};

#if HAVE_INSPECTOR
const DebugOptionsParser _dop_instance{};
const EnvironmentOptionsParser _eop_instance{_dop_instance};

// This Parse is not dead code. It is used by embedders (e.g., Electron).
template <>
void Parse(
  StringVector* const args, StringVector* const exec_args,
  StringVector* const v8_args,
  DebugOptions* const options,
  OptionEnvvarSettings required_env_settings, StringVector* const errors) {
  _dop_instance.Parse(
    args, exec_args, v8_args, options, required_env_settings, errors);
}
#else
const DebugOptionsParser _dop_instance{};
const EnvironmentOptionsParser _eop_instance{_dop_instance};
#endif  // HAVE_INSPECTOR
const PerIsolateOptionsParser _piop_instance{_eop_instance};
const PerProcessOptionsParser _ppop_instance{_piop_instance};

template <>
void Parse(
  StringVector* const args, StringVector* const exec_args,
  StringVector* const v8_args,
  PerIsolateOptions* const options,
  OptionEnvvarSettings required_env_settings, StringVector* const errors) {
  _piop_instance.Parse(
    args, exec_args, v8_args, options, required_env_settings, errors);
}

template <>
void Parse(
  StringVector* const args, StringVector* const exec_args,
  StringVector* const v8_args,
  PerProcessOptions* const options,
  OptionEnvvarSettings required_env_settings, StringVector* const errors) {
  _ppop_instance.Parse(
    args, exec_args, v8_args, options, required_env_settings, errors);
}

// XXX: If you add an option here, please also add it to doc/node.1 and
// doc/api/cli.md
// TODO(addaleax): Make that unnecessary.

DebugOptionsParser::DebugOptionsParser() {
  AddOption("--inspect-port",
            "set host:port for inspector",
            &DebugOptions::host_port,
            kAllowedInEnvironment);
  AddAlias("--debug-port", "--inspect-port");

  AddOption("--inspect",
            "activate inspector on host:port (default: 127.0.0.1:9229)",
            &DebugOptions::inspector_enabled,
            kAllowedInEnvironment);
  AddAlias("--inspect=", { "--inspect-port", "--inspect" });

  AddOption("--debug", "", &DebugOptions::deprecated_debug);
  AddAlias("--debug=", "--debug");
  AddOption("--debug-brk", "", &DebugOptions::deprecated_debug);
  AddAlias("--debug-brk=", "--debug-brk");

  AddOption("--inspect-brk",
            "activate inspector on host:port and break at start of user script",
            &DebugOptions::break_first_line,
            kAllowedInEnvironment);
  Implies("--inspect-brk", "--inspect");
  AddAlias("--inspect-brk=", { "--inspect-port", "--inspect-brk" });

  AddOption("--inspect-brk-node", "", &DebugOptions::break_node_first_line);
  Implies("--inspect-brk-node", "--inspect");
  AddAlias("--inspect-brk-node=", { "--inspect-port", "--inspect-brk-node" });

  AddOption("--inspect-publish-uid",
            "comma separated list of destinations for inspector uid"
            "(default: stderr,http)",
            &DebugOptions::inspect_publish_uid_string,
            kAllowedInEnvironment);
}

EnvironmentOptionsParser::EnvironmentOptionsParser() {
  AddOption("--enable-source-maps",
            "experimental Source Map V3 support",
            &EnvironmentOptions::enable_source_maps,
            kAllowedInEnvironment);
  AddOption("--experimental-exports",
            "experimental support for exports in package.json",
            &EnvironmentOptions::experimental_exports,
            kAllowedInEnvironment);
  AddOption("--experimental-json-modules",
            "experimental JSON interop support for the ES Module loader",
            &EnvironmentOptions::experimental_json_modules,
            kAllowedInEnvironment);
  AddOption("--experimental-loader",
            "(with --experimental-modules) use the specified file as a "
            "custom loader",
            &EnvironmentOptions::userland_loader,
            kAllowedInEnvironment);
  AddAlias("--loader", "--experimental-loader");
  AddOption("--experimental-modules",
            "experimental ES Module support and caching modules",
            &EnvironmentOptions::experimental_modules,
            kAllowedInEnvironment);
  Implies("--experimental-modules", "--experimental-exports");
  AddOption("--experimental-wasm-modules",
            "experimental ES Module support for webassembly modules",
            &EnvironmentOptions::experimental_wasm_modules,
            kAllowedInEnvironment);
  AddOption("--experimental-policy",
            "use the specified file as a "
            "security policy",
            &EnvironmentOptions::experimental_policy,
            kAllowedInEnvironment);
  AddOption("[has_policy_integrity_string]",
            "",
            &EnvironmentOptions::has_policy_integrity_string);
  AddOption("--policy-integrity",
            "ensure the security policy contents match "
            "the specified integrity",
            &EnvironmentOptions::experimental_policy_integrity,
            kAllowedInEnvironment);
  Implies("--policy-integrity", "[has_policy_integrity_string]");
  AddOption("--experimental-repl-await",
            "experimental await keyword support in REPL",
            &EnvironmentOptions::experimental_repl_await,
            kAllowedInEnvironment);
  AddOption("--experimental-vm-modules",
            "experimental ES Module support in vm module",
            &EnvironmentOptions::experimental_vm_modules,
            kAllowedInEnvironment);
  AddOption("--experimental-worker", "", NoOp{}, kAllowedInEnvironment);
#ifdef NODE_REPORT
  AddOption("--experimental-report",
            "enable report generation",
            &EnvironmentOptions::experimental_report,
            kAllowedInEnvironment);
#endif  // NODE_REPORT
  AddOption("--expose-internals", "", &EnvironmentOptions::expose_internals);
  AddOption("--frozen-intrinsics",
            "experimental frozen intrinsics support",
            &EnvironmentOptions::frozen_intrinsics,
            kAllowedInEnvironment);
  AddOption("--heapsnapshot-signal",
            "Generate heap snapshot on specified signal",
            &EnvironmentOptions::heap_snapshot_signal,
            kAllowedInEnvironment);
  AddOption("--http-parser",
            "Select which HTTP parser to use; either 'legacy' or 'llhttp' "
            "(default: llhttp).",
            &EnvironmentOptions::http_parser,
            kAllowedInEnvironment);
  AddOption("--http-server-default-timeout",
            "Default http server socket timeout in ms "
            "(default: 120000)",
            &EnvironmentOptions::http_server_default_timeout,
            kAllowedInEnvironment);
  AddOption("--insecure-http-parser",
            "Use an insecure HTTP parser that accepts invalid HTTP headers",
            &EnvironmentOptions::insecure_http_parser,
            kAllowedInEnvironment);
  AddOption("--input-type",
            "set module type for string input",
            &EnvironmentOptions::module_type,
            kAllowedInEnvironment);
  AddOption("--es-module-specifier-resolution",
            "Select extension resolution algorithm for es modules; "
            "either 'explicit' (default) or 'node'",
            &EnvironmentOptions::es_module_specifier_resolution,
            kAllowedInEnvironment);
  AddOption("--no-deprecation",
            "silence deprecation warnings",
            &EnvironmentOptions::no_deprecation,
            kAllowedInEnvironment);
  AddOption("--no-force-async-hooks-checks",
            "disable checks for async_hooks",
            &EnvironmentOptions::no_force_async_hooks_checks,
            kAllowedInEnvironment);
  AddOption("--no-warnings",
            "silence all process warnings",
            &EnvironmentOptions::no_warnings,
            kAllowedInEnvironment);
  AddOption("--force-context-aware",
            "disable loading non-context-aware addons",
            &EnvironmentOptions::force_context_aware,
            kAllowedInEnvironment);
  AddOption("--pending-deprecation",
            "emit pending deprecation warnings",
            &EnvironmentOptions::pending_deprecation,
            kAllowedInEnvironment);
  AddOption("--preserve-symlinks",
            "preserve symbolic links when resolving",
            &EnvironmentOptions::preserve_symlinks,
            kAllowedInEnvironment);
  AddOption("--preserve-symlinks-main",
            "preserve symbolic links when resolving the main module",
            &EnvironmentOptions::preserve_symlinks_main,
            kAllowedInEnvironment);
  AddOption("--prof-process",
            "process V8 profiler output generated using --prof",
            &EnvironmentOptions::prof_process);
  // Options after --prof-process are passed through to the prof processor.
  AddAlias("--prof-process", { "--prof-process", "--" });
#if HAVE_INSPECTOR
  AddOption("--cpu-prof",
            "Start the V8 CPU profiler on start up, and write the CPU profile "
            "to disk before exit. If --cpu-prof-dir is not specified, write "
            "the profile to the current working directory.",
            &EnvironmentOptions::cpu_prof);
  AddOption("--cpu-prof-name",
            "specified file name of the V8 CPU profile generated with "
            "--cpu-prof",
            &EnvironmentOptions::cpu_prof_name);
  AddOption("--cpu-prof-interval",
            "specified sampling interval in microseconds for the V8 CPU "
            "profile generated with --cpu-prof. (default: 1000)",
            &EnvironmentOptions::cpu_prof_interval);
  AddOption("--cpu-prof-dir",
            "Directory where the V8 profiles generated by --cpu-prof will be "
            "placed. Does not affect --prof.",
            &EnvironmentOptions::cpu_prof_dir);
  AddOption(
      "--heap-prof",
      "Start the V8 heap profiler on start up, and write the heap profile "
      "to disk before exit. If --heap-prof-dir is not specified, write "
      "the profile to the current working directory.",
      &EnvironmentOptions::heap_prof);
  AddOption("--heap-prof-name",
            "specified file name of the V8 CPU profile generated with "
            "--heap-prof",
            &EnvironmentOptions::heap_prof_name);
  AddOption("--heap-prof-dir",
            "Directory where the V8 heap profiles generated by --heap-prof "
            "will be placed.",
            &EnvironmentOptions::heap_prof_dir);
  AddOption("--heap-prof-interval",
            "specified sampling interval in bytes for the V8 heap "
            "profile generated with --heap-prof. (default: 512 * 1024)",
            &EnvironmentOptions::heap_prof_interval);
#endif  // HAVE_INSPECTOR
  AddOption("--redirect-warnings",
            "write warnings to file instead of stderr",
            &EnvironmentOptions::redirect_warnings,
            kAllowedInEnvironment);
  AddOption("--test-udp-no-try-send", "",  // For testing only.
            &EnvironmentOptions::test_udp_no_try_send);
  AddOption("--throw-deprecation",
            "throw an exception on deprecations",
            &EnvironmentOptions::throw_deprecation,
            kAllowedInEnvironment);
  AddOption("--trace-deprecation",
            "show stack traces on deprecations",
            &EnvironmentOptions::trace_deprecation,
            kAllowedInEnvironment);
  AddOption("--trace-sync-io",
            "show stack trace when use of sync IO is detected after the "
            "first tick",
            &EnvironmentOptions::trace_sync_io,
            kAllowedInEnvironment);
  AddOption("--trace-tls",
            "prints TLS packet trace information to stderr",
            &EnvironmentOptions::trace_tls,
            kAllowedInEnvironment);
  AddOption("--trace-warnings",
            "show stack traces on process warnings",
            &EnvironmentOptions::trace_warnings,
            kAllowedInEnvironment);
  AddOption("--unhandled-rejections",
            "define unhandled rejections behavior. Options are 'strict' (raise "
            "an error), 'warn' (enforce warnings) or 'none' (silence warnings)",
            &EnvironmentOptions::unhandled_rejections,
            kAllowedInEnvironment);

  AddOption("--check",
            "syntax check script without executing",
            &EnvironmentOptions::syntax_check_only);
  AddAlias("-c", "--check");
  // This option is only so that we can tell --eval with an empty string from
  // no eval at all. Having it not start with a dash makes it inaccessible
  // from the parser itself, but available for using Implies().
  // TODO(addaleax): When moving --help over to something generated from the
  // programmatic descriptions, this will need some special care.
  // (See also [ssl_openssl_cert_store] below.)
  AddOption("[has_eval_string]", "", &EnvironmentOptions::has_eval_string);
  AddOption("--eval", "evaluate script", &EnvironmentOptions::eval_string);
  Implies("--eval", "[has_eval_string]");
  AddOption("--print",
            "evaluate script and print result",
            &EnvironmentOptions::print_eval);
  AddAlias("-e", "--eval");
  AddAlias("--print <arg>", "-pe");
  AddAlias("-pe", { "--print", "--eval" });
  AddAlias("-p", "--print");
  AddOption("--require",
            "module to preload (option can be repeated)",
            &EnvironmentOptions::preload_modules,
            kAllowedInEnvironment);
  AddAlias("-r", "--require");
  AddOption("--interactive",
            "always enter the REPL even if stdin does not appear "
            "to be a terminal",
            &EnvironmentOptions::force_repl);
  AddAlias("-i", "--interactive");

  AddOption("--napi-modules", "", NoOp{}, kAllowedInEnvironment);

  AddOption("--tls-min-v1.0",
            "set default TLS minimum to TLSv1.0 (default: TLSv1.2)",
            &EnvironmentOptions::tls_min_v1_0,
            kAllowedInEnvironment);
  AddOption("--tls-min-v1.1",
            "set default TLS minimum to TLSv1.1 (default: TLSv1.2)",
            &EnvironmentOptions::tls_min_v1_1,
            kAllowedInEnvironment);
  AddOption("--tls-min-v1.2",
            "set default TLS minimum to TLSv1.2 (default: TLSv1.2)",
            &EnvironmentOptions::tls_min_v1_2,
            kAllowedInEnvironment);
  AddOption("--tls-min-v1.3",
            "set default TLS minimum to TLSv1.3 (default: TLSv1.2)",
            &EnvironmentOptions::tls_min_v1_3,
            kAllowedInEnvironment);
  AddOption("--tls-max-v1.2",
            "set default TLS maximum to TLSv1.2 (default: TLSv1.3)",
            &EnvironmentOptions::tls_max_v1_2,
            kAllowedInEnvironment);
  // Current plan is:
  // - 11.x and below: TLS1.3 is opt-in with --tls-max-v1.3
  // - 12.x: TLS1.3 is opt-out with --tls-max-v1.2
  // In either case, support both options they are uniformly available.
  AddOption("--tls-max-v1.3",
            "set default TLS maximum to TLSv1.3 (default: TLSv1.3)",
            &EnvironmentOptions::tls_max_v1_3,
            kAllowedInEnvironment);
}

PerIsolateOptionsParser::PerIsolateOptionsParser(
  const EnvironmentOptionsParser& eop) {
  AddOption("--track-heap-objects",
            "track heap object allocations for heap snapshots",
            &PerIsolateOptions::track_heap_objects,
            kAllowedInEnvironment);
  AddOption("--no-node-snapshot",
            "",  // It's a debug-only option.
            &PerIsolateOptions::no_node_snapshot,
            kAllowedInEnvironment);

  // Explicitly add some V8 flags to mark them as allowed in NODE_OPTIONS.
  AddOption("--abort-on-uncaught-exception",
            "aborting instead of exiting causes a core file to be generated "
            "for analysis",
            V8Option{},
            kAllowedInEnvironment);
  AddOption("--interpreted-frames-native-stack",
            "help system profilers to translate JavaScript interpreted frames",
            V8Option{}, kAllowedInEnvironment);
  AddOption("--max-old-space-size", "", V8Option{}, kAllowedInEnvironment);
  AddOption("--perf-basic-prof", "", V8Option{}, kAllowedInEnvironment);
  AddOption("--perf-basic-prof-only-functions",
            "",
            V8Option{},
            kAllowedInEnvironment);
  AddOption("--perf-prof", "", V8Option{}, kAllowedInEnvironment);
  AddOption("--perf-prof-unwinding-info",
            "",
            V8Option{},
            kAllowedInEnvironment);
  AddOption("--stack-trace-limit", "", V8Option{}, kAllowedInEnvironment);

#ifdef NODE_REPORT
  AddOption("--report-uncaught-exception",
            "generate diagnostic report on uncaught exceptions",
            &PerIsolateOptions::report_uncaught_exception,
            kAllowedInEnvironment);
  AddOption("--report-on-signal",
            "generate diagnostic report upon receiving signals",
            &PerIsolateOptions::report_on_signal,
            kAllowedInEnvironment);
  AddOption("--report-on-fatalerror",
            "generate diagnostic report on fatal (internal) errors",
            &PerIsolateOptions::report_on_fatalerror,
            kAllowedInEnvironment);
  AddOption("--report-signal",
            "causes diagnostic report to be produced on provided signal,"
            " unsupported in Windows. (default: SIGUSR2)",
            &PerIsolateOptions::report_signal,
            kAllowedInEnvironment);
  Implies("--report-signal", "--report-on-signal");
  AddOption("--report-filename",
            "define custom report file name."
            " (default: YYYYMMDD.HHMMSS.PID.SEQUENCE#.txt)",
            &PerIsolateOptions::report_filename,
            kAllowedInEnvironment);
  AddOption("--report-directory",
            "define custom report pathname."
            " (default: current working directory of Node.js process)",
            &PerIsolateOptions::report_directory,
            kAllowedInEnvironment);
#endif  // NODE_REPORT

  Insert(eop, &PerIsolateOptions::get_per_env_options);
}

PerProcessOptionsParser::PerProcessOptionsParser(
  const PerIsolateOptionsParser& iop) {
  AddOption("--title",
            "the process title to use on startup",
            &PerProcessOptions::title,
            kAllowedInEnvironment);
  AddOption("--trace-event-categories",
            "comma separated list of trace event categories to record",
            &PerProcessOptions::trace_event_categories,
            kAllowedInEnvironment);
  AddOption("--trace-event-file-pattern",
            "Template string specifying the filepath for the trace-events "
            "data, it supports ${rotation} and ${pid}.",
            &PerProcessOptions::trace_event_file_pattern,
            kAllowedInEnvironment);
  AddAlias("--trace-events-enabled", {
    "--trace-event-categories", "v8,node,node.async_hooks" });
  AddOption("--max-http-header-size",
            "set the maximum size of HTTP headers (default: 8KB)",
            &PerProcessOptions::max_http_header_size,
            kAllowedInEnvironment);
  AddOption("--v8-pool-size",
            "set V8's thread pool size",
            &PerProcessOptions::v8_thread_pool_size,
            kAllowedInEnvironment);
  AddOption("--zero-fill-buffers",
            "automatically zero-fill all newly allocated Buffer and "
            "SlowBuffer instances",
            &PerProcessOptions::zero_fill_all_buffers,
            kAllowedInEnvironment);
  AddOption("--debug-arraybuffer-allocations",
            "", /* undocumented, only for debugging */
            &PerProcessOptions::debug_arraybuffer_allocations,
            kAllowedInEnvironment);


  // 12.x renamed this inadvertently, so alias it for consistency within the
  // release line, while using the original name for consistency with older
  // release lines.
  AddOption("--security-revert", "", &PerProcessOptions::security_reverts);
  AddAlias("--security-reverts", "--security-revert");
  AddOption("--completion-bash",
            "print source-able bash completion script",
            &PerProcessOptions::print_bash_completion);
//  AddOption("--help",
//            "print node command line options",
//            &PerProcessOptions::print_help);
//  AddAlias("-h", "--help");
  AddOption(
      "--version", "print Node.js version", &PerProcessOptions::print_version);
  AddAlias("-v", "--version");
  AddOption("--v8-options",
            "print V8 command line options",
            &PerProcessOptions::print_v8_help);

#ifdef NODE_HAVE_I18N_SUPPORT
  AddOption("--icu-data-dir",
            "set ICU data load path to dir (overrides NODE_ICU_DATA)"
#ifndef NODE_HAVE_SMALL_ICU
            " (note: linked-in ICU data is present)\n"
#endif
            ,
            &PerProcessOptions::icu_data_dir,
            kAllowedInEnvironment);
#endif

#if HAVE_OPENSSL
  AddOption("--openssl-config",
            "load OpenSSL configuration from the specified file "
            "(overrides OPENSSL_CONF)",
            &PerProcessOptions::openssl_config,
            kAllowedInEnvironment);
  AddOption("--tls-cipher-list",
            "use an alternative default TLS cipher list",
            &PerProcessOptions::tls_cipher_list,
            kAllowedInEnvironment);
  AddOption("--use-openssl-ca",
            "use OpenSSL's default CA store"
#if defined(NODE_OPENSSL_CERT_STORE)
            " (default)"
#endif
            ,
            &PerProcessOptions::use_openssl_ca,
            kAllowedInEnvironment);
  AddOption("--use-bundled-ca",
            "use bundled CA store"
#if !defined(NODE_OPENSSL_CERT_STORE)
            " (default)"
#endif
            ,
            &PerProcessOptions::use_bundled_ca,
            kAllowedInEnvironment);
  // Similar to [has_eval_string] above, except that the separation between
  // this and use_openssl_ca only exists for option validation after parsing.
  // This is not ideal.
  AddOption("[ssl_openssl_cert_store]",
            "",
            &PerProcessOptions::ssl_openssl_cert_store);
  Implies("--use-openssl-ca", "[ssl_openssl_cert_store]");
  ImpliesNot("--use-bundled-ca", "[ssl_openssl_cert_store]");
#if NODE_FIPS_MODE
  AddOption("--enable-fips",
            "enable FIPS crypto at startup",
            &PerProcessOptions::enable_fips_crypto,
            kAllowedInEnvironment);
  AddOption("--force-fips",
            "force FIPS crypto (cannot be disabled)",
            &PerProcessOptions::force_fips_crypto,
            kAllowedInEnvironment);
#endif
#endif

  AddOption("--jvm", "", V8Option{}, kAllowedInEnvironment);
  AddOption("--native", "", V8Option{}, kAllowedInEnvironment);

  Insert(iop, &PerProcessOptions::get_per_isolate_options);
}

inline std::string RemoveBrackets(const std::string& host) {
  if (!host.empty() && host.front() == '[' && host.back() == ']')
    return host.substr(1, host.size() - 2);
  else
    return host;
}

inline int ParseAndValidatePort(const std::string& port,
                                std::vector<std::string>* errors) {
  char* endptr;
  errno = 0;
  const unsigned long result =                 // NOLINT(runtime/int)
    strtoul(port.c_str(), &endptr, 10);
  if (errno != 0 || *endptr != '\0'||
      (result != 0 && result < 1024) || result > 65535) {
    errors->push_back(" must be 0 or in range 1024 to 65535.");
  }
  return static_cast<int>(result);
}

HostPort SplitHostPort(const std::string& arg,
                      std::vector<std::string>* errors) {
  // remove_brackets only works if no port is specified
  // so if it has an effect only an IPv6 address was specified.
  std::string host = RemoveBrackets(arg);
  if (host.length() < arg.length())
    return HostPort{host, DebugOptions::kDefaultInspectorPort};

  size_t colon = arg.rfind(':');
  if (colon == std::string::npos) {
    // Either a port number or a host name.  Assume that
    // if it's not all decimal digits, it's a host name.
    for (char c : arg) {
      if (c < '0' || c > '9') {
        return HostPort{arg, DebugOptions::kDefaultInspectorPort};
      }
    }
    return HostPort { "", ParseAndValidatePort(arg, errors) };
  }
  // Host and port found:
  return HostPort { RemoveBrackets(arg.substr(0, colon)),
                    ParseAndValidatePort(arg.substr(colon + 1), errors) };
}

// Return a map containing all the options and their metadata as well
// as the aliases
void GetOptions(const FunctionCallbackInfo<Value>& args) {
  Mutex::ScopedLock lock(per_process::cli_options_mutex);
  Environment* env = Environment::GetCurrent(args);
  if (!env->has_run_bootstrapping_code()) {
    // No code because this is an assertion.
    return env->ThrowError(
        "Should not query options before bootstrapping is done");
  }
  env->set_has_serialized_options(true);

  Isolate* isolate = env->isolate();
  Local<Context> context = env->context();

  // Temporarily act as if the current Environment's/IsolateData's options were
  // the default options, i.e. like they are the ones we'd access for global
  // options parsing, so that all options are available from the main parser.
  auto original_per_isolate = per_process::cli_options->per_isolate;
  per_process::cli_options->per_isolate = env->isolate_data()->options();
  auto original_per_env = per_process::cli_options->per_isolate->per_env;
  per_process::cli_options->per_isolate->per_env = env->options();
  OnScopeLeave on_scope_leave([&]() {
    per_process::cli_options->per_isolate->per_env = original_per_env;
    per_process::cli_options->per_isolate = original_per_isolate;
  });

  Local<Map> options = Map::New(isolate);
  for (const auto& item : _ppop_instance.options_) {
    Local<Value> value;
    const auto& option_info = item.second;
    auto field = option_info.field;
    PerProcessOptions* opts = per_process::cli_options.get();
    switch (option_info.type) {
      case kNoOp:
      case kV8Option:
        // Special case for --abort-on-uncaught-exception which is also
        // respected by Node.js internals
        if (item.first == "--abort-on-uncaught-exception") {
          value = Boolean::New(
            isolate, original_per_env->abort_on_uncaught_exception);
        } else {
          value = Undefined(isolate);
        }
        break;
      case kBoolean:
        value = Boolean::New(isolate,
                             *_ppop_instance.Lookup<bool>(field, opts));
        break;
      case kInteger:
        value = Number::New(isolate,
                            *_ppop_instance.Lookup<int64_t>(field, opts));
        break;
      case kUInteger:
        value = Number::New(isolate,
                            *_ppop_instance.Lookup<uint64_t>(field, opts));
        break;
      case kString:
        if (!ToV8Value(context,
                       *_ppop_instance.Lookup<std::string>(field, opts))
                 .ToLocal(&value)) {
          return;
        }
        break;
      case kStringList:
        if (!ToV8Value(context,
                       *_ppop_instance.Lookup<StringVector>(field, opts))
                 .ToLocal(&value)) {
          return;
        }
        break;
      case kHostPort: {
        const HostPort& host_port =
          *_ppop_instance.Lookup<HostPort>(field, opts);
        Local<Object> obj = Object::New(isolate);
        Local<Value> host;
        if (!ToV8Value(context, host_port.host()).ToLocal(&host) ||
            obj->Set(context, env->host_string(), host).IsNothing() ||
            obj->Set(context,
                     env->port_string(),
                     Integer::New(isolate, host_port.port()))
                .IsNothing()) {
          return;
        }
        value = obj;
        break;
      }
      default:
        UNREACHABLE();
    }
    CHECK(!value.IsEmpty());

    Local<Value> name = ToV8Value(context, item.first).ToLocalChecked();
    Local<Object> info = Object::New(isolate);
    Local<Value> help_text;
    if (!ToV8Value(context, option_info.help_text).ToLocal(&help_text) ||
        !info->Set(context, env->help_text_string(), help_text)
             .FromMaybe(false) ||
        !info->Set(context,
                   env->env_var_settings_string(),
                   Integer::New(isolate,
                                static_cast<int>(option_info.env_setting)))
             .FromMaybe(false) ||
        !info->Set(context,
                   env->type_string(),
                   Integer::New(isolate, static_cast<int>(option_info.type)))
             .FromMaybe(false) ||
        info->Set(context, env->value_string(), value).IsNothing() ||
        options->Set(context, name, info).IsEmpty()) {
      return;
    }
  }

  Local<Value> aliases;
  if (!ToV8Value(context, _ppop_instance.aliases_).ToLocal(&aliases)) return;

  Local<Object> ret = Object::New(isolate);
  if (ret->Set(context, env->options_string(), options).IsNothing() ||
      ret->Set(context, env->aliases_string(), aliases).IsNothing()) {
    return;
  }

  args.GetReturnValue().Set(ret);
}

void Initialize(Local<Object> target,
                Local<Value> unused,
                Local<Context> context,
                void* priv) {
  Environment* env = Environment::GetCurrent(context);
  Isolate* isolate = env->isolate();
  env->SetMethodNoSideEffect(target, "getOptions", GetOptions);

  Local<Object> env_settings = Object::New(isolate);
  NODE_DEFINE_CONSTANT(env_settings, kAllowedInEnvironment);
  NODE_DEFINE_CONSTANT(env_settings, kDisallowedInEnvironment);
  target
      ->Set(
          context, FIXED_ONE_BYTE_STRING(isolate, "envSettings"), env_settings)
      .Check();

  Local<Object> types = Object::New(isolate);
  NODE_DEFINE_CONSTANT(types, kNoOp);
  NODE_DEFINE_CONSTANT(types, kV8Option);
  NODE_DEFINE_CONSTANT(types, kBoolean);
  NODE_DEFINE_CONSTANT(types, kInteger);
  NODE_DEFINE_CONSTANT(types, kUInteger);
  NODE_DEFINE_CONSTANT(types, kString);
  NODE_DEFINE_CONSTANT(types, kHostPort);
  NODE_DEFINE_CONSTANT(types, kStringList);
  target->Set(context, FIXED_ONE_BYTE_STRING(isolate, "types"), types)
      .Check();
}

}  // namespace options_parser
}  // namespace node

NODE_MODULE_CONTEXT_AWARE_INTERNAL(options, node::options_parser::Initialize)
