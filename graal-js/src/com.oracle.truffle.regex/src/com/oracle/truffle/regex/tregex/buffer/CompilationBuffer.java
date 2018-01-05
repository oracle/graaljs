/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.buffer;

public class CompilationBuffer {

    private ObjectBuffer objectBuffer1;
    private ObjectBuffer objectBuffer2;
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

    public ObjectBuffer getObjectBuffer1() {
        if (objectBuffer1 == null) {
            objectBuffer1 = new ObjectBuffer();
        }
        objectBuffer1.clear();
        return objectBuffer1;
    }

    public ObjectBuffer getObjectBuffer2() {
        if (objectBuffer2 == null) {
            objectBuffer2 = new ObjectBuffer();
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
