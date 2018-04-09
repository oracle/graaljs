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
