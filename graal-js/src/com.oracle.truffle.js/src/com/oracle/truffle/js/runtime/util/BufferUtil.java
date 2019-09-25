/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.Buffer;

/**
 * Covariant return types for some methods in the {@code java.nio.Buffer} were
 * <a href="https://bugs.openjdk.java.net/browse/JDK-4774077">introduced in JDK 9</a>.
 *
 * If calls to these methods are compiled with javac from JDK 9+ using {@code -target 8 -source 8}
 * then the call sites will invoke the covariant methods in the subclass. For example:
 *
 * <pre>
 * static void reset(ByteBuffer buf) {
 *     buf.reset();
 * }
 * </pre>
 *
 * will result in:
 *
 * <pre>
 *    0: aload_0
 *    1: invokevirtual #7  // Method java/nio/ByteBuffer.reset:()Ljava/nio/ByteBuffer;
 *    4: pop
 *    5: return
 * </pre>
 *
 * This will result in a {@link NoSuchMethodError} when run on JDK 8. The workaround for this is to
 * {@linkplain #asBaseBuffer(Buffer) coerce} the receiver for calls to the covariant methods to
 * {@link Buffer}.
 */
public final class BufferUtil {

    /**
     * Coerces {@code obj} to be of type {@link Buffer}.
     */
    public static Buffer asBaseBuffer(Buffer obj) {
        return obj;
    }
}
