#include <jni.h>
#include <stdio.h>
#include <string.h>

#include <v8.h>
#include <node.h>
#include <libplatform/libplatform.h>
#include <graal_isolate.h>
#include <env-inl.h>
#include <graal_object.h>
#include <callbacks.h>

#include "graal_threading.h"

// Header for com_oracle_truffle_trufflenode_GraalJSInstanceRunner
#ifndef _Included_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
#define _Included_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
#ifdef __cplusplus
extern "C" {
#endif

using namespace node;
using namespace v8;
using namespace graal::threading;

void async_execute_callback(uv_async_t *handle) {
  AsyncHandle* async = reinterpret_cast<AsyncHandle*>(handle->data);
  JNIEnv* env = async->GetJNIEnv();
  jobject lambda = async->GetCallback();
  jclass klass = env->GetObjectClass(lambda);
  jmethodID mid = env->GetMethodID(klass, "send", "()V");
  if (mid == 0) {
    fprintf(stderr, "Fatal: cannot execute async handle callback!\n");
    exit(1);
  }
  env->CallObjectMethod(lambda, mid);
}

AsyncHandle::AsyncHandle(JNIEnv* env, uv_loop_t* loopPtr, jobject lambda) {
  this->jniEnv = env;
  this->loop = loopPtr;
  this->async = new uv_async_t();
  uv_async_init(this->loop, this->async, async_execute_callback);
  this->callback = env->NewGlobalRef(lambda);
}

AsyncHandle::~AsyncHandle() {
  this->jniEnv->DeleteGlobalRef(this->callback);
  delete this->async;
}

void AsyncHandle::Send() {
  this->async->data = (void*)this;
  uv_async_send(this->async);
}

JNIEnv* AsyncHandle::GetJNIEnv() {
  return this->jniEnv;
}

jobject AsyncHandle::GetCallback() {
  return this->callback;
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    fprintf(stderr, "Failed to get the environment\n");
    exit(1);
  }
  env->FindClass("com/oracle/truffle/trufflenode/GraalJSAccess");
  if (env->ExceptionCheck()) {
    fprintf(stderr, "JNI_OnLoad failed! Are you loading libnode.so without trufflenode.jar in your path?\n");
    env->ExceptionDescribe();
    env->ExceptionClear();
    exit(1);
  }
  return JNI_VERSION_1_6;
}

/*
 * Class:     com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
 * Method:    nodeRegisterHandle
 * Signature: (JLjava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeRegisterHandle(JNIEnv *env, jobject obj, jlong loop, jobject lambda) {
  uv_loop_t* evLoop = reinterpret_cast<uv_loop_t*>(loop);
  AsyncHandle* handle = new AsyncHandle(env, evLoop, lambda);
  return (jlong)handle;
}

/*
 * Class:     com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
 * Method:    nodeAsyncHandleSend
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeAsyncHandleSend(JNIEnv *env, jobject obj, jlong handlePtr) {
  AsyncHandle* handle = reinterpret_cast<AsyncHandle*>(handlePtr);
  handle->Send();
}

/*
 * Class:     com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
 * Method:    nodeAsyncHandleSend
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeAsyncHandleDispose(JNIEnv *env, jobject obj, jlong handlePtr) {
  AsyncHandle* handle = reinterpret_cast<AsyncHandle*>(handlePtr);
  delete handle;
}

/*
 * Class:     com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
 * Method:    nodeGlobalInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeGlobalInit(JNIEnv *env, jclass klass) {
  if (!RegisterCallbacks(env, env->FindClass("com/oracle/truffle/trufflenode/NativeAccess"))) {
    exit(1);
  }
  GraalIsolate::InitThreadLocals();
}

/*
 * Class:     com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner
 * Method:    nodeRunLoop
 * Signature: ([Ljava/lang/String;Lcom/oracle/truffle/trufflenode/threading/GraalJSInstanceRunner$NodeJSLoopActivationCallback;)J
 */
JNIEXPORT jlong JNICALL Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeRunLoop(JNIEnv *env, jobject obj, jobjectArray jargv, jobject lambda) {
  long jargs = env->GetArrayLength(jargv);
  long argc = (long) jargs+1;
  char** args = new char*[jargs+1];

  for (int i=0; i<argc-1; i++) {
    jstring string = (jstring) env->GetObjectArrayElement(jargv, i);
    const char *argv = env->GetStringUTFChars(string, 0);
    args[i+1] = (char*)argv;
  }
  // The first entry has to be the executable name
  args[0] = const_cast<char*>("node");
  const v8::Isolate::CreateParams params;
  Isolate* isolate = Isolate::New(params);

  {
    Local<Context> context = Context::New(isolate);
    context->Enter();

    uv_loop_t evloop;
    uv_loop_init(&evloop);

    Environment* node_env = CreateEnvironment(
                              CreateIsolateData(isolate, &evloop),
                              context,
                              argc,
                              args,
                              argc,
                              args
                            );

    // call a java lambda to patch the new loop's env and init extra globals
    jclass klass = env->GetObjectClass(lambda);
    jmethodID mid = env->GetMethodID(klass, "init", "(Ljava/lang/Object;J)V");
    if (mid == 0) {
      fprintf(stderr, "Fatal: cannot init new Graal.js thread\n");
      // TODO should just throw?
      exit(1);
    }
    Local<Object> v8proc = node_env->process_object();
    GraalObject* gproc = reinterpret_cast<GraalObject*>(*v8proc);

    env->CallObjectMethod(lambda, mid, gproc->GetJavaObject(), (jlong)&evloop);

    LoadEnvironment(node_env);

    uv_run(&evloop, UV_RUN_DEFAULT);

    context->Exit();
    node_env->CleanupHandles();
  }
  isolate->Dispose();
  return 0;
}

// Utility functions to load native functions at runtime from node when running in non-library mode
namespace graal {
namespace threading {

#define CALLBACK_COUNT 4
#define CALLBACK(name, signature, pointer) {const_cast<char*>(name), const_cast<char*>(signature), pointer}

static const JNINativeMethod callbacks[CALLBACK_COUNT] = {
    CALLBACK("nodeRunLoop", "([Ljava/lang/String;Lcom/oracle/truffle/trufflenode/threading/GraalJSInstanceRunner$NodeJSLoopActivationCallback;)J", (void *) &Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeRunLoop),
    CALLBACK("nodeRegisterHandle", "(JLcom/oracle/truffle/trufflenode/threading/GraalJSInstanceRunner$GraalJSAsyncHandleCallback;)J", (void *) &Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeRegisterHandle),
    CALLBACK("nodeAsyncHandleSend", "(J)V", (void *) &Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeAsyncHandleSend),
    CALLBACK("nodeAsyncHandleDispose", "(J)V", (void *) &Java_com_oracle_truffle_trufflenode_threading_GraalJSInstanceRunner_nodeAsyncHandleDispose)
};

void RegisterNativeCallbacks() {
  // Register a pointer to this event loop in the JSContext.
  // We assume that this method is *always* called from the default loop
  if (Environment::GetCurrent(CurrentIsolate()->GetCurrent())->event_loop() != uv_default_loop()) {
    fprintf(stderr, "Threading support must be initialized from the main event loop!\n");
    exit(1);
  }

  // Register the jni interfaces
  JNIEnv* env = CurrentIsolate()->GetJNIEnv();
  jclass inst_class = env->FindClass("com/oracle/truffle/trufflenode/threading/GraalJSInstanceRunner");
  if (inst_class == NULL) {
    fprintf(stderr, "GraalJSInstanceRunner class not found!\n");
    exit(1);
  }
  for (int i = 0; i < CALLBACK_COUNT; i++) {
    JNINativeMethod callback = callbacks[i];
    jmethodID id = env->GetMethodID(inst_class, callback.name, callback.signature);
    if (id == NULL) {
      fprintf(stderr, "Cannot find method %s%s of GraalJSInstanceRunner!\n", callback.name, callback.signature);
      exit(1);
    }
  }
  env->RegisterNatives(inst_class, callbacks, CALLBACK_COUNT);

  // The following code is equivalent to the following java call
  // JSContext.setNodeServer(new GraalJSInstanceRunner(/* loopPointer */));
  uv_loop_t* evloop = uv_default_loop();
  jmethodID inst_constructor = env->GetMethodID(inst_class, "<init>", "(J)V");
  jobject new_inst = env->NewObject(inst_class, inst_constructor, (jlong)evloop);

  jobject context_obj = CurrentIsolate()->CurrentJavaContext();
  jclass jscontext_class = env->FindClass("com/oracle/truffle/js/runtime/JSContext");
  jmethodID set_server = env->GetMethodID(jscontext_class, "setNodeServer", "(Ljava/lang/Object;)V");

  env->CallObjectMethod(context_obj, set_server, new_inst);
}

} // namespace threading
} // namespace graal

#ifdef __cplusplus
}
#endif
#endif
