/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class InputIterator extends Node {

    private final FrameSlot inputSeqFS;
    private final FrameSlot fromIndexFS;
    private final FrameSlot indexFS;
    private final FrameSlot maxIndexFS;
    private final FrameSlot curMaxIndexFS;
    private final FrameSlot lastTransitionFS;
    private final FrameSlot resultFS;
    private final boolean forward;
    @Child private InputLengthNode lengthNode = InputLengthNode.create();
    @Child private InputCharAtNode charAtNode = InputCharAtNode.create();

    public InputIterator(
                    FrameSlot inputSeqFS,
                    FrameSlot fromIndexFS,
                    FrameSlot indexFS,
                    FrameSlot maxIndexFS,
                    FrameSlot curMaxIndexFS,
                    FrameSlot lastTransitionFS,
                    FrameSlot resultFS,
                    boolean forward) {
        this.inputSeqFS = inputSeqFS;
        this.fromIndexFS = fromIndexFS;
        this.indexFS = indexFS;
        this.maxIndexFS = maxIndexFS;
        this.curMaxIndexFS = curMaxIndexFS;
        this.lastTransitionFS = lastTransitionFS;
        this.resultFS = resultFS;
        this.forward = forward;
    }

    public boolean isBackward() {
        return !forward;
    }

    public int getIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, indexFS);
    }

    private void setIndex(VirtualFrame frame, int i) {
        frame.setInt(indexFS, i);
    }

    public int getFromIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, fromIndexFS);
    }

    public int getMaxIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, maxIndexFS);
    }

    public int getCurMaxIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, curMaxIndexFS);
    }

    public void setCurMaxIndex(VirtualFrame frame, int value) {
        frame.setInt(curMaxIndexFS, value);
    }

    public boolean validArgs(VirtualFrame frame) {
        final int initialIndex = getIndex(frame);
        final int inputLength = getInputLength(frame);
        final int fromIndex = getFromIndex(frame);
        final int maxIndex = getMaxIndex(frame);
        if (forward) {
            return inputLength >= 0 && inputLength < Integer.MAX_VALUE - 20 &&
                            fromIndex >= 0 && fromIndex <= inputLength &&
                            initialIndex >= 0 && initialIndex <= inputLength &&
                            maxIndex >= 0 && maxIndex <= inputLength &&
                            initialIndex <= maxIndex;
        } else {
            return inputLength >= 0 && inputLength < Integer.MAX_VALUE - 20 &&
                            fromIndex >= 0 && fromIndex <= inputLength &&
                            initialIndex >= -1 && initialIndex < inputLength &&
                            maxIndex >= -1 && maxIndex < inputLength &&
                            initialIndex >= maxIndex;
        }
    }

    public Object getInput(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, inputSeqFS);
    }

    public int getInputLength(VirtualFrame frame) {
        return lengthNode.execute(getInput(frame));
    }

    public char getChar(VirtualFrame frame) {
        return charAtNode.execute(getInput(frame), getIndex(frame));
    }

    public void advance(VirtualFrame frame) {
        setIndex(frame, forward ? getIndex(frame) + 1 : getIndex(frame) - 1);
    }

    public boolean hasNext(VirtualFrame frame, final int maxIndex) {
        final int index = getIndex(frame);
        return forward ? index < maxIndex : index > maxIndex;
    }

    public boolean atBegin(VirtualFrame frame) {
        return getIndex(frame) == (forward ? 0 : getInputLength(frame) - 1);
    }

    public boolean atEnd(VirtualFrame frame) {
        final int i = getIndex(frame);
        if (forward) {
            return i == getInputLength(frame);
        } else {
            return i < 0;
        }
    }

    public boolean isPastFromIndex(VirtualFrame frame) {
        assert isBackward();
        return getFromIndex(frame) > getIndex(frame);
    }

    public int rewindUpTo(VirtualFrame frame, int length) {
        if (forward) {
            final int offset = Math.min(getIndex(frame), length);
            setIndex(frame, getIndex(frame) - offset);
            return offset;
        } else {
            final int offset = Math.min(getInputLength(frame) - getIndex(frame) - 1, length);
            setIndex(frame, getIndex(frame) + offset);
            return offset;
        }
    }

    public void setLastTransition(VirtualFrame frame, short lastTransition) {
        frame.setInt(lastTransitionFS, lastTransition);
    }

    public short getLastTransition(VirtualFrame frame) {
        return (short) FrameUtil.getIntSafe(frame, lastTransitionFS);
    }

    public void setResultInt(VirtualFrame frame, int result) {
        frame.setInt(resultFS, result);
    }

    public void setResultObject(VirtualFrame frame, Object result) {
        frame.setObject(resultFS, result);
    }
}
