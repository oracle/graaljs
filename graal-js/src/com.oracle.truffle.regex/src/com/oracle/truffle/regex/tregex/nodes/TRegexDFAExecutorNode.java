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
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.regex.RegexObject;
import com.oracle.truffle.regex.tregex.nodes.input.InputCharAtNode;
import com.oracle.truffle.regex.tregex.nodes.input.InputLengthNode;
import com.oracle.truffle.regex.tregex.util.DebugUtil;

public final class TRegexDFAExecutorNode extends Node {

    public static final int NO_MATCH = -2;
    private final TRegexDFAExecutorProperties props;
    private final int maxNumberOfNFAStates;
    @Child private InputLengthNode lengthNode = InputLengthNode.create();
    @Child private InputCharAtNode charAtNode = InputCharAtNode.create();
    @Children private final DFAAbstractStateNode[] states;
    @Children private final DFACaptureGroupLazyTransitionNode[] cgTransitions;

    public TRegexDFAExecutorNode(TRegexDFAExecutorProperties props, int maxNumberOfNFAStates, DFAAbstractStateNode[] states, DFACaptureGroupLazyTransitionNode[] cgTransitions) {
        this.props = props;
        this.maxNumberOfNFAStates = maxNumberOfNFAStates;
        this.states = states;
        this.cgTransitions = cgTransitions;
    }

    private DFAInitialStateNode getInitialState() {
        return (DFAInitialStateNode) states[0];
    }

    public int getPrefixLength() {
        return getInitialState().getPrefixLength();
    }

    public boolean isAnchored() {
        return !getInitialState().hasUnAnchoredEntry();
    }

    public boolean isForward() {
        return props.isForward();
    }

    public boolean isBackward() {
        return !props.isForward();
    }

    public boolean isSearching() {
        return props.isSearching();
    }

    public DFACaptureGroupLazyTransitionNode[] getCGTransitions() {
        return cgTransitions;
    }

    public int getNumberOfStates() {
        return states.length;
    }

    public int getNumberOfCaptureGroups() {
        return props.getNumberOfCaptureGroups();
    }

    /**
     * records position of the END of the match found, or -1 if no match exists.
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void execute(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        CompilerAsserts.compilationConstant(states);
        CompilerAsserts.compilationConstant(states.length);
        if (!validArgs(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalArgumentException(String.format("Got illegal args! (fromIndex %d, initialIndex %d, maxIndex %d)",
                            getFromIndex(frame), getIndex(frame), getMaxIndex(frame)));
        }
        if (isBackward() && getFromIndex(frame) - 1 > getMaxIndex(frame)) {
            setCurMaxIndex(frame, getFromIndex(frame) - 1);
        } else {
            setCurMaxIndex(frame, getMaxIndex(frame));
        }
        if (props.isTrackCaptureGroups()) {
            createCGData(frame);
            initResultOrder(frame);
            setResultObject(frame, null);
        } else {
            setResultInt(frame, TRegexDFAExecutorNode.NO_MATCH);
        }
        int ip = 0;
        outer: while (true) {
            CompilerAsserts.partialEvaluationConstant(ip);
            if (ip == -1) {
                break;
            }
            final DFAAbstractStateNode curState = states[ip];
            CompilerAsserts.partialEvaluationConstant(curState);
            final short[] successors = curState.getSuccessors();
            CompilerAsserts.partialEvaluationConstant(successors);
            CompilerAsserts.partialEvaluationConstant(successors.length);
            curState.executeFindSuccessor(frame, this);
            for (int i = 0; i < successors.length; i++) {
                if (i == getSuccessorIndex(frame)) {
                    ip = successors[i];
                    continue outer;
                }
            }
            assert getSuccessorIndex(frame) == -1;
            break;
        }
    }

    /**
     * The index pointing into {@link #getInput(VirtualFrame)}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the current index of {@link #getInput(VirtualFrame)} that is being processed.
     */
    public int getIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, props.getIndexFS());
    }

    public void setIndex(VirtualFrame frame, int i) {
        frame.setInt(props.getIndexFS(), i);
    }

    /**
     * The <code>fromIndex</code> argument given to
     * {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the <code>fromIndex</code> argument given to
     *         {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     */
    public int getFromIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, props.getFromIndexFS());
    }

    public void setFromIndex(VirtualFrame frame, int fromIndex) {
        frame.setInt(props.getFromIndexFS(), fromIndex);
    }

    /**
     * The maximum index as given by the parent {@link TRegexExecRootNode}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the maximum index as given by the parent {@link TRegexExecRootNode}.
     */
    public int getMaxIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, props.getMaxIndexFS());
    }

    public void setMaxIndex(VirtualFrame frame, int maxIndex) {
        frame.setInt(props.getMaxIndexFS(), maxIndex);
    }

    /**
     * The maximum index as checked by {@link #hasNext(VirtualFrame)}. In most cases this value is
     * equal to {@link #getMaxIndex(VirtualFrame)}, but backward matching nodes change this value
     * while matching.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the maximum index as checked by {@link #hasNext(VirtualFrame)}.
     *
     * @see BackwardDFAStateNode
     */
    public int getCurMaxIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, props.getCurMaxIndexFS());
    }

    public void setCurMaxIndex(VirtualFrame frame, int value) {
        frame.setInt(props.getCurMaxIndexFS(), value);
    }

    /**
     * The <code>input</code> argument given to
     * {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the <code>input</code> argument given to
     *         {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     */
    public Object getInput(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, props.getInputFS());
    }

    public void setInput(VirtualFrame frame, Object input) {
        frame.setObject(props.getInputFS(), input);
    }

    /**
     * The length of the <code>input</code> argument given to
     * {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     * 
     * @param frame a virtual frame as described by {@link TRegexDFAExecutorProperties}.
     * @return the length of the <code>input</code> argument given to
     *         {@link TRegexExecRootNode#execute(VirtualFrame, RegexObject, Object, int)}.
     */
    public int getInputLength(VirtualFrame frame) {
        return lengthNode.execute(getInput(frame));
    }

    public char getChar(VirtualFrame frame) {
        char c = charAtNode.execute(getInput(frame), getIndex(frame));
        if (DebugUtil.DEBUG_STEP_EXECUTION) {
            System.out.println();
            System.out.println("read char " + c);
        }
        return c;
    }

    public void advance(VirtualFrame frame) {
        setIndex(frame, props.isForward() ? getIndex(frame) + 1 : getIndex(frame) - 1);
    }

    public boolean hasNext(VirtualFrame frame) {
        return props.isForward() ? Integer.compareUnsigned(getIndex(frame), getCurMaxIndex(frame)) < 0 : getIndex(frame) > getCurMaxIndex(frame);
    }

    public boolean atBegin(VirtualFrame frame) {
        return getIndex(frame) == (props.isForward() ? 0 : getInputLength(frame) - 1);
    }

    public boolean atEnd(VirtualFrame frame) {
        final int i = getIndex(frame);
        if (props.isForward()) {
            return i == getInputLength(frame);
        } else {
            return i < 0;
        }
    }

    public int rewindUpTo(VirtualFrame frame, int length) {
        if (props.isForward()) {
            final int offset = Math.min(getIndex(frame), length);
            setIndex(frame, getIndex(frame) - offset);
            return offset;
        } else {
            assert length == 0;
            return 0;
        }
    }

    public void setLastTransition(VirtualFrame frame, short lastTransition) {
        frame.setInt(props.getLastTransitionFS(), lastTransition);
    }

    public short getLastTransition(VirtualFrame frame) {
        return (short) FrameUtil.getIntSafe(frame, props.getLastTransitionFS());
    }

    public void setSuccessorIndex(VirtualFrame frame, int successorIndex) {
        frame.setInt(props.getSuccessorIndexFS(), successorIndex);
    }

    public int getSuccessorIndex(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, props.getSuccessorIndexFS());
    }

    public int getResultInt(VirtualFrame frame) {
        assert !props.isTrackCaptureGroups();
        return FrameUtil.getIntSafe(frame, props.getResultFS());
    }

    public void setResultInt(VirtualFrame frame, int result) {
        frame.setInt(props.getResultFS(), result);
    }

    public int[] getResultCaptureGroups(VirtualFrame frame) {
        assert props.isTrackCaptureGroups();
        return (int[]) FrameUtil.getObjectSafe(frame, props.getCaptureGroupResultFS());
    }

    public void setResultObject(VirtualFrame frame, Object result) {
        frame.setObject(props.getCaptureGroupResultFS(), result);
    }

    public DFACaptureGroupTrackingData getCGData(VirtualFrame frame) {
        return (DFACaptureGroupTrackingData) FrameUtil.getObjectSafe(frame, props.getCgDataFS());
    }

    private void createCGData(VirtualFrame frame) {
        DFACaptureGroupTrackingData trackingData = new DFACaptureGroupTrackingData(maxNumberOfNFAStates, props.getNumberOfCaptureGroups());
        frame.setObject(props.getCgDataFS(), trackingData);
    }

    private boolean validArgs(VirtualFrame frame) {
        final int initialIndex = getIndex(frame);
        final int inputLength = getInputLength(frame);
        final int fromIndex = getFromIndex(frame);
        final int maxIndex = getMaxIndex(frame);
        if (props.isForward()) {
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

    @ExplodeLoop
    private void initResultOrder(VirtualFrame frame) {
        DFACaptureGroupTrackingData cgData = getCGData(frame);
        for (int i = 0; i < maxNumberOfNFAStates; i++) {
            cgData.currentResultOrder[i] = i;
        }
    }

    public TRegexDFAExecutorProperties getProperties() {
        return props;
    }
}
