/*
 * Copyright (c) 2022, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;

public final class ThrowTypeErrorRootNode extends JavaScriptRealmBoundaryRootNode {
    private final boolean restrictedProperty;

    public ThrowTypeErrorRootNode(JavaScriptLanguage language, boolean restrictedProperty) {
        super(language, null, null);
        this.restrictedProperty = restrictedProperty;
    }

    @Override
    public Object executeInRealm(VirtualFrame frame) {
        if (restrictedProperty) {
            throw Errors.createTypeError("'caller', 'callee', and 'arguments' properties may not be accessed on strict mode functions or the arguments objects for calls to them");
        }
        // %ThrowTypeError% is used by ShadowRealm.prototype.importValue,
        // in which case we want to preserve the original error message.
        Object[] args = frame.getArguments();
        throw toTypeError(JSArguments.getUserArgumentCount(args) != 0 ? JSArguments.getUserArgument(args, 0) : Strings.EMPTY_STRING);
    }

    @TruffleBoundary
    private static AbstractTruffleException toTypeError(Object error) {
        // If the error is already a TypeError, we can just rethrow it.
        if (error instanceof JSErrorObject) {
            var exception = ((JSErrorObject) error).getException();
            if (exception instanceof JSException) {
                var jsException = (JSException) exception;
                if (jsException.getErrorType() == JSErrorType.TypeError && jsException.getRealm() == JSRealm.get(null)) {
                    return jsException;
                }
            } else {
                return Errors.createTypeError(getExceptionMessage(error), exception, null);
            }
        }

        return Errors.createTypeError(getExceptionMessage(error));
    }

    private static String getExceptionMessage(Object error) {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary interop = InteropLibrary.getUncached(error);
        if (interop.hasExceptionMessage(error)) {
            try {
                Object exceptionMessage = interop.getExceptionMessage(error);
                return InteropLibrary.getUncached().asString(exceptionMessage);
            } catch (UnsupportedMessageException e) {
                throw Errors.shouldNotReachHere(e);
            }
        }
        return Strings.toJavaString(JSRuntime.safeToString(error));
    }

    @Override
    public String getName() {
        return "%ThrowTypeError%";
    }
}
