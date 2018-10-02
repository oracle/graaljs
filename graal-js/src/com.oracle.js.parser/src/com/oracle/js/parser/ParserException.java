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

import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.Node;

/**
 * ECMAScript parser exceptions.
 */
@SuppressWarnings("serial")
public final class ParserException extends RuntimeException implements TruffleException {
    // script file name
    private String fileName;
    // script line number
    private int line;
    // script column number
    private int column;

    // Source from which this ParserException originated
    private final Source source;
    // token responsible for this exception
    private final long token;
    // if this is translated as ECMA error, which type should be used?
    private final JSErrorType errorType;

    /**
     * Constructor.
     *
     * @param msg exception message for this parser error.
     */
    public ParserException(final String msg) {
        this(JSErrorType.SyntaxError, msg, null, -1, -1, -1);
    }

    /**
     * Constructor.
     *
     * @param errorType error type
     * @param msg exception message
     * @param source source from which this exception originates
     * @param line line number of exception
     * @param column column number of exception
     * @param token token from which this exception originates
     *
     */
    public ParserException(final JSErrorType errorType, final String msg, final Source source, final int line, final int column, final long token) {
        super(msg);
        this.fileName = source != null ? source.getName() : null;
        this.line = line;
        this.column = column;
        this.source = source;
        this.token = token;
        this.errorType = errorType;
    }

    /**
     * Get the source file name for this {@code ParserException}.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the source file name for this {@code ParserException}.
     *
     * @param fileName the file name
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the line number for this {@code ParserException}.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * Set the line number for this {@code ParserException}.
     *
     * @param line the line number
     */
    public void setLineNumber(final int line) {
        this.line = line;
    }

    /**
     * Get the column for this {@code ParserException}.
     *
     * @return the column number
     */
    public int getColumnNumber() {
        return column;
    }

    /**
     * Set the column for this {@code ParserException}.
     *
     * @param column the column number
     */
    public void setColumnNumber(final int column) {
        this.column = column;
    }

    /**
     * Get the {@code Source} of this {@code ParserException}.
     *
     * @return source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Get the token responsible for this {@code ParserException}.
     *
     * @return token
     */
    public long getToken() {
        return token;
    }

    /**
     * Get token position within source where the error originated.
     *
     * @return token position if available, else -1
     */
    public int getPosition() {
        return Token.descPosition(token);
    }

    /**
     * Get the {@code JSErrorType} of this {@code ParserException}.
     *
     * @return error type
     */
    public JSErrorType getErrorType() {
        return errorType;
    }

    @Override
    public Node getLocation() {
        return null;
    }

    @Override
    public boolean isSyntaxError() {
        return true;
    }

    @Override
    public boolean isIncompleteSource() {
        return Token.descType(token) == TokenType.EOF;
    }
}
