/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex;

public class RegexSyntaxException extends RuntimeException {

    private static final String template = "Invalid regular expression: /%s/%s: %s";
    private static final String templateNoFlags = "Invalid regular expression: %s: %s";

    public RegexSyntaxException(String msg) {
        super(msg);
    }

    public RegexSyntaxException(String pattern, String msg) {
        super(String.format(templateNoFlags, pattern, msg));
    }

    public RegexSyntaxException(String pattern, RegexFlags flags, String msg) {
        super(String.format(template, pattern, flags, msg));
    }

    public RegexSyntaxException(String pattern, RegexFlags flags, String msg, Throwable ex) {
        super(String.format(template, pattern, flags, msg), ex);
    }

    private static final long serialVersionUID = 1L;

}
