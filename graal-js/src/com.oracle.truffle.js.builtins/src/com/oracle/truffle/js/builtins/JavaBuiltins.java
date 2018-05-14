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
package com.oracle.truffle.js.builtins;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import javax.script.Bindings;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaAsJSONCompatibleNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaExtendNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaFromNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsJavaFunctionNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsJavaMethodNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsJavaObjectNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsScriptFunctionNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsScriptObjectNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaIsTypeNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaSuperNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaSynchronizedNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaToNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaTypeNameNodeGen;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaTypeNodeGen;
import com.oracle.truffle.js.nodes.access.RealmNode;
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.JSForeignToJSTypeNode;
import com.oracle.truffle.js.nodes.unary.TypeOfNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.array.ScriptArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSJava;
import com.oracle.truffle.js.runtime.interop.Converters;
import com.oracle.truffle.js.runtime.interop.JSJavaWrapper;
import com.oracle.truffle.js.runtime.interop.JavaAccess;
import com.oracle.truffle.js.runtime.interop.JavaAdapterFactory;
import com.oracle.truffle.js.runtime.interop.JavaClass;
import com.oracle.truffle.js.runtime.interop.JavaMethod;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JavaBuiltins extends JSBuiltinsContainer.SwitchEnum<JavaBuiltins.Java> {
    protected JavaBuiltins() {
        super(JSJava.CLASS_NAME, Java.class);
    }

    public enum Java implements BuiltinEnum<Java> {
        type(1),
        from(1),
        to(2),
        isJavaObject(1),
        isType(1),
        typeName(1),
        synchronized_(2),

        // Nashorn Java Interop only
        extend(-1),
        super_(1),
        isJavaMethod(1),
        isJavaFunction(1),
        isScriptFunction(1),
        isScriptObject(1),
        asJSONCompatible(1);

        private final int length;

        Java(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public boolean isEnabled() {
            return JSTruffleOptions.NashornJavaInterop || EnumSet.of(type, from, to, isJavaObject, isType, typeName, synchronized_).contains(this);
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Java builtinEnum) {
        switch (builtinEnum) {
            case type:
                return JavaTypeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case typeName:
                return JavaTypeNameNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case extend:
                return JavaExtendNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case from:
                return JavaFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case to:
                return JavaToNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case super_:
                return JavaSuperNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isType:
                return JavaIsTypeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isJavaObject:
                return JavaIsJavaObjectNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isJavaMethod:
                return JavaIsJavaMethodNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isJavaFunction:
                return JavaIsJavaFunctionNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isScriptFunction:
                return JavaIsScriptFunctionNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isScriptObject:
                return JavaIsScriptObjectNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case synchronized_:
                return JavaSynchronizedNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case asJSONCompatible:
                return JavaAsJSONCompatibleNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    abstract static class JavaTypeNode extends JSBuiltinNode {

        JavaTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object type(String name) {
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            Object javaType = lookupJavaType(name, env);
            if (javaType == null) {
                throw Errors.createTypeErrorClassNotFound(name);
            }
            if (JSTruffleOptions.NashornJavaInterop) {
                return JavaClass.forClass(asJavaClass(javaType, env));
            }
            return javaType;
        }

        static Class<?> asJavaClass(Object javaType, TruffleLanguage.Env env) {
            if (env.isHostObject(javaType)) {
                Object clazz = env.asHostObject(javaType);
                if (clazz instanceof Class<?>) {
                    return (Class<?>) clazz;
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException();
        }

        static Object lookupJavaType(String name, TruffleLanguage.Env env) {
            if (env != null && env.isHostLookupAllowed()) {
                try {
                    Object found = env.lookupHostSymbol(name);
                    if (found != null) {
                        return found;
                    }
                } catch (Exception ex) {
                }
                return lookForSubclasses(name, env);
            } else {
                throw Errors.createTypeError("Java Interop is not available");
            }
        }

        // The following code is taken from Nashorn's NativeJava.simpleType(...)
        private static Object lookForSubclasses(String className, TruffleLanguage.Env env) {
            // The logic below compensates for a frequent user error - when people use dot notation
            // to separate inner class names, i.e. "java.lang.Character.UnicodeBlock"
            // vs."java.lang.Character$UnicodeBlock". The logic below will try alternative class
            // names, replacing dots at the end of the name with dollar signs.
            final StringBuilder nextName = new StringBuilder(className);
            int lastDot = nextName.length();
            for (;;) {
                lastDot = nextName.lastIndexOf(".", lastDot - 1);
                if (lastDot == -1) {
                    // Exhausted the search space, class not found - return null
                    return null;
                }
                nextName.setCharAt(lastDot, '$');
                try {
                    String innerClassName = nextName.toString();
                    Object found = env.lookupHostSymbol(innerClassName);
                    if (found == null) {
                        continue;
                    }
                    return found;
                } catch (Exception ex) {
                    // Intentionally ignored, so the loop retries with the next name
                }
            }
        }
    }

    abstract static class JavaTypeNameNode extends JSBuiltinNode {

        JavaTypeNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected String typeName(JavaClass type) {
            return type.getType().getName();
        }

        @Specialization(guards = "isClass(type)")
        protected String typeName(Object type) {
            return ((Class<?>) type).getName();
        }

        @Specialization(guards = "isJavaInteropClass(type)")
        protected String typeNameJavaInteropClass(Object type) {
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            return ((Class<?>) env.asHostObject(type)).getName();
        }

        @Fallback
        protected Object nonType(@SuppressWarnings("unused") Object value) {
            return Undefined.instance;
        }

        protected final boolean isJavaInteropClass(Object obj) {
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            return env.isHostObject(obj) && env.asHostObject(obj) instanceof Class<?>;
        }
    }

    abstract static class JavaExtendNode extends JSBuiltinNode {

        JavaExtendNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile needErrorBranches = BranchProfile.create();

        @Specialization
        @TruffleBoundary
        protected JavaClass extend(Object... arguments) {
            if (arguments.length == 0) {
                needErrorBranches.enter();
                throw Errors.createTypeError("Java.extend needs at least one argument.");
            }

            final int typesLength;
            final DynamicObject classOverrides;
            if (JSRuntime.isObject(arguments[arguments.length - 1])) {
                classOverrides = (DynamicObject) arguments[arguments.length - 1];
                typesLength = arguments.length - 1;
                if (typesLength == 0) {
                    needErrorBranches.enter();
                    throw Errors.createTypeError("Java.extend needs at least one type argument.");
                }
            } else {
                classOverrides = null;
                typesLength = arguments.length;
            }

            final Class<?>[] types = new Class<?>[typesLength];
            for (int i = 0; i < typesLength; i++) {
                if (!(arguments[i] instanceof JavaClass)) {
                    needErrorBranches.enter();
                    throw Errors.createTypeError("Java.extend needs Java types as its arguments.");
                }
                types[i] = ((JavaClass) arguments[i]).getType();
            }

            checkAccess(types);

            if (types.length == 1) {
                return JavaClass.forClass(types[0]).extend(classOverrides);
            }
            return JavaAdapterFactory.getAdapterClassFor(types, classOverrides);
        }

        private void checkAccess(final Class<?>[] types) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                boolean classFilterPresent = JSJavaWrapper.isClassFilterPresent(getContext());
                for (final Class<?> type : types) {
                    // check for restricted package access
                    JavaAccess.checkPackageAccess(type);
                    // check for classes, interfaces in reflection
                    JavaAccess.checkReflectionAccess(type, true, classFilterPresent);
                }
            }
        }
    }

    abstract static class JavaFromNode extends JSBuiltinNode {
        JavaFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final BranchProfile objectArrayBranch = BranchProfile.create();
        private final BranchProfile objectListBranch = BranchProfile.create();
        private final BranchProfile longArrayBranch = BranchProfile.create();
        private final BranchProfile intArrayBranch = BranchProfile.create();
        private final BranchProfile needErrorBranches = BranchProfile.create();

        @Child private WriteElementNode writeNode;
        @Child private Node readNode;
        @Child private Node getSizeNode;
        @Child private JSForeignToJSTypeNode foreignConvertNode;

        private void write(Object target, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(WriteElementNode.create(null, null, null, getContext(), false));
            }
            writeNode.executeWithTargetAndIndexAndValue(target, index, value);
        }

        private int sendGetSize(TruffleObject javaArray) throws UnsupportedMessageException {
            if (getSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getSizeNode = insert(Message.GET_SIZE.createNode());
            }
            return (int) ForeignAccess.sendGetSize(getSizeNode, javaArray);
        }

        private Object sendRead(TruffleObject javaArray, int i) throws UnknownIdentifierException, UnsupportedMessageException {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(Message.READ.createNode());
            }
            return foreignConvert(ForeignAccess.sendRead(readNode, javaArray, i));
        }

        private Object foreignConvert(Object value) {
            if (foreignConvertNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreignConvertNode = insert(JSForeignToJSTypeNode.create());
            }
            return foreignConvertNode.executeWithTarget(value);
        }

        @Specialization
        protected DynamicObject from(Object javaObj) {
            if (JSTruffleOptions.NashornJavaInterop) {
                if (javaObj.getClass().isArray()) {
                    int len = Array.getLength(javaObj);
                    DynamicObject jsArrayObj = JSArray.createEmptyChecked(getContext(), len);
                    if (javaObj instanceof long[]) {
                        fromLong(javaObj, len, jsArrayObj);
                    } else if (javaObj instanceof int[]) {
                        fromInt(javaObj, len, jsArrayObj);
                    } else {
                        fromObject(javaObj, len, jsArrayObj);
                    }
                    return jsArrayObj;
                } else if (javaObj instanceof List) {
                    List<?> javaList = (List<?>) javaObj;
                    int len = Boundaries.listSize(javaList);
                    DynamicObject jsArrayObj = JSArray.createEmptyChecked(getContext(), len);
                    fromList(javaList, len, jsArrayObj);
                    return jsArrayObj;
                }
            }
            if (javaObj instanceof TruffleObject) {
                TruffleLanguage.Env env = getContext().getRealm().getEnv();
                TruffleObject javaArray = (TruffleObject) javaObj;
                if (env.isHostObject(javaArray)) {
                    try {
                        int size = sendGetSize(javaArray);
                        if (size >= 0) {
                            DynamicObject jsArray = JSArray.createEmptyChecked(getContext(), size);
                            for (int i = 0; i < size; i++) {
                                Object element = sendRead(javaArray, i);
                                write(jsArray, i, convertValue(element));
                            }
                            return jsArray;
                        }
                    } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                        // fall through
                    }
                    Object hostObject = env.asHostObject(javaArray);
                    if (hostObject instanceof List<?>) {
                        List<?> javaList = (List<?>) hostObject;
                        int len = Boundaries.listSize(javaList);
                        DynamicObject jsArrayObj = JSArray.createEmptyChecked(getContext(), len);
                        fromList(javaList, len, jsArrayObj);
                        return jsArrayObj;
                    }
                }
            }
            needErrorBranches.enter();
            throw Errors.createTypeError("Cannot convert to JavaScript array.");
        }

        private void fromList(List<?> javaList, int len, DynamicObject jsArrayObj) {
            objectListBranch.enter();
            for (int i = 0; i < len; i++) {
                write(jsArrayObj, i, convertValue(Boundaries.listGet(javaList, i)));
            }
        }

        private void fromObject(Object javaObj, int len, DynamicObject jsArrayObj) {
            objectArrayBranch.enter();
            for (int i = 0; i < len; i++) {
                write(jsArrayObj, i, convertValue(Array.get(javaObj, i)));
            }
        }

        private void fromInt(Object javaObj, int len, DynamicObject jsArrayObj) {
            intArrayBranch.enter();
            int[] javaArray = (int[]) javaObj;
            for (int i = 0; i < len; i++) {
                write(jsArrayObj, i, javaArray[i]);
            }
        }

        private void fromLong(Object javaObj, int len, DynamicObject jsArrayObj) {
            longArrayBranch.enter();
            long[] javaArray = (long[]) javaObj;
            for (int i = 0; i < len; i++) {
                write(jsArrayObj, i, (double) javaArray[i]);
            }
        }

        private static Object convertValue(Object object) {
            if (object instanceof Long) {
                return ((Long) object).doubleValue();
            } else if (object instanceof Byte) {
                return ((Byte) object).intValue();
            } else if (object instanceof Short) {
                return ((Short) object).intValue();
            } else if (object instanceof Float) {
                return ((Float) object).doubleValue();
            } else if (object instanceof Character) {
                return (int) ((Character) object).charValue();
            }
            return object;
        }
    }

    abstract static class JavaToNode extends JSBuiltinNode {

        JavaToNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        private final ConditionProfile isArrayCondition = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isListCondition = ConditionProfile.createBinaryProfile();

        @Child private JSToStringNode toStringNode;
        @Child private JSToObjectArrayNode toObjectArrayNode;
        @Child private Node newNode;
        @Child private Node writeNode;

        private String toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        private Object[] toObjectArray(DynamicObject jsObj) {
            if (toObjectArrayNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toObjectArrayNode = insert(JSToObjectArrayNode.create(getContext()));
            }
            return toObjectArrayNode.executeObjectArray(jsObj);
        }

        @Specialization
        protected Object to(DynamicObject jsObj, Object toType) {
            Class<?> targetType;
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            if (toType instanceof TruffleObject && env.isHostObject(toType)) {
                if (isJavaArrayClass(toType, env)) {
                    return toArray(jsObj, (TruffleObject) toType, env);
                } else {
                    throw Errors.createTypeErrorFormat("Unsupported type: %s", toType);
                }
            } else if (toType == Undefined.instance) {
                if (JSTruffleOptions.NashornJavaInterop) {
                    return toArray(jsObj, Object[].class);
                } else {
                    return toArray(jsObj, (TruffleObject) env.asGuestValue(Object[].class), env);
                }
            } else if (JSTruffleOptions.NashornJavaInterop && toType instanceof JavaClass) {
                targetType = ((JavaClass) toType).getType();
            } else {
                String className = toString(toType);
                Object javaType = JavaTypeNode.lookupJavaType(className, env);
                if (JSTruffleOptions.NashornJavaInterop) {
                    targetType = JavaTypeNode.asJavaClass(javaType, env);
                } else if (isJavaArrayClass(javaType, env)) {
                    return toArray(jsObj, (TruffleObject) javaType, env);
                } else {
                    throw Errors.createTypeErrorFormat("Unsupported type: %s", className);
                }
            }
            assert JSTruffleOptions.NashornJavaInterop;
            if (isArrayCondition.profile(targetType.isArray())) {
                return toArray(jsObj, targetType);
            } else if (isListCondition.profile(targetType == List.class)) {
                return toList(jsObj);
            } else {
                throw Errors.createTypeErrorFormat("Unsupported type: %s", targetType);
            }
        }

        private static boolean isJavaArrayClass(Object obj, TruffleLanguage.Env env) {
            if (env.isHostObject(obj)) {
                Object javaObj = env.asHostObject(obj);
                return javaObj instanceof Class && ((Class<?>) javaObj).isArray();
            }
            return false;
        }

        private Object toArray(DynamicObject jsObj, TruffleObject arrayType, TruffleLanguage.Env env) {
            assert isJavaArrayClass(arrayType, env);

            if (newNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                newNode = insert(Message.createNew(1).createNode());
            }
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(Message.WRITE.createNode());
            }

            Object[] arr = toObjectArray(jsObj);
            try {
                TruffleObject result = (TruffleObject) ForeignAccess.sendNew(newNode, arrayType, arr.length);
                for (int i = 0; i < arr.length; i++) {
                    ForeignAccess.sendWrite(writeNode, result, i, arr[i]);
                }
                return result;
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException | UnknownIdentifierException e) {
                throw Errors.createTypeError(Boundaries.javaToString(e));
            }
        }

        private Object toArray(DynamicObject jsObj, Class<?> arrayType) {
            assert JSTruffleOptions.NashornJavaInterop;
            Object[] arr = toObjectArray(jsObj);
            if (arrayType == Object[].class) {
                Object[] result = new Object[arr.length];
                for (int i = 0; i < arr.length; i++) {
                    result[i] = toJava(arr[i]);
                }
                return result;
            } else {
                Class<?> componentType = arrayType.getComponentType();
                Object result = Array.newInstance(componentType, arr.length);
                for (int i = 0; i < arr.length; i++) {
                    Array.set(result, i, convertElement(arr[i], componentType));
                }
                return result;
            }
        }

        @TruffleBoundary
        private static Object convertElement(Object element, Class<?> componentType) {
            if (componentType == int.class) {
                return (int) JSRuntime.toInteger(element);
            } else if (componentType == Integer.class) {
                return isNullOrUndefined(element) ? null : (int) JSRuntime.toInteger(element);
            } else if (componentType == double.class) {
                return JSRuntime.toDouble(element);
            } else if (componentType == Double.class) {
                return isNull(element) ? null : JSRuntime.toDouble(element);
            } else if (componentType == float.class) {
                return (float) JSRuntime.toDouble(element);
            } else if (componentType == Float.class) {
                return isNull(element) ? null : (float) JSRuntime.toDouble(element);
            } else if (componentType == long.class) {
                return JSRuntime.longValue(JSRuntime.toNumber(element));
            } else if (componentType == Long.class) {
                return isNullOrUndefined(element) ? null : JSRuntime.longValue(JSRuntime.toNumber(element));
            } else if (componentType == boolean.class) {
                return JSRuntime.toBoolean(element);
            } else if (componentType == Boolean.class) {
                return isNullOrUndefined(element) ? null : JSRuntime.toBoolean(element);
            } else if (componentType == short.class) {
                return (short) JSRuntime.toInteger(element);
            } else if (componentType == Short.class) {
                return isNullOrUndefined(element) ? null : (short) JSRuntime.toInteger(element);
            } else if (componentType == byte.class) {
                return (byte) JSRuntime.toInteger(element);
            } else if (componentType == Byte.class) {
                return isNullOrUndefined(element) ? null : (byte) JSRuntime.toInteger(element);
            } else if (componentType == char.class) {
                return isNull(element) ? '\0' : toChar(element);
            } else if (componentType == Character.class) {
                return isNull(element) ? null : toChar(element);
            } else if (componentType == String.class) {
                return isNull(element) ? null : JSRuntime.toString(element);
            } else {
                throw Errors.createTypeErrorFormat("Unsupported component type: %s", componentType);
            }
        }

        private static boolean isNull(Object element) {
            return element == Null.instance;
        }

        private static boolean isNullOrUndefined(Object element) {
            return element == Null.instance || element == Undefined.instance;
        }

        @TruffleBoundary
        private static Object toList(DynamicObject jsObj) {
            assert JSTruffleOptions.NashornJavaInterop;
            if (JSArray.isJSArray(jsObj)) {
                return new JSArrayListWrapper(jsObj);
            } else {
                return new JSObjectListWrapper(jsObj);
            }
        }

        private static Character toChar(Object value) {
            if (value instanceof Number) {
                final int intValue = ((Number) value).intValue();
                if (intValue >= Character.MIN_VALUE && intValue <= Character.MAX_VALUE) {
                    return (char) intValue;
                }
                throw Errors.createTypeError("Cannot convert number to character; it is out of 0-65535 range");
            }
            String s = JSRuntime.toString(value);
            if (s.length() != 1) {
                throw Errors.createTypeError("Cannot convert string to character; its length must be exactly 1");
            }
            return s.charAt(0);
        }
    }

    abstract static class JavaSuperNode extends JSBuiltinNode {
        JavaSuperNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object superAdapter(Object adapter) {
            return JavaAdapterFactory.getSuperAdapter(adapter);
        }
    }

    abstract static class JavaIsTypeNode extends JSBuiltinNode {
        JavaIsTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final boolean isType(Object obj) {
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            return (env.isHostObject(obj) && env.asHostObject(obj) instanceof Class<?>) || (JSTruffleOptions.NashornJavaInterop && obj instanceof JavaClass);
        }
    }

    abstract static class JavaIsJavaObject extends JSBuiltinNode {

        JavaIsJavaObject(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final boolean isJavaObject(Object obj) {
            TruffleLanguage.Env env = getContext().getRealm().getEnv();
            return env.isHostObject(obj) || (JSTruffleOptions.NashornJavaInterop && !(obj instanceof TruffleObject) && !JSRuntime.isJSPrimitive(obj));
        }
    }

    abstract static class JavaIsJavaMethodNode extends JSBuiltinNode {
        JavaIsJavaMethodNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isJavaMethod(Object obj) {
            return obj instanceof JavaMethod;
        }
    }

    abstract static class JavaIsJavaFunctionNode extends JSBuiltinNode {
        JavaIsJavaFunctionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isJavaFunction(Object obj,
                        @Cached("create()") TypeOfNode typeofNode) {
            return typeofNode.executeString(obj).equals("function") && !JSFunction.isJSFunction(obj);
        }
    }

    abstract static class JavaIsScriptFunctionNode extends JSBuiltinNode {
        JavaIsScriptFunctionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isScriptFunction(Object obj) {
            return JSFunction.isJSFunction(obj);
        }
    }

    abstract static class JavaIsScriptObjectNode extends JSBuiltinNode {
        JavaIsScriptObjectNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean isScriptObject(Object obj) {
            return JSObject.isJSObject(obj);
        }
    }

    abstract static class JavaSynchronizedNode extends JSBuiltinNode {
        @Child private RealmNode realmNode;

        JavaSynchronizedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.realmNode = RealmNode.create(context);
        }

        @Specialization
        protected Object doSynchronize(VirtualFrame frame, Object func, Object lock) {
            if (!JSFunction.isJSFunction(func)) {
                throw Errors.createTypeErrorNotAFunction(func);
            }
            if (lock != Undefined.instance) {
                unwrapAndCheckLockObject(lock, getContext().getRealm().getEnv());
            }
            JSRealm realm = realmNode.execute(frame);
            JSFunctionData synchronizedFunctionData = createSynchronizedWrapper((DynamicObject) func);
            DynamicObject synchronizedFunction = JSFunction.create(realm, synchronizedFunctionData);
            if (lock != Undefined.instance) {
                return JSFunction.bind(realm, synchronizedFunction, lock, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            }
            return synchronizedFunction;

        }

        @TruffleBoundary
        private JSFunctionData createSynchronizedWrapper(DynamicObject func) {
            final JSContext context = getContext();
            CallTarget callTarget = Truffle.getRuntime().createCallTarget(new JavaScriptRootNode(context.getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object thisObj = JSFrameUtil.getThisObj(frame);
                    Object lock = unwrapAndCheckLockObject(thisObj, context.getRealm().getEnv());
                    Object[] arguments = JSArguments.create(thisObj, func, JSArguments.extractUserArguments(frame.getArguments()));
                    synchronized (lock) {
                        return JSFunction.call(arguments);
                    }
                }
            });
            return JSFunctionData.createCallOnly(context, callTarget, 0, "synchronizedWrapper");
        }

        static Object unwrapJavaObject(Object object, TruffleLanguage.Env env) {
            if (env.isHostObject(object)) {
                return env.asHostObject(object);
            }
            return object;
        }

        static Object unwrapAndCheckLockObject(Object thisObj, TruffleLanguage.Env env) {
            Object lock = unwrapJavaObject(thisObj, env);
            if (JSRuntime.isJSPrimitive(lock) || lock.getClass().isArray()) {
                CompilerDirectives.transferToInterpreter();
                throw Errors.createTypeError("Locking not supported on type: " + lock.getClass().getTypeName());
            }
            return lock;
        }
    }

    abstract static class JavaAsJSONCompatibleNode extends JSBuiltinNode {
        JavaAsJSONCompatibleNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected static Object asJSONCompatible(Object obj) {
            if (JSArray.isJSArray(obj)) {
                return new JSONListWrapper((DynamicObject) obj);
            } else if (JSObject.isJSObject(obj) && JSRuntime.isObject(obj)) {
                return new JSONMapWrapper((DynamicObject) obj);
            } else {
                return obj;
            }
        }
    }

    static Object asJSONCompatible(Object obj) {
        return JavaAsJSONCompatibleNode.asJSONCompatible(obj);
    }

    static Object toJava(Object element) {
        return Converters.JS_TO_JAVA_CONVERTER.convert(element);
    }

    static Object fromJava(Object element) {
        if (element instanceof JSONListWrapper) {
            return ((JSONListWrapper) element).jsObj;
        } else if (element instanceof JSONMapWrapper) {
            return ((JSONMapWrapper) element).jsObj;
        }
        return Converters.JAVA_TO_JS_CONVERTER.convert(element);
    }

    abstract static class ListWrapper extends AbstractList<Object> implements Deque<Object> {
        protected final DynamicObject jsObj;

        ListWrapper(DynamicObject jsObj) {
            assert jsObj != null;
            this.jsObj = jsObj;
        }

        @Override
        public void addFirst(Object o) {
            add(0, o);
        }

        @Override
        public void addLast(Object o) {
            add(size(), o);
        }

        @Override
        public boolean offerFirst(Object o) {
            addFirst(o);
            return true;
        }

        @Override
        public boolean offerLast(Object o) {
            addLast(o);
            return true;
        }

        @Override
        public Object removeFirst() {
            checkEmpty();
            return pollFirst();
        }

        @Override
        public Object removeLast() {
            checkEmpty();
            return pollLast();
        }

        @Override
        public Object pollFirst() {
            if (isEmpty()) {
                return null;
            }
            return remove(0);
        }

        @Override
        public Object pollLast() {
            if (isEmpty()) {
                return null;
            }
            return remove(size() - 1);
        }

        @Override
        public Object getFirst() {
            checkEmpty();
            return peekFirst();
        }

        @Override
        public Object getLast() {
            checkEmpty();
            return peekLast();
        }

        @Override
        public Object peekFirst() {
            if (isEmpty()) {
                return null;
            }
            return get(0);
        }

        @Override
        public Object peekLast() {
            if (isEmpty()) {
                return null;
            }
            return get(size() - 1);
        }

        @Override
        public boolean removeFirstOccurrence(Object o) {
            return removeFirstOccurrence(iterator(), o);
        }

        @Override
        public boolean removeLastOccurrence(Object o) {
            return removeFirstOccurrence(descendingIterator(), o);
        }

        @Override
        public boolean offer(Object o) {
            addLast(o);
            return true;
        }

        @Override
        public Object remove() {
            return removeFirst();
        }

        @Override
        public Object poll() {
            return pollFirst();
        }

        @Override
        public Object element() {
            return getFirst();
        }

        @Override
        public Object peek() {
            return peekFirst();
        }

        @Override
        public void push(Object o) {
            addFirst(o);
        }

        @Override
        public Object pop() {
            return removeFirst();
        }

        @Override
        public Iterator<Object> descendingIterator() {
            final ListIterator<Object> listIterator = listIterator(size());
            return new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return listIterator.hasPrevious();
                }

                @Override
                public Object next() {
                    return listIterator.previous();
                }

                @Override
                public void remove() {
                    listIterator.remove();
                }
            };
        }

        private void checkEmpty() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
        }

        private static boolean removeFirstOccurrence(Iterator<Object> iterator, Object o) {
            while (iterator.hasNext()) {
                if (Objects.equals(o, iterator.next())) {
                    iterator.remove();
                    return true;
                }
            }
            return false;
        }

        static void checkRange(int index, int upperRange) {
            if (index < 0 || index > upperRange) {
                throw new IndexOutOfBoundsException("index " + index + " is < 0 or > " + upperRange);
            }
        }
    }

    static class JSArrayListWrapper extends ListWrapper {

        JSArrayListWrapper(DynamicObject jsObj) {
            super(jsObj);
            assert JSArray.isJSArray(jsObj);
        }

        private ScriptArray getArray() {
            return JSObject.getArray(jsObj);
        }

        protected Object convertToJava(Object element) {
            return toJava(element);
        }

        @Override
        public Object get(int index) {
            checkRange(index, size() - 1);
            Object element = getArray().getElement(jsObj, index);
            return convertToJava(element);
        }

        @Override
        public void add(int index, Object o) {
            int size = size();
            checkRange(index, size);
            try {
                JSAbstractArray.arraySetArrayType(jsObj, getArray().addRange(jsObj, index, 1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IndexOutOfBoundsException(ex.getMessage());
            }
            JSAbstractArray.arraySetArrayType(jsObj, getArray().setElement(jsObj, index, fromJava(o), false));
            JSAbstractArray.arraySetLength(jsObj, size + 1);
        }

        @Override
        public Object set(int index, Object o) {
            checkRange(index, size() - 1);
            Object element = get(index);
            JSAbstractArray.arraySetArrayType(jsObj, getArray().setElement(jsObj, index, fromJava(o), false));
            return convertToJava(element);
        }

        @Override
        public Object remove(int index) {
            int size = size();
            checkRange(index, size - 1);
            Object element = get(index);
            try {
                JSAbstractArray.arraySetArrayType(jsObj, getArray().removeRange(jsObj, index, index + 1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IndexOutOfBoundsException(ex.getMessage());
            }
            JSAbstractArray.arraySetLength(jsObj, size - 1);
            return convertToJava(element);
        }

        @Override
        public int size() {
            return getArray().lengthInt(jsObj);
        }
    }

    static class JSObjectListWrapper extends ListWrapper {
        JSObjectListWrapper(DynamicObject array) {
            super(array);
            assert JSArray.isJSArray(array);
        }

        @Override
        public Object get(int index) {
            return toJava(JSObject.get(jsObj, index));
        }

        @Override
        public Object set(int index, Object o) {
            checkRange(index, size() - 1);
            Object element = get(index);
            JSObject.set(jsObj, index, fromJava(o));
            return toJava(element);
        }

        @Override
        public int size() {
            return JSRuntime.toInt32(JSObject.get(jsObj, JSAbstractArray.LENGTH));
        }
    }

    static class JSONListWrapper extends JSArrayListWrapper {
        JSONListWrapper(DynamicObject array) {
            super(array);
            assert JSArray.isJSArray(array);
        }

        @Override
        protected Object convertToJava(Object element) {
            return asJSONCompatible(super.convertToJava(element));
        }
    }

    static class JSONMapWrapper extends AbstractMap<String, Object> implements Bindings {
        final DynamicObject jsObj;

        JSONMapWrapper(DynamicObject object) {
            this.jsObj = object;
        }

        private static Object convertToJava(final Object element) {
            return asJSONCompatible(toJava(element));
        }

        final List<String> keyList() {
            return JSObject.enumerableOwnNames(jsObj);
        }

        @Override
        public int size() {
            return keyList().size();
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }

        @Override
        public boolean containsKey(Object key) {
            return JSObject.hasProperty(jsObj, key);
        }

        @Override
        public Object get(Object key) {
            return convertToJava(JSObject.get(jsObj, key));
        }

        @Override
        public Object put(String key, Object value) {
            final Object oldValue = get(key);
            JSObject.set(jsObj, key, fromJava(value));
            return convertToJava(oldValue);
        }

        @Override
        public Object remove(Object key) {
            final Object oldValue = get(key);
            JSObject.delete(jsObj, key);
            return convertToJava(oldValue);
        }

        @Override
        public void clear() {
            for (String key : keyList()) {
                JSObject.delete(jsObj, key);
            }
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return new AbstractSet<Map.Entry<String, Object>>() {
                @Override
                public Iterator<Map.Entry<String, Object>> iterator() {
                    return new Iterator<Map.Entry<String, Object>>() {
                        Iterator<String> keysIterator = keyList().iterator();

                        @Override
                        public Entry<String, Object> next() {
                            String key = keysIterator.next();
                            return new Map.Entry<String, Object>() {
                                @Override
                                public String getKey() {
                                    return key;
                                }

                                @Override
                                public Object getValue() {
                                    return get(key);
                                }

                                @Override
                                public Object setValue(Object value) {
                                    return put(key, value);
                                }
                            };
                        }

                        @Override
                        public boolean hasNext() {
                            return keysIterator.hasNext();
                        }
                    };
                }

                @Override
                public int size() {
                    return JSONMapWrapper.this.size();
                }
            };
        }
    }
}
