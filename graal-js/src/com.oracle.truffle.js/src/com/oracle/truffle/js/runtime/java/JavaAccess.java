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
package com.oracle.truffle.js.runtime.java;

import java.lang.reflect.Proxy;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;

/**
 * Java interop access check utility methods.
 */
public final class JavaAccess {
    private JavaAccess() {
    }

    /**
     * Permission to use Java reflection/jsr292 from script code.
     */
    private static final String PERMISSION_JAVA_REFLECTION = "truffle.js.JavaReflection";

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
            throw new SecurityException("Java reflection not allowed");
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

    @SuppressWarnings("deprecation")
    private static void checkReflectionPermission(final SecurityManager sm) {
        sm.checkPermission(new RuntimePermission(PERMISSION_JAVA_REFLECTION));
    }

    public static boolean isReflectionAllowed(TruffleLanguage.Env env) {
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
    public static void checkAccess(Class<?>[] types, TruffleLanguage.Env env) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            boolean allowReflection = JavaAccess.isReflectionAllowed(env);
            for (final Class<?> type : types) {
                // check for classes, interfaces in reflection
                JavaAccess.checkReflectionAccess(type, true, allowReflection);
            }
        }
    }
}
