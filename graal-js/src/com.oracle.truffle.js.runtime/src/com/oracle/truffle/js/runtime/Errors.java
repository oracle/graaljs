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
package com.oracle.truffle.js.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Utility class to to create all kinds of ECMAScript-defined Error Objects.
 */
public final class Errors {

    private Errors() {
        // don't instantiate this
    }

    @TruffleBoundary
    public static JSException createError(String message) {
        return JSException.create(JSErrorType.Error, message);
    }

    @TruffleBoundary
    public static JSException createRangeError(String message) {
        return JSException.create(JSErrorType.RangeError, message);
    }

    @TruffleBoundary
    public static JSException createRangeError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.RangeError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createURIError(String message) {
        return JSException.create(JSErrorType.URIError, message);
    }

    @TruffleBoundary
    public static JSException createTypeError(String message) {
        return JSException.create(JSErrorType.TypeError, message);
    }

    @TruffleBoundary
    public static JSException createTypeErrorFormat(String message, Object... args) {
        return JSException.create(JSErrorType.TypeError, String.format(message, args));
    }

    @TruffleBoundary
    public static JSException createTypeError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.TypeError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorDateTimeFormatExpected() {
        return createTypeError("DateTimeFormat object expected.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorCanNotMixBigIntWithOtherTypes() {
        return createTypeError("Cannot mix BigInt and other types, use explicit conversions.");
    }

    @TruffleBoundary
    public static JSException createErrorCanNotConvertToBigInt(JSErrorType type, Object x) {
        return JSException.create(type, String.format("Cannot convert %s to a BigInt.", JSRuntime.safeToString(x)));
    }

    @TruffleBoundary
    public static JSException createTypeErrorCanNotConvertBigIntToNumber() {
        return createTypeError("Cannot convert a BigInt value to a number.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAFunction(Object functionObj) {
        return createTypeErrorNotAFunction(functionObj, null);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAFunction(Object functionObj, Node originatingNode) {
        assert !JSFunction.isJSFunction(functionObj); // don't lie to me
        return JSException.create(JSErrorType.TypeError, String.format("%s is not a function", JSRuntime.safeToString(functionObj)), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNumberFormatExpected() {
        return createTypeError("NumberFormat object expected.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorPluralRulesExpected() {
        return createTypeError("PluralRules object expected.");
    }

    @TruffleBoundary
    public static JSException createSyntaxError(String message, Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.SyntaxError, message, cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createSyntaxError(String message) {
        return JSException.create(JSErrorType.SyntaxError, message);
    }

    @TruffleBoundary
    public static JSException createSyntaxError(String message, SourceSection sourceLocation) {
        return JSException.create(JSErrorType.SyntaxError, message, sourceLocation);
    }

    @TruffleBoundary
    public static JSException createReferenceError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.ReferenceError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createReferenceError(String message, Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.ReferenceError, message, cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createReferenceError(String message) {
        return JSException.create(JSErrorType.ReferenceError, message);
    }

    @TruffleBoundary
    public static JSException createReferenceError(String message, SourceSection sourceLocation) {
        return JSException.create(JSErrorType.ReferenceError, message, sourceLocation);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotObjectCoercible(Object value) {
        return createTypeErrorNotObjectCoercible(value, null);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotObjectCoercible(Object value, Node originatingNode) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return Errors.createTypeErrorNotAnObject(value, originatingNode);
        }
        return Errors.createTypeError("Cannot convert undefined or null to object: " + JSRuntime.safeToString(value), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAnObject(Object value) {
        return Errors.createTypeErrorNotAnObject(value, null);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAnObject(Object value, Node originatingNode) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not an Object", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorIterResultNotAnObject(Object value, Node originatingNode) {
        return Errors.createTypeErrorNotAnObject(value, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidPrototype(Object value) {
        return Errors.createTypeError("Object prototype may only be an Object or null: " + JSRuntime.safeToString(value));
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidInstanceofTarget(Object target, Node originatingNode) {
        if (!JSRuntime.isObject(target)) {
            return Errors.createTypeError("Right-hand-side of instanceof is not an object", originatingNode);
        } else {
            assert !JSRuntime.isCallable(target);
            return Errors.createTypeError("Right-hand-side of instanceof is not callable", originatingNode);
        }
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToPrimitiveValue() {
        return Errors.createTypeError("Cannot convert object to primitive value");
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToPrimitiveValue(Node originatingNode) {
        return Errors.createTypeError("Cannot convert object to primitive value", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToString(String what) {
        return Errors.createTypeErrorCannotConvertToString(what, null);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToString(String what, Node originatingNode) {
        return Errors.createTypeError("Cannot convert " + what + " to a string", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToNumber(String what) {
        return Errors.createTypeErrorCannotConvertToNumber(what, null);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertToNumber(String what, Node originatingNode) {
        return Errors.createTypeError("Cannot convert " + what + " to a number", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorIncompatibleReceiver(String methodName, Object receiver) {
        return Errors.createTypeError("Method " + methodName + " called on incompatible receiver " + JSRuntime.safeToString(receiver));
    }

    @TruffleBoundary
    public static JSException createTypeErrorIncompatibleReceiver(Object what) {
        return Errors.createTypeError("incompatible receiver: " + JSRuntime.safeToString(what));
    }

    @TruffleBoundary
    public static JSException createTypeErrorProtoCycle(DynamicObject thisObj) {
        return Errors.createTypeError("Cannot create__proto__ cycle for " + JSObject.defaultToString(thisObj));
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotWritableProperty(Object key, Object thisObj) {
        return Errors.createTypeError(keyToString(key) + " is not a writable property of " + JSRuntime.safeToString(thisObj));
    }

    @TruffleBoundary
    public static JSException createTypeErrorLengthNotWritable() {
        return Errors.createTypeError("Cannot assign to read only property 'length'");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotConfigurableProperty(Object key) {
        return JSException.create(JSErrorType.TypeError, keyToString(key) + " is not a configurable property");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotExtensible(DynamicObject thisObj, Object key) {
        return Errors.createTypeError("Cannot add new property " + keyToString(key) + " to non-extensible " + JSObject.defaultToString(thisObj));
    }

    @TruffleBoundary
    public static JSException createTypeErrorConstReassignment(Object key, Object thisObj, Node originatingNode) {
        if (JSObject.isJSObject(thisObj) && JSObject.getJSContext((DynamicObject) thisObj).isOptionV8CompatibilityMode()) {
            throw Errors.createTypeError("Assignment to constant variable.", originatingNode);
        }
        throw Errors.createTypeError("Assignment to constant \"" + key + "\"", originatingNode);
    }

    private static String keyToString(Object key) {
        assert JSRuntime.isPropertyKey(key);
        return key instanceof String ? "\"" + key + "\"" : key.toString();
    }

    @TruffleBoundary
    public static JSException createReferenceErrorNotDefined(Object key, Node originatingNode) {
        return Errors.createReferenceError(quoteKey(key) + " is not defined", originatingNode);
    }

    private static String quoteKey(Object key) {
        return JSTruffleOptions.NashornCompatibilityMode ? "\"" + key + "\"" : key.toString();
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotConstructible(DynamicObject functionObj) {
        assert JSFunction.isJSFunction(functionObj);
        String message = String.format("%s is not a constructor function", JSRuntime.toString(functionObj));
        return JSException.create(JSErrorType.TypeError, message);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotRedefineProperty(Object key) {
        assert JSRuntime.isPropertyKey(key);
        return Errors.createTypeErrorFormat("Cannot redefine property %s", key);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotSetProperty(Object key, Object object, Node originatingNode) {
        assert JSRuntime.isPropertyKey(key);
        String errorMessage;
        if (JSTruffleOptions.NashornCompatibilityMode) {
            errorMessage = "Cannot set property \"" + key + "\" of " + JSRuntime.safeToString(object);
        } else {
            errorMessage = "Cannot set property '" + key + "' of " + JSRuntime.safeToString(object);
        }
        return createTypeError(errorMessage, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotSetAccessorProperty(Object key, DynamicObject store) {
        assert JSRuntime.isPropertyKey(key);
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return Errors.createTypeErrorFormat("Cannot set property \"%s\" of %s that has only a getter", key, JSObject.defaultToString(store));
        } else {
            return Errors.createTypeErrorFormat("Cannot redefine property %s which has only a getter", key);
        }
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotGetProperty(Object key, Object object, boolean isGetMethod, Node originatingNode) {
        assert JSRuntime.isPropertyKey(key);
        String errorMessage;
        if (JSTruffleOptions.NashornCompatibilityMode) {
            if (isGetMethod) {
                errorMessage = JSRuntime.safeToString(object) + " has no such function \"" + key + "\"";
            } else {
                if (object == Null.instance) {
                    errorMessage = "Cannot get property \"" + key + "\" of " + Null.NAME;
                } else {
                    errorMessage = "Cannot read property \"" + key + "\" from " + JSRuntime.safeToString(object);
                }
            }
        } else {
            errorMessage = "Cannot read property '" + key + "' of " + JSRuntime.safeToString(object);
        }
        return createTypeError(errorMessage, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorDetachedBuffer() {
        return Errors.createTypeError("Detached buffer");
    }

    @TruffleBoundary
    public static JSException createTypeErrorArrayBufferExpected() {
        return Errors.createTypeError("ArrayBuffer expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorArrayBufferViewExpected() {
        return Errors.createTypeError("TypedArray expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorCallableExpected() {
        return Errors.createTypeError("Callable expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorConstructorExpected() {
        return Errors.createTypeError("Constructor expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorGeneratorObjectExpected() {
        return Errors.createTypeError("Not a generator object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorAsyncGeneratorObjectExpected() {
        return Errors.createTypeError("Not an async generator object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotADataView() {
        return Errors.createTypeError("Not a DataView");
    }

    @TruffleBoundary
    public static JSException createTypeErrorGlobalObjectNotExtensible(Node originatingNode) {
        return Errors.createTypeError("Global object is not extensible", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorTooManyArguments() {
        return Errors.createRangeError("Maximum call stack size exceeded");
    }

    @TruffleBoundary
    public static JSException createRangeErrorBigIntMaxSizeExceeded() {
        return Errors.createRangeError("Maximum BigInt size exceeded");
    }

    @TruffleBoundary
    public static JSException createRangeErrorStackOverflow() {
        return Errors.createRangeError("Maximum call stack size exceeded");
    }

    @TruffleBoundary
    public static JSException createRangeErrorStackOverflow(Node originatingNode) {
        return Errors.createRangeError("Maximum call stack size exceeded", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidStringLength() {
        return Errors.createRangeError("Invalid string length");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidStringLength(Node originatingNode) {
        return Errors.createRangeError("Invalid string length", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidArrayLength() {
        return Errors.createRangeError("Invalid array length");
    }

    /**
     * @see #notYetImplemented(String)
     */
    public static RuntimeException notYetImplemented() {
        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * Similar to UnsupportedOperationException, but with a flavor of a missing feature that will be
     * resolved in the future. In contrast, UnsupportedOperationException should be used for
     * operations that are expected to be unsupported forever.
     */
    public static RuntimeException notYetImplemented(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new UnsupportedOperationException("not yet implemented: " + message);
    }

    public static RuntimeException shouldNotReachHere() {
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException("should not reach here");
    }

    public static RuntimeException shouldNotReachHere(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException("should not reach here: " + message);
    }

    @TruffleBoundary
    public static JSException createTypeErrorConfigurableExpected() {
        return createTypeError("configurable expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorSameResultExpected() {
        return createTypeError("same result expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorYieldStarThrowMethodMissing(Node originatingNode) {
        return createTypeError("yield* protocol violation: iterator does not have a throw method", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotDeletePropertyOf(Object propertyKey, Object object) {
        assert JSRuntime.isPropertyKey(propertyKey);
        return createTypeError("Cannot delete property " + JSRuntime.quote(propertyKey.toString()) + " of " + JSRuntime.safeToString(object));
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotDeletePropertyOfSealedArray(long index) {
        return createTypeErrorFormat("Cannot delete property \"%d\" of sealed array", index);
    }

    public static JSException createTypeErrorJSObjectExpected() {
        return createTypeError("only JavaScript objects are supported by this operation");
    }

    @TruffleBoundary
    public static JSException createTypeErrorTrapReturnedFalsish(String trap, Object propertyKey) {
        return createTypeError("'" + trap + "' on proxy: trap returned falsish for property '" + propertyKey + "'");
    }

    public static JSException createTypeErrorProxyRevoked() {
        return createTypeError("proxy has been revoked");
    }

    @TruffleBoundary
    public static JSException createTypeErrorInteropException(TruffleObject receiver, InteropException cause, Message message, Node originatingNode) {
        String reason = cause.getMessage();
        if (reason == null) {
            reason = cause.getClass().getSimpleName();
        }
        String receiverStr = "foreign object";
        TruffleLanguage.Env env = AbstractJavaScriptLanguage.getCurrentEnv();
        if (env.isHostObject(receiver)) {
            try {
                receiverStr = receiver.toString();
            } catch (Exception e) {
                // ignore
            }
        }
        return JSException.create(JSErrorType.TypeError, message + " on " + receiverStr + " failed due to: " + reason, cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotATruffleObject(Message message) {
        return Errors.createTypeError("cannot call " + message + " on a non-interop object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidIdentifier(Object identifier) {
        return Errors.createTypeError("Invalid identifier: " + JSRuntime.safeToString(identifier));
    }

    @TruffleBoundary
    public static JSException createTypeErrorClassNotFound(String className) {
        return Errors.createTypeErrorFormat("Access to host class %s is not allowed or does not exist.", className);
    }
}
