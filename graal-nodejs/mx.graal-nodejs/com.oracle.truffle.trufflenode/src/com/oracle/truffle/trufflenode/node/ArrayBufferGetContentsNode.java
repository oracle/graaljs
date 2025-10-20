/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode.node;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

@GenerateInline
@ImportStatic(JSConfig.class)
public abstract class ArrayBufferGetContentsNode extends JavaScriptBaseNode {

    private static final int BULK_COPY_MAX_BUFFER_SIZE = 1024 * 1024; // 1 MB

    ArrayBufferGetContentsNode() {
    }

    @NeverDefault
    public static ArrayBufferGetContentsNode create() {
        return ArrayBufferGetContentsNodeGen.create();
    }

    public abstract ByteBuffer execute(Node node, Object buffer);

    @Specialization(limit = "InteropLibraryLimit")
    protected ByteBuffer doInteropBuffer(Object buffer,
                    @CachedLibrary("buffer") InteropLibrary interop) {
        try {
            final int bufferSize = (int) interop.getBufferSize(buffer);
            final int copyBufferSize = Math.min(bufferSize, BULK_COPY_MAX_BUFFER_SIZE);
            ByteBuffer byteBuffer = DirectByteBufferHelper.allocateDirect(bufferSize);
            byte[] copyBuffer = new byte[copyBufferSize];
            for (int i = 0; i < bufferSize; i += copyBufferSize) {
                int remaining = bufferSize - i;
                int copyLength = Math.min(copyBufferSize, remaining);
                interop.readBuffer(buffer, i, copyBuffer, 0, copyLength);
                Boundaries.byteBufferPutArray(byteBuffer, i, copyBuffer, 0, copyLength);
            }
            LoopNode.reportLoopCount(this, bufferSize);
            return byteBuffer;
        } catch (InteropException iex) {
            throw Errors.shouldNotReachHere(iex);
        }
    }

}
