/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.runtime.interop;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 * This class encapsulates the bytecode of the adapter class and can be used to load it into the JVM
 * as an actual Class. It can be invoked repeatedly to create multiple adapter classes from the same
 * bytecode; adapter classes that have class-level overrides must be re-created for every set of
 * such overrides. Note that while this class is named "class loader", it does not, in fact, extend
 * {@code ClassLoader}, but rather uses them internally. Instances of this class are normally
 * created by {@link JavaAdapterBytecodeGenerator}.
 */
public final class JavaAdapterClassLoader {
    private static final ProtectionDomain GENERATED_PROTECTION_DOMAIN = createGeneratedProtectionDomain();

    private final String className;
    private final byte[] classBytes;

    JavaAdapterClassLoader(String className, byte[] classBytes) {
        this.className = className.replace('/', '.');
        this.classBytes = classBytes;
    }

    /**
     * Loads the generated adapter class into the JVM.
     *
     * @param parentLoader the parent class loader for the generated class loader
     * @return the generated adapter class
     */
    public Class<?> generateClass(final ClassLoader parentLoader) {
        // return AccessController.doPrivileged(new PrivilegedAction<StaticClass>() {
        try {
            return Class.forName(className, true, createClassLoader(parentLoader));
        } catch (final ClassNotFoundException e) {
            throw new AssertionError(e); // cannot happen
        }
    }

    /*
     * Note that the adapter class is created in the protection domain of the class/interface being
     * extended/implemented, and only the privileged global setter action class is generated in the
     * protection domain of Nashorn itself. Also note that the creation and loading of the global
     * setter is deferred until it is required by JVM linker, which will only happen on first
     * invocation of any of the adapted method. We could defer it even more by separating its
     * invocation into a separate static method on the adapter class, but then someone with ability
     * to introspect on the class and use setAccessible(true) on it could invoke the method. It's a
     * security tradeoff...
     */
    private ClassLoader createClassLoader(final ClassLoader parentLoader) {
        return new SecureClassLoader(parentLoader) {
            private static final boolean PRINT_CODE = false;
            private final ClassLoader myLoader = getClass().getClassLoader();

            @Override
            public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                try {
                    return super.loadClass(name, resolve);
                } catch (final SecurityException se) {
                    /*
                     * we may be implementing an interface or extending a class that was loaded by a
                     * loader that prevents package.access. If so, it'd throw SecurityException for
                     * internal classes used by generated adapter classes.
                     */
                    if (isAccessibleInternalClassName(name)) {
                        return myLoader != null ? myLoader.loadClass(name) : Class.forName(name, false, myLoader);
                    }
                    throw se;
                }
            }

            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    if (PRINT_CODE) {
                        printCode(classBytes);
                    }
                    return defineClass(name, classBytes, 0, classBytes.length, GENERATED_PROTECTION_DOMAIN);
                }
                throw new ClassNotFoundException(name);
            }

            private void printCode(byte[] bytecode) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (final PrintWriter pw = new PrintWriter(baos)) {
                    new ClassReader(bytecode).accept(new TraceClassVisitor(pw), 0);
                }
                System.out.println(new String(baos.toByteArray()));
            }
        };
    }

    static boolean isAccessibleInternalClassName(final String name) {
        return name.startsWith("com.oracle.truffle.js.");
    }

    private static ProtectionDomain createGeneratedProtectionDomain() {
        /*
         * Generated classes need to have AllPermission. Since we require the "createClassLoader"
         * RuntimePermission, we can create a class loader that'll load new classes with any
         * permissions. Our generated classes are just delegating adapters, so having AllPermission
         * can't cause anything wrong; the effective set of permissions for the executing script
         * functions will still be limited by the permissions of the caller and the permissions of
         * the script.
         */
        final Permissions permissions = new Permissions();
        permissions.add(new AllPermission());
        return new ProtectionDomain(new CodeSource(null, (CodeSigner[]) null), permissions);
    }
}
