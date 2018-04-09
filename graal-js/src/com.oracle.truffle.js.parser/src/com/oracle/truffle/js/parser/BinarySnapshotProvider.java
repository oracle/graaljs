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
package com.oracle.truffle.js.parser;

import java.nio.ByteBuffer;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.codec.BinaryDecoder;
import com.oracle.truffle.js.codec.NodeDecoder;
import com.oracle.truffle.js.nodes.JSNodeDecoder;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.runtime.JSContext;

public class BinarySnapshotProvider implements SnapshotProvider {
    public static final int MAGIC = 0x314e4942;
    private final ByteBuffer buffer;

    public BinarySnapshotProvider(ByteBuffer buffer) {
        this.buffer = buffer;
        assert checkFormat(new BinaryDecoder(buffer));
    }

    private static boolean checkFormat(BinaryDecoder decoder) {
        int magic = decoder.getInt32();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Unknown format");
        }
        int checksum = decoder.getInt32();
        if (checksum != JSNodeDecoder.getChecksum()) {
            throw new IllegalArgumentException("Snapshot verification failed");
        }
        return true;
    }

    public BinarySnapshotProvider(byte[] bytes) {
        this(ByteBuffer.wrap(bytes));
    }

    @Override
    public Object apply(NodeFactory nodeFactory, JSContext context, Source source) {
        BinaryDecoder decoder = new BinaryDecoder(buffer);
        checkFormat(decoder);
        int sourceLength = decoder.getInt32();
        int sourceHash = decoder.getInt32();
        CharSequence code = source.getCharacters();
        if (code.length() != sourceLength || code.hashCode() != sourceHash) {
            throw new IllegalArgumentException("Snapshot verification failed");
        }
        return new JSNodeDecoder().decodeNode(new NodeDecoder.DecoderState(decoder), nodeFactory, context, source);
    }
}
