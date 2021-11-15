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
package com.oracle.truffle.js.runtime.builtins;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.CallSitePrototypeBuiltins;
import com.oracle.truffle.js.builtins.ConstructorBuiltins;
import com.oracle.truffle.js.builtins.ErrorFunctionBuiltins;
import com.oracle.truffle.js.builtins.ErrorPrototypeBuiltins;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.GraalJSException.JSStackTraceElement;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.PrepareStackTraceCallback;
import com.oracle.truffle.js.runtime.ToDisplayStringFormat;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSError extends JSNonProxy {

    public static final String MESSAGE = "message";
    public static final int MESSAGE_ATTRIBUTES = JSAttributes.getDefaultNotEnumerable();
    public static final String NAME = "name";
    public static final String CLASS_NAME = "Error";
    public static final String PROTOTYPE_NAME = "Error.prototype";
    public static final HiddenKey EXCEPTION_PROPERTY_NAME = new HiddenKey("Exception");
    public static final String STACK_NAME = "stack";
    public static final HiddenKey FORMATTED_STACK_NAME = new HiddenKey("FormattedStack");
    public static final String ERRORS_NAME = "errors";
    public static final int ERRORS_ATTRIBUTES = JSAttributes.getDefaultNotEnumerable();
    public static final String PREPARE_STACK_TRACE_NAME = "prepareStackTrace";
    public static final String LINE_NUMBER_PROPERTY_NAME = "lineNumber";
    public static final String COLUMN_NUMBER_PROPERTY_NAME = "columnNumber";
    public static final int DEFAULT_COLUMN_NUMBER = -1;
    public static final String STACK_TRACE_LIMIT_PROPERTY_NAME = "stackTraceLimit";

    public static final JSError INSTANCE = new JSError();

    // CallSite
    private static final String CALL_SITE_CLASS_NAME = "CallSite";
    public static final String CALL_SITE_PROTOTYPE_NAME = "CallSite.prototype";
    public static final HiddenKey STACK_TRACE_ELEMENT_PROPERTY_NAME = new HiddenKey("StackTraceElement");

    public static final PropertyProxy STACK_PROXY = new PropertyProxy() {
        @Override
        public Object get(DynamicObject store) {
            Object value = JSObjectUtil.getHiddenProperty(store, FORMATTED_STACK_NAME);
            if (value == null) {
                // stack not prepared yet
                GraalJSException truffleException = getException(store);
                if (truffleException == null) {
                    value = Undefined.instance;
                } else {
                    JSRealm realm = JSRealm.get(null);
                    value = prepareStack(realm, store, truffleException);
                }
                // FORMATTED_STACK_NAME could have been set during invocation
                // of user-defined Error.prepareStackTrace => do not overwrite it
                Object currentValue = JSObjectUtil.getHiddenProperty(store, FORMATTED_STACK_NAME);
                if (currentValue == null) {
                    JSObjectUtil.putHiddenProperty(store, FORMATTED_STACK_NAME, value);
                } else {
                    value = currentValue;
                }
            }
            return value;
        }

        @Override
        public boolean set(DynamicObject store, Object value) {
            JSObjectUtil.putHiddenProperty(store, FORMATTED_STACK_NAME, value);
            return true;
        }
    };

    private JSError() {
    }

    public static DynamicObject createErrorObject(JSContext context, JSRealm realm, JSErrorType errorType) {
        JSObjectFactory factory = context.getErrorFactory(errorType);
        DynamicObject obj = JSErrorObject.create(realm, factory);
        factory.initProto(obj, realm);
        assert isJSError(obj);
        return context.trackAllocation(obj);
    }

    public static void setMessage(DynamicObject obj, String message) {
        assert !JSDynamicObject.hasProperty(obj, MESSAGE);
        JSObjectUtil.putDataProperty(obj, MESSAGE, message, MESSAGE_ATTRIBUTES);
    }

    public static DynamicObject create(JSErrorType errorType, JSRealm realm, Object message) {
        assert message instanceof String || message == Undefined.instance;
        DynamicObject obj = createErrorObject(realm.getContext(), realm, errorType);
        String msg;
        if (message == Undefined.instance) {
            msg = null;
        } else {
            msg = (String) message; // can only be String or undefined
            setMessage(obj, msg);
        }
        setException(realm, obj, JSException.createCapture(errorType, msg, obj, realm), false);
        return obj;
    }

    public static DynamicObject createFromJSException(JSException exception, JSRealm realm, String message) {
        Objects.requireNonNull(message);
        JSContext context = realm.getContext();
        JSErrorType errorType = exception.getErrorType();
        DynamicObject obj = createErrorObject(context, realm, errorType);
        setMessage(obj, message);
        setException(realm, obj, exception, context.isOptionNashornCompatibilityMode());
        return obj;
    }

    @TruffleBoundary
    public static DynamicObject createAggregateError(JSRealm realm, Object errors, String msg) {
        DynamicObject errorObj = createErrorObject(realm.getContext(), realm, JSErrorType.AggregateError);
        if (msg != null) {
            setMessage(errorObj, msg);
        }
        JSObjectUtil.putDataProperty(errorObj, ERRORS_NAME, errors, ERRORS_ATTRIBUTES);
        setException(realm, errorObj, JSException.createCapture(JSErrorType.AggregateError, msg, errorObj, realm), false);
        return errorObj;
    }

    private static DynamicObject createErrorPrototype(JSRealm realm, JSErrorType errorType) {
        JSContext ctx = realm.getContext();
        DynamicObject proto = errorType == JSErrorType.Error ? realm.getObjectPrototype() : realm.getErrorPrototype(JSErrorType.Error);

        DynamicObject errorPrototype;
        if (ctx.getEcmaScriptVersion() < 6) {
            errorPrototype = JSErrorObject.create(JSShape.createPrototypeShape(ctx, INSTANCE, proto));
            JSObjectUtil.setOrVerifyPrototype(ctx, errorPrototype, proto);
        } else {
            errorPrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm, proto);
        }

        JSObjectUtil.putDataProperty(ctx, errorPrototype, MESSAGE, "", MESSAGE_ATTRIBUTES);

        if (errorType == JSErrorType.Error) {
            JSObjectUtil.putFunctionsFromContainer(realm, errorPrototype, ErrorPrototypeBuiltins.BUILTINS);
            if (ctx.isOptionNashornCompatibilityMode()) {
                JSObjectUtil.putFunctionsFromContainer(realm, errorPrototype, ErrorPrototypeBuiltins.ErrorPrototypeNashornCompatBuiltins.BUILTINS);
            }
        }
        return errorPrototype;
    }

    public static JSConstructor createErrorConstructor(JSRealm realm, JSErrorType errorType) {
        JSContext context = realm.getContext();
        String name = errorType.toString();
        DynamicObject errorConstructor = realm.lookupFunction(ConstructorBuiltins.BUILTINS, name); // (Type)Error
        DynamicObject classPrototype = JSError.createErrorPrototype(realm, errorType); // (Type)Error.prototype
        if (errorType != JSErrorType.Error) {
            JSObject.setPrototype(errorConstructor, realm.getErrorConstructor(JSErrorType.Error));
        }
        JSObjectUtil.putConstructorProperty(context, classPrototype, errorConstructor);
        JSObjectUtil.putDataProperty(context, classPrototype, NAME, name, MESSAGE_ATTRIBUTES);
        JSObjectUtil.putConstructorPrototypeProperty(context, errorConstructor, classPrototype);
        if (errorType == JSErrorType.Error) {
            JSObjectUtil.putFunctionsFromContainer(realm, errorConstructor, ErrorFunctionBuiltins.BUILTINS);
            JSObjectUtil.putDataProperty(context, errorConstructor, STACK_TRACE_LIMIT_PROPERTY_NAME, JSContextOptions.STACK_TRACE_LIMIT.getValue(realm.getOptions()), JSAttributes.getDefault());
        }

        return new JSConstructor(errorConstructor, classPrototype);
    }

    @Override
    public Shape makeInitialShape(JSContext context, DynamicObject errorPrototype) {
        return JSObjectUtil.getProtoChildShape(errorPrototype, INSTANCE, context);
    }

    private static DynamicObject createCallSitePrototype(JSRealm realm) {
        DynamicObject callSitePrototype = JSObjectUtil.createOrdinaryPrototypeObject(realm);
        JSObjectUtil.putFunctionsFromContainer(realm, callSitePrototype, CallSitePrototypeBuiltins.BUILTINS);
        return callSitePrototype;
    }

    public static JSConstructor createCallSiteConstructor(JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject constructor = JSFunction.createNamedEmptyFunction(realm, CALL_SITE_CLASS_NAME);
        DynamicObject prototype = createCallSitePrototype(realm);
        JSObjectUtil.putConstructorProperty(context, prototype, constructor);
        JSObjectUtil.putConstructorPrototypeProperty(context, constructor, prototype);
        return new JSConstructor(constructor, prototype);
    }

    public static Shape makeInitialCallSiteShape(JSContext context, DynamicObject callSitePrototype) {
        return JSObjectUtil.getProtoChildShape(callSitePrototype, JSOrdinary.INSTANCE, context);
    }

    public static void setLineNumber(JSContext context, DynamicObject errorObj, Object lineNumber) {
        setErrorProperty(context, errorObj, LINE_NUMBER_PROPERTY_NAME, lineNumber);
    }

    public static void setColumnNumber(JSContext context, DynamicObject errorObj, Object columnNumber) {
        setErrorProperty(context, errorObj, COLUMN_NUMBER_PROPERTY_NAME, columnNumber);
    }

    public static GraalJSException getException(DynamicObject errorObj) {
        Object exception = JSDynamicObject.getOrNull(errorObj, EXCEPTION_PROPERTY_NAME);
        return exception instanceof GraalJSException ? (GraalJSException) exception : null;
    }

    @TruffleBoundary
    public static DynamicObject setException(JSRealm realm, DynamicObject errorObj, GraalJSException exception, boolean defaultColumnNumber) {
        assert isJSError(errorObj);
        defineStackProperty(realm, errorObj, exception);
        JSContext context = realm.getContext();
        if (context.isOptionNashornCompatibilityMode() && exception.getJSStackTrace().length > 0) {
            JSStackTraceElement topStackTraceElement = exception.getJSStackTrace()[0];
            setLineNumber(context, errorObj, topStackTraceElement.getLineNumber());
            setColumnNumber(context, errorObj, defaultColumnNumber ? DEFAULT_COLUMN_NUMBER : topStackTraceElement.getColumnNumber());
        }
        return errorObj;
    }

    private static void setErrorProperty(JSContext context, DynamicObject errorObj, Object key, Object value) {
        JSObjectUtil.defineDataProperty(context, errorObj, key, value, JSAttributes.getDefaultNotEnumerable());
    }

    private static void defineStackProperty(JSRealm realm, DynamicObject errorObj, GraalJSException exception) {
        JSContext context = realm.getContext();
        setErrorProperty(context, errorObj, EXCEPTION_PROPERTY_NAME, exception);

        // Error.stack is not formatted until it is accessed
        JSObjectUtil.putHiddenProperty(errorObj, FORMATTED_STACK_NAME, null);
        JSObjectUtil.defineProxyProperty(errorObj, JSError.STACK_NAME, JSError.STACK_PROXY, MESSAGE_ATTRIBUTES | JSProperty.PROXY);
    }

    // GR-31094 method has deoptimization sources without corresponding deoptimization targets
    // within native image.
    @TruffleBoundary
    public static Object prepareStack(JSRealm realm, DynamicObject errorObj, GraalJSException exception) {
        JSStackTraceElement[] stackTrace = exception.getJSStackTrace();
        if (realm.isPreparingStackTrace()) {
            // Do not call Error.prepareStackTrace or PrepareStackTraceCallback
            // for errors that occur during their invocation
            return formatStackTrace(stackTrace, errorObj, realm);
        } else {
            try {
                realm.setPreparingStackTrace(true);
                PrepareStackTraceCallback prepareStackTraceCallback = realm.getContext().getPrepareStackTraceCallback();
                if (prepareStackTraceCallback == null) {
                    return prepareStackNoCallback(realm, errorObj, stackTrace);
                } else {
                    return prepareStackTraceWithCallback(realm, prepareStackTraceCallback, errorObj, stackTrace);
                }
            } finally {
                realm.setPreparingStackTrace(false);
            }
        }
    }

    /**
     * Prepares the value to be set to the errObj.stack property. If Error.prepareStackTrace() is a
     * function, it is called and the result is used; otherwise, the stack is formatted as string.
     */
    @TruffleBoundary
    public static Object prepareStackNoCallback(JSRealm realm, DynamicObject errorObj, JSStackTraceElement[] jsStackTrace) {
        DynamicObject error = realm.getErrorConstructor(JSErrorType.Error);
        Object prepareStackTrace = JSObject.get(error, PREPARE_STACK_TRACE_NAME);
        if (JSFunction.isJSFunction(prepareStackTrace)) {
            return prepareStackWithUserFunction(realm, (DynamicObject) prepareStackTrace, errorObj, jsStackTrace);
        }
        return formatStackTrace(jsStackTrace, errorObj, realm);
    }

    @TruffleBoundary
    private static Object prepareStackTraceWithCallback(JSRealm realm, PrepareStackTraceCallback callback, DynamicObject errorObj, JSStackTraceElement[] stackTrace) {
        try {
            return callback.prepareStackTrace(realm, errorObj, toStructuredStackTrace(realm, stackTrace));
        } catch (Exception ex) {
            return formatStackTrace(stackTrace, errorObj, realm);
        }
    }

    private static Object prepareStackWithUserFunction(JSRealm realm, DynamicObject prepareStackTraceFun, DynamicObject errorObj, JSStackTraceElement[] stackTrace) {
        return JSFunction.call(prepareStackTraceFun, errorObj, new Object[]{errorObj, toStructuredStackTrace(realm, stackTrace)});
    }

    private static DynamicObject toStructuredStackTrace(JSRealm realm, JSStackTraceElement[] stackTrace) {
        Object[] elements = new Object[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            elements[i] = prepareStackElement(realm, stackTrace[i]);
        }
        return JSArray.createConstant(realm.getContext(), realm, elements);
    }

    private static Object prepareStackElement(JSRealm realm, JSStackTraceElement stackTraceElement) {
        JSContext context = realm.getContext();
        DynamicObject callSite = JSOrdinary.createWithRealm(context, context.getCallSiteFactory(), realm);
        JSObjectUtil.putHiddenProperty(callSite, STACK_TRACE_ELEMENT_PROPERTY_NAME, stackTraceElement);
        return callSite;
    }

    private static String getMessage(DynamicObject errorObj) {
        Object message = JSObject.get(errorObj, MESSAGE);
        return (message == Undefined.instance) ? null : JSRuntime.toString(message);
    }

    private static String getName(DynamicObject errorObj) {
        Object name = JSObject.get(errorObj, NAME);
        return (name == Undefined.instance) ? null : JSRuntime.toString(name);
    }

    private static boolean isInstanceOfJSError(DynamicObject errorObj, JSRealm realm) {
        DynamicObject errorPrototype = realm.getErrorPrototype(JSErrorType.Error);
        return JSRuntime.isPrototypeOf(errorObj, errorPrototype);
    }

    @TruffleBoundary
    private static String formatStackTrace(JSStackTraceElement[] stackTrace, DynamicObject errObj, JSRealm realm) {
        StringBuilder builder = new StringBuilder();
        if (!realm.getContext().isOptionNashornCompatibilityMode() || isInstanceOfJSError(errObj, realm)) {
            String name = getName(errObj);
            String message = getMessage(errObj);
            if (name != null) {
                builder.append(name);
            } else {
                builder.append("Error");
            }
            if (message != null && message.length() > 0) {
                if (builder.length() != 0) {
                    builder.append(": ");
                }
                builder.append(message);
            }
        } else {
            builder.append(JSObject.defaultToString(errObj));
        }
        formatStackTraceIntl(stackTrace, builder, realm.getContext());
        return builder.toString();
    }

    private static void formatStackTraceIntl(JSStackTraceElement[] stackTrace, StringBuilder builder, JSContext context) {
        boolean nashornCompatibilityMode = context.isOptionNashornCompatibilityMode();
        for (JSStackTraceElement elem : stackTrace) {
            builder.append(JSRuntime.LINE_SEPARATOR);
            builder.append(nashornCompatibilityMode ? "\tat " : "    at ");
            if (!nashornCompatibilityMode) {
                builder.append(elem.toString(context));
            } else {
                String methodName = correctMethodName(elem.getFunctionName(), context);
                builder.append(methodName);
                builder.append(" (");
                String fileName = elem.getFileName();
                if (JSFunction.BUILTIN_SOURCE_NAME.equals(fileName)) {
                    builder.append("native");
                } else {
                    builder.append(fileName);
                    builder.append(":");
                    builder.append(elem.getLineNumber());
                }
                builder.append(")");
            }
        }
    }

    public static String correctMethodName(String methodName, JSContext context) {
        if (methodName == null) {
            return "";
        }
        if (methodName.isEmpty()) {
            return getAnonymousFunctionNameStackTrace(context);
        }
        if (Boundaries.stringEndsWith(methodName, "]")) {
            int idx = Boundaries.stringLastIndexOf(methodName, '[');
            if (idx >= 0) {
                return Boundaries.substring(methodName, idx);
            }
        }
        int idx = Boundaries.stringLastIndexOf(methodName, '.');
        if (idx >= 0) {
            return Boundaries.substring(methodName, idx + 1);
        }
        return methodName;
    }

    @Override
    public String getClassName(DynamicObject object) {
        return CLASS_NAME;
    }

    @Override
    public String getBuiltinToStringTag(DynamicObject object) {
        return getClassName(object);
    }

    public static boolean isJSError(Object obj) {
        return obj instanceof JSErrorObject;
    }

    @TruffleBoundary
    @Override
    public String toDisplayStringImpl(DynamicObject obj, boolean allowSideEffects, ToDisplayStringFormat format, int depth) {
        if (JavaScriptLanguage.get(null).getJSContext().isOptionNashornCompatibilityMode()) {
            return super.toDisplayStringImpl(obj, allowSideEffects, format, depth);
        } else {
            Object name = getPropertyWithoutSideEffect(obj, NAME);
            Object message = getPropertyWithoutSideEffect(obj, MESSAGE);
            String nameStr = name != null ? JSRuntime.toDisplayStringImpl(name, allowSideEffects, ToDisplayStringFormat.getDefaultFormat(), depth + 1, obj) : CLASS_NAME;
            String messageStr = message != null ? JSRuntime.toDisplayStringImpl(message, allowSideEffects, ToDisplayStringFormat.getDefaultFormat(), depth + 1, obj) : "";
            if (nameStr.isEmpty()) {
                if (messageStr.isEmpty()) {
                    return CLASS_NAME;
                }
                return messageStr;
            } else if (messageStr.isEmpty()) {
                return nameStr;
            } else {
                return nameStr + ": " + messageStr;
            }
        }
    }

    private static Object getPropertyWithoutSideEffect(DynamicObject obj, String key) {
        Object value = JSDynamicObject.getOrNull(obj, key);
        if (value == null) {
            if (!JSProxy.isJSProxy(obj)) {
                return getPropertyWithoutSideEffect(JSObject.getPrototype(obj), key);
            }
            return null;
        } else if (value instanceof Accessor) {
            return "{Accessor}";
        } else if (value instanceof PropertyProxy) {
            return null;
        } else {
            return value;
        }
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return true;
    }

    public static String getAnonymousFunctionNameStackTrace(JSContext context) {
        return context.isOptionNashornCompatibilityMode() ? "<program>" : "<anonymous>";
    }
}
