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
package com.oracle.truffle.js.runtime.builtins;

import java.util.EnumSet;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.GraalJSException.JSStackTraceElement;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.Accessor;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JSError extends JSBuiltinObject {

    public static final String MESSAGE = "message";
    public static final String NAME = "name";
    public static final String CLASS_NAME = "Error";
    public static final String PROTOTYPE_NAME = "Error.prototype";
    public static final HiddenKey EXCEPTION_PROPERTY_NAME = new HiddenKey("Exception");
    public static final String STACK_NAME = "stack";
    public static final HiddenKey FORMATTED_STACK_NAME = new HiddenKey("FormattedStack");
    public static final String PREPARE_STACK_TRACE_NAME = "prepareStackTrace";
    public static final String LINE_NUMBER_PROPERTY_NAME = "lineNumber";
    public static final String COLUMN_NUMBER_PROPERTY_NAME = "columnNumber";
    public static final int DEFAULT_COLUMN_NUMBER = -1;
    public static final String STACK_TRACE_LIMIT_PROPERTY_NAME = "stackTraceLimit";

    public static final String ANONYMOUS_FUNCTION_NAME_STACK_TRACE = JSTruffleOptions.NashornCompatibilityMode ? "<program>" : "<anonymous>";

    private static final JSError INSTANCE = new JSError();
    private static final Property MESSAGE_PROPERTY;

    // CallSite
    public static final String CALL_SITE_PROTOTYPE_NAME = "CallSite.prototype";
    public static final HiddenKey STACK_TRACE_ELEMENT_PROPERTY_NAME = new HiddenKey("StackTraceElement");
    private static final Property STACK_TRACE_ELEMENT_PROPERTY;

    static {
        // Error
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        MESSAGE_PROPERTY = JSObjectUtil.makeDataProperty(MESSAGE, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)), JSAttributes.getDefaultNotEnumerable());
    }
    static {
        // CallSite
        Shape.Allocator allocator = JSShape.makeAllocator(JSObject.LAYOUT);
        STACK_TRACE_ELEMENT_PROPERTY = JSObjectUtil.makeHiddenProperty(STACK_TRACE_ELEMENT_PROPERTY_NAME, allocator.locationForType(Object.class));
    }

    public static final PropertyProxy STACK_PROXY = new PropertyProxy() {
        @Override
        public Object get(DynamicObject store) {
            Object value = store.get(FORMATTED_STACK_NAME);
            if (value == null) {
                // stack not prepared yet
                GraalJSException truffleException = getException(store);
                if (truffleException == null) {
                    value = Undefined.instance;
                } else {
                    JSRealm realm = currentRealm(store);
                    value = prepareStack(realm, store, truffleException);
                }
                store.set(FORMATTED_STACK_NAME, value);
            }
            return value;
        }

        private JSRealm currentRealm(DynamicObject store) {
            return JSObject.getJSContext(store).getRealm();
        }

        @Override
        public boolean set(DynamicObject store, Object value) {
            store.set(FORMATTED_STACK_NAME, value);
            return true;
        }
    };

    private JSError() {
    }

    public static DynamicObject create(JSErrorType errorType, JSRealm realm, Object message) {
        assert message instanceof String || message == Undefined.instance;
        DynamicObject obj;
        String msg;
        JSContext context = realm.getContext();
        if (message == Undefined.instance) {
            obj = JSObject.create(context, context.getErrorFactory(errorType, false));
            msg = null;
        } else {
            obj = JSObject.create(context, context.getErrorFactory(errorType, true), message);
            msg = (String) message; // can only be String or undefined
        }
        setException(realm, obj, JSException.createCapture(errorType, msg, obj), false);
        return obj;
    }

    public static DynamicObject createFromJSException(JSException exception, JSRealm realm, String message) {
        JSErrorType errorType = exception.getErrorType();
        JSContext context = realm.getContext();
        DynamicObject obj = JSObject.create(context, context.getErrorFactory(errorType, true), Objects.requireNonNull(message));
        setException(realm, obj, exception, JSTruffleOptions.NashornCompatibilityMode);
        return obj;
    }

    private static DynamicObject createErrorPrototype(JSRealm realm, JSErrorType errorType) {
        JSContext ctx = realm.getContext();
        DynamicObject proto = errorType == JSErrorType.Error ? realm.getObjectPrototype() : realm.getErrorConstructor(JSErrorType.Error).getPrototype();

        DynamicObject errorPrototype = JSObject.create(realm, proto, ctx.getEcmaScriptVersion() < 6 ? INSTANCE : JSUserObject.INSTANCE);
        JSObjectUtil.putDataProperty(ctx, errorPrototype, MESSAGE, "", JSAttributes.getDefaultNotEnumerable());

        if (errorType == JSErrorType.Error) {
            JSObjectUtil.putFunctionsFromContainer(realm, errorPrototype, PROTOTYPE_NAME);
        }
        return errorPrototype;
    }

    public static JSConstructor createErrorConstructor(JSRealm realm, JSErrorType errorType) {
        JSContext context = realm.getContext();
        String name = errorType.toString();
        DynamicObject errorConstructor = realm.lookupFunction(JSConstructor.BUILTINS, name); // (Type)Error
        DynamicObject classPrototype = JSError.createErrorPrototype(realm, errorType); // (Type)Error.prototype
        if (errorType != JSErrorType.Error) {
            JSObject.setPrototype(errorConstructor, realm.getErrorConstructor(JSErrorType.Error).getFunctionObject());
        }
        JSObjectUtil.putConstructorProperty(context, classPrototype, errorConstructor);
        JSObjectUtil.putDataProperty(context, classPrototype, NAME, name, JSAttributes.getDefaultNotEnumerable());
        JSObjectUtil.putConstructorPrototypeProperty(context, errorConstructor, classPrototype);
        if (errorType == JSErrorType.Error) {
            JSObjectUtil.putFunctionsFromContainer(realm, errorConstructor, CLASS_NAME);
            JSObjectUtil.putDataProperty(context, errorConstructor, STACK_TRACE_LIMIT_PROPERTY_NAME, context.getStackTraceLimit(), JSAttributes.getDefault());
        }

        return new JSConstructor(errorConstructor, classPrototype);
    }

    public static Shape makeInitialShape(JSContext context, DynamicObject errorPrototype) {
        return JSObjectUtil.getProtoChildShape(errorPrototype, INSTANCE, context);
    }

    public static Shape addMessagePropertyToShape(Shape shape) {
        return shape.addProperty(MESSAGE_PROPERTY);
    }

    private static DynamicObject createCallSitePrototype(JSRealm realm) {
        DynamicObject proto = realm.getObjectPrototype();
        DynamicObject callSitePrototype = JSObject.create(realm, proto, JSUserObject.INSTANCE);
        JSObjectUtil.putFunctionsFromContainer(realm, callSitePrototype, CALL_SITE_PROTOTYPE_NAME);
        return callSitePrototype;
    }

    public static JSConstructor createCallSiteConstructor(JSRealm realm) {
        JSContext context = realm.getContext();
        DynamicObject constructor = JSFunction.createEmptyFunction(realm);
        DynamicObject prototype = createCallSitePrototype(realm);
        JSObjectUtil.putConstructorProperty(context, prototype, constructor);
        JSObjectUtil.putConstructorPrototypeProperty(context, constructor, prototype);
        return new JSConstructor(constructor, prototype);
    }

    public static Shape makeInitialCallSiteShape(JSContext context, DynamicObject callSitePrototype) {
        return JSObjectUtil.getProtoChildShape(callSitePrototype, JSUserObject.INSTANCE, context).addProperty(STACK_TRACE_ELEMENT_PROPERTY);
    }

    public static void setLineNumber(JSContext context, DynamicObject errorObj, Object lineNumber) {
        setErrorProperty(context, errorObj, LINE_NUMBER_PROPERTY_NAME, lineNumber);
    }

    public static void setColumnNumber(JSContext context, DynamicObject errorObj, Object columnNumber) {
        setErrorProperty(context, errorObj, COLUMN_NUMBER_PROPERTY_NAME, columnNumber);
    }

    public static GraalJSException getException(DynamicObject errorObj) {
        Object exception = errorObj.get(EXCEPTION_PROPERTY_NAME);
        return exception instanceof GraalJSException ? (GraalJSException) exception : null;
    }

    @TruffleBoundary
    private static DynamicObject setException(JSRealm realm, DynamicObject errorObj, GraalJSException exception, boolean defaultColumnNumber) {
        assert isJSError(errorObj);
        defineStackProperty(realm, errorObj, exception);
        if (JSTruffleOptions.NashornCompatibilityMode && exception.getJSStackTrace().length > 0) {
            JSContext context = realm.getContext();
            JSStackTraceElement topStackTraceElement = exception.getJSStackTrace()[0];
            setLineNumber(context, errorObj, topStackTraceElement.getLineNumber());
            setColumnNumber(context, errorObj, defaultColumnNumber ? DEFAULT_COLUMN_NUMBER : topStackTraceElement.getColumnNumber());
        }
        return errorObj;
    }

    private static void setErrorProperty(JSContext context, DynamicObject errorObj, Object key, Object value) {
        if (!errorObj.set(key, value)) {
            JSObjectUtil.putDataProperty(context, errorObj, key, value, JSAttributes.getDefaultNotEnumerable());
        }
    }

    private static void defineStackProperty(JSRealm realm, DynamicObject errorObj, GraalJSException exception) {
        JSContext context = realm.getContext();
        setErrorProperty(context, errorObj, EXCEPTION_PROPERTY_NAME, exception);

        // Error.stack is not formatted until it is accessed
        errorObj.define(FORMATTED_STACK_NAME, null);
        JSObjectUtil.defineProxyProperty(errorObj, JSError.STACK_NAME, JSError.STACK_PROXY, JSAttributes.getDefaultNotEnumerable() | JSProperty.PROXY);
    }

    /**
     * Prepares the value to be set to the errObj.stack property. If Error.prepareStackTrace() is a
     * function, it is called and the result is used; otherwise, the stack is formatted as string.
     */
    @TruffleBoundary
    public static Object prepareStack(JSRealm realm, DynamicObject errorObj, GraalJSException exception) {
        DynamicObject error = realm.getErrorConstructor(JSErrorType.Error).getFunctionObject();
        Object prepareStackTrace = JSObject.get(error, PREPARE_STACK_TRACE_NAME);
        JSStackTraceElement[] jsStackTrace = exception.getJSStackTrace();
        if (JSFunction.isJSFunction(prepareStackTrace)) {
            // Do not call Error.prepareStackTrace for errors that occur during its invocation
            boolean inPrepareStackTrace = realm.isPreparingStackTrace();
            if (!inPrepareStackTrace) {
                try {
                    realm.setPreparingStackTrace(true);
                    return prepareStackWithUserFunction(realm, (DynamicObject) prepareStackTrace, errorObj, jsStackTrace);
                } finally {
                    realm.setPreparingStackTrace(false);
                }
            }
        }
        return formatStackTrace(jsStackTrace, errorObj, realm);
    }

    private static Object prepareStackWithUserFunction(JSRealm realm, DynamicObject prepareStackTraceFun, DynamicObject errorObj, JSStackTraceElement[] stackTrace) {
        Object[] elements = new Object[stackTrace.length];
        for (int i = 0; i < stackTrace.length; i++) {
            elements[i] = prepareStackElement(realm, stackTrace[i]);
        }
        DynamicObject structuredStackTrace = JSArray.createConstant(realm.getContext(), elements);
        try {
            return JSFunction.call(prepareStackTraceFun, errorObj, new Object[]{errorObj, structuredStackTrace});
        } catch (Exception ex) {
            return formatStackTrace(stackTrace, errorObj, realm);
        }
    }

    private static Object prepareStackElement(JSRealm realm, JSStackTraceElement stackTraceElement) {
        return JSObject.create(realm.getContext(), realm.getCallSiteFactory(), stackTraceElement);
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
        DynamicObject errorPrototype = realm.getErrorConstructor(JSErrorType.Error).getPrototype();
        return JSRuntime.isPrototypeOf(errorObj, errorPrototype);
    }

    @TruffleBoundary
    private static String formatStackTrace(JSStackTraceElement[] stackTrace, DynamicObject errObj, JSRealm realm) {
        StringBuilder builder = new StringBuilder();
        if (!JSTruffleOptions.NashornCompatibilityMode || isInstanceOfJSError(errObj, realm)) {
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
        for (JSStackTraceElement elem : stackTrace) {
            builder.append(JSRuntime.LINE_SEPARATOR);
            builder.append(JSTruffleOptions.NashornCompatibilityMode ? "\tat " : "    at ");
            if (context.isOptionV8CompatibilityMode()) {
                builder.append(elem.toString());
            } else {
                String className = JSTruffleOptions.NashornCompatibilityMode ? null : elem.getClassName();
                String methodName = correctMethodName(elem.getFunctionName());
                boolean includeMethodName = JSTruffleOptions.NashornCompatibilityMode || (className != null) || !ANONYMOUS_FUNCTION_NAME_STACK_TRACE.equals(methodName);
                if (includeMethodName) {
                    if (className != null) {
                        builder.append(className).append('.');
                    }
                    builder.append(methodName);
                    builder.append(" (");
                }
                String fileName = elem.getFileName();
                if (JSFunction.BUILTIN_SOURCE_NAME.equals(fileName)) {
                    builder.append("native");
                } else {
                    builder.append(fileName);
                    builder.append(":");
                    builder.append(elem.getLineNumber());
                    if (!JSTruffleOptions.NashornCompatibilityMode) {
                        builder.append(":");
                        builder.append(elem.getColumnNumber());
                    }
                }
                if (includeMethodName) {
                    builder.append(")");
                }
            }
        }
    }

    public static String correctMethodName(String methodName) {
        if (methodName == null) {
            return "";
        }
        if (methodName.isEmpty()) {
            return ANONYMOUS_FUNCTION_NAME_STACK_TRACE;
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
        return JSObject.isDynamicObject(obj) && isJSError((DynamicObject) obj);
    }

    public static boolean isJSError(DynamicObject obj) {
        return isInstance(obj, INSTANCE);
    }

    @TruffleBoundary
    @Override
    public String safeToString(DynamicObject obj) {
        if (JSTruffleOptions.NashornCompatibilityMode) {
            return super.safeToString(obj);
        } else {
            String message = getPropertyWithoutSideEffect(obj, MESSAGE).toString();
            String name = getPropertyWithoutSideEffect(obj, NAME).toString();
            if (name.equals(Null.NAME)) {
                name = getPropertyWithoutSideEffect(JSObject.getPrototype(obj), NAME).toString();
            }
            return name + ": " + message;
        }
    }

    private static Object getPropertyWithoutSideEffect(DynamicObject obj, String key) {
        Object value = obj.get(key);
        if (value == null) {
            return Null.NAME;
        } else if (value instanceof Accessor) {
            return "{Accessor}";
        } else {
            return value;
        }
    }

    @Override
    public boolean hasOnlyShapeProperties(DynamicObject obj) {
        return true;
    }

}
