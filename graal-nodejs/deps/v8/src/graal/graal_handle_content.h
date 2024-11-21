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

#ifndef GRAAL_HANDLE_CONTENT_H_
#define GRAAL_HANDLE_CONTENT_H_

#include <stdio.h>

class GraalIsolate;

// GraalHandleContent leaks into v8.h that is included by native modules.
// The following two lines ensure that we do not have to bother about
// JNI includes when compiling native modules.
class _jobject;
typedef _jobject *jobject;

#ifdef _WIN32

#ifdef BUILDING_V8_SHARED
# define V8_EXPORT __declspec(dllexport)
#elif USING_V8_SHARED
# define V8_EXPORT __declspec(dllimport)
#else
# define V8_EXPORT
#endif // BUILDING_V8_SHARED

#else  // _WIN32
# define V8_EXPORT
#endif // _WIN32

class V8_EXPORT GraalHandleContent {
public:
    inline GraalHandleContent(GraalIsolate* isolate, jobject java_object);
    virtual ~GraalHandleContent();
    GraalHandleContent* Copy(bool global);
    void MakeWeak();
    void ClearWeak();
    static bool SameData(GraalHandleContent* this_content, GraalHandleContent* that_content);
    virtual bool IsString() const;
    jobject ToNewLocalJavaObject();
    void DeleteJavaRef();

    inline void ReferenceAdded() {
        ref_count++;
// A weird workaround for a weird crash of graal-nodejs compiled by devkit:VS2019-16.9.3+1.
// node.exe terminates before! it reaches wmain() when this debugging code is commented out?!?
#if defined(DEBUG) || (defined(_MSC_VER) && _MSC_FULL_VER == 192829913)
        if (ref_count > 1000 || ref_count <= 0) {
            fprintf(stderr, "Reference counting error (add)?\n");
        }
#endif
    }

    inline void ReferenceRemoved() {
#if defined(DEBUG) || (defined(_MSC_VER) && _MSC_FULL_VER == 192829913)
        if (ref_count > 1000 || ref_count <= 0) {
            fprintf(stderr, "Reference counting error (rem)?\n");
        }
#endif
        if (--ref_count == 0) {
            Recycle();
        }
    }

    inline GraalIsolate* Isolate() const {
        return isolate_;
    }

    inline jobject GetJavaObject() const {
        return java_object_;
    }

    inline bool IsGlobal() const {
        return ((ref_type_ & GLOBAL_FLAG) != 0);
    }

    inline bool IsWeak() const {
        return ((ref_type_ & WEAK_FLAG) != 0);
    }

    inline bool IsEmpty() const {
        return IsWeak() ? IsWeakCollected() : false;
    }

protected:
    virtual GraalHandleContent* CopyImpl(jobject java_object_copy) = 0;
    virtual void Recycle();
    inline void ReInitialize(jobject java_object);

private:
    GraalIsolate* isolate_;
    jobject java_object_;
    int ref_type_;
    int ref_count;
    static const int GLOBAL_FLAG = 1;
    static const int WEAK_FLAG = 2;

    bool IsWeakCollected() const;
};

#endif /* GRAAL_HANDLE_CONTENT_H_ */
