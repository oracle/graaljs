/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.oracle.truffle.regex.tregex.nodes;

public final class DFACaptureGroupTrackingData {

    public final int[][] results;
    public final int[] currentResultOrder;
    public final int[] swap;

    public DFACaptureGroupTrackingData(int maxNumberOfNFAStates, int numberOfCaptureGroups) {
        results = new int[maxNumberOfNFAStates][numberOfCaptureGroups * 2];
        currentResultOrder = new int[maxNumberOfNFAStates];
        swap = new int[maxNumberOfNFAStates];
    }
}
