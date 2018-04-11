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
package com.oracle.truffle.js.runtime.interop;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_VARARGS;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * Generates bytecode for a Java adapter class. Used by the {link JavaAdapterFactory}.
 * </p>
 * <p>
 * For every protected or public constructor in the extended class, the adapter class will have
 * between one to three public constructors (visibility of protected constructors in the extended
 * class is promoted to public).
 * <ul>
 * <li>In every case, a constructor taking a trailing ScriptObject argument preceded by original
 * constructor arguments is always created on the adapter class. When such a constructor is invoked,
 * the passed ScriptObject's member functions are used to implement and/or override methods on the
 * original class, dispatched by name. A single JavaScript function will act as the implementation
 * for all overloaded methods of the same name. When methods on an adapter instance are invoked, the
 * functions are invoked having the ScriptObject passed in the instance constructor as their "this".
 * Subsequent changes to the ScriptObject (reassignment or removal of its functions) are not
 * reflected in the adapter instance; the method implementations are bound to functions at
 * constructor invocation time. {@code java.lang.Object} methods {@code equals}, {@code hashCode},
 * and {@code toString} can also be overridden. The only restriction is that since every JavaScript
 * object already has a {@code toString} function through the {@code Object.prototype}, the
 * {@code toString} in the adapter is only overridden if the passed ScriptObject has a
 * {@code toString} function as its own property, and not inherited from a prototype. All other
 * adapter methods can be implemented or overridden through a prototype-inherited function of the
 * ScriptObject passed to the constructor too.</li>
 * <li>If the original types collectively have only one abstract method, or have several of them,
 * but all share the same name, an additional constructor is provided for every original
 * constructor; this one takes a ScriptFunction as its last argument preceded by original
 * constructor arguments. This constructor will use the passed function as the implementation for
 * all abstract methods. For consistency, any concrete methods sharing the single abstract method
 * name will also be overridden by the function. When methods on the adapter instance are invoked,
 * the ScriptFunction is invoked with global or UNDEFINED as its "this" depending whether the
 * function is non-strict or not.</li>
 * <li>If the adapter being generated can have class-level overrides, constructors taking same
 * arguments as the superclass constructors are also created. These constructors simply delegate to
 * the superclass constructor. They are used to create instances of the adapter class with no
 * instance-level overrides.</li>
 * </ul>
 * </p>
 * <p>
 * For adapter methods that return values, all the JavaScript-to-Java conversions supported by
 * Nashorn will be in effect to coerce the JavaScript function return value to the expected Java
 * return type.
 * </p>
 * <p>
 * Since we are adding a trailing argument to the generated constructors in the adapter class, they
 * will never be declared as variable arity, even if the original constructor in the superclass was
 * declared as variable arity. The reason we are passing the additional argument at the end of the
 * argument list instead at the front is that the source-level script expression
 * <code>new X(a, b) { ... }</code> (which is a proprietary syntax extension Nashorn uses to
 * resemble Java anonymous classes) is actually equivalent to <code>new X(a, b, { ... })</code>.
 * </p>
 * <p>
 * It is possible to create two different classes: those that can have both class-level and
 * instance-level overrides, and those that can only have instance-level overrides. When {link
 * JavaAdapterFactory#getAdapterClassFor(Class[], ScriptObject)} is invoked with non-null
 * {@code classOverrides} parameter, an adapter class is created that can have class-level
 * overrides, and the passed script object will be used as the implementations for its methods, just
 * as in the above case of the constructor taking a script object. Note that in the case of
 * class-level overrides, a new adapter class is created on every invocation, and the implementation
 * object is bound to the class, not to any instance. All created instances will share these
 * functions. Of course, when instances of such a class are being created, they can still take
 * another object (or possibly a function) in their constructor's trailing position and thus provide
 * further instance-specific overrides. The order of invocation is always instance-specified method,
 * then a class-specified method, and finally the superclass method.
 */
public final class JavaAdapterBytecodeGenerator {
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final String OBJECT_TYPE_NAME = OBJECT_TYPE.getInternalName();
    private static final Type JSOBJECT_TYPE = Type.getType(DynamicObject.class);
    private static final String JSOBJECT_TYPE_DESCRIPTOR = JSOBJECT_TYPE.getDescriptor();
    private static final String LONG_TYPE_DESCRIPTOR = Type.LONG_TYPE.getDescriptor();

    private static final String VOID_NOARG_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE);

    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type METHOD_TYPE_TYPE = Type.getType(MethodType.class);
    private static final Type METHOD_HANDLE_TYPE = Type.getType(MethodHandle.class);
    /** @see JavaAdapterServices#getHandle(MethodType) */
    private static final String GET_HANDLE_NAME = "getHandle";
    private static final String GET_HANDLE_DESCRIPTOR = Type.getMethodDescriptor(METHOD_HANDLE_TYPE, METHOD_TYPE_TYPE);
    /** @see JavaAdapterServices#getFunction(DynamicObject, String) */
    private static final String GET_CALLEE_NAME = "getFunction";
    private static final String GET_CALLEE_DESCRIPTOR = Type.getMethodDescriptor(JSOBJECT_TYPE, JSOBJECT_TYPE, STRING_TYPE);
    /** @see JavaAdapterServices#getClassOverrides() */
    private static final String GET_CLASS_OVERRIDES_NAME = "getClassOverrides";
    private static final String GET_CLASS_INITIALIZER_DESCRIPTOR = Type.getMethodDescriptor(JSOBJECT_TYPE);
    private static final Type RUNTIME_EXCEPTION_TYPE = Type.getType(RuntimeException.class);
    private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
    private static final Type UNSUPPORTED_OPERATION_TYPE = Type.getType(UnsupportedOperationException.class);

    private static final String SERVICES_CLASS_TYPE_NAME = Type.getInternalName(JavaAdapterServices.class);
    private static final String RUNTIME_EXCEPTION_TYPE_NAME = RUNTIME_EXCEPTION_TYPE.getInternalName();
    private static final String ERROR_TYPE_NAME = Type.getInternalName(Error.class);
    private static final String THROWABLE_TYPE_NAME = THROWABLE_TYPE.getInternalName();
    private static final String UNSUPPORTED_OPERATION_TYPE_NAME = UNSUPPORTED_OPERATION_TYPE.getInternalName();

    private static final String METHOD_HANDLE_TYPE_DESCRIPTOR = METHOD_HANDLE_TYPE.getDescriptor();

    private static final String THREAD_CLASS_TYPE_NAME = Type.getInternalName(Thread.class);
    private static final String CURRENT_THREAD_NAME = "currentThread";
    private static final String CURRENT_THREAD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(Thread.class));
    private static final String GET_ID_NAME = "getId";
    private static final String GET_ID_DESCRIPTOR = Type.getMethodDescriptor(Type.LONG_TYPE);
    /** @see JavaAdapterServices#sameThreadCheck(long) */
    private static final String SAME_THREAD_CHECK_NAME = "sameThreadCheck";
    private static final String SAME_THREAD_CHECK_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE);

    /*
     * Package used when the adapter can't be defined in the adaptee's package (either because it's
     * sealed, or because it's a java.* package.
     */
    private static final String ADAPTER_PACKAGE_PREFIX = "com/oracle/truffle/js/javaadapters/";
    /*
     * Class name suffix used to append to the adaptee class name, when it can be defined in the
     * adaptee's package.
     */
    private static final String ADAPTER_CLASS_NAME_SUFFIX = "$$JSJavaAdapter";
    private static final String JAVA_PACKAGE_PREFIX = "java/";
    private static final int MAX_GENERATED_TYPE_NAME_LENGTH = 255;

    private static final String INIT = "<init>";
    private static final String CLASS_INIT = "<clinit>";
    private static final String THISBINDING_FIELD_NAME = "thisBinding";
    private static final String STATIC_THISBINDING_FIELD_NAME = "staticThisBinding";
    private static final String THREAD_FIELD_NAME = "thread";

    // Method name prefix for invoking super-methods
    static final String SUPER_PREFIX = "super$";

    /**
     * Collection of methods we never override: Object.clone(), Object.finalize().
     */
    private static final Collection<MethodInfo> EXCLUDED = getExcludedMethods();

    // This is the superclass for our generated adapter.
    private final Class<?> superClass;
    // Interfaces implemented by our generated adapter.
    private final List<Class<?>> interfaces;
    /*
     * Class loader used as the parent for the class loader we'll create to load the generated
     * class. It will be a class loader that has the visibility of all original types (class to
     * extend and interfaces to implement) and of the Nashorn classes.
     */
    private final ClassLoader commonLoader;
    // Is this a generator for the version of the class that can have overrides on the class level?
    private final boolean classOverride;
    // Binary name of the superClass
    private final String superClassName;
    // Binary name of the generated class.
    private final String generatedClassName;
    private final Set<String> usedFieldNames = new HashSet<>();
    private final Set<String> abstractMethodNames = new HashSet<>();
    private final String samName;
    private final Set<MethodInfo> finalMethods = new HashSet<>(EXCLUDED);
    private final Set<MethodInfo> methodInfos = new HashSet<>();
    private boolean autoConvertibleFromFunction = false;

    private final ClassWriter cw;

    private final boolean emitSameThreadCheck;

    /**
     * Creates a generator for the bytecode for the adapter for the specified superclass and
     * interfaces.
     *
     * @param superClass the superclass the adapter will extend.
     * @param interfaces the interfaces the adapter will implement.
     * @param commonLoader the class loader that can see all of superClass, interfaces, and Nashorn
     *            classes.
     * @param classOverride true to generate the bytecode for the adapter that has both class-level
     *            and instance-level overrides, false to generate the bytecode for the adapter that
     *            only has instance-level overrides.
     *
     *            throws AdaptationException if the adapter can not be generated for some reason.
     */
    public JavaAdapterBytecodeGenerator(final Class<?> superClass, final List<Class<?>> interfaces, final ClassLoader commonLoader, final boolean classOverride) {
        assert superClass != null && !superClass.isInterface();
        assert interfaces != null;

        this.superClass = superClass;
        this.interfaces = interfaces;
        this.classOverride = classOverride;
        this.commonLoader = commonLoader;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                /*
                 * We need to override ClassWriter.getCommonSuperClass to use this factory's
                 * commonLoader as a class loader to find the common superclass of two types when
                 * needed.
                 */
                return JavaAdapterBytecodeGenerator.this.getCommonSuperClass(type1, type2);
            }
        };
        superClassName = Type.getInternalName(superClass);
        generatedClassName = getGeneratedClassName(superClass, interfaces);

        cw.visit(Opcodes.V1_8, ACC_PUBLIC | ACC_SUPER, generatedClassName, null, superClassName, getInternalTypeNames(interfaces));

        generateThisBindingFields();

        gatherMethods(superClass);
        gatherMethods(interfaces);
        samName = abstractMethodNames.size() == 1 ? abstractMethodNames.iterator().next() : null;

        emitSameThreadCheck = JSTruffleOptions.SingleThreaded;
        if (emitSameThreadCheck) {
            generateThreadField();
        }

        generateHandleFields();
        generateClassInit();
        generateConstructors();
        generateMethods();
        generateSuperMethods();

        cw.visitEnd();
    }

    private void generateThreadField() {
        cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, THREAD_FIELD_NAME, LONG_TYPE_DESCRIPTOR, null, null).visitEnd();
        usedFieldNames.add(THREAD_FIELD_NAME);
    }

    private void generateThisBindingFields() {
        cw.visitField(ACC_PRIVATE | ACC_FINAL, THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR, null, null).visitEnd();
        usedFieldNames.add(THISBINDING_FIELD_NAME);
        if (classOverride) {
            cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, STATIC_THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR, null, null).visitEnd();
            usedFieldNames.add(STATIC_THISBINDING_FIELD_NAME);
        }
    }

    public JavaAdapterClassLoader createAdapterClassLoader() {
        return new JavaAdapterClassLoader(generatedClassName, cw.toByteArray());
    }

    boolean isAutoConvertibleFromFunction() {
        return autoConvertibleFromFunction;
    }

    private static String getGeneratedClassName(final Class<?> superType, final List<Class<?>> interfaces) {
        /*
         * The class we use to primarily name our adapter is either the superclass, or if it is
         * Object (meaning we're just implementing interfaces or extending Object), then the first
         * implemented interface or Object.
         */
        final Class<?> namingType = superType == Object.class ? (interfaces.isEmpty() ? Object.class : interfaces.get(0)) : superType;
        final Package pkg = namingType.getPackage();
        final String namingTypeName = Type.getInternalName(namingType);
        final StringBuilder buf = new StringBuilder();
        if (namingTypeName.startsWith(JAVA_PACKAGE_PREFIX) || pkg == null || pkg.isSealed()) {
            // Can't define new classes in java.* packages
            buf.append(ADAPTER_PACKAGE_PREFIX).append(namingTypeName);
        } else {
            buf.append(namingTypeName).append(ADAPTER_CLASS_NAME_SUFFIX);
        }
        final Iterator<Class<?>> it = interfaces.iterator();
        if (superType == Object.class && it.hasNext()) {
            it.next(); // Skip first interface, it was used to primarily name the adapter
        }
        // Append interface names to the adapter name
        while (it.hasNext()) {
            buf.append("$$").append(it.next().getSimpleName());
        }
        return buf.toString().substring(0, Math.min(MAX_GENERATED_TYPE_NAME_LENGTH, buf.length()));
    }

    /**
     * Given a list of class objects, return an array with their binary names. Used to generate the
     * array of interface names to implement.
     *
     * @param classes the classes
     * @return an array of names
     */
    private static String[] getInternalTypeNames(final List<Class<?>> classes) {
        final int interfaceCount = classes.size();
        final String[] interfaceNames = new String[interfaceCount];
        for (int i = 0; i < interfaceCount; ++i) {
            interfaceNames[i] = Type.getInternalName(classes.get(i));
        }
        return interfaceNames;
    }

    private void generateHandleFields() {
        for (final MethodInfo mi : methodInfos) {
            cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, mi.methodHandleFieldName, METHOD_HANDLE_TYPE_DESCRIPTOR, null, null).visitEnd();

            cw.visitField(ACC_PRIVATE | ACC_FINAL, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR, null, null).visitEnd();
            if (classOverride) {
                cw.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, mi.calleeClassFieldName, JSOBJECT_TYPE_DESCRIPTOR, null, null).visitEnd();
            }
        }
    }

    private void generateClassInit() {
        final InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(ACC_STATIC, CLASS_INIT, Type.getMethodDescriptor(Type.VOID_TYPE), null, null));

        if (classOverride) {
            mv.invokestatic(SERVICES_CLASS_TYPE_NAME, GET_CLASS_OVERRIDES_NAME, GET_CLASS_INITIALIZER_DESCRIPTOR, false);
            final Label initGlobal;
            if (samName != null) {
                // If the class is a SAM, allow having a ScriptFunction passed as class overrides
                final Label notAFunction = new Label();
                mv.dup();
                emitIsJSFunction(mv);
                mv.ifeq(notAFunction);
                mv.checkcast(JSOBJECT_TYPE); // mv.checkcast(JSFUNCTION_TYPE);

                /*
                 * Assign MethodHandle fields through invoking getHandle() for a ScriptFunction,
                 * only assigning the SAM method(s).
                 */
                for (final MethodInfo mi : methodInfos) {
                    if (mi.getName().equals(samName)) {
                        mv.dup();
                    } else {
                        mv.visitInsn(ACONST_NULL);
                    }
                    mv.putstatic(generatedClassName, mi.calleeClassFieldName, JSOBJECT_TYPE_DESCRIPTOR);
                }

                loadUndefined(mv);
                mv.putstatic(generatedClassName, STATIC_THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);

                initGlobal = new Label();
                mv.goTo(initGlobal);
                mv.visitLabel(notAFunction);
            } else {
                initGlobal = null;
            }

            mv.dup();
            mv.putstatic(generatedClassName, STATIC_THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);

            // Assign MethodHandle fields through invoking getHandle() for a ScriptObject
            for (final MethodInfo mi : methodInfos) {
                mv.dup();
                mv.aconst(mi.getName());
                mv.invokestatic(SERVICES_CLASS_TYPE_NAME, GET_CALLEE_NAME, GET_CALLEE_DESCRIPTOR, false);
                mv.putstatic(generatedClassName, mi.calleeClassFieldName, JSOBJECT_TYPE_DESCRIPTOR);
            }

            if (initGlobal != null) {
                mv.visitLabel(initGlobal);
            }
        }

        // Assign MethodHandle fields through invoking getHandle() for a ScriptObject
        for (final MethodInfo mi : methodInfos) {
            mv.aconst(Type.getMethodType(mi.type.toMethodDescriptorString()));
            mv.invokestatic(SERVICES_CLASS_TYPE_NAME, GET_HANDLE_NAME, GET_HANDLE_DESCRIPTOR, false);
            mv.putstatic(generatedClassName, mi.methodHandleFieldName, METHOD_HANDLE_TYPE_DESCRIPTOR);
        }

        if (emitSameThreadCheck) {
            mv.invokestatic(THREAD_CLASS_TYPE_NAME, CURRENT_THREAD_NAME, CURRENT_THREAD_DESCRIPTOR, false);
            mv.invokevirtual(THREAD_CLASS_TYPE_NAME, GET_ID_NAME, GET_ID_DESCRIPTOR, false);
            mv.putstatic(generatedClassName, THREAD_FIELD_NAME, LONG_TYPE_DESCRIPTOR);
        }

        endInitMethod(mv);
    }

    private static void loadUndefined(final InstructionAdapter mv) {
        mv.getstatic("com/oracle/truffle/js/runtime/objects/Undefined", "instance", JSOBJECT_TYPE_DESCRIPTOR);
    }

    /**
     * @see JavaAdapterServices#isJSFunction(Object)
     */
    private static void emitIsJSFunction(final InstructionAdapter mv) {
        // mv.invokestatic(JSFUNCTION_TYPE.getInternalName(), "isJSFunction",
        // Type.getMethodDescriptor(Type.getType(boolean.class), OBJECT_TYPE));
        mv.invokestatic(SERVICES_CLASS_TYPE_NAME, "isJSFunction", Type.getMethodDescriptor(Type.getType(boolean.class), OBJECT_TYPE), false);
    }

    private void generateConstructors() {
        boolean gotCtor = false;
        for (final Constructor<?> ctor : superClass.getDeclaredConstructors()) {
            final int modifier = ctor.getModifiers();
            if ((modifier & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0 && !isCallerSensitive(ctor)) {
                generateConstructors(ctor);
                gotCtor = true;
            }
        }
        if (!gotCtor) {
            throwNoAccessibleConstructorError();
        }
    }

    @TruffleBoundary
    private void throwNoAccessibleConstructorError() {
        // throw new AdaptationException(ERROR_NO_ACCESSIBLE_CONSTRUCTOR,
        // superClass.getCanonicalName());
        throw new RuntimeException("No accessible constructor: " + superClass.getCanonicalName());
    }

    private void generateConstructors(final Constructor<?> ctor) {
        if (classOverride) {
            /*
             * Generate a constructor that just delegates to ctor. This is used with class-level
             * overrides, when we want to create instances without further per-instance overrides.
             */
            generateDelegatingConstructor(ctor);
        }

        /*
         * Generate a constructor that delegates to ctor, but takes an additional ScriptObject
         * parameter at the beginning of its parameter list.
         */
        // generateOverridingConstructor(ctor, false);
        generateOverridingConstructor(ctor, samName != null);

        if (samName != null) {
            if (!autoConvertibleFromFunction && ctor.getParameterTypes().length == 0) {
                /*
                 * If the original type only has a single abstract method name, as well as a default
                 * ctor, then it can be automatically converted from JS function.
                 */
                autoConvertibleFromFunction = true;
            }
            /*
             * If all our abstract methods have a single name, generate an additional constructor,
             * one that takes a ScriptFunction as its first parameter and assigns it as the
             * implementation for all abstract methods.
             */
            // generateOverridingConstructor(ctor, true);
        }
    }

    private void generateDelegatingConstructor(final Constructor<?> ctor) {
        final Type originalCtorType = Type.getType(ctor);
        final Type[] argTypes = originalCtorType.getArgumentTypes();

        // All constructors must be public, even if in the superclass they were protected.
        final InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(ACC_PUBLIC, INIT, Type.getMethodDescriptor(originalCtorType.getReturnType(), argTypes), null, null));

        mv.visitCode();
        // Invoke super constructor with the same arguments.
        mv.visitVarInsn(ALOAD, 0);
        int offset = 1; // First arg is at position 1, after this.
        for (Type argType : argTypes) {
            mv.load(offset, argType);
            offset += argType.getSize();
        }
        mv.invokespecial(superClassName, INIT, originalCtorType.getDescriptor(), false);

        endInitMethod(mv);
    }

    /**
     * Generates a constructor for the adapter class. This constructor will take the same arguments
     * as the supertype constructor passed as the argument here, and delegate to it. However, it
     * will take an additional argument of either ScriptObject or ScriptFunction type (based on the
     * value of the "fromFunction" parameter), and initialize all the method handle fields of the
     * adapter instance with functions from the script object (or the script function itself, if
     * that's what's passed). There is one method handle field in the adapter class for every method
     * that can be implemented or overridden; the name of every field is same as the name of the
     * method, with a number suffix that makes it unique in case of overloaded methods. The
     * generated constructor will invoke {link #getHandle(ScriptFunction, MethodType, boolean)} or
     * {link #getHandle(Object, String, MethodType, boolean)} to obtain the method handles; these
     * methods make sure to add the necessary conversions and arity adjustments so that the
     * resulting method handles can be invoked from generated methods using {@code invokeExact}. The
     * constructor that takes a script function will only initialize the methods with the same name
     * as the single abstract method. The constructor will also store the Nashorn global that was
     * current at the constructor invocation time in a field named "global". The generated
     * constructor will be public, regardless of whether the supertype constructor was public or
     * protected. The generated constructor will not be variable arity, even if the supertype
     * constructor was.
     *
     * @param ctor the supertype constructor that is serving as the base for the generated
     *            constructor.
     * @param isSam true if we're generating a constructor that initializes SAM types from a single
     *            ScriptFunction passed to it, false if we're generating a constructor that
     *            initializes an arbitrary type from a ScriptObject passed to it.
     */
    private void generateOverridingConstructor(final Constructor<?> ctor, final boolean isSam) {
        final Type originalCtorType = Type.getType(ctor);
        final Type[] originalArgTypes = originalCtorType.getArgumentTypes();
        final int argLen = originalArgTypes.length;
        final Type[] newArgTypes = new Type[argLen + 1];

        // Insert ScriptFunction|Object as the last argument to the constructor
        final Type extraArgumentType = JSOBJECT_TYPE; // fromFunction ? JSOBJECT_TYPE : OBJECT_TYPE;
        newArgTypes[argLen] = extraArgumentType;
        System.arraycopy(originalArgTypes, 0, newArgTypes, 0, argLen);

        // All constructors must be public, even if in the superclass they were protected.
        // Existing super constructor <init>(this, args...) triggers generating <init>(this,
        // scriptObj, args...).
        final InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(ACC_PUBLIC, INIT, Type.getMethodDescriptor(originalCtorType.getReturnType(), newArgTypes), null, null));

        mv.visitCode();
        /*
         * First, invoke super constructor with original arguments. If the form of the constructor
         * we're generating is <init>(this, args..., scriptFn), then we're invoking
         * super.<init>(this, args...).
         */
        mv.visitVarInsn(ALOAD, 0);
        final Class<?>[] argTypes = ctor.getParameterTypes();
        int offset = 1; // First arg is at position 1, after this.
        for (int i = 0; i < argLen; ++i) {
            final Type argType = Type.getType(argTypes[i]);
            mv.load(offset, argType);
            offset += argType.getSize();
        }
        mv.invokespecial(superClassName, INIT, originalCtorType.getDescriptor(), false);

        if (isSam) {
            final Label notAFunction = new Label();
            final Label end = new Label();
            mv.visitVarInsn(ALOAD, offset);
            emitIsJSFunction(mv);
            mv.ifeq(notAFunction);
            // mv.checkcast(JSOBJECT_TYPE); // mv.checkcast(JSFUNCTION_TYPE);

            generateOverridingConstructorPart(true, mv, offset);

            mv.goTo(end);
            mv.visitLabel(notAFunction);

            generateOverridingConstructorPart(false, mv, offset);

            mv.visitLabel(end);
        } else {
            generateOverridingConstructorPart(false, mv, offset);
        }

        endInitMethod(mv);
    }

    private void generateOverridingConstructorPart(final boolean fromFunction, final InstructionAdapter mv, int offset) {
        // Assign MethodHandle fields through invoking getHandle()
        for (final MethodInfo mi : methodInfos) {
            mv.visitVarInsn(ALOAD, 0);
            if (fromFunction && !mi.getName().equals(samName)) {
                /*
                 * Constructors initializing from a ScriptFunction only initialize methods with the
                 * SAM name. NOTE: if there's a concrete overloaded method sharing the SAM name,
                 * it'll be overridden too. This is a deliberate design choice. All other method
                 * handles are initialized to null.
                 */
                mv.visitInsn(ACONST_NULL);
                mv.putfield(generatedClassName, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR);
            } else {
                mv.visitVarInsn(ALOAD, offset); // overrides or function object
                if (!fromFunction) {
                    mv.aconst(mi.getName());
                    // stack [name, overrides, this]
                    mv.invokestatic(SERVICES_CLASS_TYPE_NAME, GET_CALLEE_NAME, GET_CALLEE_DESCRIPTOR, false);
                    mv.putfield(generatedClassName, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR);
                } else {
                    // stack [function, this]
                    mv.putfield(generatedClassName, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR);
                }
            }
        }

        if (fromFunction) {
            mv.visitVarInsn(ALOAD, 0);
            loadUndefined(mv);
            mv.putfield(generatedClassName, THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, offset); // overrides object
            mv.putfield(generatedClassName, THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);
        }
    }

    private static void endInitMethod(final InstructionAdapter mv) {
        mv.visitInsn(RETURN);
        endMethod(mv);
    }

    private static void endMethod(final InstructionAdapter mv) {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Encapsulation of the information used to generate methods in the adapter classes. Basically,
     * a wrapper around the reflective Method object, a cached MethodType, and the name of the field
     * in the adapter class that will hold the method handle serving as the implementation of this
     * method in adapter instances.
     *
     */
    private static final class MethodInfo {
        private final Method method;
        private final MethodType type;
        private String methodHandleFieldName;

        private String calleeInstanceFieldName;
        private String calleeClassFieldName;

        private MethodInfo(final Class<?> clazz, final String name, final Class<?>... argTypes) throws NoSuchMethodException {
            this(clazz.getDeclaredMethod(name, argTypes));
        }

        private MethodInfo(final Method method) {
            this.method = method;
            this.type = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof MethodInfo && equals((MethodInfo) obj);
        }

        private boolean equals(final MethodInfo other) {
            // Only method name and type are used for comparison; method handle field name is not.
            return getName().equals(other.getName()) && type.equals(other.type);
        }

        String getName() {
            return method.getName();
        }

        @Override
        public int hashCode() {
            return getName().hashCode() ^ type.hashCode();
        }

        void setIsCanonical(final Set<String> usedFieldNames, boolean classOverride) {
            methodHandleFieldName = nextName(usedFieldNames);

            calleeInstanceFieldName = nextName(usedFieldNames);
            if (classOverride) {
                calleeClassFieldName = nextName(usedFieldNames);
            }
        }

        String nextName(final Set<String> usedFieldNames) {
            int i = 0;
            final String name = getName();
            String nextName = name;
            while (!usedFieldNames.add(nextName)) {
                final String ordinal = String.valueOf(i++);
                final int maxNameLen = 255 - ordinal.length();
                nextName = (name.length() <= maxNameLen ? name : name.substring(0, maxNameLen)).concat(ordinal);
            }
            return nextName;
        }

    }

    private void generateMethods() {
        for (final MethodInfo mi : methodInfos) {
            generateMethod(mi);
        }
    }

    /**
     * Generates a method in the adapter class that adapts a method from the original class. The
     * generated methods will inspect the method handle field assigned to them. If it is null (the
     * JS object doesn't provide an implementation for the method) then it will either invoke its
     * version in the supertype, or if it is abstract, throw an
     * {@link UnsupportedOperationException}. Otherwise, if the method handle field's value is not
     * null, the handle is invoked using invokeExact (signature polymorphic invocation as per JLS
     * 15.12.3). Before the invocation, the current Nashorn {link Context} is checked, and if it is
     * different than the global used to create the adapter instance, the creating global is set to
     * be the current global. In this case, the previously current global is restored after the
     * invocation. If invokeExact results in a Throwable that is not one of the method's declared
     * exceptions, and is not an unchecked throwable, then it is wrapped into a
     * {@link RuntimeException} and the runtime exception is thrown. The method handle retrieved
     * from the field is guaranteed to exactly match the signature of the method; this is guaranteed
     * by the way constructors of the adapter class obtain them using {link #getHandle(Object,
     * String, MethodType, boolean)}.
     *
     * @param mi the method info describing the method to be generated.
     */
    private void generateMethod(final MethodInfo mi) {
        final Method method = mi.method;
        final Class<?>[] exceptions = method.getExceptionTypes();
        final String[] exceptionNames = getExceptionNames(exceptions);
        final MethodType type = mi.type;
        final String methodDesc = type.toMethodDescriptorString();
        final String name = mi.getName();

        final Type asmType = Type.getMethodType(methodDesc);
        final Type[] asmArgTypes = asmType.getArgumentTypes();

        final InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(getAccessModifiers(method), name, methodDesc, null, exceptionNames));
        mv.visitCode();

        final Label instanceHandleDefined = new Label();
        final Label classHandleDefined = new Label();

        final Type asmReturnType = Type.getType(type.returnType());

        // See if we have instance handle defined
        mv.getstatic(generatedClassName, mi.methodHandleFieldName, METHOD_HANDLE_TYPE_DESCRIPTOR);
        // stack: [instanceHandle]
        mv.visitVarInsn(ALOAD, 0);
        mv.getfield(generatedClassName, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR);
        jumpIfNonNullKeepOperand(mv, instanceHandleDefined);

        if (classOverride) {
            // See if we have the static handle
            mv.getstatic(generatedClassName, mi.calleeClassFieldName, JSOBJECT_TYPE_DESCRIPTOR);
            // stack: [classHandle]
            jumpIfNonNullKeepOperand(mv, classHandleDefined);
        }

        // else: no override available, pop handle
        mv.pop();

        // No handle is available, fall back to default behavior
        if (Modifier.isAbstract(method.getModifiers())) {
            // If the super method is abstract, throw an exception
            mv.anew(UNSUPPORTED_OPERATION_TYPE);
            mv.dup();
            mv.invokespecial(UNSUPPORTED_OPERATION_TYPE_NAME, INIT, VOID_NOARG_METHOD_DESCRIPTOR, false);
            mv.athrow();
        } else {
            // If the super method is not abstract, delegate to it.
            emitSuperCall(mv, method.getDeclaringClass(), name, methodDesc);
        }

        final Label setupArguments = new Label();

        if (classOverride) {
            mv.visitLabel(classHandleDefined);
            // mv.getstatic(generatedClassName, mi.calleeClassFieldName, JSOBJECT_TYPE_DESCRIPTOR);
            mv.getstatic(generatedClassName, STATIC_THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);
            // stack: [this binding, callee, classHandle]
            mv.goTo(setupArguments);
        }

        mv.visitLabel(instanceHandleDefined);
        // mv.visitVarInsn(ALOAD, 0);
        // mv.getfield(generatedClassName, mi.calleeInstanceFieldName, JSOBJECT_TYPE_DESCRIPTOR);
        mv.visitVarInsn(ALOAD, 0);
        mv.getfield(generatedClassName, THISBINDING_FIELD_NAME, JSOBJECT_TYPE_DESCRIPTOR);
        // stack: [this binding, callee, instanceHandle]

        // fallthrough to setupArguments

        mv.visitLabel(setupArguments);
        // stack: [this binding, callee, someHandle]

        if (emitSameThreadCheck) {
            mv.getstatic(generatedClassName, THREAD_FIELD_NAME, LONG_TYPE_DESCRIPTOR);
            mv.invokestatic(SERVICES_CLASS_TYPE_NAME, SAME_THREAD_CHECK_NAME, SAME_THREAD_CHECK_DESCRIPTOR, false);
        }

        // Load all parameters back on stack for dynamic invocation.
        int varOffset = 1;
        for (final Type t : asmArgTypes) {
            mv.load(varOffset, t);
            varOffset += t.getSize();
        }

        // Invoke the target method handle
        final Label tryBlockStart = new Label();
        mv.visitLabel(tryBlockStart);
        mv.invokevirtual(METHOD_HANDLE_TYPE.getInternalName(), "invokeExact", type.insertParameterTypes(0, DynamicObject.class, Object.class).toMethodDescriptorString(), false);
        final Label tryBlockEnd = new Label();
        mv.visitLabel(tryBlockEnd);
        mv.areturn(asmReturnType);

        // If Throwable is not declared, we need an adapter from Throwable to RuntimeException
        final boolean throwableDeclared = isThrowableDeclared(exceptions);
        final Label throwableHandler;
        if (!throwableDeclared) {
            // Add "throw new RuntimeException(Throwable)" handler for Throwable
            throwableHandler = new Label();
            mv.visitLabel(throwableHandler);
            mv.anew(RUNTIME_EXCEPTION_TYPE);
            mv.dupX1();
            mv.swap();
            mv.invokespecial(RUNTIME_EXCEPTION_TYPE_NAME, INIT, Type.getMethodDescriptor(Type.VOID_TYPE, THROWABLE_TYPE), false);
            // Fall through to rethrow handler
        } else {
            throwableHandler = null;
        }
        final Label rethrowHandler = new Label();
        mv.visitLabel(rethrowHandler);
        // Rethrow handler for RuntimeException, Error, and all declared exception types
        mv.athrow();
        final Label methodEnd = new Label();
        mv.visitLabel(methodEnd);

        if (throwableDeclared) {
            mv.visitTryCatchBlock(tryBlockStart, tryBlockEnd, rethrowHandler, THROWABLE_TYPE_NAME);
            assert throwableHandler == null;
        } else {
            mv.visitTryCatchBlock(tryBlockStart, tryBlockEnd, rethrowHandler, RUNTIME_EXCEPTION_TYPE_NAME);
            mv.visitTryCatchBlock(tryBlockStart, tryBlockEnd, rethrowHandler, ERROR_TYPE_NAME);
            for (final String excName : exceptionNames) {
                mv.visitTryCatchBlock(tryBlockStart, tryBlockEnd, rethrowHandler, excName);
            }
            mv.visitTryCatchBlock(tryBlockStart, tryBlockEnd, throwableHandler, THROWABLE_TYPE_NAME);
        }

        endMethod(mv);
    }

    /**
     * Emits code for jumping to a label if the top stack operand is not null. The operand is kept
     * on the stack if it is not null (so is available to code at the jump address) and is popped if
     * it is null.
     *
     * @param mv the instruction adapter being used to emit code
     * @param label the label to jump to
     */
    private static void jumpIfNonNullKeepOperand(final InstructionAdapter mv, final Label label) {
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, label);
        mv.visitInsn(POP);
    }

    private static boolean isThrowableDeclared(final Class<?>[] exceptions) {
        for (final Class<?> exception : exceptions) {
            if (exception == Throwable.class) {
                return true;
            }
        }
        return false;
    }

    private void generateSuperMethods() {
        for (final MethodInfo mi : methodInfos) {
            if (!Modifier.isAbstract(mi.method.getModifiers())) {
                generateSuperMethod(mi);
            }
        }
    }

    private void generateSuperMethod(MethodInfo mi) {
        final Method method = mi.method;

        final String methodDesc = mi.type.toMethodDescriptorString();
        final String name = mi.getName();

        final InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(getAccessModifiers(method), SUPER_PREFIX + name, methodDesc, null, getExceptionNames(method.getExceptionTypes())));
        mv.visitCode();

        emitSuperCall(mv, method.getDeclaringClass(), name, methodDesc);

        endMethod(mv);
    }

    // find the appropriate super type to use for invokespecial on the given interface
    private Class<?> findInvokespecialOwnerFor(final Class<?> owner) {
        assert Modifier.isInterface(owner.getModifiers()) : owner + " is not an interface";

        if (owner.isAssignableFrom(superClass)) {
            return superClass;
        }

        for (final Class<?> iface : interfaces) {
            if (owner.isAssignableFrom(iface)) {
                return iface;
            }
        }

        throw new AssertionError("Cannot find the class/interface that extends " + owner);
    }

    private void emitSuperCall(final InstructionAdapter mv, final Class<?> owner, final String name, final String methodDesc) {
        mv.visitVarInsn(ALOAD, 0);
        int nextParam = 1;
        final Type methodType = Type.getMethodType(methodDesc);
        for (final Type t : methodType.getArgumentTypes()) {
            mv.load(nextParam, t);
            nextParam += t.getSize();
        }

        // default method - non-abstract interface method
        if (Modifier.isInterface(owner.getModifiers())) {
            // we should call default method on the immediate "super" type - not on (possibly)
            // the indirectly inherited interface class!
            final Class<?> superType = findInvokespecialOwnerFor(owner);
            mv.invokespecial(Type.getInternalName(superType), name, methodDesc, Modifier.isInterface(superType.getModifiers()));
        } else {
            mv.invokespecial(superClassName, name, methodDesc, false);
        }
        mv.areturn(methodType.getReturnType());
    }

    private static String[] getExceptionNames(final Class<?>[] exceptions) {
        final String[] exceptionNames = new String[exceptions.length];
        for (int i = 0; i < exceptions.length; ++i) {
            exceptionNames[i] = Type.getInternalName(exceptions[i]);
        }
        return exceptionNames;
    }

    private static int getAccessModifiers(final Method method) {
        return ACC_PUBLIC | (method.isVarArgs() ? ACC_VARARGS : 0);
    }

    /**
     * Gathers methods that can be implemented or overridden from the specified type into this
     * factory's {@link #methodInfos} set. It will add all non-final, non-static methods that are
     * either public or protected from the type if the type itself is public. If the type is a
     * class, the method will recursively invoke itself for its superclass and the interfaces it
     * implements, and add further methods that were not directly declared on the class.
     *
     * @param type the type defining the methods.
     */
    private void gatherMethods(final Class<?> type) {
        if (Modifier.isPublic(type.getModifiers())) {
            final Method[] typeMethods = type.isInterface() ? type.getMethods() : type.getDeclaredMethods();

            for (final Method typeMethod : typeMethods) {
                final String name = typeMethod.getName();
                if (name.startsWith(SUPER_PREFIX)) {
                    continue;
                }
                final int m = typeMethod.getModifiers();
                if (Modifier.isStatic(m)) {
                    continue;
                }
                if (Modifier.isPublic(m) || Modifier.isProtected(m)) {
                    final MethodInfo mi = new MethodInfo(typeMethod);
                    if (Modifier.isFinal(m) || isCallerSensitive(typeMethod)) {
                        finalMethods.add(mi);
                    } else if (!finalMethods.contains(mi) && methodInfos.add(mi)) {
                        if (Modifier.isAbstract(m)) {
                            abstractMethodNames.add(mi.getName());
                        }
                        mi.setIsCanonical(usedFieldNames, classOverride);
                    }
                }
            }
        }
        /*
         * If the type is a class, visit its superclasses and declared interfaces. If it's an
         * interface, we're done. Needing to invoke the method recursively for a non-interface Class
         * object is the consequence of needing to see all declared protected methods, and
         * Class.getDeclaredMethods() doesn't provide those declared in a superclass. For
         * interfaces, we used Class.getMethods(), as we're only interested in public ones there,
         * and getMethods() does provide those declared in a superinterface.
         */
        if (!type.isInterface()) {
            final Class<?> superType = type.getSuperclass();
            if (superType != null) {
                gatherMethods(superType);
            }
            for (final Class<?> itf : type.getInterfaces()) {
                gatherMethods(itf);
            }
        }
    }

    private void gatherMethods(final List<Class<?>> classes) {
        for (final Class<?> c : classes) {
            gatherMethods(c);
        }
    }

    /**
     * Creates a collection of methods that are not final, but we still never allow them to be
     * overridden in adapters, as explicitly declaring them automatically is a bad idea. Currently,
     * this means {@code Object.finalize()} and {@code Object.clone()}.
     *
     * @return a collection of method infos representing those methods that we never override in
     *         adapter classes.
     */
    private static Collection<MethodInfo> getExcludedMethods() {
        // return AccessController.doPrivileged(new PrivilegedAction<Collection<MethodInfo>>() {
        // public Collection<MethodInfo> run() {
        try {
            return Arrays.asList(new MethodInfo(Object.class, "finalize"), new MethodInfo(Object.class, "clone"));
        } catch (final NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        // }
        // }, ClassAndLoader.createPermAccCtxt("accessDeclaredMembers"));
    }

    private String getCommonSuperClass(final String type1, final String type2) {
        try {
            final Class<?> c1 = Class.forName(type1.replace('/', '.'), false, commonLoader);
            final Class<?> c2 = Class.forName(type2.replace('/', '.'), false, commonLoader);
            if (c1.isAssignableFrom(c2)) {
                return type1;
            }
            if (c2.isAssignableFrom(c1)) {
                return type2;
            }
            if (c1.isInterface() || c2.isInterface()) {
                return OBJECT_TYPE_NAME;
            }
            return assignableSuperClass(c1, c2).getName().replace('.', '/');
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> assignableSuperClass(final Class<?> c1, final Class<?> c2) {
        final Class<?> superClass = c1.getSuperclass();
        return superClass.isAssignableFrom(c2) ? superClass : assignableSuperClass(superClass, c2);
    }

    private static boolean isCallerSensitive(final Executable e) {
        return CALLER_SENSITIVE_ANNOTATION_CLASS != null && e.isAnnotationPresent(CALLER_SENSITIVE_ANNOTATION_CLASS);
    }

    private static final Class<? extends Annotation> CALLER_SENSITIVE_ANNOTATION_CLASS = findCallerSensitiveAnnotationClass();

    private static Class<? extends Annotation> findCallerSensitiveAnnotationClass() {
        try {
            // JDK 8
            return Class.forName("sun.reflect.CallerSensitive").asSubclass(Annotation.class);
        } catch (ClassNotFoundException e1) {
        }
        try {
            // JDK 9
            return Class.forName("jdk.internal.reflect.CallerSensitive").asSubclass(Annotation.class);
        } catch (ClassNotFoundException e2) {
        }
        return null; // not found
    }
}
