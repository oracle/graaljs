/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.regex.tregex.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;

public final class TRegexDFAExecutorProperties {

    private final FrameDescriptor frameDescriptor;
    private final FrameSlot inputFS;
    private final FrameSlot fromIndexFS;
    private final FrameSlot indexFS;
    private final FrameSlot maxIndexFS;
    private final FrameSlot curMaxIndexFS;
    private final FrameSlot successorIndexFS;
    private final FrameSlot resultFS;
    private final FrameSlot captureGroupResultFS;
    private final FrameSlot lastTransitionFS;
    private final FrameSlot cgDataFS;
    private final boolean forward;
    private final boolean searching;
    private final boolean trackCaptureGroups;
    private final int numberOfCaptureGroups;

    public TRegexDFAExecutorProperties(FrameDescriptor frameDescriptor,
                    FrameSlot inputFS,
                    FrameSlot fromIndexFS,
                    FrameSlot indexFS,
                    FrameSlot maxIndexFS,
                    FrameSlot curMaxIndexFS,
                    FrameSlot successorIndexFS,
                    FrameSlot resultFS,
                    FrameSlot captureGroupResultFS,
                    FrameSlot lastTransitionFS,
                    FrameSlot cgDataFS,
                    boolean forward,
                    boolean searching,
                    boolean trackCaptureGroups,
                    int numberOfCaptureGroups) {
        this.frameDescriptor = frameDescriptor;
        this.inputFS = inputFS;
        this.fromIndexFS = fromIndexFS;
        this.indexFS = indexFS;
        this.maxIndexFS = maxIndexFS;
        this.curMaxIndexFS = curMaxIndexFS;
        this.lastTransitionFS = lastTransitionFS;
        this.successorIndexFS = successorIndexFS;
        this.resultFS = resultFS;
        this.captureGroupResultFS = captureGroupResultFS;
        this.cgDataFS = cgDataFS;
        this.forward = forward;
        this.searching = searching;
        this.trackCaptureGroups = trackCaptureGroups;
        this.numberOfCaptureGroups = numberOfCaptureGroups;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public FrameSlot getInputFS() {
        return inputFS;
    }

    public FrameSlot getFromIndexFS() {
        return fromIndexFS;
    }

    public FrameSlot getIndexFS() {
        return indexFS;
    }

    public FrameSlot getMaxIndexFS() {
        return maxIndexFS;
    }

    public FrameSlot getCurMaxIndexFS() {
        return curMaxIndexFS;
    }

    public FrameSlot getSuccessorIndexFS() {
        return successorIndexFS;
    }

    public FrameSlot getResultFS() {
        return resultFS;
    }

    public FrameSlot getCaptureGroupResultFS() {
        return captureGroupResultFS;
    }

    public FrameSlot getLastTransitionFS() {
        return lastTransitionFS;
    }

    public FrameSlot getCgDataFS() {
        return cgDataFS;
    }

    public boolean isForward() {
        return forward;
    }

    public boolean isSearching() {
        return searching;
    }

    public boolean isTrackCaptureGroups() {
        return trackCaptureGroups;
    }

    public int getNumberOfCaptureGroups() {
        return numberOfCaptureGroups;
    }
}
