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
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.JavaBuiltinsFactory.JavaAddToClasspathNodeGen;
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
import com.oracle.truffle.js.nodes.access.WriteElementNode;
import com.oracle.truffle.js.nodes.cast.JSToObjectArrayNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.nodes.interop.ImportValueNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.interop.JSInteropUtil;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public final class JavaBuiltins extends JSBuiltinsContainer.SwitchEnum<JavaBuiltins.Java> {

    public static final TruffleString SYNCHRONIZED_WRAPPER_NAME = Strings.constant("synchronizedWrapper");

    public static final JSBuiltinsContainer BUILTINS = new JavaBuiltins();
    public static final JSBuiltinsContainer BUILTINS_NASHORN_COMPAT = new JavaNashornCompatBuiltins();

    protected JavaBuiltins() {
        super(JSRealm.JAVA_CLASS_NAME, Java.class);
    }

    public enum Java implements BuiltinEnum<Java> {
        type(1),
        from(1),
        to(2),
        isJavaObject(1),
        isType(1),
        typeName(1),
        addToClasspath(1),

        extend(1) {
            @Override
            public boolean isAOTSupported() {
                return false;
            }
        },
        super_(1) {
            @Override
            public boolean isAOTSupported() {
                return false;
            }
        };

        private final int length;

        Java(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Java builtinEnum) {
        switch (builtinEnum) {
            case type:
                return JavaTypeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case typeName:
                return JavaTypeNameNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case from:
                return JavaFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case to:
                return JavaToNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case isType:
                return JavaIsTypeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case isJavaObject:
                return JavaIsJavaObjectNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case addToClasspath:
                return JavaAddToClasspathNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));

            case extend:
                if (!JSConfig.SubstrateVM) {
                    return JavaExtendNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
                }
                break;
            case super_:
                if (!JSConfig.SubstrateVM) {
                    return JavaSuperNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                }
                break;
        }
        return null;
    }

    public static final class JavaNashornCompatBuiltins extends JSBuiltinsContainer.SwitchEnum<JavaNashornCompatBuiltins.JavaNashornCompat> {
        protected JavaNashornCompatBuiltins() {
            super(JSRealm.JAVA_CLASS_NAME_NASHORN_COMPAT, JavaNashornCompat.class);
        }

        public enum JavaNashornCompat implements BuiltinEnum<JavaNashornCompat> {
            isJavaMethod(1),
            isJavaFunction(1),
            isScriptFunction(1),
            isScriptObject(1),

            synchronized_(2) {
                @Override
                public boolean isAOTSupported() {
                    return false;
                }
            };

            private final int length;

            JavaNashornCompat(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, JavaNashornCompat builtinEnum) {
            switch (builtinEnum) {
                case isJavaMethod:
                    return JavaIsJavaMethodNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case isJavaFunction:
                    return JavaIsJavaFunctionNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case isScriptFunction:
                    return JavaIsScriptFunctionNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case isScriptObject:
                    return JavaIsScriptObjectNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
                case synchronized_:
                    if (!JSConfig.SubstrateVM) {
                        return JavaSynchronizedNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
                    }
                    break;
            }
            return null;
        }
    }

    abstract static class JavaTypeNode extends JSBuiltinNode {

        JavaTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary
        protected Object type(TruffleString name) {
            TruffleLanguage.Env env = getRealm().getEnv();
            Object javaType = lookupJavaType(name, env);
            if (javaType == null) {
                throw Errors.createTypeErrorClassNotFound(name);
            }
            return javaType;
        }

        @Specialization(guards = "!isString(obj)")
        protected Object typeNoString(@SuppressWarnings("unused") Object obj) {
            throw Errors.createTypeError("Java.type expects one string argument");
        }

        @TruffleBoundary
        static Object lookupJavaType(TruffleString name, TruffleLanguage.Env env) {
            if (env != null && env.isHostLookupAllowed()) {
                try {
                    Object found = env.lookupHostSymbol(Strings.toJavaString(name));
                    if (found != null) {
                        return found;
                    }
                } catch (Exception ex) {
                }
                return lookForSubclasses(Strings.toJavaString(name), env);
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

    @ImportStatic(JSConfig.class)
    abstract static class JavaTypeNameNode extends JSBuiltinNode {

        JavaTypeNameNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = "isJavaInteropClass(type, typeInterop)")
        protected Object typeNameJavaInteropClass(Object type,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary typeInterop,
                        @Cached ImportValueNode importValue) {
            try {
                return importValue.executeWithTarget(typeInterop.getMetaQualifiedName(type));
            } catch (UnsupportedMessageException e) {
                throw Errors.createTypeErrorInteropException(type, e, "Java.typeName", this);
            }
        }

        @Fallback
        protected Object nonType(@SuppressWarnings("unused") Object value) {
            return Undefined.instance;
        }

        protected final boolean isJavaInteropClass(Object obj, InteropLibrary typeInterop) {
            TruffleLanguage.Env env = getRealm().getEnv();
            return env.isHostObject(obj) && typeInterop.isMetaObject(obj);
        }
    }

    abstract static class JavaExtendNode extends JSBuiltinNode {

        JavaExtendNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected Object extend(Object[] arguments,
                        @Cached InlinedBranchProfile errorBranch) {
            if (JSConfig.SubstrateVM) {
                throw Errors.unsupported("JavaAdapter");
            }
            if (arguments.length == 0) {
                errorBranch.enter(this);
                throw Errors.createTypeError("Java.extend needs at least one argument.");
            }

            final int typesLength;
            final Object classOverrides;
            if (JSRuntime.isObject(arguments[arguments.length - 1])) {
                classOverrides = arguments[arguments.length - 1];
                typesLength = arguments.length - 1;
                if (typesLength == 0) {
                    errorBranch.enter(this);
                    throw Errors.createTypeError("Java.extend needs at least one type argument.");
                }
            } else {
                classOverrides = null;
                typesLength = arguments.length;
            }

            final TruffleLanguage.Env env = getRealm().getEnv();
            final Object[] types = new Object[typesLength];
            for (int i = 0; i < typesLength; i++) {
                if (!isType(arguments[i], env)) {
                    errorBranch.enter(this);
                    throw Errors.createTypeError("Java.extend needs Java types as its arguments.");
                }
                types[i] = arguments[i];
            }

            try {
                if (classOverrides != null) {
                    return env.createHostAdapterWithClassOverrides(types, classOverrides);
                } else {
                    return env.createHostAdapter(types);
                }
            } catch (Exception ex) {
                throw Errors.createTypeError(ex.getMessage(), ex, this);
            }
        }

        protected static boolean isType(Object obj, TruffleLanguage.Env env) {
            return env.isHostObject(obj) && (env.isHostSymbol(obj) || InteropLibrary.getUncached().isMetaObject(obj));
        }
    }

    @ImportStatic(JSConfig.class)
    abstract static class JavaFromNode extends JSBuiltinNode {

        JavaFromNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected JSDynamicObject from(Object javaArray,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached ImportValueNode importValueNode,
                        @Cached("createCachedInterop()") WriteElementNode writeNode,
                        @Cached InlinedBranchProfile errorBranch) {
            JSRealm realm = getRealm();
            TruffleLanguage.Env env = realm.getEnv();
            if (env.isHostObject(javaArray)) {
                // Handles Java arrays and java.util.List.
                try {
                    long size = interop.getArraySize(javaArray);
                    if (size < 0 || size >= Integer.MAX_VALUE) {
                        throw Errors.createRangeErrorInvalidArrayLength(this);
                    }
                    JSDynamicObject jsArray = JSArray.createEmptyChecked(getContext(), realm, size);
                    for (int i = 0; i < size; i++) {
                        Object element = importValueNode.executeWithTarget(interop.readArrayElement(javaArray, i));
                        writeNode.executeWithTargetAndIndexAndValue(jsArray, i, element);
                    }
                    return jsArray;
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    // fall through
                }
            }
            errorBranch.enter(this);
            throw Errors.createTypeError("Cannot convert to JavaScript array.");
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class JavaToNode extends JSBuiltinNode {

        @Child private JSToObjectArrayNode toObjectArrayNode;
        @Child private ExportValueNode exportValue;
        @Child private InteropLibrary newArray;
        @Child private InteropLibrary arrayElements;
        @Child private JSToStringNode toStringNode;

        JavaToNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            this.toObjectArrayNode = JSToObjectArrayNode.create();
            this.exportValue = ExportValueNode.create();
            this.newArray = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);
            this.arrayElements = InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit);

        }

        private TruffleString toString(Object target) {
            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStringNode = insert(JSToStringNode.create());
            }
            return toStringNode.executeString(target);
        }

        @Specialization(guards = {"isJSObject(jsObj)"})
        protected Object to(Object jsObj, Object toType,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("typeInterop") InteropLibrary interop) {
            TruffleLanguage.Env env = getRealm().getEnv();
            Object javaType;
            boolean knownArrayClass = false;
            if (env.isHostObject(toType)) {
                javaType = toType;
            } else if (toType == Undefined.instance) {
                if (env.isHostLookupAllowed()) {
                    javaType = env.lookupHostSymbol("java.lang.Object[]");
                } else {
                    javaType = env.asGuestValue(Object[].class);
                }
                knownArrayClass = true;
            } else {
                TruffleString className = toString(toType);
                javaType = JavaTypeNode.lookupJavaType(className, env);
                if (javaType == null) {
                    throw Errors.createTypeErrorClassNotFound(className);
                }
            }
            if (knownArrayClass || isJavaArrayClass(javaType, env, interop)) {
                return toArray(jsObj, javaType);
            } else {
                throw Errors.createTypeErrorFormat("Unsupported type: %s", toString(javaType));
            }
        }

        @Specialization(guards = {"!isJSObject(obj)"}, limit = "InteropLibraryLimit")
        protected Object toNonObject(Object obj, @SuppressWarnings("unused") Object toType,
                        @CachedLibrary("obj") InteropLibrary objInterop,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Shared("typeInterop") InteropLibrary typeInterop) {
            if (objInterop.hasArrayElements(obj)) {
                return to(obj, toType, typeInterop);
            }
            throw Errors.createTypeErrorNotAnObject(obj);
        }

        private static boolean isJavaArrayClass(Object type, TruffleLanguage.Env env, InteropLibrary interop) {
            try {
                return env.isHostObject(type) && interop.isMetaObject(type) && interop.asString(interop.getMetaQualifiedName(type)).endsWith("[]");
            } catch (UnsupportedMessageException e) {
                assert false : e;
                return false;
            }
        }

        private Object toArray(Object jsObj, Object arrayType) {
            Object[] arr = toObjectArrayNode.executeObjectArray(jsObj);
            try {
                Object result = newArray.instantiate(arrayType, arr.length);
                for (int i = 0; i < arr.length; i++) {
                    arrayElements.writeArrayElement(result, i, exportValue.execute(arr[i]));
                }
                return result;
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException | InvalidArrayIndexException e) {
                throw Errors.createTypeError(e, this);
            }
        }
    }

    @ImportStatic({JSConfig.class})
    abstract static class JavaSuperNode extends JSBuiltinNode {
        JavaSuperNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object superAdapter(Object adapter,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop,
                        @Cached ImportValueNode toJSType) {
            return JSInteropUtil.readMemberOrDefault(adapter, Strings.SUPER, Undefined.instance, interop, toJSType, this);
        }
    }

    @ImportStatic(JSConfig.class)
    abstract static class JavaIsTypeNode extends JSBuiltinNode {
        JavaIsTypeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final boolean isType(Object obj) {
            TruffleLanguage.Env env = getRealm().getEnv();
            return env.isHostSymbol(obj);
        }
    }

    abstract static class JavaIsJavaObject extends JSBuiltinNode {

        JavaIsJavaObject(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected final boolean isJavaObject(Object obj) {
            TruffleLanguage.Env env = getRealm().getEnv();
            return env.isHostObject(obj) || env.isHostFunction(obj);
        }
    }

    abstract static class JavaIsJavaMethodNode extends JSBuiltinNode {
        JavaIsJavaMethodNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isJavaMethod(Object obj) {
            TruffleLanguage.Env env = getRealm().getEnv();
            return env.isHostFunction(obj);
        }
    }

    @ImportStatic(JSConfig.class)
    abstract static class JavaIsJavaFunctionNode extends JSBuiltinNode {
        JavaIsJavaFunctionNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected boolean isJavaFunction(Object obj,
                        @CachedLibrary(limit = "InteropLibraryLimit") InteropLibrary interop) {
            TruffleLanguage.Env env = getRealm().getEnv();
            return env.isHostFunction(obj) || (env.isHostObject(obj) && interop.isMetaObject(obj));
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
            return JSDynamicObject.isJSDynamicObject(obj);
        }
    }

    abstract static class JavaSynchronizedNode extends JSBuiltinNode {

        JavaSynchronizedNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @TruffleBoundary
        @Specialization
        protected Object doSynchronize(Object func, Object lock) {
            if (!JSFunction.isJSFunction(func)) {
                throw Errors.createTypeErrorNotAFunction(func);
            }
            JSRealm realm = getRealm();
            if (lock != Undefined.instance) {
                unwrapAndCheckLockObject(lock, realm.getEnv());
            }
            JSFunctionData synchronizedFunctionData = createSynchronizedWrapper((JSFunctionObject) func);
            JSFunctionObject synchronizedFunction = JSFunction.create(realm, synchronizedFunctionData);
            if (lock != Undefined.instance) {
                return JSFunction.bind(realm, synchronizedFunction, lock, JSArguments.EMPTY_ARGUMENTS_ARRAY);
            }
            return synchronizedFunction;

        }

        @TruffleBoundary
        private JSFunctionData createSynchronizedWrapper(JSFunctionObject func) {
            CallTarget callTarget = new JavaScriptRootNode(getContext().getLanguage(), null, null) {
                @Override
                public Object execute(VirtualFrame frame) {
                    Object thisObj = JSFrameUtil.getThisObj(frame);
                    Object lock = unwrapAndCheckLockObject(thisObj, getRealm().getEnv());
                    Object[] arguments = JSArguments.create(thisObj, func, JSArguments.extractUserArguments(frame.getArguments()));
                    synchronized (lock) {
                        return JSFunction.call(arguments);
                    }
                }
            }.getCallTarget();
            return JSFunctionData.createCallOnly(getContext(), callTarget, 0, SYNCHRONIZED_WRAPPER_NAME);
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

    abstract static class JavaAddToClasspathNode extends JSBuiltinNode {
        JavaAddToClasspathNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doString(TruffleString fileName) {
            TruffleLanguage.Env env = getRealm().getEnv();
            try {
                TruffleFile file = env.getPublicTruffleFile(Strings.toJavaString(fileName));
                env.addToHostClassPath(file);
            } catch (SecurityException | UnsupportedOperationException | IllegalArgumentException e) {
                throw Errors.createErrorFromException(e);
            }
            return Undefined.instance;
        }

        @Specialization(replaces = "doString")
        protected Object doObject(Object fileName,
                        @Cached JSToStringNode toStringNode) {
            return doString(toStringNode.executeString(fileName));
        }
    }
}
