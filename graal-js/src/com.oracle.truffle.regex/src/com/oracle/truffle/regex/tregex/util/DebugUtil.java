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
package com.oracle.truffle.regex.tregex.util;

import com.oracle.truffle.api.CompilerDirectives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DebugUtil {

    public static final boolean DEBUG = false;
    public static final boolean DEBUG_STEP_EXECUTION = false;
    public static final boolean DEBUG_ALWAYS_EAGER = false;
    public static final boolean LOG_SWITCH_TO_EAGER = false;
    public static final boolean LOG_TOTAL_COMPILATION_TIME = false;
    public static final boolean LOG_PHASES = false;
    public static final boolean LOG_BAILOUT_MESSAGES = false;
    public static final boolean LOG_AUTOMATON_SIZES = false;

    @CompilerDirectives.TruffleBoundary
    public static String charToString(int c) {
        if (c <= 0xffff && (Character.isDigit(c) || (0 < c && c < 128 && !Character.isISOControl(c)))) {
            return String.valueOf((char) c);
        } else if (c <= 0xff) {
            return String.format("\\x%02x", c);
        } else if (c <= 0xffff) {
            return String.format("\\u%04x", c);
        } else {
            return String.format("\\u{%06x}", c);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static String escapeString(String s) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            ret.append(charToString(s.charAt(i)));
        }
        return ret.toString();
    }

    @CompilerDirectives.TruffleBoundary
    public static StringBuilder appendNodeId(StringBuilder sb, int id) {
        return sb.append(nodeID(id));
    }

    @CompilerDirectives.TruffleBoundary
    public static String nodeID(int id) {
        return String.format("%04x", id);
    }

    private static final Pattern specialChars = Pattern.compile("[\"\\\\\u0000-\u001F\u007F-\u009F]");

    public static String jsStringEscape(String str) {
        StringBuffer escapedString = new StringBuffer();
        Matcher m = specialChars.matcher(str);
        while (m.find()) {
            String replacement;
            char c = str.charAt(m.start());
            if (c == '"') {
                replacement = "\\\\\"";
            } else if (c == '\\') {
                replacement = "\\\\\\\\";
            } else {
                assert Character.isISOControl(c);
                replacement = String.format("\\\\u%04x", (int) c);
            }
            m.appendReplacement(escapedString, replacement);
        }
        m.appendTail(escapedString);
        return escapedString.toString();
    }

    public static String randomJsStringFromRanges(char[] ranges, int length) {
        Random random = new Random(System.currentTimeMillis());
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int rangeIndex = random.nextInt(ranges.length / 2);
            char lo = ranges[rangeIndex * 2];
            char hi = ranges[rangeIndex * 2 + 1];
            char randChar = (char) (lo + random.nextInt((hi + 1) - lo));
            if (randChar == '"') {
                stringBuilder.append("\\\\\"");
            } else if (randChar == '\\') {
                stringBuilder.append("\\\\\\\\");
            } else if (randChar > 0x7f || Character.isISOControl(randChar)) {
                stringBuilder.append(String.format("\\u%04x", (int) randChar));
            } else {
                stringBuilder.append(randChar);
            }
        }
        return stringBuilder.toString();
    }

    public static class Timer {

        private long startTime = 0;

        public void start() {
            startTime = System.nanoTime();
        }

        public long getElapsed() {
            return System.nanoTime() - startTime;
        }

        public String elapsedToString() {
            return elapsedToString(getElapsed());
        }

        public static String elapsedToString(long elapsed) {
            return String.format("%fms", elapsed / 1e6);
        }
    }

    public static class DebugLogger {

        private final String prefix;
        private final boolean enable;

        public DebugLogger(String prefix, boolean enable) {
            this.prefix = prefix;
            this.enable = enable;
        }

        public void log(String msg) {
            if (enable) {
                System.out.println(prefix + msg);
            }
        }
    }

    public abstract static class AbstractValue {

        protected String name;

        public AbstractValue(String name) {
            this.name = name;
        }

        public abstract String toString(int indent, int nameWidth);

        protected static String nameFmtString(int indent, int nameWidth) {
            return "%" + (indent == 0 ? "" : indent) + "s%-" + nameWidth + "s: ";
        }
    }

    public static class Value extends AbstractValue {

        private final Object value;

        public Value(String name, Object value) {
            super(name);
            this.value = value;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public String toString(int indent, int nameWidth) {
            return String.format(nameFmtString(indent, nameWidth) + "%s", "", name, value);
        }
    }

    public static class Table extends AbstractValue {

        private final ArrayList<AbstractValue> values;

        public Table(String name, ArrayList<AbstractValue> values) {
            super(name);
            this.values = values;
        }

        public Table(String name, AbstractValue... values) {
            super(name);
            this.values = new ArrayList<>();
            Collections.addAll(this.values, values);
        }

        public Table append(AbstractValue... appendValues) {
            Collections.addAll(values, appendValues);
            return this;
        }

        public Table append(Stream<AbstractValue> appendValues) {
            appendValues.forEach(values::add);
            return this;
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public String toString() {
            return toString(0, name.length());
        }

        @Override
        @CompilerDirectives.TruffleBoundary
        public String toString(int indent, int nameWidth) {
            final int newNameWidth = values.stream().map(x -> x.name.length()).max(Comparator.naturalOrder()).orElse(0);
            final String fmtString = nameFmtString(indent, nameWidth) + "{\n%s\n%" + (indent + 1) + "s";
            return String.format(fmtString, "", name, values.stream().map(x -> x.toString(indent + 2, newNameWidth)).collect(Collectors.joining("\n")), "}");
        }
    }
}
