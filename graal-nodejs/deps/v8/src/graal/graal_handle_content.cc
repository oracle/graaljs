/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "graal_handle_content.h"
#include "graal_isolate.h"
#include "graal_value.h"
#include <string.h>

#include "graal_handle_content-inl.h"

GraalHandleContent::~GraalHandleContent() {
    DeleteJavaRef();
}

void GraalHandleContent::DeleteJavaRef() {
    JNIEnv* env = isolate_->GetJNIEnv();
    // env can be nullptr during the destruction of static variables
    // on process exit (when the isolate was disposed already)
    if (env != nullptr) {
        if (IsGlobal()) {
            if (IsWeak()) {
                env->DeleteWeakGlobalRef(java_object_);
            } else {
                env->DeleteGlobalRef(java_object_);
            }
        } else {
            env->DeleteLocalRef(java_object_);
        }
    }
#ifdef DEBUG
    if (ref_count != 0) {
        fprintf(stderr, "GraalHandleContent deleted while being referenced!\n");
    }
#endif
}

GraalHandleContent* GraalHandleContent::Copy(bool global) {
    jobject java_object = GetJavaObject();
    if (java_object != NULL) {
        if (global) {
            java_object = isolate_->GetJNIEnv()->NewGlobalRef(java_object);
        } else {
            java_object = isolate_->GetJNIEnv()->NewLocalRef(java_object);
        }
    }
    GraalHandleContent* copy = CopyImpl(java_object);
    if (global) {
        copy->ref_type_ = GLOBAL_FLAG;
        copy->ReferenceAdded();
    }
    return copy;
}

void GraalHandleContent::ClearWeak() {
    jobject java_object = java_object_;
    JNIEnv* env = isolate_->GetJNIEnv();
    java_object_ = env->NewGlobalRef(java_object);
    env->DeleteWeakGlobalRef(java_object);
    ref_type_ = GLOBAL_FLAG;
}

void GraalHandleContent::MakeWeak() {
    if (IsWeak()) {
        return;
    }
    jobject java_object = java_object_;
    JNIEnv* env = isolate_->GetJNIEnv();
    java_object_ = env->NewWeakGlobalRef(java_object);
    env->DeleteGlobalRef(java_object);
    ref_type_ = GLOBAL_FLAG | WEAK_FLAG;
}

bool GraalHandleContent::IsString() const {
    return false;
}

bool GraalHandleContent::SameData(GraalHandleContent* this_content, GraalHandleContent* that_content) {
    if (this_content == that_content) {
        return true;
    }
    jobject this_java = this_content->GetJavaObject();
    jobject that_java = that_content->GetJavaObject();
    if (this_java == NULL || that_java == NULL) {
        // If the handle content is not supported by jobject then it is
        // considered equal to itself only.
        return false;
    }
    JNIEnv* env = this_content->Isolate()->GetJNIEnv();
    if (env->IsSameObject(this_java, that_java)) {
        // Check for same jobjects
        return true;
    } else if (this_content->IsString() && that_content->IsString()) {
        // Check for same strings
        jobject this_string = this_content->GetJavaObject();
        jobject that_string = that_content->GetJavaObject();

        JNI_CALL(jboolean, result, this_content->Isolate(), GraalAccessMethod::string_equals, Boolean, this_string, that_string);
        return result;
    }
    return false;
}

jobject GraalHandleContent::ToNewLocalJavaObject() {
    return isolate_->GetJNIEnv()->NewLocalRef(java_object_);
}

bool GraalHandleContent::IsWeakCollected() const {
    return isolate_->GetJNIEnv()->IsSameObject(java_object_, NULL);
}

void GraalHandleContent::Recycle() {
    // Graal types override this method to pool object instances where appropriate.
    delete this;
}