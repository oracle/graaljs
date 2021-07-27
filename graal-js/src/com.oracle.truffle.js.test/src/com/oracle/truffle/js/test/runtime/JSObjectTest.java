/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;

public class JSObjectTest extends JSTest {

    @Override
    public void setup() {
        super.setup();
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    private DynamicObject createOrdinaryObject() {
        return JSOrdinary.create(testHelper.getJSContext(), testHelper.getRealm());
    }

    @Test
    public void testSetGet() {
        DynamicObject obj = createOrdinaryObject();
        JSObject.set(obj, "x", 10);
        assertEquals(10, JSObject.get(obj, "x"));
        JSObject.set(obj, "y", 20);
        assertEquals(10, JSObject.get(obj, "x"));
        assertEquals(20, JSObject.get(obj, "y"));
    }

    @Test
    public void testRemove() {
        DynamicObject obj = createOrdinaryObject();
        JSObject.set(obj, "x", 10);
        JSObject.set(obj, "y", 20);
        assertEquals(10, JSObject.get(obj, "x"));
        assertEquals(20, JSObject.get(obj, "y"));
        assertEquals(2, JSObject.ownPropertyKeys(obj).size());

        JSObject.delete(obj, "x");
        assertEquals(20, JSObject.get(obj, "y"));
        assertEquals(1, JSObject.ownPropertyKeys(obj).size());
        assertEquals("y", JSObject.ownPropertyKeys(obj).get(0));
        assertEquals(false, JSObject.hasProperty(obj, "x"));
    }

    @Test
    public void testRemove2() {
        DynamicObject obj = createOrdinaryObject();
        JSObject.set(obj, "x", 10);
        JSObject.set(obj, "y", 20);
        assertEquals(10, JSObject.get(obj, "x"));
        assertEquals(20, JSObject.get(obj, "y"));
        assertEquals(2, JSObject.ownPropertyKeys(obj).size());

        JSObject.delete(obj, "x");
        assertEquals(20, JSObject.get(obj, "y"));
        assertEquals(1, JSObject.ownPropertyKeys(obj).size());
        assertEquals("y", JSObject.ownPropertyKeys(obj).get(0));
        assertEquals(false, JSObject.hasProperty(obj, "x"));

        JSObject.set(obj, "x", 11);
        assertEquals(11, JSObject.get(obj, "x"));
        assertEquals(20, JSObject.get(obj, "y"));
        assertEquals(2, JSObject.ownPropertyKeys(obj).size());
        assertTrue(JSObject.ownPropertyKeys(obj).contains("x") && JSObject.ownPropertyKeys(obj).contains("y"));
        assertEquals(true, JSObject.hasProperty(obj, "x"));
        assertEquals(true, JSObject.hasProperty(obj, "y"));

        JSObject.delete(obj, "x");
        JSObject.delete(obj, "x");

        JSObject.set(obj, "x", 12);
        JSObject.delete(obj, "y");
        JSObject.set(obj, "z", 13);

        JSObject.set(obj, "y", 21);
        assertEquals(3, JSObject.ownPropertyKeys(obj).size());
    }

    @Test
    public void propertyTest() {
        DynamicObject po = createOrdinaryObject();
        for (int i = 0; i < 10000; i++) {
            JSObject.set(po, String.valueOf(i), i);
        }
        boolean ok = true;
        for (int i = 0; i < 10000; i++) {
            ok = ok && (i == (Integer) JSObject.get(po, String.valueOf(i)));
        }
        assertTrue(ok);
    }
}
