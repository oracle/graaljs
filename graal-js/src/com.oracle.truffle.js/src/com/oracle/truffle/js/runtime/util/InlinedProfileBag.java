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

import java.util.Arrays;
import java.util.Objects;

import com.oracle.truffle.api.dsl.InlineSupport.InlineTarget;
import com.oracle.truffle.api.dsl.InlineSupport.IntField;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;

/**
 * Helper interface for {@link com.oracle.truffle.api.profiles.InlinedProfile inlined profile} bags.
 */
public interface InlinedProfileBag {

    final class Builder extends InlinedProfileBuilder implements AutoCloseable {

        private final InlineTarget inlineTarget;
        private final int[] stateFieldBits;
        private int stateFieldIndex;
        private int intFieldIndex;

        /**
         * Allocates a new inlined profile builder for the given inline target and state field bits.
         */
        public Builder(InlineTarget inlineTarget, int... requiredStateBits) {
            super(null, 0, 0);
            this.inlineTarget = Objects.requireNonNull(inlineTarget);
            this.stateFieldBits = requiredStateBits;
            this.stateFieldIndex = -1;
            this.intFieldIndex = requiredStateBits.length;
        }

        /**
         * Allocates a new builder for uncached profiles.
         */
        public Builder() {
            super();
            this.inlineTarget = null;
            this.stateFieldBits = null;
        }

        @Override
        protected void advanceStateField(int bits) {
            do {
                stateFieldOffset = 0;
                stateFieldIndex += 1;
                stateFieldLength = stateFieldBits[stateFieldIndex];
                stateField = inlineTarget.getState(stateFieldIndex, stateFieldLength);
            } while (stateFieldLength < bits);
        }

        /**
         * Adds and returns a new {@link InlinedCountingConditionProfile}.
         */
        public InlinedCountingConditionProfile countingConditionProfile() {
            if (isUncached()) {
                return InlinedCountingConditionProfile.getUncached();
            }
            return InlinedCountingConditionProfile.inline(InlineTarget.create(InlinedCountingConditionProfile.class,
                            inlineTarget.getPrimitive(intFieldIndex++, IntField.class),
                            inlineTarget.getPrimitive(intFieldIndex++, IntField.class)));
        }

        /**
         * Asserts that the used bit count is in sync with the expected required state bits.
         */
        @Override
        public void close() {
            assert isUncached() || totalUsedBits == Arrays.stream(stateFieldBits).sum() : totalUsedBits;
        }
    }
}
