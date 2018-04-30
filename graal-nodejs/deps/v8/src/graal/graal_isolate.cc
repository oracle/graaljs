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

#include "callbacks.h"
#include "graal_boolean.h"
#include "graal_isolate.h"
#include "graal_missing_primitive.h"
#include "graal_number.h"
#include "graal_object.h"
#include "graal_string.h"
#include "../../../uv/include/uv.h"
#include "graal_context.h"
#include <stdlib.h>
#include <string>
#include <string.h>
#include <unistd.h>
#include <dlfcn.h>
#include <algorithm>

#ifdef __APPLE__
#define LIBNODESVM_RELPATH "/jre/lib/polyglot/libpolyglot.dylib"
#define LIBJVM_RELPATH     "/lib/server/libjvm.dylib"
#define LIBJLI_RELPATH     "/lib/jli/libjli.dylib"
#elif defined(__sparc__)
// SVM currently not supported
#define LIBJVM_RELPATH     "/lib/sparcv9/server/libjvm.so"
#else
#define LIBNODESVM_RELPATH "/jre/lib/polyglot/libpolyglot.so"
#define LIBJVM_RELPATH     "/lib/amd64/server/libjvm.so"
#endif

#define EXIT_WITH_MESSAGE(env, message) { \
    fprintf(stderr, "%s", message); \
    if (env->ExceptionCheck()) { \
        env->ExceptionDescribe(); \
    } \
    exit(1); \
}

extern "C" int uv_exepath(char* buffer, size_t* size) __attribute__((weak));
extern "C" int uv_key_create(uv_key_t* key) __attribute__((weak));
extern "C" void uv_key_set(uv_key_t* key, void* value) __attribute__((weak));
extern "C" void* uv_key_get(uv_key_t* key) __attribute__((weak));
extern "C" uv_loop_t* uv_default_loop(void) __attribute__((weak));
extern "C" int uv__cloexec_ioctl(int fd, int set) __attribute__((weak));
extern "C" int uv__cloexec_fcntl(int fd, int set) __attribute__((weak));

#if defined(_AIX) || \
    defined(__APPLE__) || \
    defined(__DragonFly__) || \
    defined(__FreeBSD__) || \
    defined(__FreeBSD_kernel__) || \
    defined(__linux__)
#define uv__cloexec uv__cloexec_ioctl
#else
#define uv__cloexec uv__cloexec_fcntl
#endif

// Key for the current (per-thread) isolate
static uv_key_t current_isolate_key;

GraalIsolate* CurrentIsolate() {
    return reinterpret_cast<GraalIsolate*> (uv_key_get(&current_isolate_key));
}

#define ACCESS_METHOD(id, name, signature) \
    jni_methods_[id] = jni_env_->GetMethodID(access_class_, name, signature); \
    if (jni_methods_[id] == NULL) { \
        fprintf(stderr, "Method %s not found!\n", name); \
        exit(1); \
    }

typedef jint(*InitJVM)(JavaVM **, void **, void *);
typedef jint(*CreatedJVMs)(JavaVM **vmBuffer, jsize bufferLength, jsize *written);

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
        at = path.find_last_of('/', at - 1);
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

v8::Isolate* GraalIsolate::New(v8::Isolate::CreateParams const& params) {
    JavaVM *jvm;
    JNIEnv *env;
    void* jvm_handle;

    std::string node = nodeExe();

    if (ends_with(up(node), "/jre/languages/js/bin")) {
        // Part of GraalVM: take precedence over any JAVA_HOME.
        // We set environment variables to ensure these values are correctly
        // propagated to child processes.
        std::string graalvm_home = up(node, 5);
        setenv("JAVA_HOME", graalvm_home.c_str(), 1);

#       ifdef LIBNODESVM_RELPATH
            bool force_native = false;
            if (mode == kModeJVM) {
                unsetenv("NODE_JVM_LIB"); // will be set to appropriate libjvm path below
            } else if (mode == kModeNative) {
                force_native = true;
            } else { // mode == kModeDefault
                if (getstdenv("NODE_JVM_LIB").empty()) {
                    force_native = true;
                } // else reuse NODE_JVM_LIB
            }
            if (force_native) {
                std::string node_jvm_lib = graalvm_home + LIBNODESVM_RELPATH;
                setenv("NODE_JVM_LIB", node_jvm_lib.c_str(), 1);
            }
#       else
            if (mode == kModeNative) {
                fprintf(stderr, "`--native` mode not available.\n");
                exit(9);
            }
#       endif
    }

    std::string jdk_path = getstdenv("JAVA_HOME");
    if (jdk_path.empty()) {
        fprintf(stderr, "JAVA_HOME is not set. Specify JAVA_HOME so $JAVA_HOME%s exists.\n", LIBJVM_RELPATH);
        exit(1);
    }
    std::string jre_sub_dir = jdk_path + "/jre";
    if (access(jre_sub_dir.c_str(), F_OK) != -1) {
        jdk_path = jre_sub_dir;
    }
    std::string jvmlib_path = getstdenv("NODE_JVM_LIB");
    if (jvmlib_path.empty()) {
        jvmlib_path = jdk_path + LIBJVM_RELPATH;
        setenv("NODE_JVM_LIB", jvmlib_path.c_str(), 1);
    }
    if (access(jvmlib_path.c_str(), F_OK) == -1) {
        fprintf(stderr, "Cannot find %s. Specify JAVA_HOME so $JAVA_HOME%s exists, or specify NODE_JVM_LIB directly.\n", jvmlib_path.c_str(), LIBJVM_RELPATH);
        exit(1);
    }
    jvm_handle = dlopen(jvmlib_path.c_str(), RTLD_NOW);
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
    jsize existingJVMs;
    createdJVMs(&jvm, 1, &existingJVMs);
    bool spawn_jvm = (existingJVMs == 0);

    if (spawn_jvm) {
        std::vector<JavaVMOption> options;
        std::string graal_sdk_jar_path = getstdenv("GRAAL_SDK_JAR_PATH");
        if (!graal_sdk_jar_path.empty() && access(graal_sdk_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update GRAAL_SDK_JAR_PATH environment variable!\n", graal_sdk_jar_path.c_str());
            exit(1);
        }
        std::string truffle_jar_path = getstdenv("TRUFFLE_JAR_PATH");
        if (!truffle_jar_path.empty() && access(truffle_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update TRUFFLE_JAR_PATH environment variable!\n", truffle_jar_path.c_str());
            exit(1);
        }
        std::string graaljs_jar_path = getstdenv("GRAALJS_JAR_PATH");
        if (!graaljs_jar_path.empty() && access(graaljs_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update GRAALJS_JAR_PATH environment variable!\n", graaljs_jar_path.c_str());
            exit(1);
        }
        std::string tregex_jar_path = getstdenv("TREGEX_JAR_PATH");
        if (!tregex_jar_path.empty() && access(tregex_jar_path.c_str(), F_OK) == -1) {
            fprintf(stderr, "Cannot find %s. Update TREGEX_JAR_PATH environment variable!\n", tregex_jar_path.c_str());
            exit(1);
        }
        std::string truffleom_jar_path = getstdenv("TRUFFLEOM_JAR_PATH");
        if (!truffleom_jar_path.empty() && access(truffleom_jar_path.c_str(), F_OK) == -1) {
            // Cannot find the jar of the enterprise object model.
            // Unless this is added to the classpath using the NODE_JVM_CLASSPATH env var, run with the basic object model.
            truffleom_jar_path = "";
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
                    extra_jvm_path += ":";
                }
                extra_jvm_path += classpath;
            }
        }

        EnsureValidWorkingDir();

        std::string boot_classpath = getstdenv("NODE_JVM_BOOTCLASSPATH");
        if (boot_classpath.empty()) {
            if (!graal_sdk_jar_path.empty()) {
                boot_classpath += ":";
                boot_classpath += graal_sdk_jar_path;
            }
            if (!truffle_jar_path.empty()) {
                boot_classpath += ":";
                boot_classpath += truffle_jar_path;
            }
            if (!truffleom_jar_path.empty()) {
                boot_classpath += ":";
                boot_classpath += truffleom_jar_path;
            }
        } else {
            boot_classpath = ":" + boot_classpath;
        }
        if (!boot_classpath.empty()) {
            boot_classpath = "-Xbootclasspath/a" + boot_classpath;
            options.push_back({const_cast<char*>(boot_classpath.c_str()), nullptr});
        }

        std::string classpath = "";
        std::string classpath_sep = "";
        if (!graaljs_jar_path.empty()) {
            classpath += graaljs_jar_path;
            classpath_sep = ":";
        }
        if (!tregex_jar_path.empty()) {
            classpath += classpath_sep;
            classpath += tregex_jar_path;
            classpath_sep = ":";
        }
        if (!graalnode_jar_path.empty()) {
            classpath += classpath_sep;
            classpath += graalnode_jar_path;
            classpath_sep = ":";
        }
        if (!extra_jvm_path.empty()) {
            classpath += classpath_sep;
            classpath += extra_jvm_path;
            classpath_sep = ":";
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

    #if defined(DEBUG)
        options.push_back({const_cast<char*>("-Xdebug"), nullptr});
        options.push_back({const_cast<char*>("-Xnoagent"), nullptr});
        std::string debugParam = "-Xrunjdwp:transport=dt_socket";
        std::string debugPort = getstdenv("DEBUG_PORT");
        if (!debugPort.empty()) {
            unsetenv("DEBUG_PORT"); // do not debug child processes
            debugParam += ",server=n,suspend=y,address=";
            debugParam += debugPort;
        } else {
            debugParam += ",server=y,suspend=n";
        }
        options.push_back({const_cast<char*>(debugParam.c_str()), nullptr});
        options.push_back({const_cast<char*>("-Dtruffle.node.js.verbose=true"), nullptr});
        options.push_back({const_cast<char*>("-Dgraal.TruffleCompilationExceptionsArePrinted=true"), nullptr});
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
                            // Re-enable inheritance of stdout and stderr disabled
                            // by uv_disable_stdio_inheritance() in node::Init()
                            uv__cloexec(1, 0);
                            uv__cloexec(2, 0);
                            // Delegate to java -help
                            std::string java = jdk_path + "/bin/java";
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
        unsetenv(no_spawn_options);

    #if __APPLE__
        if (dlopen((jdk_path + LIBJLI_RELPATH).c_str(), RTLD_NOW) == NULL) {
            fprintf(stderr, "warning: could not load libjli: %s\n", dlerror());
        }
    #endif

        JavaVMInitArgs vm_args;
        vm_args.version = JNI_VERSION_1_8;
        vm_args.nOptions = options.size();
        vm_args.options = options.data();
        vm_args.ignoreUnrecognized = false;

        InitJVM createJvm = (InitJVM) dlsym(jvm_handle, "JNI_CreateJavaVM");
        if (createJvm == NULL) {
            fprintf(stderr, "JNI_CreateJavaVM symbol could not be resolved: %s\n", dlerror());
            exit(1);
        }
        jint result = createJvm(&jvm, (void**) &env, &vm_args);
        if (result != JNI_OK) {
            fprintf(stderr, "Creation of the JVM failed!\n");
            exit(1);
        }

        jclass callback_class = findClassExtra(env, "com/oracle/truffle/trufflenode/NativeAccess");
        if (!RegisterCallbacks(env, callback_class)) {
            exit(1);
        }
        uv_key_create(&current_isolate_key);
    } else {
        // As the JVM already exists, attach to it
        jvm->AttachCurrentThread(reinterpret_cast<void**> (&env), nullptr);
    }

    internal_error_check_ = !getstdenv("NODE_INTERNAL_ERROR_CHECK").empty();

    GraalIsolate* isolate = new GraalIsolate(jvm, env);
    uv_key_set(&current_isolate_key, isolate);

    isolate->main_ = spawn_jvm;
    if (spawn_jvm) {
        isolate->InitStackOverflowCheck((long) &params);
    }

    return reinterpret_cast<v8::Isolate*> (isolate);
}

GraalIsolate::GraalIsolate(JavaVM* jvm, JNIEnv* env) : function_template_functions(), function_template_data(), function_template_callbacks(), jvm_(jvm), jni_env_(env), jni_methods_(), jni_fields_(), message_listener_(nullptr), function_template_count_(0), lock_(PTHREAD_MUTEX_INITIALIZER), promise_hook_(nullptr), promise_reject_callback_(nullptr) {
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
    jmethodID createID = env->GetStaticMethodID(access_class, "create", "([Ljava/lang/String;J)Ljava/lang/Object;");
    if (createID == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.create(String[],long) method not found!\n")
    jobject access = env->functions->CallStaticObjectMethod(env, access_class, createID, args, (jlong) uv_default_loop());
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
    ACCESS_METHOD(GraalAccessMethod::value_unknown, "valueUnknown", "(Ljava/lang/Object;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::value_to_object, "valueToObject", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::value_to_string, "valueToString", "(Ljava/lang/Object;)Ljava/lang/String;")
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
    ACCESS_METHOD(GraalAccessMethod::value_equals, "valueEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_strict_equals, "valueStrictEquals", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::value_instance_of, "valueInstanceOf", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_new, "objectNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set, "objectSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_index, "objectSetIndex", "(Ljava/lang/Object;ILjava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_private, "objectSetPrivate", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_force_set, "objectForceSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)Z")
    ACCESS_METHOD(GraalAccessMethod::object_get, "objectGet", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_index, "objectGetIndex", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_private, "objectGetPrivate", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_real_named_property, "objectGetRealNamedProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_real_named_property_attributes, "objectGetRealNamedPropertyAttributes", "(Ljava/lang/Object;Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::object_get_own_property_descriptor, "objectGetOwnPropertyDescriptor", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_has, "objectHas", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_has_own_property, "objectHasOwnProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_has_real_named_property, "objectHasRealNamedProperty", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete, "objectDelete", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete_index, "objectDelete", "(Ljava/lang/Object;J)Z")
    ACCESS_METHOD(GraalAccessMethod::object_delete_private, "objectDeletePrivate", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_set_accessor, "objectSetAccessor", "(Ljava/lang/Object;Ljava/lang/Object;JJLjava/lang/Object;I)Z")
    ACCESS_METHOD(GraalAccessMethod::object_clone, "objectClone", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_prototype, "objectGetPrototype", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_set_prototype, "objectSetPrototype", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::object_get_constructor_name, "objectGetConstructorName", "(Ljava/lang/Object;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::object_get_property_names, "objectGetPropertyNames", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_get_own_property_names, "objectGetOwnPropertyNames", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_creation_context, "objectCreationContext", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_define_property, "objectDefineProperty", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;ZZZZZZ)Z")
    ACCESS_METHOD(GraalAccessMethod::array_new, "arrayNew", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_length, "arrayLength", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_new, "arrayBufferNew", "(Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_new_buffer, "arrayBufferNew", "(Ljava/lang/Object;Ljava/lang/Object;J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_get_contents, "arrayBufferGetContents", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_buffer, "arrayBufferViewBuffer", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_byte_length, "arrayBufferViewByteLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::array_buffer_view_byte_offset, "arrayBufferViewByteOffset", "(Ljava/lang/Object;)I")
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
    ACCESS_METHOD(GraalAccessMethod::data_view_new, "dataViewNew", "(Ljava/lang/Object;II)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::external_new, "externalNew", "(Ljava/lang/Object;J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::integer_new, "integerNew", "(J)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::number_new, "numberNew", "(D)Ljava/lang/Object;")
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
    ACCESS_METHOD(GraalAccessMethod::isolate_create_internal_field_count_key, "isolateCreateInternalFieldCountKey", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_create_internal_field_key, "isolateCreateInternalFieldKey", "(I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_internal_error_check, "isolateInternalErrorCheck", "(Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_throw_stack_overflow_error, "isolateThrowStackOverflowError", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_heap_statistics, "isolateGetHeapStatistics", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_terminate_execution, "isolateTerminateExecution", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_cancel_terminate_execution, "isolateCancelTerminateExecution", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_int_placeholder, "isolateGetIntPlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_large_int_placeholder, "isolateGetLargeIntPlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_double_placeholder, "isolateGetDoublePlaceholder", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_dispose, "isolateDispose", "(ZI)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enter_polyglot_engine, "isolateEnterPolyglotEngine", "(JJJJIJIJ)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_perform_gc, "isolatePerformGC", "()V")
    ACCESS_METHOD(GraalAccessMethod::isolate_get_debug_context, "isolateGetDebugContext", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_promise_hook, "isolateEnablePromiseHook", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::isolate_enable_promise_reject_callback, "isolateEnablePromiseRejectCallback", "(Z)V")
    ACCESS_METHOD(GraalAccessMethod::template_set, "templateSet", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_new, "objectTemplateNew", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_template_new_instance, "objectTemplateNewInstance", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_accessor, "objectTemplateSetAccessor", "(Ljava/lang/Object;Ljava/lang/Object;JJLjava/lang/Object;Ljava/lang/Object;I)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_named_property_handler, "objectTemplateSetNamedPropertyHandler", "(Ljava/lang/Object;JJJJJLjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_handler, "objectTemplateSetHandler", "(Ljava/lang/Object;JJJJJLjava/lang/Object;ZZ)V")
    ACCESS_METHOD(GraalAccessMethod::object_template_set_call_as_function_handler, "objectTemplateSetCallAsFunctionHandler", "(Ljava/lang/Object;IJLjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_new_instance, "functionNewInstance", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_set_name, "functionSetName", "(Ljava/lang/Object;Ljava/lang/String;)V")
    ACCESS_METHOD(GraalAccessMethod::function_get_name, "functionGetName", "(Ljava/lang/Object;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::function_call, "functionCall", "(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_resource_name, "functionResourceName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_get_script_line_number, "functionGetScriptLineNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::function_get_script_column_number, "functionGetScriptColumnNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::function_template_new, "functionTemplateNew", "(IJLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_set_class_name, "functionTemplateSetClassName", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_template_instance_template, "functionTemplateInstanceTemplate", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_prototype_template, "functionTemplatePrototypeTemplate", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_get_function, "functionTemplateGetFunction", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_get_function_to_cache, "functionTemplateGetFunction", "(I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::function_template_has_instance, "functionTemplateHasInstance", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::function_template_set_call_handler, "functionTemplateSetCallHandler", "(Ljava/lang/Object;JLjava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::function_template_inherit, "functionTemplateInherit", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::script_compile, "scriptCompile", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_run, "scriptRun", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::script_get_unbound_script, "scriptGetUnboundScript", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_compile, "unboundScriptCompile", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_bind_to_context, "unboundScriptBindToContext", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::unbound_script_get_id, "unboundScriptGetId", "(Ljava/lang/Object;)I")
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
    ACCESS_METHOD(GraalAccessMethod::stack_trace_current_stack_trace, "stackTraceCurrentStackTrace", "()Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_line_number, "stackFrameGetLineNumber", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_column, "stackFrameGetColumn", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_script_name, "stackFrameGetScriptName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::stack_frame_get_function_name, "stackFrameGetFunctionName", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::make_weak, "makeWeak", "(Ljava/lang/Object;JJJI)V")
    ACCESS_METHOD(GraalAccessMethod::clear_weak, "clearWeak", "(Ljava/lang/Object;J)J")
    ACCESS_METHOD(GraalAccessMethod::string_external_resource_callback, "stringExternalResourceCallback", "(Ljava/lang/Object;JJ)V")
    ACCESS_METHOD(GraalAccessMethod::context_new, "contextNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::context_set_security_token, "contextSetSecurityToken", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    ACCESS_METHOD(GraalAccessMethod::context_get_security_token, "contextGetSecurityToken", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::find_dynamic_object_fields, "findDynamicObjectFields", "(Ljava/lang/Object;)[Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::proxy_is_function, "proxyIsFunction", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::proxy_get_handler, "proxyGetHandler", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::proxy_get_target, "proxyGetTarget", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::boolean_object_new, "booleanObjectNew", "(Ljava/lang/Object;Z)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::boolean_object_value_of, "booleanObjectValueOf", "(Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::regexp_new, "regexpNew", "(Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::regexp_get_source, "regexpGetSource", "(Ljava/lang/Object;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::regexp_get_flags, "regexpGetFlags", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::string_object_new, "stringObjectNew", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::string_object_value_of, "stringObjectValueOf", "(Ljava/lang/Object;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::number_object_new, "numberObjectNew", "(Ljava/lang/Object;D)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::object_internal_field_count, "objectInternalFieldCount", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::object_slow_get_aligned_pointer_from_internal_field, "objectSlowGetAlignedPointerFromInternalField", "(Ljava/lang/Object;)J")
    ACCESS_METHOD(GraalAccessMethod::object_set_aligned_pointer_in_internal_field, "objectSetAlignedPointerInInternalField", "(Ljava/lang/Object;J)V")
    ACCESS_METHOD(GraalAccessMethod::json_parse, "jsonParse", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::json_stringify, "jsonStringify", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::symbol_new, "symbolNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_result, "promiseResult", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_state, "promiseState", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_new, "promiseResolverNew", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_reject, "promiseResolverReject", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::promise_resolver_resolve, "promiseResolverResolve", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    ACCESS_METHOD(GraalAccessMethod::module_compile, "moduleCompile", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_instantiate, "moduleInstantiate", "(Ljava/lang/Object;Ljava/lang/Object;J)V")
    ACCESS_METHOD(GraalAccessMethod::module_evaluate, "moduleEvaluate", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_get_status, "moduleGetStatus", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::module_get_requests_length, "moduleGetRequestsLength", "(Ljava/lang/Object;)I")
    ACCESS_METHOD(GraalAccessMethod::module_get_request, "moduleGetRequest", "(Ljava/lang/Object;I)Ljava/lang/String;")
    ACCESS_METHOD(GraalAccessMethod::module_get_namespace, "moduleGetNamespace", "(Ljava/lang/Object;)Ljava/lang/Object;")
    ACCESS_METHOD(GraalAccessMethod::module_get_identity_hash, "moduleGetIdentityHash", "(Ljava/lang/Object;)I")

    int root_offset = v8::internal::Internals::kIsolateRootsOffset / v8::internal::kApiPointerSize;
    slot[v8::internal::Internals::kExternalMemoryOffset / v8::internal::kApiPointerSize] = (void*) 0;
    slot[v8::internal::Internals::kExternalMemoryLimitOffset / v8::internal::kApiPointerSize] = (void*) (64*1024*1024); // v8::internal::kExternalAllocationSoftLimit

    // Undefined
    JNI_CALL(jobject, java_undefined, this, GraalAccessMethod::undefined_instance, Object);
    if (java_undefined == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.undefinedInstance() failed!\n")
    GraalMissingPrimitive* undefined_local = new GraalMissingPrimitive(this, java_undefined, true);
    undefined_instance_ = reinterpret_cast<GraalPrimitive*> (undefined_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kUndefinedValueRootIndex] = undefined_instance_;
    delete undefined_local;

    // Null
    JNI_CALL(jobject, java_null, this, GraalAccessMethod::null_instance, Object);
    if (java_null == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.nullInstance() failed!\n")
    GraalMissingPrimitive* null_local = new GraalMissingPrimitive(this, java_null, false);
    null_instance_ = reinterpret_cast<GraalPrimitive*> (null_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kNullValueRootIndex] = null_instance_;
    delete null_local;

    // True
    GraalBoolean* true_local = new GraalBoolean(this, true);
    true_instance_ = reinterpret_cast<GraalBoolean*> (true_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kTrueValueRootIndex] = true_instance_;
    delete true_local;

    // False
    GraalBoolean* false_local = new GraalBoolean(this, false);
    false_instance_ = reinterpret_cast<GraalBoolean*> (false_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kFalseValueRootIndex] = false_instance_;
    delete false_local;

    // EmptyString
    const jchar empty_string[] = {};
    GraalString* empty_string_local = new GraalString(this, env->NewString(empty_string, 0));
    GraalString* empty_string_global = reinterpret_cast<GraalString*> (empty_string_local->Copy(true));
    slot[root_offset + v8::internal::Internals::kEmptyStringRootIndex] = empty_string_global;
    delete empty_string_local;

    // int32 placeholder
    JNI_CALL(jobject, java_int32_placeholder, this, GraalAccessMethod::isolate_get_int_placeholder, Object);
    GraalNumber* int32_placeholder_local = new GraalNumber(this, 0, java_int32_placeholder);
    GraalNumber* int32_placeholder_global = reinterpret_cast<GraalNumber*> (int32_placeholder_local->Copy(true));
    int32_placeholder_ = int32_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kInt32ReturnValuePlaceholderIndex] = int32_placeholder_global;

    // uint32 placeholder
    JNI_CALL(jobject, java_uint32_placeholder, this, GraalAccessMethod::isolate_get_large_int_placeholder, Object);
    GraalNumber* uint32_placeholder_local = new GraalNumber(this, 0, java_uint32_placeholder);
    GraalNumber* uint32_placeholder_global = reinterpret_cast<GraalNumber*> (uint32_placeholder_local->Copy(true));
    uint32_placeholder_ = uint32_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kUint32ReturnValuePlaceholderIndex] = uint32_placeholder_global;

    // double placeholder
    JNI_CALL(jobject, java_double_placeholder, this, GraalAccessMethod::isolate_get_double_placeholder, Object);
    GraalNumber* double_placeholder_local = new GraalNumber(this, 0, java_double_placeholder);
    GraalNumber* double_placeholder_global = reinterpret_cast<GraalNumber*> (double_placeholder_local->Copy(true));
    double_placeholder_ = double_placeholder_global->GetJavaObject();
    slot[root_offset + v8::internal::Internals::kDoubleReturnValuePlaceholderIndex] = double_placeholder_global;

    // InternalFieldCountKey
    JNI_CALL(jobject, internal_field_count_key, this, GraalAccessMethod::isolate_create_internal_field_count_key, Object);
    if (internal_field_count_key == NULL) EXIT_WITH_MESSAGE(env, "GraalJSAccess.isolateCreateInternalFieldCountKey() failed!\n")
    GraalValue* internal_field_count_key_local = new GraalObject(this, internal_field_count_key);
    internal_field_count_key_ = reinterpret_cast<v8::Value*> (internal_field_count_key_local->Copy(true));
    delete internal_field_count_key_local;

    sending_message_ = false;
    abort_on_uncaught_exception_callback_ = nullptr;
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
    if (field_info_array != NULL && env->GetArrayLength(field_info_array) == field_count * 2) {
        for (int i = 0; i < field_count; i++) {
            jobject class_obj = env->GetObjectArrayElement(field_info_array, i * 2);
            jobject field_name_obj = env->GetObjectArrayElement(field_info_array, i * 2 + 1);
            SetJNIField(static_cast<GraalAccessField>(i), class_obj, field_name_obj, "Ljava/lang/Object;");
        }
    }
}

void GraalIsolate::SetJNIField(GraalAccessField id, jobject holder_class_obj, jobject field_name_obj, const char* sig) {
    JNIEnv* env = GetJNIEnv();
    if (holder_class_obj == NULL || field_name_obj == NULL) {
        return;
    }
    jclass holder_class = static_cast<jclass>(holder_class_obj);
    jstring field_name_string = static_cast<jstring>(field_name_obj);
    const char* field_name_utf = env->GetStringUTFChars(field_name_string, NULL);
    jfieldID field = env->GetFieldID(holder_class, field_name_utf, sig);
    env->ReleaseStringUTFChars(field_name_string, field_name_utf);
    SetJNIField(id, field);
}

bool GraalIsolate::AddMessageListener(v8::MessageCallback callback, v8::Local<v8::Value> data) {
    message_listener_ = callback;
    return true;
}

void GraalIsolate::SendMessage(v8::Local<v8::Message> message, v8::Local<v8::Value> error, jthrowable java_error) {
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
}

void GraalIsolate::InitThreadLocals() {
    uv_key_create(&current_isolate_key);
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

v8::Local<v8::Value> GraalIsolate::InternalFieldKey(int index) {
    if (index >= (int) internal_field_keys.size()) {
        internal_field_keys.resize(index + 1);
    }
    if (internal_field_keys[index] == nullptr) {
        JNI_CALL(jobject, key, this, GraalAccessMethod::isolate_create_internal_field_key, Object, (jint) index);
        GraalValue* key_local = new GraalObject(this, key);
        v8::Value* key_global = reinterpret_cast<v8::Value*> (key_local->Copy(true));
        delete key_local;
        internal_field_keys[index] = key_global;
    }
    return internal_field_keys[index];
}

void GraalIsolate::Dispose() {
    Dispose(main_, 0);
}

void GraalIsolate::Dispose(bool exit, int status) {
    JNIEnv* env = jni_env_;
    jni_env_ = nullptr; // mark the isolate as disposed, see ~GraalHandleContent()

    // we do not use JNI_CALL_VOID because jni_env_ is cleared
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_dispose);
    env->functions->CallVoidMethod(env, access_, method_id, exit, status);

    // this is executed when exit is false only
    env->ExceptionClear();
    jvm_->DetachCurrentThread();
    uv_key_set(&current_isolate_key, nullptr);
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
            delete graal_number;
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

void GraalIsolate::InitStackOverflowCheck(long stack_bottom) {
    char* stack_size_str = getenv("NODE_STACK_SIZE");
    size_t stack_size = 0;
    if (stack_size_str != nullptr) {
        stack_size = strtol(stack_size_str, nullptr, 10);
    }
    if (stack_size > 0) {
        stack_check_enabled_ = true;
        stack_bottom_ = stack_bottom;
        stack_size_limit_ = stack_size - 150000;
    }
}

// This is a poor-man's check that attempts to avoid stack-overflow
// during invocation of an average native JavaScript function.
// It's main purpose is to avoid stack-overflow during JNI calls
// back to Graal.js engine, it does not handle possible large stack
// demands of the user-implemented parts of the native function.
// It is an experimental feature with a very naive implementation.
// It should be replaced by more sophisticated techniques if it
// turns out to be useful.
bool GraalIsolate::StackOverflowCheck(long stack_top) {
    if (labs(stack_top - stack_bottom_) > stack_size_limit_) {
        JNI_CALL_VOID(this, GraalAccessMethod::isolate_throw_stack_overflow_error);
        return true;
    }
    return false;
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
        // The following line breaks test-vm-timeout-rethrow
        // env->ExceptionClear(); // Clear potential KillException
    }
    jmethodID method_id = GetJNIMethod(GraalAccessMethod::isolate_cancel_terminate_execution);
    env->functions->CallVoidMethod(env, access_, method_id);
    if (current_isolate == nullptr) {
        jvm_->DetachCurrentThread();
    }
}

GraalValue* GraalIsolate::CacheFunctionTemplateFunction(unsigned id) {
    JNI_CALL(jobject, java_function, this, GraalAccessMethod::function_template_get_function_to_cache, Object, (jint) id);
    GraalValue* function = GraalValue::FromJavaObject(this, java_function);
    SetFunctionTemplateFunction(id, function);
    return function;
}

void GraalIsolate::SetFunctionTemplateFunction(unsigned id, GraalValue* function) {
    while (function_template_functions.size() <= id) function_template_functions.push_back(nullptr);
    GraalValue* cached_function = reinterpret_cast<GraalValue*> (function->Copy(true));
    cached_function->MakeWeak();
    cached_function->ReferenceAdded();
    function_template_functions[id] = cached_function;
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
            WriteInt32ToSharedBuffer((uint32_t) return_value_);
        } else if (result == uint32_placeholder_) {
            ResetSharedBuffer();
            WriteInt64ToSharedBuffer((uint64_t) return_value_);
        } else if (result == double_placeholder_) {
            ResetSharedBuffer();
            WriteDoubleToSharedBuffer((double) return_value_);
        }
        result = GetJNIEnv()->NewLocalRef(result);
    }
    return result;
}

v8::Local<v8::Context> GraalIsolate::GetDebugContext() {
    JNI_CALL(jobject, java_context, this, GraalAccessMethod::isolate_get_debug_context, Object);
    GraalContext* graal_context = new GraalContext(this, java_context);
    return reinterpret_cast<v8::Context*> (graal_context);
}

int GraalIsolate::argc = 0;
char** GraalIsolate::argv = nullptr;
int GraalIsolate::mode = GraalIsolate::kModeDefault;
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

void GraalIsolate::EnqueueMicrotask(v8::MicrotaskCallback microtask, void* data) {
    microtasks.push_back(std::pair<v8::MicrotaskCallback, void*>(microtask, data));
}

void GraalIsolate::RunMicrotasks() {
    for (std::pair<v8::MicrotaskCallback, void*> pair : microtasks) {
        pair.first(pair.second);
    }
    microtasks.clear();
    JNI_CALL_VOID(this, GraalAccessMethod::isolate_run_microtasks);
}
