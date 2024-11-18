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

#include "callbacks.h"
#include "graal_boolean.h"
#include "graal_context.h"
#include "graal_function.h"
#include "graal_isolate.h"
#include "graal_missing_primitive.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_string.h"
#include "uv.h"
#include <algorithm>
#include <stdlib.h>
#include <string>
#include <string.h>
#include <tuple>

#include "graal_array-inl.h"
#include "graal_boolean-inl.h"
#include "graal_context-inl.h"
#include "graal_external-inl.h"
#include "graal_function-inl.h"
#include "graal_missing_primitive-inl.h"
#include "graal_number-inl.h"
#include "graal_object-inl.h"
#include "graal_string-inl.h"

#if defined(__cpp_lib_filesystem)
#include <filesystem>
#elif defined(__POSIX__)
// for posix platforms we have fallback implementations based on C functions
#include <sys/stat.h> // stat
#include <dirent.h> // opendir,readdir,closedir
#else
#error "Missing <filesystem>"
#endif

#ifdef __POSIX__

#include <dlfcn.h>
#include <unistd.h>

#else

#include <io.h>

#endif

#ifdef __APPLE__
#define LIBNODESVM_NAME    "libgraal-nodejs.dylib"
#define LIBPOLYGLOT_NAME   "libpolyglot.dylib"
#define LIBNODESVM_RELPATH "/languages/nodejs/lib/" LIBNODESVM_NAME
#define LIBPOLYGLOT_RELPATH "/lib/polyglot/" LIBPOLYGLOT_NAME
#define LIBJVM_RELPATH     "/lib/server/libjvm.dylib"
// libjli.dylib has moved in JDK 12, see https://bugs.openjdk.java.net/browse/JDK-8210931
#define LIBJLI_RELPATH     "/lib/libjli.dylib"
#elif defined(_WIN32)
#define LIBNODESVM_NAME    "graal-nodejs.dll"
#define LIBPOLYGLOT_NAME   "polyglot.dll"
#define LIBNODESVM_RELPATH "\\languages\\nodejs\\lib\\" LIBNODESVM_NAME
#define LIBPOLYGLOT_RELPATH "\\lib\\polyglot\\" LIBPOLYGLOT_NAME
#define LIBJVM_RELPATH     "\\bin\\server\\jvm.dll"
#else
#define LIBNODESVM_NAME    "libgraal-nodejs.so"
#define LIBPOLYGLOT_NAME   "libpolyglot.so"
#define LIBNODESVM_RELPATH "/languages/nodejs/lib/" LIBNODESVM_NAME
#define LIBPOLYGLOT_RELPATH "/lib/polyglot/" LIBPOLYGLOT_NAME
#define LIBJVM_RELPATH     "/lib/server/libjvm.so"
#endif

#define EXIT_WITH_MESSAGE(env, message) { \
    fprintf(stderr, "%s", message); \
    if (env->ExceptionCheck()) { \
        env->ExceptionDescribe(); \
    } \
    exit(1); \
}

#ifdef __POSIX__
#define WEAK_ATTRIBUTE  __attribute__((weak))
#else
#define WEAK_ATTRIBUTE
#endif

extern "C" int uv_exepath(char* buffer, size_t* size) WEAK_ATTRIBUTE;
extern "C" int uv_key_create(uv_key_t* key) WEAK_ATTRIBUTE;
extern "C" void uv_key_set(uv_key_t* key, void* value) WEAK_ATTRIBUTE;
extern "C" void* uv_key_get(uv_key_t* key) WEAK_ATTRIBUTE;
extern "C" uv_loop_t* uv_default_loop(void) WEAK_ATTRIBUTE;
#ifdef __POSIX__
extern "C" int uv__cloexec(int fd, int set) WEAK_ATTRIBUTE;
#endif

#undef WEAK_ATTRIBUTE

// Key for the current (per-thread) isolate
static uv_key_t current_isolate_key;
static bool current_isolate_initialized = false;

GraalIsolate* CurrentIsolate() {
    return reinterpret_cast<GraalIsolate*> (uv_key_get(&current_isolate_key));
}

v8::Isolate* GraalIsolate::TryGetCurrent() {
    return current_isolate_initialized ? GetCurrent() : nullptr;
}

#define ACCESS_METHOD(id, name, signature) \
    jni_methods_[id] = jni_env_->GetMethodID(access_class_, name, signature); \
    if (jni_methods_[id] == NULL) { \
        fprintf(stderr, "Method %s not found!\n", name); \
        exit(1); \
    }

typedef jint(*InitJVM)(JavaVM **, void **, void *);
typedef jint(*CreatedJVMs)(JavaVM **vmBuffer, jsize bufferLength, jsize *written);

const jint REQUESTED_JNI_VERSION = JNI_VERSION_9;

#ifdef __POSIX__
    static const std::string file_separator = "/";
    static const std::string path_separator = ":";
#else
    static const std::string file_separator = "\\";
    static const std::string path_separator = ";";
#endif

std::string getstdenv(const char* var) {
    std::string ret;
    char* value = getenv(var);
    if (value != nullptr) {
        ret.assign(value);
    }
    return ret;
}

std::string nodeExe() {
    const int BUF_SIZE = 1024;
    char exepath[BUF_SIZE];
    size_t length = BUF_SIZE;
    if (uv_exepath(exepath, &length)) {
        fprintf(stderr, "Cannot get executable path.\n");
        exit(1);
    }
    return std::string(exepath, length);
}

std::string up(std::string path, int cnt = 1) {
    int at = path.length();
    while (cnt-- > 0) {
        at = path.find_last_of(file_separator, at - 1);
    }
    if (at >= 0) {
        return path.substr(0, at);
    }
    return "";
}

bool ends_with(std::string const & s, std::string const & end) {
    return (end.size() <= s.size()) && std::equal(end.rbegin(), end.rend(), s.rbegin());
}

jclass findClassExtra(JNIEnv* env, const char* name) {
    jclass loadedClass = env->FindClass(name);
    if (loadedClass == NULL) {
#if defined(DEBUG)
        env->ExceptionDescribe();
#endif
        env->ExceptionClear();
        jclass engineClass = env->FindClass("org/graalvm/polyglot/Engine");
        if (engineClass == NULL) {
            EXIT_WITH_MESSAGE(env, "org.graalvm.polyglot.Engine class not found!\n");
        }
        jmethodID loadLanguageClassID = env->GetStaticMethodID(engineClass, "loadLanguageClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        if (loadLanguageClassID == NULL) {
            EXIT_WITH_MESSAGE(env, "Engine.loadLanguageClass(String) doesn't exist!\n");
        }

        std::string dotName = name;
        std::replace(dotName.begin(), dotName.end(), '/', '.');

        jstring dotNameString = env->NewStringUTF(dotName.c_str());
        loadedClass = (jclass) env->CallStaticObjectMethod(engineClass, loadLanguageClassID, dotNameString);
        if (loadedClass == NULL) {
            std::string msg = dotName;
            msg.append(" class not found!\n");
            EXIT_WITH_MESSAGE(env, msg.c_str())
        }
    }
    return loadedClass;
}

#ifndef __POSIX__
#define access _access
#endif

bool file_exists(std::string const& path) {
    return access(path.c_str(), F_OK) == 0;
}

bool is_directory(std::string const& path) {
#ifdef __cpp_lib_filesystem
    return std::filesystem::is_directory(path);
#else
    struct stat st;
    if (stat(path.c_str(), &st) == 0) {
        return S_ISDIR(st.st_mode);
    }
    return false;
#endif
}

std::string expand_class_or_module_path(std::string const& modules_dir, bool include_dir = true, bool include_jars = true) {
    std::string sep = "";
    std::string module_path;
    if (is_directory(modules_dir)) {
        if (include_dir) {
            module_path.append(sep);
            module_path.append(modules_dir);
            sep = path_separator;
        }
        if (include_jars) {
#ifdef __cpp_lib_filesystem
            for (auto const& entry : std::filesystem::directory_iterator(modules_dir)) {
                if (entry.path().extension().string() == ".jar") {
                    module_path.append(sep);
                    module_path.append(entry.path().string());
                    sep = path_separator;
                }
            }
#else
            DIR* dir = opendir(modules_dir.c_str());
            if (dir != nullptr) {
                dirent* entry;
                while ((entry = readdir(dir)) != nullptr) {
                    std::string entry_path = modules_dir + file_separator + entry->d_name;
                    if (ends_with(entry_path, ".jar")) {
                        module_path.append(sep);
                        module_path.append(entry_path);
                        sep = path_separator;
                    }
                }
                closedir(dir);
            }
#endif
        }
    }
    return module_path;
}

// Workaround for a bug in SVM's JNI_GetCreatedJavaVMs
static JavaVM* existing_jvm = nullptr;

v8::Isolate* GraalIsolate::New(v8::Isolate::CreateParams const& params, v8::Isolate* placement) {
    JavaVM *jvm;
    JNIEnv *env;

    std::string node_exe = nodeExe();
    std::string node_bin_path = up(node_exe);
    std::string jdk_path;
    std::string jvmlib_path;
    std::string standalone_home;
    bool is_graalvm = ends_with(node_bin_path, file_separator + "languages" + file_separator + "nodejs" + file_separator + "bin");
    bool is_jvm_standalone = false;
    int effective_mode = kModeDefault;
    if (is_graalvm) {
        // Part of GraalVM: take precedence over any JAVA_HOME.
        // We set environment variables to ensure these values are correctly
        // propagated to child processes.
        std::string graalvm_home = up(node_exe, 4); // ${graalvm_home}/languages/nodejs/bin/node
        jdk_path = graalvm_home;
        SetEnv("JAVA_HOME", jdk_path.c_str());

        jvmlib_path = graalvm_home + (polyglot ? LIBPOLYGLOT_RELPATH : LIBNODESVM_RELPATH);

        if (mode == kModeNative || (mode == kModeDefault && file_exists(jvmlib_path))) {
            SetEnv("NODE_JVM_LIB", jvmlib_path.c_str());
            effective_mode = kModeNative;
        } else {
            // mode == kModeJVM || (mode == kModeDefault && !file_exists(jvmlib_path))
            std::string node_jvm_lib = getstdenv("NODE_JVM_LIB");
            if (!node_jvm_lib.empty()) {
                jvmlib_path = node_jvm_lib;
            } else {
                // Use JAVA_HOME based libjvm path.
                jvmlib_path = jdk_path + LIBJVM_RELPATH;
                SetEnv("NODE_JVM_LIB", jvmlib_path.c_str());
                effective_mode = kModeJVM;
            }
        }
    } else {
        // Assume standalone distribution with libs in ../lib.
        standalone_home = up(node_exe, 2); // ${standalone_home}/bin/node

        // SVM standalone
        jvmlib_path = standalone_home + file_separator + "lib" + file_separator + LIBNODESVM_NAME;
        // JVM standalone has a JDK in ${standalone_home}/jvm

        if (mode == kModeNative || (mode == kModeDefault && file_exists(jvmlib_path))) {
            SetEnv("NODE_JVM_LIB", jvmlib_path.c_str());
            effective_mode = kModeNative;
        } else {
            // mode == kModeJVM || (mode == kModeDefault && !file_exists(jvmlib_path))
            std::string jvm_subdir = standalone_home + file_separator + "jvm";
            std::string jvm_subdir_libjvm = jvm_subdir + LIBJVM_RELPATH;
            if (file_exists(jvm_subdir_libjvm)) {
                is_jvm_standalone = true;
                jdk_path = jvm_subdir;
                jvmlib_path = jvm_subdir_libjvm;
                SetEnv("JAVA_HOME", jvm_subdir.c_str());
                SetEnv("NODE_JVM_LIB", jvm_subdir_libjvm.c_str());
                effective_mode = kModeJVM;
            } else {
                // Try using JAVA_HOME/NODE_JVM_LIB or fail
                jdk_path = getstdenv("JAVA_HOME");
                std::string node_jvm_lib = getstdenv("NODE_JVM_LIB");
                if (!node_jvm_lib.empty()) {
                    jvmlib_path = node_jvm_lib;
                } else if (!jdk_path.empty()) {
                    jvmlib_path = jdk_path + LIBJVM_RELPATH;
                    SetEnv("NODE_JVM_LIB", jvmlib_path.c_str());
                    effective_mode = kModeJVM;
                } else if (mode == kModeJVM) {
                    fprintf(stderr, "JAVA_HOME is not set. Specify JAVA_HOME so $JAVA_HOME%s exists.\n", LIBJVM_RELPATH);
                    exit(1);
                }
            }
        }
    }

    std::string verbose_graalvm_launchers_str = getstdenv("VERBOSE_GRAALVM_LAUNCHERS");
    bool verbose_graalvm_launchers = verbose_graalvm_launchers_str == "true";
    if (verbose_graalvm_launchers) {
        fprintf(stderr, "mode: %s\n", effective_mode == kModeNative ? "native" : effective_mode == kModeJVM ? "jvm" : "default");
    }

    if (!file_exists(jvmlib_path)) {
        if (is_graalvm && polyglot) {
            fprintf(stderr, "Cannot find %s. Rebuild the polyglot library with `gu rebuild-images libpolyglot`, specify JAVA_HOME so that $JAVA_HOME%s exists, or specify NODE_JVM_LIB directly.\n", jvmlib_path.c_str(), LIBJVM_RELPATH);
        } else if (mode == kModeNative) {
            fprintf(stderr, "Cannot find %s. Specify NODE_JVM_LIB directly.\n", jvmlib_path.c_str());
        } else if (mode == kModeJVM) {
            fprintf(stderr, "Cannot find %s. Specify JAVA_HOME so that $JAVA_HOME%s exists.\n", jvmlib_path.c_str(), LIBJVM_RELPATH);
        } else {
            // mode == kModeDefault
            fprintf(stderr, "Cannot find %s. Specify JAVA_HOME so that $JAVA_HOME%s exists, or specify NODE_JVM_LIB directly.\n", jvmlib_path.c_str(), LIBJVM_RELPATH);
        }
        exit(1);
    }

#ifdef __POSIX__
    void* jvm_handle = dlopen(jvmlib_path.c_str(), RTLD_NOW);
    if (jvm_handle == NULL) {
        fprintf(stderr, "jvm library could not be loaded: %s\n", dlerror());
        exit(1);
    }

    // Check if there already is a JVM created
    CreatedJVMs createdJVMs = (CreatedJVMs) dlsym(jvm_handle, "JNI_GetCreatedJavaVMs");
    if (createdJVMs == NULL) {
        fprintf(stderr, "JNI_GetCreatedJavaVMs symbol could not be resolved: %s\n", dlerror());
        exit(1);
    }
#else
    HMODULE jvm_handle = LoadLibraryA(jvmlib_path.c_str());
    if (jvm_handle == NULL) {
        fprintf(stderr, "jvm library could not be loaded: %lu\n", GetLastError());
        exit(1);
    }

    CreatedJVMs createdJVMs = (CreatedJVMs) GetProcAddress(jvm_handle, "JNI_GetCreatedJavaVMs");
    if (createdJVMs == NULL) {
        fprintf(stderr, "JNI_GetCreatedJavaVMs symbol could not be resolved: %lu\n", GetLastError());
        exit(1);
    }
#endif
    jsize existingJVMs;
    createdJVMs(&jvm, 1, &existingJVMs);
    bool spawn_jvm = (existingJVMs == 0);

    if (spawn_jvm && existing_jvm) {
        // Workaround for a bug in SVM's JNI_GetCreatedJavaVMs
        jvm = existing_jvm;
        spawn_jvm = false;
    }

    if (spawn_jvm) {
        std::vector<JavaVMOption> options;
        std::string graal_sdk_jar_path = getstdenv("GRAAL_SDK_JAR_PATH");
        if (!graal_sdk_jar_path.empty() && access(graal_sdk_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update GRAAL_SDK_JAR_PATH environment variable!\n", graal_sdk_jar_path.c_str());
            exit(1);
        }
        std::string graaljs_jar_path = getstdenv("GRAALJS_JAR_PATH");
        if (!graaljs_jar_path.empty() && access(graaljs_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update GRAALJS_JAR_PATH environment variable!\n", graaljs_jar_path.c_str());
            exit(1);
        }
        std::string graalnode_jar_path = getstdenv("TRUFFLENODE_JAR_PATH");
        if (!graalnode_jar_path.empty() && access(graalnode_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update TRUFFLENODE_JAR_PATH environment variable!\n", graalnode_jar_path.c_str());
            exit(1);
        }

        std::string extra_jvm_path = getstdenv("NODE_JVM_CLASSPATH");
        if (use_classpath_env_var) {
            std::string classpath = getstdenv("CLASSPATH");
            if (!classpath.empty()) {
                if (!extra_jvm_path.empty()) {
                    extra_jvm_path += path_separator;
                }
                extra_jvm_path += classpath;
            }
        }

        EnsureValidWorkingDir();

        std::string boot_classpath = getstdenv("NODE_JVM_BOOTCLASSPATH");
        if (boot_classpath.empty()) {
            if (!graal_sdk_jar_path.empty()) {
                boot_classpath += path_separator;
                boot_classpath += graal_sdk_jar_path;
            }
        } else {
            boot_classpath = path_separator + boot_classpath;
        }
        if (!boot_classpath.empty()) {
            boot_classpath = "-Xbootclasspath/a:" + boot_classpath.substr(1);
            options.push_back({const_cast<char*>(boot_classpath.c_str()), nullptr});
        }

        std::string module_path = "";
        std::string module_path_sep = "";
        if (is_jvm_standalone) {
            std::string standalone_modules_dir = standalone_home + file_separator + "modules";
            if (is_directory(standalone_modules_dir)) {
                module_path += module_path_sep;
                module_path += standalone_modules_dir;
                module_path_sep = path_separator;
            }
        }
        std::string env_module_path = getstdenv("NODE_JVM_MODULE_PATH");
        if (!env_module_path.empty()) {
            module_path += module_path_sep;
            module_path += env_module_path;
            module_path_sep = path_separator;
        }
        if (!module_path.empty()) {
            module_path = "--module-path=" + module_path;
            options.push_back({const_cast<char*>(module_path.c_str()), nullptr});
            options.push_back({const_cast<char*>("--add-modules=org.graalvm.nodejs"), nullptr});
        }

        std::string classpath = "";
        std::string classpath_sep = "";
        if (!graaljs_jar_path.empty()) {
            classpath += graaljs_jar_path;
            classpath_sep = path_separator;
        }
        if (!graalnode_jar_path.empty()) {
            classpath += classpath_sep;
            classpath += graalnode_jar_path;
            classpath_sep = path_separator;
        }
        if (is_jvm_standalone) {
            std::string standalone_jars_path = standalone_home + file_separator + "jars";
            std::string standalone_classpath = expand_class_or_module_path(standalone_jars_path, false);
            if (!standalone_classpath.empty()) {
                classpath += classpath_sep;
                classpath += standalone_classpath;
                classpath_sep = path_separator;
            }
        }
        if (!extra_jvm_path.empty()) {
            classpath += classpath_sep;
            classpath += extra_jvm_path;
            classpath_sep = path_separator;
        }
        if (!classpath.empty()) {
            classpath = "-Djava.class.path=" + classpath;
            options.push_back({const_cast<char*>(classpath.c_str()), nullptr});
        }

    // The object model requires a lot of malloc memory; we must shift he heap base further away
    // to give room for the native heap GR-1570
    #if defined(__sparc__)
        options.push_back({const_cast<char*>("-XX:HeapBaseMinAddress=24g"), nullptr});
    #endif

        // Set process name (it would be shown in jcmd, jps)
        options.push_back({const_cast<char*>("-Dsun.java.command=node"), nullptr});

        options.push_back({const_cast<char*>("--enable-native-access=org.graalvm.truffle"), nullptr});

    #if defined(DEBUG)
        std::string debugPort = getstdenv("DEBUG_PORT");
        std::string debugParam;
        if (!debugPort.empty()) {
            // do not debug child processes
            UnsetEnv("DEBUG_PORT");
            debugParam = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort;
            options.push_back({const_cast<char*>(debugParam.c_str()), nullptr});
        }
        options.push_back({const_cast<char*>("-Dtruffle.node.js.verbose=true"), nullptr});
    #endif

        const char* no_spawn_options = "NODE_JVM_OPTIONS_NO_SPAWN";
        const char* option_names[2] = {"NODE_JVM_OPTIONS", no_spawn_options};
        for (int i = 0; i < 2; i++) {
            const char* jvm_options = getenv(option_names[i]);
            if (jvm_options != nullptr) {
                // value returned by getenv should not be modified
                // strtok modifies the given string => we make a copy
                size_t jvm_options_length = strlen(jvm_options) + 1;
                char* jvm_options_copy = new char[jvm_options_length];
                strncpy(jvm_options_copy, jvm_options, jvm_options_length);

                const char* delimiters = " ";
                char* option = strtok(jvm_options_copy, delimiters);
                while (option != nullptr) {
                    if (strcmp(option, "-help")) {
                        options.push_back({option, nullptr});
                    } else {
                        // -help is not supported by JNI_CreateJavaVM
                        if (mode == GraalIsolate::kModeJVM) {
#ifdef __POSIX__
                            // Re-enable inheritance of stdout and stderr disabled
                            // by uv_disable_stdio_inheritance() in node::Init()
                            uv__cloexec(1, 0);
                            uv__cloexec(2, 0);
#endif
                            // Delegate to java -help
                            std::string java = jdk_path + file_separator + "bin" + file_separator + "java";
                            char * argv[] = {const_cast<char*> (java.c_str()), (char*) "-help", nullptr};
                            execv(java.c_str(), argv);
                            perror(java.c_str());
                            exit(errno);
                        }
                    }
                    option = strtok(nullptr, delimiters);
                }
            }
        }
        UnsetEnv(no_spawn_options);

    #if __APPLE__
        if (!jdk_path.empty()) {
            if (dlopen((jdk_path + LIBJLI_RELPATH).c_str(), RTLD_NOW) == NULL) {
                fprintf(stderr, "warning: could not load libjli: %s\n", dlerror());
            }
        }
    #endif

        if (verbose_graalvm_launchers) {
            fprintf(stderr, "load: %s", jvmlib_path.c_str());
            for (auto i = 0; i < options.size(); i++) {
                fprintf(stderr, " %s", options[i].optionString);
            }
            fprintf(stderr, "\n");
        }

        JavaVMInitArgs vm_args;
        vm_args.version = REQUESTED_JNI_VERSION;
        vm_args.nOptions = options.size();
        vm_args.options = options.data();
        vm_args.ignoreUnrecognized = false;

#ifdef __POSIX__
        InitJVM createJvm = (InitJVM) dlsym(jvm_handle, "JNI_CreateJavaVM");
        if (createJvm == NULL) {
            fprintf(stderr, "JNI_CreateJavaVM symbol could not be resolved: %s\n", dlerror());
            exit(1);
        }
#else
        InitJVM createJvm = (InitJVM) GetProcAddress(jvm_handle, "JNI_CreateJavaVM");
        if (createJvm == NULL) {
            fprintf(stderr, "JNI_CreateJavaVM symbol could not be resolved: %lu\n", GetLastError());
            exit(1);
        }
#endif
        jint result = createJvm(&jvm, (void**) &env, &vm_args);
        if (result != JNI_OK) {
            fprintf(stderr, "Creation of the JVM failed!\n");
            exit(1);
        }
        existing_jvm = jvm;

        jclass callback_class = findClassExtra(env, "com/oracle/truffle/trufflenode/NativeAccess");
        if (!RegisterCallbacks(env, callback_class)) {
            exit(1);
        }
    } else {
        if (jvm->GetEnv(reinterpret_cast<void**> (&env), REQUESTED_JNI_VERSION) == JNI_EDETACHED) {
            jvm->AttachCurrentThread(reinterpret_cast<void**> (&env), nullptr);
        }
    }

    internal_error_check_ = !getstdenv("NODE_INTERNAL_ERROR_CHECK").empty();

    GraalIsolate* isolate;
    if (placement == nullptr) {
        isolate = new GraalIsolate(jvm, env, params);
    } else {
        isolate = new(placement) GraalIsolate(jvm, env, params);
    }

    isolate->main_ = spawn_jvm;
    if (spawn_jvm) {
        isolate->InitStackOverflowCheck((intptr_t) &jvm);
    }

    return reinterpret_cast<v8::Isolate*> (isolate);
}

#undef access

GraalIsolate::GraalIsolate(JavaVM* jvm, JNIEnv* env, v8::Isolate::CreateParams const& params) : function_template_data(), function_template_callbacks(), jvm_(jvm), jni_env_(env), jni_methods_(), jni_fields_(),
    message_listener_(nullptr), function_template_count_(0), promise_hook_(nullptr), promise_reject_callback_(nullptr), import_meta_initializer(nullptr), import_module_dynamically(nullptr),
    fatal_error_handler_(nullptr), prepare_stack_trace_callback_(nullptr), wasm_streaming_callback_(nullptr) {

#ifdef __POSIX__
    lock_ = PTHREAD_MUTEX_INITIALIZER;
#else
    lock_ = CreateMutex(NULL, false, NULL);
#endif

    array_pool_ =  new GraalObjectPool<GraalArray>();
    context_pool_ = new GraalObjectPool<GraalContext>();
    external_pool_ = new GraalObjectPool<GraalExternal>();
    function_pool_ = new GraalObjectPool<GraalFunction>();
    number_pool_ = new GraalObjectPool<GraalNumber>();
    object_pool_ = new GraalObjectPool<GraalObject>();
    string_pool_ = new GraalObjectPool<GraalString>();

    // Object.class
    jclass object_class = env->FindClass("java/lang/Object");
    object_class_ = (jclass) env->NewGlobalRef(object_class);

    // Boolean.TRUE, Boolean.FALSE
    jclass boolean_class = env->FindClass("java/lang/Boolean");
    jfieldID boolean_true_id = env->GetStaticFieldID(boolean_class, "TRUE", "Ljava/lang/Boolean;");
    jfieldID boolean_false_id = env->GetStaticFieldID(boolean_class, "FALSE", "Ljava/lang/Boolean;");
    jobject boolean_true = env->GetStaticObjectField(boolean_class, boolean_true_id);
    jobject boolean_false = env->GetStaticObjectField(boolean_class, boolean_false_id);
    boolean_true_ = env->NewGlobalRef(boolean_true);
    boolean_false_ = env->NewGlobalRef(boolean_false);

    // Arguments
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray args = env->NewObjectArray(GraalIsolate::argc, string_class, nullptr);
    for (int i = 0; i < GraalIsolate::argc; i++) {
        jstring arg = env->NewStringUTF(GraalIsolate::argv[i]);
        env->SetObjectArrayElement(args, i, arg);
    }

    // Graal.js access
    jclass access_class = findClassExtra(env, "com/oracle/truffle/trufflenode/GraalJSAccess");
    jmethodID createID = env->GetStaticMethodID(access_class, "create", "([Ljava/lang/String;)Ljava/lang/Object;");
    if (createID == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.create(String[],long) method not found!\n")
    jobject access = env->functions->CallStaticObjectMethod(env, access_class, createID, args);
    if (access == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.create() failed!\n")
    access_class_ = (jclass) env->NewGlobalRef(access_class);
    access_ = env->NewGlobalRef(access);

    // Shared buffer
    jfieldID shared_buffer_id = env->GetFieldID(access_class, "sharedBuffer", "Ljava/nio/ByteBuffer;");
    if (shared_buffer_id == NULL) EXIT_WITH_MESSAGE(env, "GraalAccess.sharedBuffer field not found!\n")
    jobject shared_buffer = env->GetObjectField(access_, shared_buffer_id);
    shared_buffer_ = env->GetDirectBufferAddress(shared_buffer);
    ResetSharedBuffer();

    ACCESS_METHOD(GraalAccessMethod::undefined_instance, "undefinedInstance", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::null_instance, "nullInstance", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_type, "valueType", "(Ljava/lang/Object;)I");
    ACCESS_METHOD(GraalAccessMethod::value_double, "valueDouble", "(Ljava/lang/Object;)D")
    ACCESS_METHOD(GraalAccessMethod::value_string, "valueFlatten", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_external, "valueExternal", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::value_unknown, "valueUnknown", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_object, "valueToObject", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_string, "valueToString", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_integer, "valueToInteger", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_int32, "valueToInt32", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_uint32, "valueToUint32", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_number, "valueToNumber", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_boolean, "valueToBoolean", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_to_array_index, "valueToArrayIndex", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_int32_value, "valueInt32Value", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::value_uint32_value, "valueUint32Value", "(Ljava/lang/Object;)D")
    ACCESS_METHOD(GraalAccessMethod::value_integer_value, "valueIntegerValue", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::value_is_native_error, "valueIsNativeError", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_set_iterator, "valueIsSetIterator", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_map_iterator, "valueIsMapIterator", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_shared_array_buffer, "valueIsSharedArrayBuffer", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_arguments_object, "valueIsArgumentsObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_boolean_object, "valueIsBooleanObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_number_object, "valueIsNumberObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_string_object, "valueIsStringObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_symbol_object, "valueIsSymbolObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_big_int_object, "valueIsBigIntObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_weak_map, "valueIsWeakMap", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_weak_set, "valueIsWeakSet", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_async_function, "valueIsAsyncFunction", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_generator_function, "valueIsGeneratorFunction", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_generator_object, "valueIsGeneratorObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_module_namespace_object, "valueIsModuleNamespaceObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_is_wasm_memory_object, "valueIsWasmMemoryObject", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_equals, "valueEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_strict_equals, "valueStrictEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_instance_of, "valueInstanceOf", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_type_of, "valueTypeOf", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_detail_string, "valueToDetailString", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_new, "objectNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set, "objectSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_index, "objectSetIndex", "(Ljava/lang/Object;ILjava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_private, "objectSetPrivate", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_force_set, "objectForceSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)Z")
    ACCESS_METHOD(GraalAccessMethod::object_get, "objectGet", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_index, "objectGetIndex", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_private, "objectGetPrivate", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_real_named_property, "objectGetRealNamedProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_real_named_property_attributes, "objectGetRealNamedPropertyAttributes", "(Ljava/lang/Object;Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::object_get_own_property_descriptor, "objectGetOwnPropertyDescriptor", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_has, "objectHas", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_has_own_property, "objectHasOwnProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_has_private, "objectHasPrivate", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_has_real_named_property, "objectHasRealNamedProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete, "objectDelete", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete_index, "objectDelete", "(Ljava/lang/Object;J)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete_private, "objectDeletePrivate", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_accessor, "objectSetAccessor", "(Ljava/lang/Object;Ljava/lang/Object;JJLjava/lang/Object;I)Z")
    ACCESS_METHOD(GraalAccessMethod::object_clone, "objectClone", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_prototype, "objectGetPrototype", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set_prototype, "objectSetPrototype", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_get_constructor_name, "objectGetConstructorName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_property_names, "objectGetPropertyNames", "(Ljava/lang/Object;ZZZZZZZZ)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_own_property_names, "objectGetOwnPropertyNames", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_creation_context, "objectCreationContext", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_create_data_property, "objectCreateDataProperty", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_create_data_property_index, "objectCreateDataProperty", "(Ljava/lang/Object;JLjava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_define_property, "objectDefineProperty", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ZZZZZZ)Z")
    ACCESS_METHOD(GraalAccessMethod::object_preview_entries, "objectPreviewEntries", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set_integrity_level, "objectSetIntegrityLevel", "(Ljava/lang/Object;Z)V")
    ACCESS_METHOD(GraalAccessMethod::object_is_constructor, "objectIsConstructor", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::array_new, "arrayNew", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_new_from_elements, "arrayNewFromElements", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_length, "arrayLength", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_byte_length, "arrayBufferByteLength", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_new, "arrayBufferNew", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_new_buffer, "arrayBufferNew", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_new_backing_store, "arrayBufferNewBackingStore", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_get_contents, "arrayBufferGetContents", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_buffer, "arrayBufferViewBuffer", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_byte_length, "arrayBufferViewByteLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_byte_offset, "arrayBufferViewByteOffset", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_detach, "arrayBufferDetach", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_was_detached, "arrayBufferWasDetached", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::typed_array_length, "typedArrayLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::uint8_array_new, "uint8ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::uint8_clamped_array_new, "uint8ClampedArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::int8_array_new, "int8ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::uint16_array_new, "uint16ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::int16_array_new, "int16ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::uint32_array_new, "uint32ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::int32_array_new, "int32ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::float32_array_new, "float32ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::float64_array_new, "float64ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::big_int64_array_new, "bigInt64ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::big_uint64_array_new, "bigUint64ArrayNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::data_view_new, "dataViewNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::external_new, "externalNew", "(Ljava/lang/Object;J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::integer_new, "integerNew", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::number_new, "numberNew", "(D)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::date_time_configuration_change_notification, "dateTimeConfigurationChangeNotification", "(ILjava/lang/String;)V")
    ACCESS_METHOD(GraalAccessMethod::date_new, "dateNew", "(Ljava/lang/Object;D)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::date_value_of, "dateValueOf", "(Ljava/lang/Object;)D")
    ACCESS_METHOD(GraalAccessMethod::exception_error, "exceptionError", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::exception_type_error, "exceptionTypeError", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::exception_syntax_error, "exceptionSyntaxError", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::exception_range_error, "exceptionRangeError", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::exception_reference_error, "exceptionReferenceError", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::exception_create_message, "exceptionCreateMessage", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_throw_exception, "isolateThrowException", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_run_microtasks, "isolateRunMicrotasks", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_internal_error_check, "isolateInternalErrorCheck", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_throw_stack_overflow_error, "isolateThrowStackOverflowError", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_heap_statistics, "isolateGetHeapStatistics", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_terminate_execution, "isolateTerminateExecution", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_cancel_terminate_execution, "isolateCancelTerminateExecution", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_is_execution_terminating, "isolateIsExecutionTerminating", "()Z")
    ACCESS_METHOD(GraalAccessMethod::isolate_request_interrupt, "isolateRequestInterrupt", "(JJ)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_int_placeholder, "isolateGetIntPlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_safe_int_placeholder, "isolateGetSafeIntPlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_double_placeholder, "isolateGetDoublePlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_dispose, "isolateDispose", "(ZI)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enter_polyglot_engine, "isolateEnterPolyglotEngine", "(JJJJJJ)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_perform_gc, "isolatePerformGC", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_promise_hook, "isolateEnablePromiseHook", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_promise_reject_callback, "isolateEnablePromiseRejectCallback", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_import_meta_initializer, "isolateEnableImportMetaInitializer", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_import_module_dynamically, "isolateEnableImportModuleDynamically", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_prepare_stack_trace_callback, "isolateEnablePrepareStackTraceCallback", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enter, "isolateEnter", "(J)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_exit, "isolateExit", "(J)J")
    ACCESS_METHOD(GraalAccessMethod::isolate_enqueue_microtask, "isolateEnqueueMicrotask", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_schedule_pause_on_next_statement, "isolateSchedulePauseOnNextStatement", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_measure_memory, "isolateMeasureMemory", "(Ljava/lang/Object;Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_set_task_runner, "isolateSetTaskRunner", "(J)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_execute_runnable, "isolateExecuteRunnable", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::template_set, "templateSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::template_set_accessor_property, "templateSetAccessorProperty", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_new, "objectTemplateNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_template_new_instance, "objectTemplateNewInstance", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_accessor, "objectTemplateSetAccessor", "(Ljava/lang/Object;Ljava/lang/Object;JJLjava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_handler, "objectTemplateSetHandler", "(Ljava/lang/Object;JJJJJJJLjava/lang/Object;ZZ)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_call_as_function_handler, "objectTemplateSetCallAsFunctionHandler", "(Ljava/lang/Object;IJLjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_internal_field_count, "objectTemplateSetInternalFieldCount", "(Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::function_new_instance, "functionNewInstance", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_set_name, "functionSetName", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_get_name, "functionGetName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call, "functionCall", "(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call0, "functionCall0", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call1, "functionCall1", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call2, "functionCall2", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call3, "functionCall3", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call4, "functionCall4", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call5, "functionCall5", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call6, "functionCall6", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call7, "functionCall7", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call8, "functionCall8", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_call9, "functionCall9", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_resource_name, "functionResourceName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_get_script_line_number, "functionGetScriptLineNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::function_get_script_column_number, "functionGetScriptColumnNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::function_template_new, "functionTemplateNew", "(IJLjava/lang/Object;Ljava/lang/Object;IZZ)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_set_class_name, "functionTemplateSetClassName", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_template_instance_template, "functionTemplateInstanceTemplate", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_prototype_template, "functionTemplatePrototypeTemplate", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_get_function, "functionTemplateGetFunction", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_has_instance, "functionTemplateHasInstance", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::function_template_set_call_handler, "functionTemplateSetCallHandler", "(Ljava/lang/Object;JLjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_template_inherit, "functionTemplateInherit", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_template_read_only_prototype, "functionTemplateReadOnlyPrototype", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::script_compile, "scriptCompile", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_run, "scriptRun", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_get_unbound_script, "scriptGetUnboundScript", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_compile, "unboundScriptCompile", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_bind_to_context, "unboundScriptBindToContext", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_get_id, "unboundScriptGetId", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_get_content, "unboundScriptGetContent", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_global, "contextGlobal", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_set_pointer_in_embedder_data, "contextSetPointerInEmbedderData", "(Ljava/lang/Object;IJ)V")
    ACCESS_METHOD(GraalAccessMethod::context_get_pointer_in_embedder_data, "contextGetPointerInEmbedderData", "(Ljava/lang/Object;I)J")
    ACCESS_METHOD(GraalAccessMethod::context_set_embedder_data, "contextSetEmbedderData", "(Ljava/lang/Object;ILjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::context_get_embedder_data, "contextGetEmbedderData", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::try_catch_exception, "tryCatchException", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::try_catch_has_terminated, "tryCatchHasTerminated", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::message_get_script_resource_name, "messageGetScriptResourceName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::message_get_line_number, "messageGetLineNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::message_get_source_line, "messageGetSourceLine", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::message_get_start_column, "messageGetStartColumn", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::message_get_stack_trace, "messageGetStackTrace", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::message_get_start_position, "messageGetStartPosition", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::message_get_end_position, "messageGetEndPosition", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::message_get, "messageGet", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_trace_current_stack_trace, "stackTraceCurrentStackTrace", "(I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_line_number, "stackFrameGetLineNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_column, "stackFrameGetColumn", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_script_name, "stackFrameGetScriptName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_function_name, "stackFrameGetFunctionName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_is_eval, "stackFrameIsEval", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::make_weak, "makeWeak", "(Ljava/lang/Object;JJJI)V")
    ACCESS_METHOD(GraalAccessMethod::clear_weak, "clearWeak", "(Ljava/lang/Object;J)J")
    ACCESS_METHOD(GraalAccessMethod::string_external_resource_callback, "stringExternalResourceCallback", "(Ljava/lang/Object;JJ)V")
    ACCESS_METHOD(GraalAccessMethod::context_new, "contextNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_set_security_token, "contextSetSecurityToken", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::context_get_security_token, "contextGetSecurityToken", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_get_extras_binding_object, "contextGetExtrasBindingObject", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_set_promise_hooks, "contextSetPromiseHooks", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::context_is_code_generation_from_strings_allowed, "contextIsCodeGenerationFromStringsAllowed", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::find_dynamic_object_fields, "findDynamicObjectFields", "(Ljava/lang/Object;)[Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::proxy_is_function, "proxyIsFunction", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::proxy_get_handler, "proxyGetHandler", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::proxy_get_target, "proxyGetTarget", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::boolean_object_new, "booleanObjectNew", "(Ljava/lang/Object;Z)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::boolean_object_value_of, "booleanObjectValueOf", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::regexp_new, "regexpNew", "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::regexp_get_source, "regexpGetSource", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::regexp_get_flags, "regexpGetFlags", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::string_empty, "stringEmpty", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_new, "stringNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_new_from_two_byte, "stringNewFromTwoByte", "(JI)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_length, "stringLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::string_equals, "stringEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::string_concat, "stringConcat", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_utf8_length, "stringUTF8Length", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::string_utf8_write, "stringUTF8Write", "(Ljava/lang/Object;JI)J")
    ACCESS_METHOD(GraalAccessMethod::string_write_one_byte, "stringWriteOneByte", "(Ljava/lang/Object;JII)I")
    ACCESS_METHOD(GraalAccessMethod::string_write, "stringWrite", "(Ljava/lang/Object;JII)I")
    ACCESS_METHOD(GraalAccessMethod::string_contains_only_one_byte, "stringContainsOnlyOneByte", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::string_object_new, "stringObjectNew", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_object_value_of, "stringObjectValueOf", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::number_object_new, "numberObjectNew", "(Ljava/lang/Object;D)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_internal_field_count, "objectInternalFieldCount", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::object_slow_get_aligned_pointer_from_internal_field, "objectSlowGetAlignedPointerFromInternalField", "(Ljava/lang/Object;I)J")
    ACCESS_METHOD(GraalAccessMethod::object_set_aligned_pointer_in_internal_field, "objectSetAlignedPointerInInternalField", "(Ljava/lang/Object;IJ)V")
    ACCESS_METHOD(GraalAccessMethod::object_slow_get_internal_field, "objectSlowGetInternalField", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set_internal_field, "objectSetInternalField", "(Ljava/lang/Object;ILjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::json_parse, "jsonParse", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::json_stringify, "jsonStringify", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_new, "symbolNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_name, "symbolName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_for, "symbolFor", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_for_api, "symbolForApi", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_async_iterator, "symbolGetAsyncIterator", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_has_instance, "symbolGetHasInstance", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_is_concat_spreadable, "symbolGetIsConcatSpreadable", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_iterator, "symbolGetIterator", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_match, "symbolGetMatch", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_replace, "symbolGetReplace", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_search, "symbolGetSearch", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_split, "symbolGetSplit", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_to_primitive, "symbolGetToPrimitive", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_to_string_tag, "symbolGetToStringTag", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_get_unscopables, "symbolGetUnscopables", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_private_for_api, "symbolPrivateForApi", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::symbol_private_new, "symbolPrivateNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_result, "promiseResult", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_state, "promiseState", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_new, "promiseResolverNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_reject, "promiseResolverReject", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_resolve, "promiseResolverResolve", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::module_compile, "moduleCompile", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_instantiate, "moduleInstantiate", "(Ljava/lang/Object;Ljava/lang/Object;J)V")
    ACCESS_METHOD(GraalAccessMethod::module_evaluate, "moduleEvaluate", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_get_status, "moduleGetStatus", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::module_get_namespace, "moduleGetNamespace", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_get_identity_hash, "moduleGetIdentityHash", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::module_get_exception, "moduleGetException", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_get_unbound_module_script, "moduleGetUnboundModuleScript", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_create_synthetic_module, "moduleCreateSyntheticModule", "(Ljava/lang/Object;[Ljava/lang/Object;J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_set_synthetic_module_export, "moduleSetSyntheticModuleExport", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::module_get_module_requests, "moduleGetModuleRequests", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_request_get_specifier, "moduleRequestGetSpecifier", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_request_get_import_assertions, "moduleRequestGetImportAssertions", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_or_module_get_resource_name, "scriptOrModuleGetResourceName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_or_module_get_host_defined_options, "scriptOrModuleGetHostDefinedOptions", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_new, "valueSerializerNew", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_release, "valueSerializerRelease", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_size, "valueSerializerSize", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_header, "valueSerializerWriteHeader", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_value, "valueSerializerWriteValue", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_uint32, "valueSerializerWriteUint32", "(Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_uint64, "valueSerializerWriteUint64", "(Ljava/lang/Object;J)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_double, "valueSerializerWriteDouble", "(Ljava/lang/Object;D)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_write_raw_bytes, "valueSerializerWriteRawBytes", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_set_treat_array_buffer_views_as_host_objects, "valueSerializerSetTreatArrayBufferViewsAsHostObjects", "(Ljava/lang/Object;Z)V")
    ACCESS_METHOD(GraalAccessMethod::value_serializer_transfer_array_buffer, "valueSerializerTransferArrayBuffer", "(Ljava/lang/Object;ILjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_new, "valueDeserializerNew", "(JLjava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_header, "valueDeserializerReadHeader", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_value, "valueDeserializerReadValue", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_uint32, "valueDeserializerReadUint32", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_uint64, "valueDeserializerReadUint64", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_double, "valueDeserializerReadDouble", "(Ljava/lang/Object;)D")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_read_raw_bytes, "valueDeserializerReadRawBytes", "(Ljava/lang/Object;I)I")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_transfer_array_buffer, "valueDeserializerTransferArrayBuffer", "(Ljava/lang/Object;ILjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::value_deserializer_get_wire_format_version, "valueDeserializerGetWireFormatVersion", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::map_new, "mapNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::map_set, "mapSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::set_new, "setNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::set_add, "setAdd", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::big_int_int64_value, "bigIntInt64Value", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::big_int_uint64_value, "bigIntUint64Value", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::big_int_new, "bigIntNew", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::big_int_new_from_unsigned, "bigIntNewFromUnsigned", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::big_int_new_from_words, "bigIntNewFromWords", "(II[J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::big_int_word_count, "bigIntWordCount", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::big_int_to_words_array, "bigIntToWordsArray", "(Ljava/lang/Object;I)[J")
    ACCESS_METHOD(GraalAccessMethod::shared_array_buffer_new, "sharedArrayBufferNew", "(Ljava/lang/Object;Ljava/lang/Object;J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::shared_array_buffer_get_contents, "sharedArrayBufferGetContents", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::shared_array_buffer_externalize, "sharedArrayBufferExternalize", "(Ljava/lang/Object;J)V")
    ACCESS_METHOD(GraalAccessMethod::script_compiler_compile_function_in_context, "scriptCompilerCompileFunctionInContext", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::backing_store_register_callback, "backingStoreRegisterCallback", "(Ljava/lang/Object;JIJJ)V")
    ACCESS_METHOD(GraalAccessMethod::fixed_array_length, "fixedArrayLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::fixed_array_get, "fixedArrayGet", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::shared_array_buffer_byte_length, "sharedArrayBufferByteLength", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::wasm_module_object_get_compiled_module, "wasmModuleObjectGetCompiledModule", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::wasm_module_object_from_compiled_module, "wasmModuleObjectFromCompiledModule", "(Ljava/lang/Object;)Ljava/lang/Object;")

    int root_offset = v8::internal::Internals::kIsolateRootsOffset / v8::internal::kApiSystemPointerSize;

    v8::HandleScope scope(reinterpret_cast<v8::Isolate*> (this));

    // Undefined
    JNI_CALL(jobject, java_undefined, this, GraalAccessMethod::undefined_instance, Object);
    if (java_undefined == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.undefinedInstance() failed!\n")
    GraalMissingPrimitive* undefined_local = GraalMissingPrimitive::Allocate(this, java_undefined, true);
    undefined_instance_ = reinterpret_cast<GraalPrimitive*> (undefined_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kUndefinedValueRootIndex] = undefined_instance_;

    // Null
    JNI_CALL(jobject, java_null, this, GraalAccessMethod::null_instance, Object);
    if (java_null == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.nullInstance() failed!\n")
    GraalMissingPrimitive* null_local = GraalMissingPrimitive::Allocate(this, java_null, false);
    null_instance_ = reinterpret_cast<GraalPrimitive*> (null_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kNullValueRootIndex] = null_instance_;

    // True
    GraalBoolean* true_local = GraalBoolean::Allocate(this, true);
    true_instance_ = reinterpret_cast<GraalBoolean*> (true_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kTrueValueRootIndex] = true_instance_;

    // False
    GraalBoolean* false_local = GraalBoolean::Allocate(this, false);
    false_instance_ = reinterpret_cast<GraalBoolean*> (false_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kFalseValueRootIndex] = false_instance_;

    // EmptyString
    JNI_CALL(jobject, empty_string, this, GraalAccessMethod::string_empty, Object);
    GraalString* empty_string_local = GraalString::Allocate(this, empty_string);
    GraalString* empty_string_global = reinterpret_cast<GraalString*> (empty_string_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kEmptyStringRootIndex] = empty_string_global;

    // int32 placeholder
    JNI_CALL(jobject, java_int32_placeholder, this, GraalAccessMethod::isolate_get_int_placeholder, Object);
    GraalNumber* int32_placeholder_local = GraalNumber::Allocate(this, 0, java_int32_placeholder);
    GraalNumber* int32_placeholder_global = reinterpret_cast<GraalNumber*> (int32_placeholder_local->Copy(true));
    int32_placeholder_ = int32_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kInt32ReturnValuePlaceholderIndex] = int32_placeholder_global;

    // uint32 placeholder
    JNI_CALL(jobject, java_uint32_placeholder, this, GraalAccessMethod::isolate_get_safe_int_placeholder, Object);
    GraalNumber* uint32_placeholder_local = GraalNumber::Allocate(this, 0, java_uint32_placeholder);
    GraalNumber* uint32_placeholder_global = reinterpret_cast<GraalNumber*> (uint32_placeholder_local->Copy(true));
    uint32_placeholder_ = uint32_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kUint32ReturnValuePlaceholderIndex] = uint32_placeholder_global;

    // double placeholder
    JNI_CALL(jobject, java_double_placeholder, this, GraalAccessMethod::isolate_get_double_placeholder, Object);
    GraalNumber* double_placeholder_local = GraalNumber::Allocate(this, 0, java_double_placeholder);
    GraalNumber* double_placeholder_global = reinterpret_cast<GraalNumber*> (double_placeholder_local->Copy(true));
    double_placeholder_ = double_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kDoubleReturnValuePlaceholderIndex] = double_placeholder_global;

    sending_message_ = false;
    abort_on_uncaught_exception_callback_ = nullptr;
    array_buffer_allocator_ = params.array_buffer_allocator_shared;
    try_catch_count_ = 0;
    stack_check_enabled_ = false;
    error_to_ignore_ = nullptr;
    calls_on_stack_ = 0;

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        exit(1);
    }
}

// Find DynamicObject fields for direct access
void GraalIsolate::FindDynamicObjectFields(jobject context) {
    if (jni_fields_[0] != 0) {
        return; // already initialized
    }
    JNIEnv* env = GetJNIEnv();
    JNI_CALL(jobject, field_info_obj, this, GraalAccessMethod::find_dynamic_object_fields, Object, context);
    if (field_info_obj == NULL) {
      if (env->ExceptionCheck()) {
        EXIT_WITH_MESSAGE(env, "GraalJSAccess.findDynamicObjectFields() failed!\n")
      }
      return;
    }
    jobjectArray field_info_array = static_cast<jobjectArray>(field_info_obj);
    int field_count = static_cast<int>(GraalAccessField::count);
    if (field_info_array != NULL && env->GetArrayLength(field_info_array) == field_count) {
        for (int i = 0; i < field_count; i++) {
            jobject reflectedField = env->GetObjectArrayElement(field_info_array, i);
            if (reflectedField != NULL) {
                jfieldID field = env->FromReflectedField(reflectedField);
                if (field == NULL) {
                    env->ExceptionClear();
                    continue;
                }
                SetJNIField(static_cast<GraalAccessField>(i), field);
            }
        }
    }
}

bool GraalIsolate::AddMessageListener(v8::MessageCallback callback, v8::Local<v8::Value> data) {
    message_listener_ = callback;
    return true;
}

void GraalIsolate::NotifyMessageListener(v8::Local<v8::Message> message, v8::Local<v8::Value> error, jthrowable java_error) {
    if (message_listener_ != nullptr && (error_to_ignore_ == nullptr || !GetJNIEnv()->IsSameObject(java_error, error_to_ignore_))) {
        error_to_ignore_ = nullptr;
        sending_message_ = true;
        message_listener_(message, error);
        sending_message_ = false;
    }
}

v8::Local<v8::Value> GraalIsolate::ThrowException(v8::Local<v8::Value> exception) {
    jobject java_exception = reinterpret_cast<GraalValue*> (*exception)->GetJavaObject();
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_throw_exception, java_exception);
    return exception;
}

void GraalIsolate::EnsureValidWorkingDir() {
#ifdef __POSIX__
    // JVM is unable to start from a deleted directory
    char* cwd = getcwd(nullptr, 0);
    if (cwd == nullptr) {
        if (chdir(getenv("HOME"))) {
            fprintf(stderr, "Unable to start in an invalid working directory!\n");
            exit(1);
        }
    } else {
        free(cwd);
    }
#endif
}

void GraalIsolate::InitThreadLocals() {
    uv_key_create(&current_isolate_key);
    current_isolate_initialized = true;
}

void GraalIsolate::SetAbortOnUncaughtExceptionCallback(v8::Isolate::AbortOnUncaughtExceptionCallback callback) {
    abort_on_uncaught_exception_callback_ = callback;
}

bool GraalIsolate::AbortOnUncaughtExceptionCallbackValue() {
    return sending_message_ &&
            (abort_on_uncaught_exception_callback_ == nullptr ||
            abort_on_uncaught_exception_callback_(reinterpret_cast<v8::Isolate*> (this)));
}

bool GraalIsolate::abort_on_uncaught_exception_ = false;

void GraalIsolate::Dispose() {
    Dispose(false, 0);
}

void GraalIsolate::Dispose(bool exit, int status) {
    JNIEnv* env = jni_env_;
    jni_env_ = nullptr; // mark the isolate as disposed, see ~GraalHandleContent()

    // we do not use JNI_CALL_VOID because jni_env_ is cleared
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_dispose);
    env->functions->CallVoidMethod(env, access_, method_id, exit, status);

    // Release global references
    env->DeleteGlobalRef(object_class_);
    env->DeleteGlobalRef(access_class_);
    env->DeleteGlobalRef(access_);
    env->DeleteGlobalRef(boolean_true_);
    env->DeleteGlobalRef(boolean_false_);
    env->DeleteGlobalRef(int32_placeholder_);
    env->DeleteGlobalRef(uint32_placeholder_);
    env->DeleteGlobalRef(double_placeholder_);
    object_class_ = nullptr;
    access_class_ = nullptr;
    access_ = nullptr;
    boolean_true_ = nullptr;
    boolean_false_ = nullptr;
    int32_placeholder_ = nullptr;
    uint32_placeholder_ = nullptr;
    double_placeholder_ = nullptr;
    if (error_to_ignore_ != nullptr) {
        env->DeleteGlobalRef(error_to_ignore_);
        error_to_ignore_ = nullptr;
    }

    // this is executed when exit is false only
    env->ExceptionClear();
    jvm_->DetachCurrentThread();

#ifdef __POSIX__
    pthread_mutex_destroy(&lock_);
#else
    CloseHandle(lock_);
#endif
    delete array_pool_;
    delete context_pool_;
    delete external_pool_;
    delete function_pool_;
    delete number_pool_;
    delete object_pool_;
    delete string_pool_;

    // GraalIsolate is allocated using malloc and placement new,
    // see Isolate::Allocate() and Isolate::Initialize()
    this->~GraalIsolate();
    free(this);
}

jobject GraalIsolate::JNIGetObjectFieldOrCall(jobject java_object, GraalAccessField graal_field_id, GraalAccessMethod graal_method_id) {
    jfieldID field = GetJNIField(graal_field_id);
    jobject result;
    if (field != NULL) {
        result = GetJNIEnv()->GetObjectField(java_object, field);
    } else {
        JNI_CALL(jobject, jni_call_result, this, graal_method_id, Object, java_object);
        result = jni_call_result;
    }
    return result;
}

GraalNumber* GraalIsolate::CachedNumber(int value) {
    if (number_cache_low_ <= value && value <= number_cache_high_) {
        int index = value - number_cache_low_;
        if (number_cache_[index] == nullptr) {
            GraalNumber* graal_number = GraalNumber::NewNotCached(this, value);
            number_cache_[index] = reinterpret_cast<GraalNumber*> (graal_number->Copy(true));
        }
        return number_cache_[index];
    }
    return nullptr;
}

bool GraalIsolate::internal_error_check_ = false;

void GraalIsolate::InternalErrorCheck() {
    JNIEnv* env = GetJNIEnv();
    if (env->ExceptionCheck()) {
        jobject exception = env->ExceptionOccurred();
        jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_internal_error_check);
        env->functions->CallVoidMethod(env, GetGraalAccess(), method_id, exception);
    }
}

void GraalIsolate::InitStackOverflowCheck(intptr_t stack_bottom) {
    char* stack_size_str = getenv("NODE_STACK_SIZE");
    ptrdiff_t stack_size = 0;
    if (stack_size_str != nullptr) {
        stack_size = strtol(stack_size_str, nullptr, 10);
    }
    if (stack_size > 0) {
        stack_check_enabled_ = true;
        stack_bottom_ = stack_bottom;
        stack_size_limit_ = stack_size - 150000;
    }
}

void GraalIsolate::ThrowStackOverflowError() {
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_throw_stack_overflow_error);
}

void GraalIsolate::AddGCPrologueCallback(GCCallbackType type, void* callback, void* data) {
    prolog_callbacks.push_back(std::make_tuple(type, callback, data));
}

void GraalIsolate::RemoveGCPrologueCallback(void* callback) {
    RemoveCallback(prolog_callbacks, callback);
}

void GraalIsolate::AddGCEpilogueCallback(GCCallbackType type, void* callback, void* data) {
    epilog_callbacks.push_back(std::make_tuple(type, callback, data));
}

void GraalIsolate::RemoveGCEpilogueCallback(void* callback) {
    RemoveCallback(epilog_callbacks, callback);
}

void GraalIsolate::RemoveCallback(std::vector<std::tuple<GCCallbackType,void*,void*>>& vector, void* callback) {
    unsigned int i = 0;
    while (i < vector.size()) {
        if (std::get<1>(vector[i]) == callback) {
            vector[i] = vector.back();
            vector.pop_back();
        } else {
            i++;
        }
    }
}

void GraalIsolate::NotifyGCCallbacks(bool prolog) {
    std::vector<std::tuple<GCCallbackType, void*, void*>>&vector = prolog ? prolog_callbacks : epilog_callbacks;
    v8::Isolate* isolate = reinterpret_cast<v8::Isolate*> (this);
    for (unsigned int i = 0; i < vector.size(); i++) {
        GCCallbackType type = std::get<0>(vector[i]);
        void* cb = std::get<1>(vector[i]);
        if (type == kIsolateGCCallbackType) {
            v8::Isolate::GCCallback callback = (v8::Isolate::GCCallback) cb;
            callback(isolate, v8::GCType::kGCTypeMarkSweepCompact, v8::GCCallbackFlags::kGCCallbackFlagForced);
        } else if (type == kIsolateGCCallbackWithDataType) {
            void* data = std::get<2>(vector[i]);
            v8::Isolate::GCCallbackWithData callback = (v8::Isolate::GCCallbackWithData) cb;
            callback(isolate, v8::GCType::kGCTypeMarkSweepCompact, v8::GCCallbackFlags::kGCCallbackFlagForced, data);
        } else if (type == kV8GCCallbackType) {
            v8::GCCallback callback = (v8::GCCallback) cb;
            callback(v8::GCType::kGCTypeMarkSweepCompact, v8::GCCallbackFlags::kGCCallbackFlagForced);
        } else {
            fprintf(stderr, "Unexpected callback type %d!\n", type);
        }
    }
}

void GraalIsolate::TerminateExecution() {
    // We cannot use GetJNIEnv()/JNI_CALL_VOID because TerminateExecution()
    // can be called from a thread that does not correspond to this isolate
    GraalIsolate* current_isolate = CurrentIsolate();
    JNIEnv* env;
    if (current_isolate == nullptr) {
        // the thread does not belong to any isolate (i.e. is not attached to JVM)
        jvm_->AttachCurrentThread((void**) &env, nullptr);
    } else {
        env = current_isolate->GetJNIEnv();
    }
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_terminate_execution);
    env->functions->CallVoidMethod(env, access_, method_id);
    if (current_isolate == nullptr) {
        jvm_->DetachCurrentThread();
    }
}

void GraalIsolate::CancelTerminateExecution() {
    // We cannot use GetJNIEnv()/JNI_CALL_VOID because TerminateExecution()
    // can be called from a thread that does not correspond to this isolate
    GraalIsolate* current_isolate = CurrentIsolate();
    JNIEnv* env;
    if (current_isolate == nullptr) {
        // the thread does not belong to any isolate (i.e. is not attached to JVM)
        jvm_->AttachCurrentThread((void**) &env, nullptr);
    } else {
        env = current_isolate->GetJNIEnv();
        if (current_isolate == this) {
            env->ExceptionClear(); // Clear potential termination exception in this thread
        }
    }
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_cancel_terminate_execution);
    env->functions->CallVoidMethod(env, access_, method_id);
    if (current_isolate == nullptr) {
        jvm_->DetachCurrentThread();
    }
}

bool GraalIsolate::IsExecutionTerminating() {
    JNI_CALL(jboolean, terminating, this, GraalAccessMethod::isolate_is_execution_terminating, Boolean);
    return terminating;
}

void GraalIsolate::SetFunctionTemplateData(unsigned id, GraalValue* data) {
    while (function_template_data.size() <= id) function_template_data.push_back(nullptr);
    data->ReferenceAdded();
    function_template_data[id] = data;
}

void GraalIsolate::SetFunctionTemplateCallback(unsigned id, v8::FunctionCallback callback) {
    while (function_template_callbacks.size() <= id) function_template_callbacks.push_back(nullptr);
    function_template_callbacks[id] = callback;
}

jobject GraalIsolate::CorrectReturnValue(GraalValue* value, jobject null_replacement) {
    jobject result;
    if (value == nullptr) {
        result = null_replacement;
    } else {
        result = value->GetJavaObject();
        if (result == int32_placeholder_) {
            ResetSharedBuffer();
            WriteInt32ToSharedBuffer((int32_t) return_value_);
        } else if (result == uint32_placeholder_) {
            ResetSharedBuffer();
            WriteInt64ToSharedBuffer((int64_t) return_value_);
        } else if (result == double_placeholder_) {
            ResetSharedBuffer();
            WriteDoubleToSharedBuffer((double) return_value_);
        }
        result = GetJNIEnv()->NewLocalRef(result);
    }
    return result;
}

int GraalIsolate::argc = 0;
char** GraalIsolate::argv = nullptr;
int GraalIsolate::mode = GraalIsolate::kModeDefault;
bool GraalIsolate::polyglot = false;
bool GraalIsolate::use_classpath_env_var = false;

void GraalIsolate::SetPromiseHook(v8::PromiseHook promise_hook) {
    bool wasNull = promise_hook_ == nullptr;
    bool isNull = promise_hook == nullptr;
    if (wasNull != isNull) {
        // turn the notification on/off
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enable_promise_hook, (jboolean) !isNull);
    }
    promise_hook_ = promise_hook;
}

void GraalIsolate::NotifyPromiseHook(v8::PromiseHookType type, v8::Local<v8::Promise> promise, v8::Local<v8::Value> parent) {
    if (promise_hook_ != nullptr) {
        promise_hook_(type, promise, parent);
    }
}

void GraalIsolate::SetPromiseRejectCallback(v8::PromiseRejectCallback callback) {
    bool wasNull = promise_reject_callback_ == nullptr;
    bool isNull = callback == nullptr;
    if (wasNull != isNull) {
        // turn the notification on/off
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enable_promise_reject_callback, (jboolean) !isNull);
    }
    promise_reject_callback_ = callback;
}

void GraalIsolate::NotifyPromiseRejectCallback(v8::PromiseRejectMessage message) {
    if (promise_reject_callback_ != nullptr) {
        promise_reject_callback_(message);
    }
}

void GraalIsolate::SetImportMetaInitializer(v8::HostInitializeImportMetaObjectCallback callback) {
    bool wasNull = import_meta_initializer == nullptr;
    bool isNull = callback == nullptr;
    if (wasNull != isNull) {
        // turn the notification on/off
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enable_import_meta_initializer, (jboolean) !isNull);
    }
    import_meta_initializer = callback;
}

void GraalIsolate::NotifyImportMetaInitializer(v8::Local<v8::Object> import_meta, v8::Local<v8::Module> module) {
    if (import_meta_initializer != nullptr) {
        import_meta_initializer(GetCurrentContext(), module, import_meta);
    }
}

void GraalIsolate::SetImportModuleDynamicallyCallback(v8::HostImportModuleDynamicallyCallback callback) {
    bool wasNull = import_module_dynamically == nullptr;
    bool isNull = callback == nullptr;
    if (wasNull != isNull) {
        // turn the notification on/off
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enable_import_module_dynamically, (jboolean) !isNull);
    }
    import_module_dynamically = callback;
}

v8::MaybeLocal<v8::Promise> GraalIsolate::NotifyImportModuleDynamically(v8::Local<v8::Context> context, v8::Local<v8::Data> host_defined_options, v8::Local<v8::Value> resource_name, v8::Local<v8::String> specifier, v8::Local<v8::FixedArray> import_assertions) {
    if (import_module_dynamically != nullptr) {
        return import_module_dynamically(context, host_defined_options, resource_name, specifier, import_assertions);
    } else {
        return v8::MaybeLocal<v8::Promise>();
    }
}

void GraalIsolate::SetPrepareStackTraceCallback(v8::PrepareStackTraceCallback callback) {
    bool wasNull = prepare_stack_trace_callback_ == nullptr;
    bool isNull = callback == nullptr;
    if (wasNull != isNull) {
        // turn the notification on/off
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_enable_prepare_stack_trace_callback, (jboolean) !isNull);
    }
    prepare_stack_trace_callback_ = callback;
}

v8::MaybeLocal<v8::Value> GraalIsolate::NotifyPrepareStackTraceCallback(v8::Local<v8::Context> context, v8::Local<v8::Value> error, v8::Local<v8::Array> sites) {
    if (prepare_stack_trace_callback_ != nullptr) {
        return prepare_stack_trace_callback_(context, error, sites);
    } else {
        return v8::MaybeLocal<v8::Value>();
    }
}

void GraalIsolate::SetWasmStreamingCallback(v8::WasmStreamingCallback callback) {
    wasm_streaming_callback_ = callback;
}

v8::WasmStreamingCallback GraalIsolate::GetWasmStreamingCallback() {
    return wasm_streaming_callback_;
}

void GraalIsolate::EnqueueMicrotask(v8::MicrotaskCallback microtask, void* data) {
    microtasks.push_back(std::pair<v8::MicrotaskCallback, void*>(microtask, data));
}

void GraalIsolate::RunMicrotasks() {
    for (std::pair<v8::MicrotaskCallback, void*> pair : microtasks) {
        pair.first(pair.second);
    }
    microtasks.clear();

    // Enter "dummy TryCatch" to ensure that TryCatchExists() returns true
    // for microtasks (i.e., fatal error handler is not invoked when an error occurs).
    TryCatchEnter();

    JNI_CALL_VOID(this, GraalAccessMethod::isolate_run_microtasks);

    // Exit "dummy TryCatch"
    TryCatchExit();
}

void GraalIsolate::Enter() {
    if (jvm_->GetEnv(reinterpret_cast<void**> (&jni_env_), REQUESTED_JNI_VERSION) == JNI_EDETACHED) {
        jvm_->AttachCurrentThread(reinterpret_cast<void**> (&jni_env_), nullptr);
    }
    uv_key_set(&current_isolate_key, this);
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_enter, (jlong) this);
}

void GraalIsolate::Exit() {
    JNI_CALL(jlong, previous, this, GraalAccessMethod::isolate_exit, Long, (jlong) this);
    uv_key_set(&current_isolate_key, reinterpret_cast<void*> (previous));
}

void GraalIsolate::HandleEmptyCallResult() {
    JNIEnv* env = GetJNIEnv();
    if (!TryCatchExists()) {
        jthrowable java_exception = env->ExceptionOccurred();
        env->ExceptionClear();
        JNI_CALL(jboolean, termination_exception, this, GraalAccessMethod::try_catch_has_terminated, Boolean, java_exception);
        if (termination_exception) {
            env->Throw(java_exception);
        } else {
            jobject java_context = CurrentJavaContext();
            JNI_CALL(jobject, exception_object, this, GraalAccessMethod::try_catch_exception, Object, java_context, java_exception);
            GraalValue* graal_exception = GraalValue::FromJavaObject(this, exception_object);
            v8::Value* v8_exception = reinterpret_cast<v8::Value*> (graal_exception);
            v8::Isolate* v8_isolate = reinterpret_cast<v8::Isolate*> (this);
            v8::Local<v8::Value> exception = v8::Local<v8::Value>::New(v8_isolate, v8_exception);
            NotifyMessageListener(v8::Exception::CreateMessage(v8_isolate, exception), exception, java_exception);
            if (error_to_ignore_ != nullptr) {
                env->DeleteGlobalRef(error_to_ignore_);
                error_to_ignore_ = nullptr;
            }
            if (calls_on_stack_ != 0) {
                // If the process was not terminated then we have to ensure that
                // the rest of the script is not executed => we re-throw
                // the exception but remember that we should not report it again.
                error_to_ignore_ = env->NewGlobalRef(java_exception);
                env->Throw(java_exception);
            }
        }
    }
}

void GraalIsolate::SetEnv(const char * name, const char * value) {
#ifdef __POSIX__
    setenv(name, value, 1);
#else
    _putenv_s(name, value);
#endif
}

void GraalIsolate::UnsetEnv(const char * name) {
#ifdef __POSIX__
    unsetenv(name);
#else
    _putenv_s(name, "");
#endif
}

void GraalIsolate::ReportAPIFailure(const char* location, const char* message) {
    if (fatal_error_handler_ == nullptr) {
        fprintf(stderr, "FATAL ERROR: %s %s\n", location, message);
        fflush(stderr);
        abort();
    } else {
        fatal_error_handler_(location, message);
    }
}

void GraalIsolate::EnqueueMicrotask(v8::Local<v8::Function> microtask) {
    GraalFunction* graal_microtask = reinterpret_cast<GraalFunction*> (*microtask);
    jobject java_microtask = graal_microtask->GetJavaObject();
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_enqueue_microtask, java_microtask);
}

v8::ArrayBuffer::Allocator* GraalIsolate::GetArrayBufferAllocator() {
    return array_buffer_allocator_.get();
}

void GraalIsolate::SchedulePauseOnNextStatement() {
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_schedule_pause_on_next_statement);
}

void GraalIsolate::RequestInterrupt(v8::InterruptCallback callback, void* data) {
    // We cannot use GetJNIEnv()/JNI_CALL_VOID because RequestInterrupt()
    // can be called from a thread that does not correspond to this isolate
    GraalIsolate* current_isolate = CurrentIsolate();
    JNIEnv* env;
    if (current_isolate == nullptr) {
        // the thread does not belong to any isolate (i.e. is not attached to JVM)
        jvm_->AttachCurrentThread((void**) &env, nullptr);
    } else {
        env = current_isolate->GetJNIEnv();
    }
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_request_interrupt);
    env->functions->CallVoidMethod(env, access_, method_id, (jlong) callback, (jlong) data);
    if (current_isolate == nullptr) {
        jvm_->DetachCurrentThread();
    }
}

void GraalIsolate::JSExecutionViolation(JSExecutionAction action) {
    if (action == kJSExecutionThrow) {
        ThrowException(v8::String::NewFromUtf8(reinterpret_cast<v8::Isolate*> (this), "Illegal operation.").ToLocalChecked());
    } else {
        ReportAPIFailure("DisallowJavascriptExecutionScope", "Illegal operation.");
    }
}

void GraalIsolate::SetTaskRunner(std::shared_ptr<v8::TaskRunner> task_runner) {
    this->task_runner_ = task_runner;
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_set_task_runner, (jlong) task_runner.get());
}

void GraalIsolate::ExecuteRunnable(jobject runnable) {
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_execute_runnable, runnable);
}
