/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;

public class ForeignTestMapTest extends JSTest {

    @Test
    public void equalTest() {
        ForeignTestMap a = new ForeignTestMap();
        a.getContainer().put("BOX", "boxed_A");
        ForeignTestMap a2 = new ForeignTestMap();
        a2.getContainer().put("BOX", "boxed_A");
        ForeignTestMap b = new ForeignTestMap();
        b.getContainer().put("BOX", "boxed_B");

        assertTrue(JSRuntime.equal(a, a));
        assertTrue(JSRuntime.equal(a, a2));
        assertTrue(JSRuntime.equal(a2, a2));

        assertFalse(JSRuntime.equal(a, b));
        assertFalse(JSRuntime.equal(a2, b));
        assertFalse(JSRuntime.equal(a, 0));
        assertFalse(JSRuntime.equal(a, true));
    }

    @Test
    public void foreignTestMapTests() {
        ForeignTestMap map = new ForeignTestMap();
        map.getContainer().put("value", "boxed");

        testHelper.getPolyglotContext().getBindings("js").putMember("foreign", map);

        assertEquals("boxed", testHelper.run("foreign.value"));
        assertEquals(42, testHelper.run("foreign.othervalue=42"));
        assertEquals(42, testHelper.run("foreign.othervalue"));
        assertEquals(true, testHelper.run("delete foreign.othervalue"));
        assertEquals(true, testHelper.run("foreign.othervalue === undefined"));
        assertEquals(84, testHelper.run("foreign.fn = (a)=>{ return a+a; }; foreign.fn(42);"));

        assertEquals(true, testHelper.run("foreign.length=10; foreign[0]=42; foreign[1]=41; Array.prototype.sort.call(foreign); foreign[0]===41 && foreign[1]===42 && foreign[2]===undefined;"));
    }
}
