/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.buffer;

import com.oracle.truffle.regex.RegexSource;
import com.oracle.truffle.regex.tregex.TRegexEngine;

/**
 * This class is instantiated once per compilation of a regular expression in
 * {@link TRegexEngine#compile(RegexSource)} and is supposed to reduce the amount of allocations
 * during automaton generation. It provides various "scratch-pad" buffers for the creation of arrays
 * of unknown size. When using these buffers, take extra care not to use them in two places
 * simultaneously! {@link TRegexEngine#compile(RegexSource)} is designed to be run single-threaded,
 * but nested functions may still lead to "simultaneous" use of these buffers.
 *
 * @see ObjectArrayBuffer
 * @see ByteArrayBuffer
 * @see RangesArrayBuffer
 */
public class CompilationBuffer {

    private ObjectArrayBuffer objectBuffer1;
    private ObjectArrayBuffer objectBuffer2;
    private ByteArrayBuffer byteArrayBuffer;
    private RangesArrayBuffer rangesArrayBuffer1;
    private RangesArrayBuffer rangesArrayBuffer2;
    private RangesArrayBuffer rangesArrayBuffer3;

    public RangesArrayBuffer getRangesArrayBuffer1() {
        if (rangesArrayBuffer1 == null) {
            rangesArrayBuffer1 = new RangesArrayBuffer(64);
        }
        rangesArrayBuffer1.clear();
        return rangesArrayBuffer1;
    }

    public RangesArrayBuffer getRangesArrayBuffer2() {
        if (rangesArrayBuffer2 == null) {
            rangesArrayBuffer2 = new RangesArrayBuffer(64);
        }
        rangesArrayBuffer2.clear();
        return rangesArrayBuffer2;
    }

    public RangesArrayBuffer getRangesArrayBuffer3() {
        if (rangesArrayBuffer3 == null) {
            rangesArrayBuffer3 = new RangesArrayBuffer(64);
        }
        rangesArrayBuffer3.clear();
        return rangesArrayBuffer3;
    }

    public ObjectArrayBuffer getObjectBuffer1() {
        if (objectBuffer1 == null) {
            objectBuffer1 = new ObjectArrayBuffer();
        }
        objectBuffer1.clear();
        return objectBuffer1;
    }

    public ObjectArrayBuffer getObjectBuffer2() {
        if (objectBuffer2 == null) {
            objectBuffer2 = new ObjectArrayBuffer();
        }
        objectBuffer2.clear();
        return objectBuffer2;
    }

    public ByteArrayBuffer getByteArrayBuffer() {
        if (byteArrayBuffer == null) {
            byteArrayBuffer = new ByteArrayBuffer();
        }
        byteArrayBuffer.clear();
        return byteArrayBuffer;
    }
}
