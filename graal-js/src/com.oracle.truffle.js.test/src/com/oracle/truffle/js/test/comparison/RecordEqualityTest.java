/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.comparison;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSSimpleTest;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecordEqualityTest extends JSSimpleTest {

    public RecordEqualityTest() {
        super("record-equality-test");
        addOption(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022");
    }

    @Test
    public void testEqual() {
        assertTrue(execute("#{ a: 1 } == #{ a: 1 }").asBoolean());
        assertFalse(execute("#{ a: 1 } == #{}").asBoolean());

        assertTrue(execute("#{ a: -0 } == #{ a: +0 }").asBoolean());
        assertTrue(execute("#{ a: NaN } == #{ a: NaN }").asBoolean());

        assertFalse(execute("#{ a: 0 } == #{ a: '0' }").asBoolean());
        assertFalse(execute("#{ a: 0 } == #{ a: false }").asBoolean());
        assertFalse(execute("#{ a: '' } == #{ a: false }").asBoolean());
    }

    @Test
    public void testIdentical() {
        assertTrue(execute("#{ a: 1 } === #{ a: 1 }").asBoolean());
        assertFalse(execute("#{ a: 1 } === #{}").asBoolean());

        assertTrue(execute("#{ a: -0 } === #{ a: +0 }").asBoolean());
        assertTrue(execute("#{ a: NaN } === #{ a: NaN }").asBoolean());

        assertFalse(execute("#{ a: 0 } === #{ a: '0' }").asBoolean());
        assertFalse(execute("#{ a: 0 } === #{ a: false }").asBoolean());
        assertFalse(execute("#{ a: '' } === #{ a: false }").asBoolean());
    }

    @Test
    public void testSameValue() {
        assertTrue(execute("Object.is(#{ a: 1 }, #{ a: 1 })").asBoolean());
        assertFalse(execute("Object.is(#{ a: 1 }, #{})").asBoolean());

        assertFalse(execute("Object.is(#{ a: -0 }, #{ a: +0 })").asBoolean());
        assertTrue(execute("Object.is(#{ a: NaN }, #{ a: NaN })").asBoolean());

        assertFalse(execute("Object.is(#{ a: 0 }, #{ a: '0' })").asBoolean());
        assertFalse(execute("Object.is(#{ a: 0 }, #{ a: false })").asBoolean());
        assertFalse(execute("Object.is(#{ a: '' }, #{ a: false })").asBoolean());
    }

    @Test
    public void testSameValueZero() {
        assertTrue(execute("Array.of(#{ a: 1 }).includes(#{ a: 1 })").asBoolean());
        assertFalse(execute("Array.of(#{ a: 1 }).includes(#{})").asBoolean());

        assertTrue(execute("Array.of(#{ a: -0 }).includes(#{ a: +0 })").asBoolean());
        assertTrue(execute("Array.of(#{ a: NaN }).includes(#{ a: NaN })").asBoolean());

        assertFalse(execute("Array.of(#{ a: 0 }).includes(#{ a: '0' })").asBoolean());
        assertFalse(execute("Array.of(#{ a: 0 }).includes(#{ a: false })").asBoolean());
        assertFalse(execute("Array.of(#{ a: '' }).includes(#{ a: false })").asBoolean());
    }

    @Test
    public void testJSHashMap() {
        // Map/Set keys are compared using the SameValueZero algorithm, but the actual graal-js implementations differs
        // as keys are being normalized (see JSCollectionsNormalizeNode) before accessing the internal JSHashMap.
        assertTrue(execute("new Map().set(#{ a: -0 }, true).get(#{ a: 0 }) === true").asBoolean());
        assertTrue(execute("new Set().add(#{ a: -0 }).has(#{ a: 0 })").asBoolean());
    }
}
