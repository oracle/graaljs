/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.js.parser;

/**
 * Manages global runtime options.
 */
final class Options {
    private static final String OPTION_NAME_PREFIX = "truffle.js.";

    /**
     * Convenience function for getting system properties in a safe way.
     *
     * @param name of boolean property
     * @param defValue default value of boolean property
     * @return true if set to true, default value if unset or set to false
     */
    public static boolean getBooleanProperty(final String name, final Boolean defValue) {
        try {
            final String property = System.getProperty(OPTION_NAME_PREFIX + name);
            if (property == null && defValue != null) {
                return defValue;
            }
            return property != null && !"false".equalsIgnoreCase(property);
        } catch (final SecurityException e) {
            // if no permission to read, assume false
            return false;
        }
    }

    /**
     * Convenience function for getting system properties in a safe way.
     *
     * @param name of boolean property
     * @return true if set to true, false if unset or set to false
     */
    public static boolean getBooleanProperty(final String name) {
        return getBooleanProperty(name, null);
    }
}
