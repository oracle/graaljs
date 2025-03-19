/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime;

import java.util.Objects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.Undefined;

@SuppressWarnings("serial")
@ImportStatic({JSConfig.class})
@ExportLibrary(InteropLibrary.class)
public final class JSException extends GraalJSException {

    private final JSErrorType type;
    private JSObject exceptionObj;
    private final JSRealm realm;
    private final boolean isIncompleteSource;

    private JSException(JSErrorType type, String message, Throwable cause, Node originatingNode, JSRealm realm, int stackTraceLimit) {
        super(message, cause, originatingNode, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = null;
        this.realm = Objects.requireNonNull(realm);
        this.isIncompleteSource = false;
    }

    private JSException(JSErrorType type, String message, Node originatingNode, JSObject exceptionObj, JSRealm realm, int stackTraceLimit) {
        super(message, originatingNode, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = exceptionObj;
        this.realm = Objects.requireNonNull(realm);
        this.isIncompleteSource = false;
    }

    private JSException(JSErrorType type, String message, SourceSection sourceLocation, JSRealm realm, int stackTraceLimit, boolean isIncompleteSource) {
        this(type, message, null, sourceLocation, realm, stackTraceLimit, isIncompleteSource);
    }

    private JSException(JSErrorType type, String message, Throwable cause, SourceSection sourceLocation, JSRealm realm, int stackTraceLimit, boolean isIncompleteSource) {
        super(message, cause, sourceLocation, stackTraceLimit);
        CompilerAsserts.neverPartOfCompilation("JSException constructor");
        this.type = type;
        this.exceptionObj = null;
        this.realm = Objects.requireNonNull(realm);
        this.isIncompleteSource = isIncompleteSource;
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, JSObject exceptionObj, JSRealm realm, int stackTraceLimit, JSDynamicObject skipFramesUpTo, boolean customSkip) {
        return fillInStackTrace(new JSException(type, message, null, exceptionObj, realm, stackTraceLimit), true, skipFramesUpTo, customSkip);
    }

    @TruffleBoundary
    public static JSException createCapture(JSErrorType type, String message, JSObject exceptionObj, JSRealm realm) {
        return createCapture(type, message, exceptionObj, realm, getStackTraceLimit(realm), Undefined.instance, false);
    }

    public static JSException create(JSErrorType type, String message, JSObject exceptionObj, JSRealm realm) {
        return create(type, message, (Node) null, exceptionObj, realm);
    }

    @TruffleBoundary
    public static JSException create(JSErrorType type, String message, Node originatingNode, JSObject exceptionObj, JSRealm realm) {
        return fillInStackTrace(new JSException(type, message, originatingNode, exceptionObj, realm, getStackTraceLimit(realm)), false);
    }

    public static JSException create(JSErrorType type, String message) {
        return create(type, message, (Node) null);
    }

    public static JSException create(JSErrorType type, String message, Node originatingNode) {
        JSRealm realm = JSRealm.get(originatingNode);
        return fillInStackTrace(new JSException(type, message, originatingNode, null, realm, getStackTraceLimit(realm)), false);
    }

    public static JSException create(JSErrorType type, String message, Throwable cause, Node originatingNode) {
        JSRealm realm = JSRealm.get(originatingNode);
        return fillInStackTrace(new JSException(type, message, cause, originatingNode, realm, getStackTraceLimit(realm)), false);
    }

    public static JSException create(JSErrorType type, String message, SourceSection sourceLocation, boolean isIncompleteSource) {
        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        return fillInStackTrace(new JSException(type, message, sourceLocation, realm, getStackTraceLimit(realm), isIncompleteSource), false);
    }

    public static JSException create(JSErrorType type, String message, Throwable cause, SourceSection sourceLocation, boolean isIncompleteSource) {
        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        return fillInStackTrace(new JSException(type, message, cause, sourceLocation, realm, getStackTraceLimit(realm), isIncompleteSource), false);
    }

    public static int getStackTraceLimit(JSRealm realm) {
        JSContextOptions contextOptions = realm.getContextOptions();
        if (contextOptions.isStackTraceAPI()) {
            JSFunctionObject errorConstructor = realm.getErrorConstructor(JSErrorType.Error);
            DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
            if (JSProperty.isData(lib.getPropertyFlagsOrDefault(errorConstructor, JSError.STACK_TRACE_LIMIT_PROPERTY_NAME, JSProperty.ACCESSOR))) {
                Object stackTraceLimit = lib.getOrDefault(errorConstructor, JSError.STACK_TRACE_LIMIT_PROPERTY_NAME, Undefined.instance);
                if (JSRuntime.isNumber(stackTraceLimit)) {
                    final long limit = JSRuntime.toInteger((Number) stackTraceLimit);
                    return (int) Math.max(0, Math.min(limit, Integer.MAX_VALUE));
                }
            }
            return 0;
        } else {
            return contextOptions.getStackTraceLimit();
        }
    }

    @Override
    @TruffleBoundary
    public String getMessage() {
        String message = getRawMessage();
        return (message == null || message.isEmpty()) ? type.name() : type.name() + ": " + message;
    }

    public String getRawMessage() {
        return super.getMessage();
    }

    public JSErrorType getErrorType() {
        return this.type;
    }

    @Override
    public JSObject getErrorObjectLazy() {
        return exceptionObj;
    }

    public void setErrorObject(JSObject exceptionObj) {
        this.exceptionObj = exceptionObj;
    }

    @TruffleBoundary
    @Override
    public Object getErrorObject() {
        JSObject jserror = exceptionObj;
        if (jserror == null) {
            String message = getRawMessage();
            exceptionObj = jserror = JSError.createFromJSException(this, this.realm, (message == null) ? "" : message);
        }
        return jserror;
    }

    public JSRealm getRealm() {
        return this.realm;
    }

    @ExportMessage
    public ExceptionType getExceptionType() {
        return this.type == JSErrorType.SyntaxError ? ExceptionType.PARSE_ERROR : ExceptionType.RUNTIME_ERROR;
    }

    @ExportMessage
    public boolean isExceptionIncompleteSource() {
        return isIncompleteSource;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public Object getMembers(boolean internal,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) throws UnsupportedMessageException {
        return delegateLib.getMembers(getErrorObject(), internal);
    }

    @ExportMessage
    public boolean isMemberReadable(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.isMemberReadable(getErrorObject(), key);
    }

    @ExportMessage
    public boolean isMemberModifiable(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.isMemberModifiable(getErrorObject(), key);
    }

    @ExportMessage
    public boolean isMemberInsertable(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.isMemberInsertable(getErrorObject(), key);
    }

    @ExportMessage
    public boolean isMemberRemovable(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.isMemberRemovable(getErrorObject(), key);
    }

    @ExportMessage
    public boolean isMemberInvocable(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.isMemberInvocable(getErrorObject(), key);
    }

    @ExportMessage
    public boolean hasMemberReadSideEffects(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.hasMemberReadSideEffects(getErrorObject(), key);
    }

    @ExportMessage
    public boolean hasMemberWriteSideEffects(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.hasMemberWriteSideEffects(getErrorObject(), key);
    }

    @ExportMessage
    public Object readMember(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) throws UnknownIdentifierException, UnsupportedMessageException {
        return delegateLib.readMember(getErrorObject(), key);
    }

    @ExportMessage
    public void writeMember(String key, Object value,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        delegateLib.writeMember(getErrorObject(), key, value);
    }

    @ExportMessage
    public void removeMember(String key,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) throws UnsupportedMessageException, UnknownIdentifierException {
        delegateLib.removeMember(getErrorObject(), key);
    }

    @ExportMessage
    public Object invokeMember(String key, Object[] args,
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib)
                    throws UnsupportedMessageException, UnknownIdentifierException, ArityException, UnsupportedTypeException {
        return delegateLib.invokeMember(getErrorObject(), key, args);
    }

    @ExportMessage
    public boolean hasMetaObject(
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) {
        return delegateLib.hasMetaObject(getErrorObject());
    }

    @ExportMessage
    public Object getMetaObject(
                    @CachedLibrary(limit = "InteropLibraryLimit") @Shared InteropLibrary delegateLib) throws UnsupportedMessageException {
        return delegateLib.getMetaObject(getErrorObject());
    }

    public static void ensureInitialized() throws ClassNotFoundException {
        // Ensure InteropLibrary is initialized, too.
        Class.forName(JSExceptionGen.class.getName());
        Class.forName(GraalJSExceptionGen.class.getName());
        Class.forName(UserScriptExceptionGen.class.getName());
    }
}
