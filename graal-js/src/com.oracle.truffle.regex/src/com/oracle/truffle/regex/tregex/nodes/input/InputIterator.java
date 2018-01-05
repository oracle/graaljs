/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes.input;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public final class InputIterator extends Node {

    private final FrameSlot inputSeqFS;
    private final FrameSlot fromIndexFS;
    private final FrameSlot indexFS;
    private final FrameSlot maxIndexFS;
    private final boolean forward;
    @Child private InputLengthNode lengthNode = InputLengthNode.create();
    @Child private InputCharAtNode charAtNode = InputCharAtNode.create();
    @Child private InputIndexOfNode indexOfNode;
    @Child private InputLastIndexOfNode lastIndexOfNode;

    public InputIterator(FrameSlot inputSeqFS, FrameSlot fromIndexFS, FrameSlot indexFS, FrameSlot maxIndexFS, boolean forward) {
        this.inputSeqFS = inputSeqFS;
        this.fromIndexFS = fromIndexFS;
        this.indexFS = indexFS;
        this.maxIndexFS = maxIndexFS;
        this.forward = forward;
    }

    private InputIndexOfNode getIndexOfNode() {
        if (indexOfNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            indexOfNode = InputIndexOfNode.create();
        }
        return indexOfNode;
    }

    private InputLastIndexOfNode getLastIndexOfNode() {
        if (lastIndexOfNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastIndexOfNode = InputLastIndexOfNode.create();
        }
        return lastIndexOfNode;
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

    public boolean hasNext(VirtualFrame frame) {
        final int index = getIndex(frame);
        return forward ? index < getMaxIndex(frame) : index > getMaxIndex(frame);
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

    /**
     * Find a single character using the intrinsic {@link String#indexOf(int)}, if possible.
     * 
     * @param frame frame with fields input, index and maxIndex.
     * @param c char to look for.
     * @return true if char c was found, else false. If the character was found, index is set to the
     *         location of the character, else index is set to the end of input.
     */
    public boolean findChar(VirtualFrame frame, char c) {
        final int result = forward ? getIndexOfNode().execute(getInput(frame), c, getIndex(frame), getMaxIndex(frame))
                        : getLastIndexOfNode().execute(getInput(frame), c, getIndex(frame), isPastFromIndex(frame) ? getMaxIndex(frame) : getFromIndex(frame));
        if (result == -1) {
            setIndex(frame, forward ? getMaxIndex(frame) - 1 : getMaxIndex(frame) + 1);
            return false;
        }
        setIndex(frame, result);
        return true;
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

    public int distanceTo(VirtualFrame frame, int otherIndex) {
        return Math.abs(getIndex(frame) - otherIndex);
    }
}
