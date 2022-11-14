/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.buffer;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferObject;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

@ImportStatic(JSArrayBuffer.class)
public abstract class NIOBufferAccessNode extends JSBuiltinNode {

    private static final TruffleString CODE = Strings.constant("code");
    private static final TruffleString ERR_OUT_OF_RANGE = Strings.constant("ERR_OUT_OF_RANGE");
    private static final TruffleString ERR_BUFFER_OUT_OF_BOUNDS = Strings.constant("ERR_BUFFER_OUT_OF_BOUNDS");
    private static final TruffleString ERR_STRING_TOO_LONG = Strings.constant("ERR_STRING_TOO_LONG");
    private static final TruffleString ERR_INVALID_ARG_TYPE = Strings.constant("ERR_INVALID_ARG_TYPE");

    protected NIOBufferAccessNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    protected static ByteBuffer getDirectByteBuffer(JSArrayBufferObject arrayBuffer) {
        if (JSArrayBuffer.isJSDirectArrayBuffer(arrayBuffer)) {
            return JSArrayBuffer.getDirectByteBuffer(arrayBuffer);
        } else if (JSSharedArrayBuffer.isJSSharedArrayBuffer(arrayBuffer)) {
            return JSSharedArrayBuffer.getDirectByteBuffer(arrayBuffer);
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    protected final int getOffset(JSTypedArrayObject target) {
        return JSArrayBufferView.getByteOffset(target, getContext());
    }

    @TruffleBoundary
    protected static JSException indexOutOfRange() {
        throw setErrorCode(Errors.createRangeError("Index out of range"), ERR_OUT_OF_RANGE);
    }

    @TruffleBoundary
    protected static JSException offsetOutOfBounds() {
        throw setErrorCode(Errors.createRangeError("\"offset\" is outside of buffer bounds"), ERR_BUFFER_OUT_OF_BOUNDS);
    }

    @TruffleBoundary
    protected final JSException stringTooLong() {
        throw setErrorCode(Errors.createError(String.format("Cannot create a string longer than 0x%x characters", getContext().getStringLengthLimit())), ERR_STRING_TOO_LONG);
    }

    @TruffleBoundary
    protected static JSException notBuffer() {
        throw setErrorCode(Errors.createTypeError("argument must be a buffer"), ERR_INVALID_ARG_TYPE);
    }

    @TruffleBoundary
    private static JSException setErrorCode(JSException exception, TruffleString errorCode) {
        JSObject errorObject = (JSObject) exception.getErrorObject();
        JSObject.set(errorObject, CODE, errorCode);
        return exception;
    }
}
