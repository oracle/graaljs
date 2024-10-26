/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.js.parser.ParserException;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSNonProxy;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.Null;

/**
 * Utility class to to create all kinds of ECMAScript-defined Error Objects.
 */
public final class Errors {

    private Errors() {
        // don't instantiate this
    }

    @TruffleBoundary
    public static JSException createAggregateError(Object errors, String message, Node originatingNode) {
        JSContext context = JavaScriptLanguage.get(originatingNode).getJSContext();
        JSRealm realm = JSRealm.get(originatingNode);
        JSErrorObject errorObj = JSError.createErrorObject(context, realm, JSErrorType.AggregateError);
        if (message != null) {
            JSError.setMessage(errorObj, Strings.fromJavaString(message));
        }
        JSObjectUtil.putDataProperty(errorObj, JSError.ERRORS_NAME, errors, JSError.ERRORS_ATTRIBUTES);
        JSException exception = JSException.create(JSErrorType.AggregateError, message, errorObj, realm);
        JSError.setException(realm, errorObj, exception, false);
        return exception;
    }

    @TruffleBoundary
    public static JSException createAggregateError(Object errors, Node originatingNode) {
        return createAggregateError(errors, null, originatingNode);
    }

    @TruffleBoundary
    public static JSException createError(String message) {
        return JSException.create(JSErrorType.Error, message);
    }

    @TruffleBoundary
    public static JSException createEvalError(String message) {
        return JSException.create(JSErrorType.EvalError, message);
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
    public static JSException createRangeErrorFormat(String message, Node originatingNode, Object... args) {
        return JSException.create(JSErrorType.RangeError, String.format(message, args), originatingNode);
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
    public static JSException createTypeError(String message, Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.TypeError, message, cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotMixBigIntWithOtherTypes(Node originatingNode) {
        return createTypeError("Cannot mix BigInt and other types, use explicit conversions.", originatingNode);
    }

    @TruffleBoundary
    public static JSException createErrorCannotConvertToBigInt(JSErrorType type, Object value, Node originatingNode) {
        return JSException.create(type, String.format("Cannot convert %s to a BigInt.", JSRuntime.safeToString(value)), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotConvertBigIntToNumber(Node originatingNode) {
        return createTypeError("Cannot convert a BigInt value to a number.", originatingNode);
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
    public static JSException createTypeErrorNotAConstructor(Object object, JSContext context) {
        return createTypeErrorNotAConstructor(object, null, context);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAConstructor(Object object, Node originatingNode, JSContext context) {
        String msg = String.format(context.isOptionNashornCompatibilityMode() ? "%s is not a constructor function" : "%s is not a constructor", JSRuntime.safeToString(object));
        return JSException.create(JSErrorType.TypeError, msg, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorTypeXExpected(Object type) {
        return createTypeErrorFormat("%s object expected.", type);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCalledOnNonObject() {
        return createTypeError("called on non-object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorMethodCalledOnNonObjectOrWrongType(String method) {
        return createTypeErrorFormat("Method %s called on a non-object or on a wrong type of object.", method);
    }

    @TruffleBoundary
    public static JSException createTypeErrorSegmenterExpected() {
        return createTypeError("Segmenter object expected.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorSegmentsExpected() {
        return createTypeError("Segments object expected.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorDisplayNamesExpected() {
        return createTypeError("DisplayNames object expected.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorLocaleExpected() {
        return createTypeError("Locale object expected.");
    }

    private static TruffleString displayOperand(Object operand) {
        if (JSRuntime.isObject(operand)) {
            return JSRuntime.getConstructorName((JSObject) operand);
        } else if (operand instanceof Boolean) {
            return Strings.UC_BOOLEAN;
        } else if (JSRuntime.isNumber(operand) || operand instanceof Long) {
            return Strings.UC_NUMBER;
        } else if (Strings.isTString(operand)) {
            return Strings.UC_STRING;
        } else if (operand instanceof Symbol) {
            return Strings.UC_SYMBOL;
        } else {
            return JSRuntime.toString(operand);
        }
    }

    @TruffleBoundary
    public static JSException createTypeErrorNoOverloadFoundUnary(TruffleString operatorName, Object operand, Node originatingNode) {
        return createTypeError(String.format("No overload found for %s %s", Strings.toJavaString(operatorName), Strings.toJavaString(displayOperand(operand))), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNoOverloadFoundBinary(TruffleString operatorName, Object left, Object right, Node originatingNode) {
        return createTypeError(
                        String.format("No overload found for %s %s %s", Strings.toJavaString(displayOperand(left)), Strings.toJavaString(operatorName), Strings.toJavaString(displayOperand(right))),
                        originatingNode);
    }

    @TruffleBoundary
    public static JSException createSyntaxError(ParserException cause, JSContext context) {
        String message = context.isOptionV8CompatibilityMode() ? cause.getRawMessage() : cause.getMessage();
        return JSException.create(JSErrorType.SyntaxError, message, cause, null);
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
    public static JSException createSyntaxError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.SyntaxError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createSyntaxErrorFormat(String message, Node originatingNode, Object... args) {
        return JSException.create(JSErrorType.SyntaxError, String.format(message, args), originatingNode);
    }

    @TruffleBoundary
    public static JSException createSyntaxError(String message, Throwable cause, SourceSection sourceLocation, boolean isIncompleteSource) {
        return JSException.create(JSErrorType.SyntaxError, message, cause, sourceLocation, isIncompleteSource);
    }

    @TruffleBoundary
    public static JSException createSyntaxErrorVariableAlreadyDeclared(TruffleString varName, Node originatingNode) {
        return Errors.createSyntaxError("Variable \"" + varName + "\" has already been declared", originatingNode);
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
        return JSException.create(JSErrorType.ReferenceError, message, sourceLocation, false);
    }

    @TruffleBoundary
    public static JSException createReferenceErrorDerivedConstructorThisNotInitialized(Node originatingNode) {
        return createReferenceError("Must call super constructor in derived class before accessing 'this' or returning from derived constructor", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorDerivedConstructorReturnedIllegalType(Node originatingNode) {
        return createTypeError("Derived constructors may only return object or undefined", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorClassConstructorRequiresNew(TruffleString className, Node originatingNode) {
        if (className != null && !Strings.isEmpty(className)) {
            return createTypeError("Class constructor " + className + " cannot be invoked without 'new'", originatingNode);
        }
        return createTypeError("Class constructors cannot be invoked without 'new'", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotObjectCoercible(Object value, Node originatingNode) {
        JavaScriptLanguage language = JavaScriptLanguage.get(originatingNode);
        if (language.getJSContext().isOptionNashornCompatibilityMode()) {
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
    public static JSException createTypeErrorNotIterable(Object value, Node originatingNode) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not iterable", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotIterator(Object value, Node originatingNode) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not iterator", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorPropertyDescriptorNotAnObject(Object value, Node originatingNode) {
        return Errors.createTypeError("Property description must be an object: " + JSRuntime.safeToString(value), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidPrototype(Object value) {
        return Errors.createTypeError("Object prototype may only be an Object or null: " + JSRuntime.safeToString(value));
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidInstanceofTarget(Object target, Node originatingNode) {
        if (JSRuntime.isForeignObject(target)) {
            return Errors.createTypeError("Right-hand-side of instanceof is not a meta object", originatingNode);
        } else if (!JSRuntime.isObject(target)) {
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
    public static JSException createTypeErrorIncompatibleReceiver(TruffleString methodName, Object receiver) {
        return createTypeErrorIncompatibleReceiver(Strings.toJavaString(methodName), receiver);
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
    public static JSException createTypeErrorCannotSetProto(JSDynamicObject thisObj, JSDynamicObject proto) {
        if (!JSNonProxy.checkProtoCycle(thisObj, proto)) {
            if (JSObject.getJSContext(thisObj).isOptionNashornCompatibilityMode()) {
                return Errors.createTypeError("Cannot create__proto__ cycle for " + JSObject.defaultToString(thisObj));
            }
            return Errors.createTypeError("Cyclic __proto__ value");
        }
        throw Errors.createTypeError("Cannot set __proto__ of non-extensible " + JSObject.defaultToString(thisObj));
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotWritableProperty(Object key, Object thisObj, Node originatingNode) {
        String message;
        JavaScriptLanguage language = JavaScriptLanguage.get(originatingNode);
        if (language.getJSContext().isOptionNashornCompatibilityMode()) {
            message = quoteIfString(key) + " is not a writable property of " + JSRuntime.safeToString(thisObj);
        } else {
            message = "Cannot assign to read only property '" + keyToString(key) + "' of " + JSRuntime.safeToString(thisObj);
        }
        return Errors.createTypeError(message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotWritableProperty(long index, Object thisObj, Node originatingNode) {
        return createTypeErrorNotWritableProperty(Strings.fromLong(index), thisObj, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorLengthNotWritable() {
        return Errors.createTypeError("Cannot assign to read only property 'length'");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotConfigurableProperty(Object key) {
        return JSException.create(JSErrorType.TypeError, quoteIfString(key) + " is not a configurable property");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotExtensible(JSDynamicObject thisObj, Object key) {
        return Errors.createTypeError("Cannot add new property " + quoteIfString(key) + " to non-extensible " + JSObject.defaultToString(thisObj));
    }

    @TruffleBoundary
    public static JSException createTypeErrorSetNonObjectReceiver(Object receiver, Object key) {
        return Errors.createTypeError("Cannot add property " + quoteIfString(key) + " to non-object " + JSRuntime.safeToString(receiver));
    }

    @TruffleBoundary
    public static JSException createTypeErrorConstReassignment(Object key, Node originatingNode) {
        if (JavaScriptLanguage.get(originatingNode).getJSContext().isOptionV8CompatibilityMode()) {
            return Errors.createTypeError("Assignment to constant variable.", originatingNode);
        }
        return Errors.createTypeError("Assignment to constant \"" + key + "\"", originatingNode);
    }

    private static String keyToString(Object key) {
        return JSRuntime.safeToString(key).toString();
    }

    private static String quoteIfString(Object key) {
        return key instanceof TruffleString str ? '"' + Strings.toJavaString(str) + '"' : keyToString(key);
    }

    @TruffleBoundary
    public static JSException createReferenceErrorNotDefined(Object key, Node originatingNode) {
        String format = JavaScriptLanguage.get(originatingNode).getJSContext().isOptionNashornCompatibilityMode()
                        ? "\"%s\" is not defined"
                        : "%s is not defined";
        return Errors.createReferenceError(String.format(format, keyToString(key)), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotRedefineProperty(Object key) {
        return Errors.createTypeErrorFormat("Cannot redefine property: %s", keyToString(key));
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotRedefineProperty(long index) {
        return Errors.createTypeErrorCannotRedefineProperty((Object) index);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotRedefineTypedArrayElement() {
        throw Errors.createTypeError("Cannot redefine a property of an object with external array elements");
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotSetProperty(Object keyOrIndex, Object object, Node originatingNode) {
        String key = keyToString(keyOrIndex);
        String errorMessage;
        if (JavaScriptLanguage.get(originatingNode).getJSContext().isOptionNashornCompatibilityMode()) {
            errorMessage = "Cannot set property \"" + key + "\" of " + JSRuntime.safeToString(object);
        } else {
            errorMessage = "Cannot set property '" + key + "' of " + JSRuntime.safeToString(object);
        }
        return createTypeError(errorMessage, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotSetAccessorProperty(Object key, JSDynamicObject store, Node originatingNode) {
        JavaScriptLanguage language = JavaScriptLanguage.get(originatingNode);
        String message = language.getJSContext().isOptionNashornCompatibilityMode()
                        ? "Cannot set property \"%s\" of %s that has only a getter"
                        : "Cannot set property %s of %s which has only a getter";
        return createTypeError(String.format(message, keyToString(key), JSObject.defaultToString(store)), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotGetAccessorProperty(Object key, JSDynamicObject store, Node originatingNode) {
        return createTypeError(String.format("Cannot get property %s of %s which has only a setter", keyToString(key), JSObject.defaultToString(store)), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotGetProperty(Object keyOrIndex, Object object, boolean isGetMethod, Node originatingNode) {
        String key = keyToString(keyOrIndex);
        String errorMessage;
        if (JavaScriptLanguage.get(originatingNode).getJSContext().isOptionNashornCompatibilityMode()) {
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
    public static JSException createTypeErrorCannotDeclareGlobalFunction(Object varName, Node originatingNode) {
        return Errors.createTypeError("Cannot declare global function '" + varName + "'", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorCurrencyNotWellFormed(String currencyCode) {
        return createRangeError(String.format("Currency, %s, is not well formed.", currencyCode));
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidUnitArgument(String functionName, Object unit) {
        return createRangeError(String.format("Invalid unit argument for %s() '%s'", functionName, unit));
    }

    public static JSException createRangeErrorInvalidLanguageId(String languageId) {
        return createRangeErrorFormat("Invalid language ID: %s", null, languageId);
    }

    public static JSException createRangeErrorInvalidLanguageSubtag(String language) {
        return createRangeErrorFormat("Invalid language subtag: %s", null, language);
    }

    public static JSException createRangeErrorInvalidRegion(String region) {
        return createRangeErrorFormat("Invalid region subtag: %s", null, region);
    }

    public static JSException createRangeErrorInvalidScript(String script) {
        return createRangeErrorFormat("Invalid script subtag: %s", null, script);
    }

    public static JSException createRangeErrorInvalidCalendar(String calendar) {
        return createRangeErrorFormat("Invalid calendar: %s", null, calendar);
    }

    public static JSException createRangeErrorInvalidDateTimeField(String dateTimeField) {
        return createRangeErrorFormat("Invalid date-time field: %s", null, dateTimeField);
    }

    public static JSException createRangeErrorInvalidUnitIdentifier(String unitIdentifier) {
        return createRangeErrorFormat("Invalid unit identifier: %s", null, unitIdentifier);
    }

    public static JSException createRangeErrorInvalidTimeValue() {
        return createRangeError("Invalid time value");
    }

    public static JSException createTypeErrorInvalidTimeValue() {
        return createTypeError("Invalid time value");
    }

    @TruffleBoundary
    public static JSException createTypeErrorMapExpected() {
        return Errors.createTypeError("Map expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorSetExpected() {
        return Errors.createTypeError("Set expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorOutOfBoundsTypedArray() {
        return Errors.createTypeError("Out of bounds typed array");
    }

    @TruffleBoundary
    public static JSException createTypeErrorDetachedBuffer() {
        return Errors.createTypeError("Detached buffer");
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidDetachKey() {
        return Errors.createTypeError("Invalid detach key");
    }

    @TruffleBoundary
    public static JSException createTypeErrorReadOnlyBuffer() {
        return Errors.createTypeError("Read-only buffer");
    }

    @TruffleBoundary
    public static JSException createTypeErrorArrayBufferExpected() {
        return Errors.createTypeError("ArrayBuffer expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorSharedArrayBufferExpected() {
        throw Errors.createTypeError("SharedArrayBuffer expected");
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
    public static JSException createTypeErrorNotADate() {
        return Errors.createTypeError("not a Date object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorFinalizationRegistryExpected() {
        return Errors.createTypeError("FinalizationRegistry expected");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotANumber(Object value) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not a Number");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotAString(Object value) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not a String");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotARegExp(Object value) {
        return Errors.createTypeError(JSRuntime.safeToString(value) + " is not a RegExp");
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
    public static JSException createRangeErrorStackOverflow(Throwable cause, Node originatingNode) {
        return Errors.createRangeError("Maximum call stack size exceeded", cause, originatingNode);
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
    public static JSException createRangeErrorInvalidArrayLength(Node originatingNode) {
        return Errors.createRangeError("Invalid array length", originatingNode);
    }

    public static JSException createRangeErrorInvalidArrayLength() {
        return createRangeErrorInvalidArrayLength(null);
    }

    @TruffleBoundary
    public static JSException createRangeErrorIndexNegative(Node originatingNode) {
        return Errors.createRangeError("index is negative", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorIndexTooLarge(Node originatingNode) {
        return Errors.createRangeError("index is too large", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorIndexTooLarge() {
        return Errors.createTypeError("index is too large");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidBufferSize() {
        return Errors.createRangeError("Buffer too large");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidBufferOffset() {
        return Errors.createRangeError("Invalid buffer offset");
    }

    @TruffleBoundary
    public static JSException createRangeErrorInvalidTimeZone(String timeZoneName) {
        return Errors.createRangeError(String.format("Invalid time zone %s", timeZoneName));
    }

    public static RuntimeException unsupported(String message) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException(message);
    }

    public static RuntimeException notImplemented(String message) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException("not implemented: " + message);
    }

    public static RuntimeException shouldNotReachHere() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new IllegalStateException("should not reach here");
    }

    public static RuntimeException shouldNotReachHere(String message) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new IllegalStateException("should not reach here: " + message);
    }

    public static RuntimeException shouldNotReachHere(Throwable exception) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new IllegalStateException("should not reach here", exception);
    }

    public static RuntimeException shouldNotReachHereUnexpectedValue(Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw shouldNotReachHere("unexpected value: " + value);
    }

    @TruffleBoundary
    public static OutOfMemoryError outOfMemoryError() {
        return new OutOfMemoryError();
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
    public static JSException createTypeErrorCannotDeletePropertyOf(Object key, Object object) {
        return createTypeError("Cannot delete property \"" + keyToString(key) + "\" of " + JSRuntime.safeToString(object));
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotDeletePropertyOfSealedArray(long index) {
        return createTypeErrorFormat("Cannot delete property \"%d\" of sealed array", index);
    }

    public static JSException createTypeErrorJSObjectExpected() {
        return createTypeError("only JavaScript objects are supported by this operation");
    }

    public static JSException createTypeErrorPrivateSymbolInProxy() {
        return createTypeErrorPrivateSymbolInProxy(null);
    }

    public static JSException createTypeErrorPrivateSymbolInProxy(Node originatingNode) {
        return createTypeError("Cannot pass private property name to proxy trap", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorTrapReturnedFalsish(Object trap, Object propertyKey) {
        return createTypeError("'" + trap + "' on proxy: trap returned falsish for property '" + propertyKey + "'");
    }

    public static JSException createTypeErrorOwnKeysTrapMissingKey(Object propertyKey) {
        return createTypeErrorFormat("'ownKeys' on proxy: trap result did not include '%s'", propertyKey);
    }

    @TruffleBoundary
    public static JSException createTypeErrorProxyRevoked(TruffleString trap, Node originatingNode) {
        return createTypeError(trap != null ? "Cannot perform '" + trap + "' on a proxy that has been revoked" : "proxy has been revoked", originatingNode);
    }

    public static JSException createTypeErrorProxyRevoked() {
        return createTypeErrorProxyRevoked(null, null);
    }

    public static JSException createTypeErrorProxyTargetNotExtensible() {
        return createTypeError("target is not extensible");
    }

    @TruffleBoundary
    public static JSException createTypeErrorProxyGetInvariantViolated(Object propertyKey, Object expectedValue, Object actualValue) {
        String propertyName = propertyKey.toString();
        Object expected = JSRuntime.safeToString(expectedValue);
        Object actual = JSRuntime.safeToString(actualValue);
        return createTypeError("'get' on proxy: property '" + propertyName +
                        "' is a read-only and non-configurable data property on the proxy target but the proxy did not return its actual value (expected '" + expected + "' but got '" + actual + "')");
    }

    public static JSException createTypeErrorInteropException(Object receiver, InteropException cause, String message, Node originatingNode) {
        return createTypeErrorInteropException(receiver, cause, message, null, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorInteropException(Object receiver, InteropException cause, String message, Object messageDetails, Node originatingNode) {
        String reason = cause.getMessage();
        if (reason == null) {
            reason = cause.getClass().getSimpleName();
        }
        String receiverStr = toDisplayStringSafe(receiver);
        String messageTxt = (messageDetails == null) ? message : String.format("%s (%s)", message, messageDetails);
        return JSException.create(JSErrorType.TypeError, messageTxt + " on " + receiverStr + " failed due to: " + reason, cause, originatingNode);
    }

    private static String toDisplayStringSafe(Object receiver) {
        CompilerAsserts.neverPartOfCompilation();
        InteropLibrary interop = InteropLibrary.getUncached();
        try {
            return interop.asString(interop.toDisplayString(receiver, false));
        } catch (Exception e) {
            // ignore
            return "foreign object";
        }
    }

    @TruffleBoundary
    public static JSException createTypeErrorUnboxException(Object receiver, InteropException cause, Node originatingNode) {
        return createTypeErrorInteropException(receiver, cause, "ToPrimitive", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorUnsupportedInteropType(Object value) {
        return Errors.createTypeError("type " + value.getClass().getSimpleName() + " not supported in JavaScript");
    }

    @TruffleBoundary
    public static JSException createTypeErrorNotATruffleObject(String message) {
        return Errors.createTypeError("cannot call " + message + " on a non-interop object");
    }

    @TruffleBoundary
    public static JSException createTypeErrorInvalidIdentifier(Object identifier) {
        return Errors.createTypeError("Invalid identifier: " + JSRuntime.safeToString(identifier));
    }

    @TruffleBoundary
    public static JSException createTypeErrorClassNotFound(Object className) {
        return Errors.createTypeErrorFormat("Access to host class %s is not allowed or does not exist.", className);
    }

    @TruffleBoundary
    public static JSException createNotAFileError(String path) {
        return Errors.createTypeError("Not a file: " + path);
    }

    @TruffleBoundary
    public static JSException createErrorFromException(Throwable e) {
        return JSException.create(JSErrorType.Error, e.getMessage(), e, null);
    }

    @TruffleBoundary
    public static JSException createError(String message, Throwable e) {
        return JSException.create(JSErrorType.Error, message, e, null);
    }

    @TruffleBoundary
    public static JSException createICU4JDataError(Exception e) {
        return Errors.createError("ICU data not found. ICU4J library not properly configured. " +
                        "Set the system property " +
                        "org.graalvm.shadowed.com.ibm.icu.impl.ICUBinary.dataPath" +
                        " to your icudt path." +
                        (e.getMessage() != null && !e.getMessage().isEmpty() ? " (" + e.getMessage() + ")" : ""), e);
    }

    @TruffleBoundary
    public static JSException createEvalDisabled() {
        return Errors.createEvalError("dynamic evaluation of code is disabled.");
    }

    @TruffleBoundary
    public static JSException createTypeErrorIteratorResultNotObject(Object value, Node originatingNode) {
        return Errors.createTypeError("Iterator result " + JSRuntime.safeToString(value) + " is not an object", originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotGetPrivateMember(boolean fieldAccess, TruffleString name, Node originatingNode) {
        String message;
        if (fieldAccess) {
            message = String.format("Cannot read private member %s from an object whose class did not declare it.", name);
        } else {
            message = "Receiver must be an instance of class";
        }
        return createTypeError(message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotSetPrivateMember(Object name, Node originatingNode) {
        return createTypeError(String.format("Cannot write private member %s to an object whose class did not declare it.", name), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorCannotAddPrivateMember(String name, Node originatingNode) {
        return createTypeError(String.format("Duplicate private member %s.", name), originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeError(Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.TypeError, cause.getMessage(), cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeError(Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.RangeError, cause.getMessage(), cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeError(String message, Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.RangeError, message, cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createCompileError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.CompileError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createCompileError(Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.CompileError, cause.getMessage(), cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createLinkError(String message) {
        return JSException.create(JSErrorType.LinkError, message);
    }

    @TruffleBoundary
    public static JSException createLinkError(String message, Node originatingNode) {
        return JSException.create(JSErrorType.LinkError, message, originatingNode);
    }

    @TruffleBoundary
    public static JSException createLinkError(Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.LinkError, cause.getMessage(), cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createRuntimeError(String message, JSRealm realm) {
        return JSException.create(JSErrorType.RuntimeError, message, null, null, realm);
    }

    @TruffleBoundary
    public static JSException createRuntimeError(Throwable cause, Node originatingNode) {
        return JSException.create(JSErrorType.RuntimeError, cause.getMessage(), cause, originatingNode);
    }

    @TruffleBoundary
    public static JSException createTypeErrorWrongDecoratorReturn(Node originatingNode) {
        return Errors.createTypeError("Class decorator must return undefined or function", originatingNode);
    }

    @TruffleBoundary
    public static JSException createRangeErrorEncodingNotSupported(TruffleString encoding) {
        return Errors.createRangeError("Unsupported encoding: " + encoding);
    }
}
