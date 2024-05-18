/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public class SimpleArrayListTest {

    @Test
    public void testSimple() {
        SimpleArrayList<Integer> l = new SimpleArrayList<>();
        assertEquals(0, l.size());
        l.add(42, null, InlinedBranchProfile.getUncached());
        l.add(43, null, InlinedBranchProfile.getUncached());
        l.add(44, null, InlinedBranchProfile.getUncached());
        assertEquals(3, l.size());
        assertEquals(42, (int) l.get(0));
        assertEquals(43, (int) l.get(1));
        assertEquals(44, (int) l.get(2));
    }

    @Test
    public void testHuge() {
        SimpleArrayList<Integer> l = new SimpleArrayList<>();
        int n = 1000000;
        for (int i = 0; i < n; i++) {
            l.add(i, null, InlinedBranchProfile.getUncached());
        }
        assertEquals(n, l.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i, (int) l.get(i));
        }
    }

    @Test
    public void testUnchecked() {
        SimpleArrayList<Integer> l = new SimpleArrayList<>(3);
        assertEquals(0, l.size());
        l.addUnchecked(42);
        l.addUnchecked(43);
        l.addUnchecked(44);
        assertEquals(3, l.size());
        assertEquals(42, (int) l.get(0));
        assertEquals(43, (int) l.get(1));
        assertEquals(44, (int) l.get(2));

        try {
            l.addUnchecked(44);
            Assert.fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof ArrayIndexOutOfBoundsException);
        }
    }

    /**
     * Tests whether growing also works in corner cases (capacity of 0 or 1).
     */
    @Test
    public void testGrow() {
        SimpleArrayList<Integer> l = new SimpleArrayList<>(0);
        assertEquals(0, l.size());
        l.add(42, null, InlinedBranchProfile.getUncached());
        assertEquals(1, l.size());

        l = new SimpleArrayList<>(1);
        assertEquals(0, l.size());
        l.add(42, null, InlinedBranchProfile.getUncached());
        assertEquals(1, l.size());
    }

}
