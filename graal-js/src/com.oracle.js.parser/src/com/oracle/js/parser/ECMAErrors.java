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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// @formatter:off
/**
 * Helper class to throw various standard "ECMA error" exceptions such as Error, ReferenceError, TypeError etc.
 */
public final class ECMAErrors {
    private static final String MESSAGES_RESOURCE = "com.oracle.js.parser.resources.Messages";

    private static final ResourceBundle MESSAGES_BUNDLE;
    static {
        MESSAGES_BUNDLE = ResourceBundle.getBundle(MESSAGES_RESOURCE, Locale.getDefault());
    }

    private ECMAErrors() {
    }

    /**
     * Get the exception message by placing the args in the resource defined
     * by the resource tag. This is visible to, e.g. the {@link com.oracle.js.parser.Parser}
     * can use it to generate compile time messages with the correct locale
     *
     * @param msgId the resource tag (message id)
     * @param args  arguments to error string
     *
     * @return the filled out error string
     */
    public static String getMessage(final String msgId, final String... args) {
        try {
            return new MessageFormat(MESSAGES_BUNDLE.getString(msgId)).format(args);
        } catch (final MissingResourceException e) {
            throw new RuntimeException("no message resource found for message id: " + msgId);
        }
    }
}
