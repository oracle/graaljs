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

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Objects;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;

/**
 * Java interop access check utility methods, mostly taken from Nashorn.
 */
public final class JavaAccess {
    private JavaAccess() {
    }

    private static final AccessControlContext NO_PERMISSIONS_CONTEXT = createNoPermissionsContext();
    /**
     * Permission to use Java reflection/jsr292 from script code.
     */
    private static final String PERMISSION_JAVA_REFLECTION = "truffle.js.JavaReflection";

    private static AccessControlContext createNoPermissionsContext() {
        return new AccessControlContext(new ProtectionDomain[]{new ProtectionDomain(null, new Permissions())});
    }

    private static void checkPackageAccessInner(final SecurityManager sm, final String pkgName) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                sm.checkPackageAccess(pkgName);
                return null;
            }
        }, NO_PERMISSIONS_CONTEXT);
    }

    /**
     * Checks that the given package can be accessed from no permissions context.
     *
     * @param sm current security manager instance
     * @param fullName fully qualified package name
     * @throw SecurityException if not accessible
     */
    public static void checkPackageAccess(final SecurityManager sm, final String fullName) {
        Objects.requireNonNull(sm);
        final int index = fullName.lastIndexOf('.');
        if (index != -1) {
            final String pkgName = fullName.substring(0, index);
            checkPackageAccessInner(sm, pkgName);
        }
    }

    /**
     * Returns true if the class is either not public, or it resides in a package with restricted
     * access.
     *
     * @param clazz the class to test
     * @return true if the class is either not public, or it resides in a package with restricted
     *         access.
     */
    public static boolean isRestrictedClass(final Class<?> clazz) {
        if (!Modifier.isPublic(clazz.getModifiers())) {
            // Non-public classes are always restricted
            return true;
        }
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            // No further restrictions if we don't have a security manager
            return false;
        }
        final String name = clazz.getName();
        final int i = name.lastIndexOf('.');
        if (i == -1) {
            // Classes in default package are never restricted
            return false;
        }
        final String pkgName = name.substring(0, i);
        // Do a package access check from within an access control context with no permissions
        try {
            checkPackageAccessInner(sm, pkgName);
        } catch (final SecurityException e) {
            return true;
        }
        return false;
    }

    public static boolean isReflectionClass(final Class<?> type) {
        // Class or ClassLoader subclasses
        if (type == Class.class || ClassLoader.class.isAssignableFrom(type)) {
            return true;
        }

        // package name check
        final String name = type.getName();
        return name.startsWith("java.lang.reflect.") || name.startsWith("java.lang.invoke.") || name.startsWith("java.beans.");
    }

    public static void checkReflectionAccess(final Class<?> clazz, final boolean isStatic, final boolean allowReflection) {
        if (!allowReflection && isReflectiveCheckNeeded(clazz, isStatic)) {
            throw Errors.createTypeError("Java reflection not allowed");
        }

        final SecurityManager sm = System.getSecurityManager();
        if (sm != null && isReflectiveCheckNeeded(clazz, isStatic)) {
            checkReflectionPermission(sm);
        }
    }

    public static boolean isReflectiveCheckNeeded(final Class<?> type, final boolean isStatic) {
        // special handling for Proxy subclasses
        if (Proxy.class.isAssignableFrom(type)) {
            if (Proxy.isProxyClass(type)) {
                // real Proxy class - filter only static access
                return isStatic;
            }

            // fake Proxy subclass - filter it always!
            return true;
        }

        // check for any other reflective Class
        return isReflectionClass(type);
    }

    private static void checkReflectionPermission(final SecurityManager sm) {
        sm.checkPermission(new RuntimePermission(PERMISSION_JAVA_REFLECTION));
    }

    /**
     * Checks that the given Class can be accessed from no permissions context.
     *
     * @param clazz Class object
     * @throws SecurityException if not accessible
     */
    public static void checkPackageAccess(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Class<?> bottomClazz = clazz;
            while (bottomClazz.isArray()) {
                bottomClazz = bottomClazz.getComponentType();
            }
            checkPackageAccess(sm, bottomClazz.getName());
        }
    }

    /**
     * Checks that the given Class can be accessed from no permissions context.
     *
     * @param clazz Class object
     * @return true if package is accessible, false otherwise
     */
    private static boolean isAccessiblePackage(final Class<?> clazz) {
        try {
            checkPackageAccess(clazz);
            return true;
        } catch (final SecurityException se) {
            return false;
        }
    }

    /**
     * Checks that the given Class is public and it can be accessed from no permissions context.
     *
     * @param clazz Class object to check
     * @return true if Class is accessible, false otherwise
     */
    public static boolean isAccessibleClass(final Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers()) && isAccessiblePackage(clazz);
    }

    public static boolean isReflectionAllowed(JSContext context) {
        TruffleLanguage.Env env = context.getRealm().getEnv();
        if (env != null && env.isHostLookupAllowed()) {
            try {
                Object found = env.lookupHostSymbol(Class.class.getName());
                if (found != null) {
                    return false;
                }
            } catch (Exception ex) {
            }
        }
        return true;
    }

    @TruffleBoundary
    public static void checkAccess(Class<?>[] types, JSContext context) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            boolean allowReflection = JavaAccess.isReflectionAllowed(context);
            for (final Class<?> type : types) {
                // check for restricted package access
                JavaAccess.checkPackageAccess(type);
                // check for classes, interfaces in reflection
                JavaAccess.checkReflectionAccess(type, true, allowReflection);
            }
        }
    }
}
