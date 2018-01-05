/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaTexExport {

    private static final Pattern specialChars = Pattern.compile("[&%$#_{}~^\\\\]");

    public static String escape(String str) {
        StringBuffer escapedString = new StringBuffer();
        Matcher m = specialChars.matcher(str);
        while (m.find()) {
            String replacement;
            switch (str.charAt(m.start())) {
                case '&':
                    replacement = "\\\\&";
                    break;
                case '%':
                    replacement = "\\\\%";
                    break;
                case '$':
                    replacement = "\\\\\\$";
                    break;
                case '#':
                    replacement = "\\\\#";
                    break;
                case '_':
                    replacement = "\\\\_";
                    break;
                case '{':
                    replacement = "\\\\{";
                    break;
                case '}':
                    replacement = "\\\\}";
                    break;
                case '~':
                    replacement = "\\\\textasciitilde ";
                    break;
                case '^':
                    replacement = "\\\\textasciicircum ";
                    break;
                case '\\':
                    replacement = "\\\\textbackslash ";
                    break;
                default:
                    throw new IllegalStateException();
            }
            m.appendReplacement(escapedString, replacement);
        }
        m.appendTail(escapedString);
        return escapedString.toString();
    }
}
