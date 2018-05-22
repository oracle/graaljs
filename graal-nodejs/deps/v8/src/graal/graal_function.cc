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

#include "graal_function.h"
#include "graal_isolate.h"
#include "graal_string.h"

GraalFunction::GraalFunction(GraalIsolate* isolate, jobject java_function) : GraalObject(isolate, java_function) {
}

GraalHandleContent* GraalFunction::CopyImpl(jobject java_object_copy) {
    return new GraalFunction(Isolate(), java_object_copy);
}

bool GraalFunction::IsFunction() const {
    return true;
}

jobjectArray GraalFunction::CreateJavaObjectArray(int argc, v8::Local<v8::Value> argv[]) const {
    JNIEnv* jni_env = Isolate()->GetJNIEnv();
    jobjectArray array = jni_env->NewObjectArray(argc, Isolate()->GetObjectClass(), NULL);
    for (int i = 0; i < argc; i++) {
        GraalValue* graal_value = reinterpret_cast<GraalValue*> (*argv[i]);
        jobject element = graal_value->GetJavaObject();
        jni_env->SetObjectArrayElement(array, i, element);
    }
    return array;
}

v8::Local<v8::Object> GraalFunction::NewInstance(int argc, v8::Local<v8::Value> argv[]) const {
    JNIEnv* env = Isolate()->GetJNIEnv();
    env->ExceptionClear();
    jobjectArray arguments = CreateJavaObjectArray(argc, argv);
    JNI_CALL(jobject, java_object, Isolate(), GraalAccessMethod::function_new_instance, Object, GetJavaObject(), arguments);
    env->DeleteLocalRef(arguments);
    EXCEPTION_CHECK(env, v8::Object)
    GraalValue* graal_value = GraalValue::FromJavaObject(Isolate(), java_object);
    return reinterpret_cast<v8::Object*> (graal_value);
}

void GraalFunction::SetName(v8::Local<v8::String> name) {
    jobject java_name = reinterpret_cast<GraalString*> (*name)->GetJavaObject();
    JNI_CALL_VOID(Isolate(), GraalAccessMethod::function_set_name, GetJavaObject(), java_name);
}

v8::Local<v8::Value> GraalFunction::GetName() const {
    JNI_CALL(jobject, java_name, Isolate(), GraalAccessMethod::function_get_name, Object, GetJavaObject());
    GraalString* graal_name = new GraalString(Isolate(), (jstring) java_name);
    return reinterpret_cast<v8::Value*> (graal_name);
}

v8::Local<v8::Value> GraalFunction::Call(v8::Local<v8::Value> recv, int argc, v8::Local<v8::Value> argv[]) {
    jobject java_receiver = reinterpret_cast<GraalValue*> (*recv)->GetJavaObject();
    jobject java_object;
    switch (argc) {
        case 0:
            java_object = Call0(java_receiver);
            break;
        case 1:
            java_object = Call1(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject());
            break;
        case 2:
            java_object = Call2(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject());
            break;
        case 3:
            java_object = Call3(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject());
            break;
        case 4:
            java_object = Call4(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject());
            break;
        case 5:
            java_object = Call5(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[4])->GetJavaObject());
            break;
        case 6:
            java_object = Call6(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[4])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[5])->GetJavaObject());
            break;
        case 7:
            java_object = Call7(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[4])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[5])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[6])->GetJavaObject());
            break;
        case 8:
            java_object = Call8(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[4])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[5])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[6])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[7])->GetJavaObject());
            break;
        case 9:
            java_object = Call9(java_receiver,
                    reinterpret_cast<GraalValue*> (*argv[0])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[1])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[2])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[3])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[4])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[5])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[6])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[7])->GetJavaObject(),
                    reinterpret_cast<GraalValue*> (*argv[8])->GetJavaObject());
            break;
        default:
            jobjectArray arguments = CreateJavaObjectArray(argc, argv);
            java_object = Call(java_receiver, arguments);
    }
    if (java_object == NULL) {
        return v8::Local<v8::Value>();
    } else {
        Isolate()->ResetSharedBuffer();
        int32_t value_t = Isolate()->ReadInt32FromSharedBuffer();
        GraalValue* graal_value = GraalValue::FromJavaObject(Isolate(), java_object, value_t, true);
        return reinterpret_cast<v8::Object*> (graal_value);
    }
}

jobject GraalFunction::Call(jobject recv, int argc, jobject argv[]) {
    GraalIsolate* graal_isolate = Isolate();
    JNIEnv* env = graal_isolate->GetJNIEnv();
    jobjectArray array = env->NewObjectArray(argc, graal_isolate->GetObjectClass(), NULL);
    for (int i = 0; i < argc; i++) {
        env->SetObjectArrayElement(array, i, argv[i]);
    }
    return Call(recv, array);
}

jobject GraalFunction::CallResult(jobject java_object) {
    if (java_object == NULL) {
        GraalIsolate* graal_isolate = Isolate();
        JNIEnv* env = graal_isolate->GetJNIEnv();
        if (!graal_isolate->TryCatchExists()) {
            jthrowable java_exception = env->ExceptionOccurred();
            env->ExceptionClear();
            jobject java_context = graal_isolate->CurrentJavaContext();
            JNI_CALL(jobject, exception_object, graal_isolate, GraalAccessMethod::try_catch_exception, Object, java_context, java_exception);
            GraalValue* graal_exception = GraalValue::FromJavaObject(graal_isolate, exception_object);
            v8::Value* exception = reinterpret_cast<v8::Value*> (graal_exception);
            graal_isolate->SendMessage(v8::Exception::CreateMessage(exception), exception, java_exception);
            if (graal_isolate->error_to_ignore_ != nullptr) {
                env->DeleteGlobalRef(graal_isolate->error_to_ignore_);
                graal_isolate->error_to_ignore_ = nullptr;
            }
            if (graal_isolate->calls_on_stack_ != 0) {
                // If the process was not terminated then we have to ensure that
                // the rest of the script is not executed => we re-throw
                // the exception but remember that we should not report it again.
                graal_isolate->error_to_ignore_ = env->NewGlobalRef(java_exception);
                env->Throw(java_exception);
            }
        }
    }
    return java_object;
}

jobject GraalFunction::Call0(jobject recv) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call0, Object, GetJavaObject(), recv);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call1(jobject recv, jobject arg0) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call1, Object, GetJavaObject(), recv, arg0);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call2(jobject recv, jobject arg0, jobject arg1) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call2, Object, GetJavaObject(), recv, arg0, arg1);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call3(jobject recv, jobject arg0, jobject arg1, jobject arg2) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call3, Object, GetJavaObject(), recv, arg0, arg1, arg2);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}


jobject GraalFunction::Call4(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call4, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call5(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call5, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3, arg4);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call6(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call6, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3, arg4, arg5);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call7(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call7, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call8(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call8, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call9(jobject recv, jobject arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4, jobject arg5, jobject arg6, jobject arg7, jobject arg8) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call9, Object, GetJavaObject(), recv, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    graal_isolate->calls_on_stack_--;
    return CallResult(java_object);
}

jobject GraalFunction::Call(jobject recv, jobjectArray arguments) {
    GraalIsolate* graal_isolate = Isolate();
    graal_isolate->calls_on_stack_++;
    JNI_CALL(jobject, java_object, graal_isolate, GraalAccessMethod::function_call, Object, GetJavaObject(), recv, arguments);
    graal_isolate->calls_on_stack_--;
    JNIEnv* env = graal_isolate->GetJNIEnv();
    env->DeleteLocalRef(arguments);
    return CallResult(java_object);
}

v8::ScriptOrigin GraalFunction::GetScriptOrigin() const {
    GraalIsolate* graal_isolate = Isolate();
    JNI_CALL(jobject, java_resource_name, graal_isolate, GraalAccessMethod::function_resource_name, Object, GetJavaObject());
    GraalValue* resource_name = (java_resource_name == nullptr) ? graal_isolate->GetUndefined() : new GraalString(graal_isolate, (jstring) java_resource_name);
    return v8::ScriptOrigin(reinterpret_cast<v8::Value*> (resource_name));
}

int GraalFunction::GetScriptLineNumber() const {
    JNI_CALL(jint, line, Isolate(), GraalAccessMethod::function_get_script_line_number, Int, GetJavaObject());
    return line;
}

int GraalFunction::GetScriptColumnNumber() const {
    JNI_CALL(jint, column, Isolate(), GraalAccessMethod::function_get_script_column_number, Int, GetJavaObject());
    return column;
}
