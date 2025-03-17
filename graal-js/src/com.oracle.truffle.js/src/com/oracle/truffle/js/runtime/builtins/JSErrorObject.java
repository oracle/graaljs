/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.JSCopyableObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic({JSConfig.class})
@ExportLibrary(InteropLibrary.class)
public final class JSErrorObject extends JSNonProxyObject implements JSCopyableObject {

    private GraalJSException exception;

    protected JSErrorObject(Shape shape, JSDynamicObject proto) {
        super(shape, proto);
    }

    public static JSErrorObject create(Shape shape, JSDynamicObject proto) {
        return new JSErrorObject(shape, proto);
    }

    @Override
    protected JSObject copyWithoutProperties(Shape shape) {
        return new JSErrorObject(shape, getPrototypeOf());
    }

    public GraalJSException getException() {
        assert exception != null : this;
        return exception;
    }

    public void setException(GraalJSException exception) {
        this.exception = exception;
    }

    @Override
    public TruffleString getClassName() {
        return getBuiltinToStringTag();
    }

    @Override
    public TruffleString getBuiltinToStringTag() {
        return JSError.CLASS_NAME;
    }

    @ExportMessage
    public boolean isException() {
        return exception != null;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public RuntimeException throwException() {
        throw getException();
    }

    @ExportMessage
    public ExceptionType getExceptionType(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionType(getException());
    }

    @ExportMessage
    public boolean isExceptionIncompleteSource(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.isExceptionIncompleteSource(getException());
    }

    @ExportMessage
    public boolean hasExceptionMessage(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) {
        return exceptions.hasExceptionMessage(getException());
    }

    @ExportMessage
    public Object getExceptionMessage(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionMessage(getException());
    }

    @ExportMessage
    public boolean hasExceptionStackTrace(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) {
        return exceptions.hasExceptionStackTrace(getException());
    }

    @ExportMessage
    public Object getExceptionStackTrace(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionStackTrace(getException());
    }

    @ExportMessage
    public boolean hasExceptionCause(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) {
        return exceptions.hasExceptionCause(getException());
    }

    @ExportMessage
    public Object getExceptionCause(
                    @Shared @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionCause(getException());
    }

    @ExportMessage
    public static final class IsIdenticalOrUndefined {
        @Specialization
        public static TriState doError(JSErrorObject receiver, JSDynamicObject other) {
            return TriState.valueOf(receiver == other);
        }

        @Specialization
        public static TriState doException(JSErrorObject receiver, GraalJSException other) {
            return TriState.valueOf(receiver == other.getErrorObjectLazy());
        }

        @SuppressWarnings("unused")
        @Fallback
        public static TriState doOther(JSErrorObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    @TruffleBoundary
    @Override
    public TruffleString toDisplayStringImpl(boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return super.toDisplayStringImpl(allowSideEffects, format, depth);
        } else {
            return Strings.fromJavaString(getException().getMessage());
        }
    }

    public static void ensureInitialized() throws ClassNotFoundException {
        // Ensure InteropLibrary is initialized, too.
        Class.forName(JSErrorObjectGen.class.getName());
    }
}
