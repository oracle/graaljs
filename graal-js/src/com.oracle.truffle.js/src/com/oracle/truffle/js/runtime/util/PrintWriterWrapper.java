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
package com.oracle.truffle.js.runtime.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Creation of PrintWriter is expensive, this is why we change just the delegate writer in this
 * wrapper class.
 */
public final class PrintWriterWrapper extends PrintWriter {

    private OutputStreamWrapper outWrapper;

    public PrintWriterWrapper(OutputStream out, boolean autoFlush) {
        this(new OutputStreamWrapper(out), autoFlush);
    }

    private PrintWriterWrapper(OutputStreamWrapper outWrapper, boolean autoFlush) {
        this(new OutputStreamWriter(outWrapper), autoFlush);
        this.outWrapper = outWrapper;
    }

    private PrintWriterWrapper(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public void setDelegate(Writer out) {
        synchronized (this.lock) {
            this.out = out;
            this.outWrapper = null;
        }
    }

    public void setDelegate(OutputStream out) {
        synchronized (this.lock) {
            if (outWrapper != null) {
                outWrapper.setDelegate(out);
            } else {
                outWrapper = new OutputStreamWrapper(out);
                this.out = new OutputStreamWriter(outWrapper);
            }
        }
    }

    public void setFrom(PrintWriterWrapper otherWrapper) {
        synchronized (this.lock) {
            boolean newWrapper = false;
            if (otherWrapper.outWrapper != null) {
                // Need to keep separate OutputStreamWrapper instances
                if (this.outWrapper != null) {
                    // We both have wrappers, great, just need to update the delegate
                    this.outWrapper.setDelegate(otherWrapper.outWrapper.getDelegate());
                } else {
                    // The other has a wrapper, but we do not. Create our own.
                    this.outWrapper = new OutputStreamWrapper(otherWrapper.outWrapper.getDelegate());
                    newWrapper = true;
                }
            } else {
                // No wrapper. Will copy the Writer only.
                this.outWrapper = null;
            }
            if (this.outWrapper != null) {
                if (newWrapper) {
                    this.out = new OutputStreamWriter(this.outWrapper);
                }
            } else {
                this.out = otherWrapper.out;
            }
        }
    }
}
