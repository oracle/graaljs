/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSCopyableObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic({JSConfig.class})
@ExportLibrary(InteropLibrary.class)
public final class JSErrorObject extends JSNonProxyObject implements JSCopyableObject {

    protected JSErrorObject(Shape shape) {
        super(shape);
    }

    public static DynamicObject create(Shape shape) {
        return new JSErrorObject(shape);
    }

    public static DynamicObject create(JSRealm realm, JSObjectFactory factory) {
        return factory.initProto(new JSErrorObject(factory.getShape(realm)), realm);
    }

    @Override
    protected JSObject copyWithoutProperties(Shape shape) {
        return new JSErrorObject(shape);
    }

    public GraalJSException getException() {
        return JSError.getException(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isException() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public RuntimeException throwException() {
        throw getException();
    }

    @ExportMessage
    public ExceptionType getExceptionType(
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionType(getException());
    }

    @ExportMessage
    public boolean isExceptionIncompleteSource(
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.isExceptionIncompleteSource(getException());
    }

    @ExportMessage
    public boolean hasExceptionMessage(
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) {
        return exceptions.hasExceptionMessage(getException());
    }

    @ExportMessage
    public Object getExceptionMessage(
                    @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary exceptions) throws UnsupportedMessageException {
        return exceptions.getExceptionMessage(getException());
    }

    @ExportMessage
    public static final class IsIdenticalOrUndefined {
        @Specialization
        public static TriState doError(JSErrorObject receiver, JSDynamicObject other) {
            return TriState.valueOf(receiver == other);
        }

        @Specialization
        public static TriState doException(JSErrorObject receiver, GraalJSException other) {
            return TriState.valueOf(receiver == other.getErrorObject());
        }

        @SuppressWarnings("unused")
        @Fallback
        public static TriState doOther(JSErrorObject receiver, Object other) {
            return TriState.UNDEFINED;
        }
    }

    public static void ensureInitialized() throws ClassNotFoundException {
        // Ensure InteropLibrary is initialized, too.
        Class.forName(JSErrorObjectGen.class.getName());
    }
}
