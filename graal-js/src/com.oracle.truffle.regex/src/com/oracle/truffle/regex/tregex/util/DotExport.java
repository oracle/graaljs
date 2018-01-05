/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DotExport {

    public static void printConnection(BufferedWriter writer, String from, String to, String label) throws IOException {
        writer.write(String.format("    \"%s\" -> \"%s\" [ label = \"%s\" ];", escape(from), escape(to), escape(label)));
        writer.newLine();
    }

    private static final Pattern specialChars = Pattern.compile("[\"\\\\]");

    public static String escape(String str) {
        StringBuffer escapedString = new StringBuffer();
        Matcher m = specialChars.matcher(str);
        while (m.find()) {
            String replacement;
            switch (str.charAt(m.start())) {
                case '"':
                    replacement = "\\\\\"";
                    break;
                case '\\':
                    replacement = "\\\\\\\\";
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
