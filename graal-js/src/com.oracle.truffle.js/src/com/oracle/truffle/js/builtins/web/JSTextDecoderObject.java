/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.web;

import java.util.Arrays;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;

public final class JSTextDecoderObject extends JSNonProxyObject {

    private final TruffleString encoding;
    private final TruffleString.Encoding truffleStringEncoding;
    private final boolean fatal;
    private final boolean ignoreBOM;

    // mutable decoder state
    private boolean bomSeen;
    private byte[] pendingBytes;

    protected JSTextDecoderObject(Shape shape, JSDynamicObject proto, TruffleString encoding, TruffleString.Encoding truffleStringEncoding, boolean fatal, boolean ignoreBOM) {
        super(shape, proto);
        this.fatal = fatal;
        this.ignoreBOM = ignoreBOM;
        this.encoding = encoding;
        this.truffleStringEncoding = truffleStringEncoding;
    }

    public TruffleString getEncoding() {
        return encoding;
    }

    public TruffleString.Encoding getTruffleStringEncoding() {
        return truffleStringEncoding;
    }

    public boolean isFatal() {
        return fatal;
    }

    public boolean isIgnoreBOM() {
        return ignoreBOM;
    }

    public void setBomSeen() {
        this.bomSeen = true;
    }

    public boolean isBomSeen() {
        return bomSeen;
    }

    public byte[] getPendingBytes() {
        return pendingBytes;
    }

    public void setPendingBytes(byte[] pendingBytes, int start, int end) {
        if (start == end) {
            clearPendingBytes();
        } else {
            this.pendingBytes = Arrays.copyOfRange(pendingBytes, start, end);
        }
    }

    private void clearPendingBytes() {
        pendingBytes = null;
    }

    /**
     * Maybe (partially or fully) reset decoder state.
     */
    public void endDecode(boolean stream, boolean onError) {
        if (!stream) {
            bomSeen = false;
        }
        if (!stream || onError) {
            clearPendingBytes();
        }
    }
}
