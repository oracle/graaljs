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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.InlineSupport;
import com.oracle.truffle.api.dsl.InlineSupport.StateField;
import com.oracle.truffle.api.nodes.Node;

/**
 * A collections of {@link com.oracle.truffle.api.profiles.InlinedProfile inlined profiles}.
 */
public abstract class InlinedProfileBag {

    protected static final int BRANCH_PROFILE_STATE_BITS = 1;
    protected static final int CONDITION_PROFILE_STATE_BITS = 2;

    private final InlineSupport.StateField state;

    public static final class Builder extends InlinedProfileBuilder implements AutoCloseable {

        public Builder(int length) {
            this(0, length);
        }

        public Builder(int offset, int length) {
            super(offset, length);
        }

        /**
         * Asserts that the used bit count is in sync with the expected required state bits.
         */
        @Override
        public void close() {
            assert stateFieldCursor == stateFieldStart + stateFieldLength : stateFieldCursor;
        }

    }

    protected InlinedProfileBag(StateField state) {
        this.state = state;
    }

    protected final boolean profile(Node node, boolean value, int offset) {
        if (state != null) {
            int s = this.state.get(node);
            if (value) {
                int trueBit = 0b01 << offset;
                if ((s & trueBit) == 0) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    state.set(node, s | trueBit);
                }
                return true;
            } else {
                int falseBit = 0b10 << offset;
                if ((s & falseBit) == 0) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    state.set(node, s | falseBit);
                }
                return false;
            }
        }
        return value;
    }

    protected final void enter(Node node, int offset) {
        if (state == null) {
            return;
        }
        int branchBit = 0b1 << offset;
        int s = state.get(node);
        if ((s & branchBit) == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            state.set(node, s | branchBit);
        }
    }
}
