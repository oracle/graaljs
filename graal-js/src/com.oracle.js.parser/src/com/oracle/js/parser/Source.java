/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.js.parser;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

// @formatter:off
/**
 * Source objects track the origin of JavaScript entities.
 */
public final class Source {
    private static final int BUF_SIZE = 8 * 1024;

    /**
     * Descriptive name of the source as supplied by the user. Used for error
     * reporting to the user. For example, SyntaxError will use this to print message.
     * Used to implement __FILE__. Also used for SourceFile in .class for debugger usage.
     */
    private final String name;

    /**
     * Base path or URL of this source. Used to implement __DIR__, which can be
     * used to load scripts relative to the location of the current script.
     * This will be null when it can't be computed.
     */
    private final String base;

    /** Source content */
    private final Data data;

    /** Cached hash code */
    private int hash;

    /** Base64-encoded SHA1 digest of this source object */
    private volatile byte[] digest;

    /** source URL set via //@ sourceURL or //# sourceURL directive */
    private String explicitURL;

    // Do *not* make this public, ever! Trusts the URL and content.
    private Source(final String name, final String base, final Data data) {
        this.name = name;
        this.base = base;
        this.data = data;
    }

    // Wrapper to manage lazy loading
    private interface Data {

        URL url();

        int length();

        long lastModified();

        CharSequence data();

        boolean isEvalCode();
    }

    private static final class RawData implements Data {
        private final CharSequence source;
        private final boolean evalCode;
        private int hash;

        private RawData(final CharSequence source, final boolean evalCode) {
            this.source = Objects.requireNonNull(source);
            this.evalCode = evalCode;
        }

        private RawData(final Reader reader) throws IOException {
            this(readFully(reader), false);
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = hash = source.hashCode() ^ (evalCode ? 1 : 0);
            }
            return h;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof RawData) {
                final RawData other = (RawData)obj;
                return source.equals(other.source) && evalCode == other.evalCode;
            }
            return false;
        }

        @Override
        public String toString() {
            return data().toString();
        }

        @Override
        public URL url() {
            return null;
        }

        @Override
        public int length() {
            return source.length();
        }

        @Override
        public long lastModified() {
            return 0;
        }

        @Override
        public CharSequence data() {
            return source;
        }

        @Override
        public boolean isEvalCode() {
            return evalCode;
        }
    }

    private CharSequence data() {
        return data.data();
    }

    /**
     * Returns a Source instance
     *
     * @param name    source name
     * @param content contents as {@link CharSequence}
     * @param isEval does this represent code from 'eval' call?
     * @return source instance
     */
    public static Source sourceFor(final String name, final CharSequence content, final boolean isEval) {
        return new Source(name, baseName(name), new RawData(content, isEval));
    }

    /**
     * Returns a Source instance
     *
     * @param name    source name
     * @param content contents as string
     * @return source instance
     */
    public static Source sourceFor(final String name, final String content) {
        return sourceFor(name, content, false);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Source)) {
            return false;
        }
        final Source other = (Source) obj;
        return Objects.equals(name, other.name) && data.equals(other.data);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = hash = data.hashCode() ^ Objects.hashCode(name);
        }
        return h;
    }

    /**
     * Fetch source content.
     * @return Source content.
     */
    public String getString() {
        return data.toString();
    }

    /**
     * Get the user supplied name of this script.
     * @return User supplied source name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the last modified time of this script.
     * @return Last modified time.
     */
    public long getLastModified() {
        return data.lastModified();
    }

    /**
     * Get the "directory" part of the file or "base" of the URL.
     * @return base of file or URL.
     */
    public String getBase() {
        return base;
    }

    /**
     * Fetch a portion of source content.
     * @param start start index in source
     * @param len length of portion
     * @return Source content portion.
     */
    public String getString(final int start, final int len) {
        return data().subSequence(start, start + len).toString();
    }

    /**
     * Fetch a portion of source content associated with a token.
     * @param token Token descriptor.
     * @return Source content portion.
     */
    public String getString(final long token) {
        final int start = Token.descPosition(token);
        final int len = Token.descLength(token);
        return getString(start, len);
    }

    /**
     * Returns the source URL of this script Source. Can be null if Source
     * was created from a String or a char[].
     *
     * @return URL source or null
     */
    public URL getURL() {
        return data.url();
    }

    /**
     * Get explicit source URL.
     * @return URL set via sourceURL directive
     */
    public String getExplicitURL() {
        return explicitURL;
    }

    /**
     * Set explicit source URL.
     * @param explicitURL URL set via sourceURL directive
     */
    public void setExplicitURL(String explicitURL) {
        this.explicitURL = explicitURL;
    }

    /**
     * Returns whether this source was submitted via 'eval' call or not.
     *
     * @return true if this source represents code submitted via 'eval'
     */
    public boolean isEvalCode() {
        return data.isEvalCode();
    }

    /**
     * Find the beginning of the line containing position.
     * @param position Index to offending token.
     * @return Index of first character of line.
     */
    private int findBOLN(final int position) {
        final CharSequence d = data();
        for (int i = position - 1; i > 0; i--) {
            final char ch = d.charAt(i);

            if (ch == '\n' || ch == '\r') {
                return i + 1;
            }
        }

        return 0;
    }

    /**
     * Find the end of the line containing position.
     * @param position Index to offending token.
     * @return Index of last character of line.
     */
    private int findEOLN(final int position) {
        final CharSequence d = data();
        final int length = d.length();
        for (int i = position; i < length; i++) {
            final char ch = d.charAt(i);

            if (ch == '\n' || ch == '\r') {
                return i - 1;
            }
        }

        return length - 1;
    }

    /**
     * Return line number of character position.
     *
     * <p>This method can be expensive for large sources as it iterates through
     * all characters up to {@code position}.</p>
     *
     * @param position Position of character in source content.
     * @return Line number.
     */
    public int getLine(final int position) {
        final CharSequence d = data();
        // Line count starts at 1.
        int line = 1;

        for (int i = 0; i < position; i++) {
            final char ch = d.charAt(i);
            // Works for both \n and \r\n.
            if (ch == '\n') {
                line++;
            }
        }

        return line;
    }

    /**
     * Return column number of character position.
     * @param position Position of character in source content.
     * @return Column number.
     */
    public int getColumn(final int position) {
        // TODO - column needs to account for tabs.
        return position - findBOLN(position);
    }

    /**
     * Return line text including character position.
     * @param position Position of character in source content.
     * @return Line text.
     */
    public CharSequence getSourceLine(final int position) {
        // Find end of previous line.
        final int first = findBOLN(position);
        // Find end of this line.
        final int last = findEOLN(position);

        return data().subSequence(first, last + 1);
    }

    /**
     * Get the content of this source as a {@link CharSequence}.
     */
    public CharSequence getContent() {
        return data();
    }

    /**
     * Get the length in chars for this source
     * @return length
     */
    public int getLength() {
        return data.length();
    }

    /**
     * Read all of the source until end of file.
     *
     * @param reader reader opened to source stream
     * @return source as content
     * @throws IOException if source could not be read
     */
    public static String readFully(final Reader reader) throws IOException {
        final char[]        arr = new char[BUF_SIZE];
        final StringBuilder sb  = new StringBuilder();

        try {
            int numChars;
            while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
                sb.append(arr, 0, numChars);
            }
        } finally {
            reader.close();
        }

        return sb.toString();
    }

    /**
     * Get a Base64-encoded SHA1 digest for this source.
     *
     * @return a Base64-encoded SHA1 digest for this source
     */
    public String getDigest() {
        return new String(getDigestBytes(), StandardCharsets.US_ASCII);
    }

    private byte[] getDigestBytes() {
        byte[] ldigest = digest;
        if (ldigest == null) {
            final CharSequence content = data();
            final byte[] bytes = new byte[content.length() * 2];

            for (int i = 0; i < content.length(); i++) {
                bytes[i * 2]     = (byte)  (content.charAt(i) & 0x00ff);
                bytes[i * 2 + 1] = (byte) ((content.charAt(i) & 0xff00) >> 8);
            }

            try {
                final MessageDigest md = MessageDigest.getInstance("SHA-1");
                if (name != null) {
                    md.update(name.getBytes(StandardCharsets.UTF_8));
                }
                if (base != null) {
                    md.update(base.getBytes(StandardCharsets.UTF_8));
                }
                if (getURL() != null) {
                    md.update(getURL().toString().getBytes(StandardCharsets.UTF_8));
                }
                // Message digest to file name encoder
                Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
                digest = ldigest = base64.encode(md.digest(bytes));
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return ldigest;
    }

    // fake directory like name
    private static String baseName(final String name) {
        int idx = name.lastIndexOf('/');
        if (idx == -1) {
            idx = name.lastIndexOf('\\');
        }
        return (idx != -1) ? name.substring(0, idx + 1) : null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
