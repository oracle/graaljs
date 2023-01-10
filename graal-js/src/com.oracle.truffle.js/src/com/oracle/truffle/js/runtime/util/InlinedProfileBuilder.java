/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import com.oracle.truffle.api.dsl.InlineSupport.InlineTarget;
import com.oracle.truffle.api.dsl.InlineSupport.StateField;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

/**
 * Inlined profile builder.
 *
 * @see com.oracle.truffle.api.profiles.InlinedProfile
 */
public class InlinedProfileBuilder {

    private static final int BRANCH_PROFILE_STATE_BITS = 1;
    private static final int CONDITION_PROFILE_STATE_BITS = 2;

    protected StateField stateField;
    protected int stateFieldOffset;
    protected int stateFieldLength;
    protected int totalUsedBits;

    /**
     * Allocates a new inlined profile builder for a full stateField.
     */
    public InlinedProfileBuilder(StateField stateField) {
        this(stateField, 0, 32);
    }

    /**
     * Allocates a new inlined profile builder for a partial stateField.
     */
    public InlinedProfileBuilder(StateField stateField, int offset, int length) {
        this.stateField = stateField;
        this.stateFieldOffset = offset;
        this.stateFieldLength = length;
    }

    /**
     * Allocates a new builder for uncached profiles.
     */
    public InlinedProfileBuilder() {
    }

    protected final void maybeAdvanceStateField(int bits) {
        if (stateFieldOffset + bits > stateFieldLength) {
            advanceStateField(bits);
        }
    }

    protected void advanceStateField(@SuppressWarnings("unused") int bits) {
    }

    /**
     * Returns the next state subfield for the required number of bits.
     */
    public final StateField stateFieldForBits(int bits) {
        maybeAdvanceStateField(bits);
        StateField subField = stateField.subUpdater(stateFieldOffset, bits);
        stateFieldOffset += bits;
        totalUsedBits += bits;
        return subField;
    }

    /**
     * Adds and returns a new {@link InlinedConditionProfile}.
     */
    public final InlinedConditionProfile conditionProfile() {
        if (stateField == null) {
            return InlinedConditionProfile.getUncached();
        }
        return InlinedConditionProfile.inline(InlineTarget.create(InlinedConditionProfile.class, stateFieldForBits(CONDITION_PROFILE_STATE_BITS)));
    }

    /**
     * Adds and returns a new {@link InlinedBranchProfile}.
     */
    public final InlinedBranchProfile branchProfile() {
        if (stateField == null) {
            return InlinedBranchProfile.getUncached();
        }
        return InlinedBranchProfile.inline(InlineTarget.create(InlinedBranchProfile.class, stateFieldForBits(BRANCH_PROFILE_STATE_BITS)));
    }
}
