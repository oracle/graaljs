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
/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.regex.nashorn.regexp.joni;

// @formatter:off

public class Option {

    /* options */
    public static final int NONE                 = 0;
    public static final int IGNORECASE           = (1<<0);
    public static final int EXTEND               = (1<<1);
    public static final int MULTILINE            = (1<<2);
    public static final int SINGLELINE           = (1<<3);
    public static final int FIND_LONGEST         = (1<<4);
    public static final int FIND_NOT_EMPTY       = (1<<5);
    public static final int NEGATE_SINGLELINE    = (1<<6);
    public static final int DONT_CAPTURE_GROUP   = (1<<7);
    public static final int CAPTURE_GROUP        = (1<<8);
    public static final int UNICODE              = (1<<13);

    /* options (search time) */
    public static final int NOTBOL               = (1<<9);
    public static final int NOTEOL               = (1<<10);
    public static final int POSIX_REGION         = (1<<11);
    public static final int MAXBIT               = (1<<12); /* limit */

    public static final int DEFAULT              = NONE;

    public static String toString(final int option) {
        String options = "";
        if (isIgnoreCase(option)) {
            options += "IGNORECASE ";
        }
        if (isExtend(option)) {
            options += "EXTEND ";
        }
        if (isMultiline(option)) {
            options += "MULTILINE ";
        }
        if (isSingleline(option)) {
            options += "SINGLELINE ";
        }
        if (isFindLongest(option)) {
            options += "FIND_LONGEST ";
        }
        if (isFindNotEmpty(option)) {
            options += "FIND_NOT_EMPTY  ";
        }
        if (isNegateSingleline(option)) {
            options += "NEGATE_SINGLELINE ";
        }
        if (isDontCaptureGroup(option)) {
            options += "DONT_CAPTURE_GROUP ";
        }
        if (isCaptureGroup(option)) {
            options += "CAPTURE_GROUP ";
        }

        if (isNotBol(option)) {
            options += "NOTBOL ";
        }
        if (isNotEol(option)) {
            options += "NOTEOL ";
        }
        if (isPosixRegion(option)) {
            options += "POSIX_REGION ";
        }

        return options;
    }

    public static boolean isIgnoreCase(final int option) {
        return (option & IGNORECASE) != 0;
    }

    public static boolean isExtend(final int option) {
        return (option & EXTEND) != 0;
    }

    public static boolean isSingleline(final int option) {
        return (option & SINGLELINE) != 0;
    }

    public static boolean isMultiline(final int option) {
        return (option & MULTILINE) != 0;
    }

    public static boolean isUnicode(final int option) {
        return (option & UNICODE) != 0;
    }

    public static boolean isFindLongest(final int option) {
        return (option & FIND_LONGEST) != 0;
    }

    public static boolean isFindNotEmpty(final int option) {
        return (option & FIND_NOT_EMPTY) != 0;
    }

    public static boolean isFindCondition(final int option) {
        return (option & (FIND_LONGEST | FIND_NOT_EMPTY)) != 0;
    }

    public static boolean isNegateSingleline(final int option) {
        return (option & NEGATE_SINGLELINE) != 0;
    }

    public static boolean isDontCaptureGroup(final int option) {
        return (option & DONT_CAPTURE_GROUP) != 0;
    }

    public static boolean isCaptureGroup(final int option) {
        return (option & CAPTURE_GROUP) != 0;
    }

    public static boolean isNotBol(final int option) {
        return (option & NOTBOL) != 0;
    }

    public static boolean isNotEol(final int option) {
        return (option & NOTEOL) != 0;
    }

    public static boolean isPosixRegion(final int option) {
        return (option & POSIX_REGION) != 0;
    }

    /* OP_SET_OPTION is required for these options.  ??? */
    //    public static boolean isDynamic(int option) {
    //        return (option & (MULTILINE | IGNORECASE)) != 0;
    //    }
    @SuppressWarnings("unused")
    public static boolean isDynamic(final int option) {
        return false;
    }
}
