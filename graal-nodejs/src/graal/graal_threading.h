#include <jni.h>

#ifndef GRAAL_THREADING
#define GRAAL_THREADING
#ifdef __cplusplus
extern "C" {
#endif

namespace graal {
namespace threading {

void RegisterNativeCallbacks();

class AsyncHandle {
public:
  AsyncHandle(JNIEnv* env, uv_loop_t* loopPtr, jobject lambda);
  ~AsyncHandle();
  void Send();
  JNIEnv* GetJNIEnv();
  jobject GetCallback();
private:
  JNIEnv* jniEnv;
  uv_loop_t* loop;
  uv_async_t* async;
  jobject callback;
};

} // namespace threading
} // namespace graal

#ifdef __cplusplus
}
#endif
#endif
