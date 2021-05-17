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
package com.oracle.truffle.js.test.runtime.builtins;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.Tuple;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSTuple;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JSTupleTest extends JSTest {

    private static final Tuple EMPTY_TUPLE = Tuple.EMPTY_TUPLE;
    private static final Tuple SIMPLE_TUPLE = Tuple.create(new Object[]{1, 2, 3});

    @Override
    public void setup() {
        super.setup();
        testHelper = new TestHelper(newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022"));
        testHelper.enterContext();
    }

    @Override
    public void close() {
        testHelper.leaveContext();
        super.close();
    }

    @Test
    public void testIsExtensible() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, EMPTY_TUPLE);
        assertFalse(JSObject.isExtensible(obj));
    }

    @Test
    public void testGetOwnProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        PropertyDescriptor desc = JSObject.getOwnProperty(obj, "1");
        assertTrue(desc.hasValue() && desc.hasWritable() && desc.hasEnumerable() && desc.hasConfigurable());
        assertEquals(2, desc.getValue());
        assertFalse(desc.getWritable());
        assertTrue(desc.getEnumerable());
        assertFalse(desc.getConfigurable());

        assertNull(JSObject.getOwnProperty(obj, "3"));
        assertNull(JSObject.getOwnProperty(obj, "-0.0"));

        assertNull(JSObject.getOwnProperty(obj, "foo"));
        assertNull(JSObject.getOwnProperty(obj, Symbol.create("foo")));
    }

    @Test
    public void testDefineOwnProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        assertTrue(JSObject.defineOwnProperty(obj, "1", PropertyDescriptor.createData(2, true, false, false)));
        assertFalse(JSObject.defineOwnProperty(obj, "1", PropertyDescriptor.createData(2, true, true, false)));

        assertTrue(JSObject.defineOwnProperty(obj, "1", PropertyDescriptor.createData(2)));
        assertFalse(JSObject.defineOwnProperty(obj, "1", PropertyDescriptor.createData(0)));
        assertFalse(JSObject.defineOwnProperty(obj, "3", PropertyDescriptor.createData(0)));
        assertFalse(JSObject.defineOwnProperty(obj, "foo", PropertyDescriptor.createData(0)));
        assertFalse(JSObject.defineOwnProperty(obj, "-0.0", PropertyDescriptor.createData(1)));

        assertFalse(JSObject.defineOwnProperty(obj, Symbol.create("foo"), PropertyDescriptor.createData(0)));

        assertFalse(JSObject.defineOwnProperty(obj, "1", PropertyDescriptor.createAccessor(Undefined.instance, Undefined.instance)));
    }

    @Test
    public void testHasProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        assertTrue(JSObject.hasProperty(obj, 1L));
        assertTrue(JSObject.hasProperty(obj, "1"));
        assertFalse(JSObject.hasProperty(obj, 3L));
        assertFalse(JSObject.hasProperty(obj, "3"));

        assertTrue(JSObject.hasProperty(obj, "length"));
        assertTrue(JSObject.hasProperty(obj, "with"));
        assertFalse(JSObject.hasProperty(obj, "foo"));
        assertFalse(JSObject.hasProperty(obj, Symbol.create("foo")));
    }

    @Test
    public void testGet() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        assertEquals(1, JSObject.get(obj, 0L));
        assertEquals(1, JSObject.get(obj, "0"));
        assertEquals(Undefined.instance, JSObject.get(obj, "-0"));
        assertEquals(Undefined.instance, JSObject.get(obj, "3"));

        assertEquals(3L, JSObject.get(obj, "length"));
        assertTrue(JSFunction.isJSFunction(JSObject.get(obj, "with")));
        assertEquals(Undefined.instance, JSObject.get(obj, "foo"));
    }

    @Test
    public void testSet() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        assertFalse(JSObject.set(obj, 1L, Integer.valueOf(0)));
        assertFalse(JSObject.set(obj, "1", 0));
        assertFalse(JSObject.set(obj, "3", 0));

        assertFalse(JSObject.set(obj, "length", 0));
        assertFalse(JSObject.set(obj, "with", 0));
        assertFalse(JSObject.set(obj, "foo", 0));
    }

    @Test
    public void testDelete() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        assertFalse(JSObject.delete(obj, 1L));
        assertFalse(JSObject.delete(obj, "1"));
        assertTrue(JSObject.delete(obj, 3L));
        assertTrue(JSObject.delete(obj, "3"));

        assertFalse(JSObject.delete(obj, "0"));
        assertTrue(JSObject.delete(obj, "-0"));

        assertTrue(JSObject.delete(obj, "foo"));
        assertTrue(JSObject.delete(obj, Symbol.create("foo")));
    }

    @Test
    public void testOwnPropertyKeys() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSTuple.create(context, SIMPLE_TUPLE);

        List<Object> keys = JSObject.ownPropertyKeys(obj);
        assertEquals(3, keys.size());
        assertEquals("0", keys.get(0));
        assertEquals("1", keys.get(1));
        assertEquals("2", keys.get(2));

        obj = JSTuple.create(context, EMPTY_TUPLE);
        keys = JSObject.ownPropertyKeys(obj);
        assertEquals(0, keys.size());
    }

    @Test
    public void testPrototype() {
        DynamicObject constructor = testHelper.getRealm().getTupleConstructor();
        DynamicObject prototype = testHelper.getRealm().getTuplePrototype();

        PropertyDescriptor desc = JSObject.getOwnProperty(constructor, "prototype");
        assertFalse(desc.getWritable());
        assertFalse(desc.getEnumerable());
        assertFalse(desc.getConfigurable());

        desc = JSObject.getOwnProperty(prototype, Symbol.SYMBOL_TO_STRING_TAG);
        assertFalse(desc.getWritable());
        assertFalse(desc.getEnumerable());
        assertTrue(desc.getConfigurable());
        assertEquals(desc.getValue(), "Tuple");

        desc = JSObject.getOwnProperty(prototype, Symbol.SYMBOL_ITERATOR);
        assertTrue(desc.getWritable());
        assertFalse(desc.getEnumerable());
        assertTrue(desc.getConfigurable());
        assertEquals(JSObject.get(prototype, "values"), desc.getValue());
    }
}
