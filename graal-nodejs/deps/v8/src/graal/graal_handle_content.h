/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef GRAAL_HANDLE_CONTENT_H_
#define GRAAL_HANDLE_CONTENT_H_

#include <stdio.h>

class GraalIsolate;

// GraalHandleContent leaks into v8.h that is included by native modules.
// The following two lines ensure that we do not have to bother about
// JNI includes when compiling native modules.
class _jobject;
typedef _jobject *jobject;

class GraalHandleContent {
public:
    GraalHandleContent(GraalIsolate* isolate, jobject java_object);
    virtual ~GraalHandleContent();
    GraalHandleContent* Copy(bool global);
    void MakeWeak();
    void ClearWeak();
    static bool SameData(GraalHandleContent* this_content, GraalHandleContent* that_content);
    virtual bool IsString() const;
    jobject ToNewLocalJavaObject();

    inline void ReferenceAdded() {
        ref_count++;
#ifdef DEBUG
        if (ref_count > 1000 || ref_count <= 0) {
            fprintf(stderr, "Reference counting error (add)?\n");
        }
#endif
    }

    inline void ReferenceRemoved() {
#ifdef DEBUG
        if (ref_count > 1000 || ref_count <= 0) {
            fprintf(stderr, "Reference counting error (rem)?\n");
        }
#endif
        if (--ref_count == 0) {
            delete this;
        }
    }

    inline GraalIsolate* Isolate() const {
        return isolate_;
    }

    inline jobject GetJavaObject() const {
        return java_object_;
    }

    inline bool IsWeak() const {
        return ((ref_type_ & WEAK_FLAG) != 0);
    }
protected:
    virtual GraalHandleContent* CopyImpl(jobject java_object_copy) = 0;
private:
    GraalIsolate* isolate_;
    jobject java_object_;
    int ref_type_;
    int ref_count;
    static const int GLOBAL_FLAG = 1;
    static const int WEAK_FLAG = 2;

    inline bool IsGlobal() const {
        return ((ref_type_ & GLOBAL_FLAG) != 0);
    }
};

#endif /* GRAAL_HANDLE_CONTENT_H_ */
