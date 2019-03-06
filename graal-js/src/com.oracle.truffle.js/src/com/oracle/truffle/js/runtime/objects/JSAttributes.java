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
package com.oracle.truffle.js.runtime.objects;

public final class JSAttributes {
    /**
     * ES5 8.6.1 Property Attributes.
     */
    public static final String VALUE = "value";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String WRITABLE = "writable";
    public static final String ENUMERABLE = "enumerable";
    public static final String CONFIGURABLE = "configurable";

    /** ES5 8.6.1 - Is this property not enumerable? */
    public static final int NOT_ENUMERABLE = 1 << 0;

    /** ES5 8.6.1 - Is this property not configurable? */
    public static final int NOT_CONFIGURABLE = 1 << 1;

    /** ES5 8.6.1 - Is this property not writable? */
    public static final int NOT_WRITABLE = 1 << 2;

    public static final int ATTRIBUTES_MASK = NOT_ENUMERABLE | NOT_CONFIGURABLE | NOT_WRITABLE;

    private JSAttributes() {
    }

    public static int getDefault() {
        return configurableEnumerableWritable();
    }

    public static int getDefaultNotEnumerable() {
        return configurableNotEnumerableWritable();
    }

    public static int configurableEnumerableWritable() {
        return CONFIGURABLE_ENUMERABLE_WRITABLE;
    }

    public static int configurableNotEnumerableWritable() {
        return CONFIGURABLE_NOT_ENUMERABLE_WRITABLE;
    }

    public static int configurableEnumerableNotWritable() {
        return CONFIGURABLE_ENUMERABLE_NOT_WRITABLE;
    }

    public static int configurableNotEnumerableNotWritable() {
        return CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE;
    }

    public static int notConfigurableNotEnumerableNotWritable() {
        return NOT_CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE;
    }

    public static int notConfigurableNotEnumerableWritable() {
        return NOT_CONFIGURABLE_NOT_ENUMERABLE_WRITABLE;
    }

    public static int notConfigurableEnumerableWritable() {
        return NOT_CONFIGURABLE_ENUMERABLE_WRITABLE;
    }

    public static int notConfigurableEnumerableNotWritable() {
        return NOT_CONFIGURABLE_ENUMERABLE_NOT_WRITABLE;
    }

    public static int getAccessorDefault() {
        return configurableEnumerableWritable();
    }

    public static int notConfigurableNotEnumerable() {
        return notConfigurableNotEnumerableWritable();
    }

    public static int configurableNotEnumerable() {
        return configurableNotEnumerableWritable();
    }

    public static int fromConfigurableEnumerableWritable(boolean configurable, boolean enumerable, boolean writable) {
        return (!configurable ? NOT_CONFIGURABLE : 0) | (!enumerable ? NOT_ENUMERABLE : 0) | (!writable ? NOT_WRITABLE : 0);
    }

    public static int fromConfigurableEnumerable(boolean configurable, boolean enumerable) {
        return (!configurable ? NOT_CONFIGURABLE : 0) | (!enumerable ? NOT_ENUMERABLE : 0);
    }

    public static boolean isConfigurable(int flags) {
        return (flags & NOT_CONFIGURABLE) == 0;
    }

    public static boolean isEnumerable(int flags) {
        return (flags & NOT_ENUMERABLE) == 0;
    }

    public static boolean isWritable(int flags) {
        return (flags & NOT_WRITABLE) == 0;
    }

    private static final int NOT_CONFIGURABLE_ENUMERABLE_WRITABLE = NOT_CONFIGURABLE;
    private static final int NOT_CONFIGURABLE_ENUMERABLE_NOT_WRITABLE = NOT_CONFIGURABLE | NOT_WRITABLE;
    private static final int NOT_CONFIGURABLE_NOT_ENUMERABLE_WRITABLE = NOT_CONFIGURABLE | NOT_ENUMERABLE;
    private static final int NOT_CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE = NOT_CONFIGURABLE | NOT_ENUMERABLE | NOT_WRITABLE;
    private static final int CONFIGURABLE_NOT_ENUMERABLE_WRITABLE = NOT_ENUMERABLE;
    private static final int CONFIGURABLE_NOT_ENUMERABLE_NOT_WRITABLE = NOT_ENUMERABLE | NOT_WRITABLE;
    private static final int CONFIGURABLE_ENUMERABLE_NOT_WRITABLE = NOT_WRITABLE;
    private static final int CONFIGURABLE_ENUMERABLE_WRITABLE = 0;
}
