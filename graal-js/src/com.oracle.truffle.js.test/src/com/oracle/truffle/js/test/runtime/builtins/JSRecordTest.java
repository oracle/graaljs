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
import com.oracle.truffle.js.runtime.Record;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JSRecordTest extends JSTest {

    private static final Record EMPTY_RECORD = Record.create(Collections.emptyMap());
    private static final Record SIMPLE_RECORD = Record.create(mapOf("id", 1, "name", "John Doe"));

    private static Map<String, Object> mapOf(Object... data) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 1; i < data.length; i = i + 2) {
            map.put((String) data[i - 1], data[i]);
        }
        return map;
    }

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
        DynamicObject obj = JSRecord.create(context, EMPTY_RECORD);
        assertFalse(JSObject.isExtensible(obj));
    }

    @Test
    public void testGetOwnProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        PropertyDescriptor desc = JSObject.getOwnProperty(obj, "id");
        assertTrue(desc.hasValue() && desc.hasWritable() && desc.hasEnumerable() && desc.hasConfigurable());
        assertEquals(1, desc.getValue());
        assertFalse(desc.getWritable());
        assertTrue(desc.getEnumerable());
        assertFalse(desc.getConfigurable());

        assertNull(JSObject.getOwnProperty(obj, "foo"));
        assertNull(JSObject.getOwnProperty(obj, Symbol.create("foo")));
    }

    @Test
    public void testDefineOwnProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        assertTrue(JSObject.defineOwnProperty(obj, "id", PropertyDescriptor.createData(1, true, false, false)));
        assertFalse(JSObject.defineOwnProperty(obj, "id", PropertyDescriptor.createData(1, true, true, false)));

        assertTrue(JSObject.defineOwnProperty(obj, "id", PropertyDescriptor.createData(1)));
        assertFalse(JSObject.defineOwnProperty(obj, "id", PropertyDescriptor.createData(0)));

        assertFalse(JSObject.defineOwnProperty(obj, "foo", PropertyDescriptor.createData(0)));
        assertFalse(JSObject.defineOwnProperty(obj, Symbol.create("foo"), PropertyDescriptor.createData(0)));

        assertFalse(JSObject.defineOwnProperty(obj, "id", PropertyDescriptor.createAccessor(Undefined.instance, Undefined.instance)));
    }

    @Test
    public void testHasProperty() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        assertTrue(JSObject.hasProperty(obj, "id"));
        assertTrue(JSObject.hasProperty(obj, "name"));
        assertFalse(JSObject.hasProperty(obj, "foo"));
        assertFalse(JSObject.hasProperty(obj, Symbol.create("foo")));
    }

    @Test
    public void testGet() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        assertEquals(1, JSObject.get(obj, "id"));
        assertEquals("John Doe", JSObject.get(obj, "name"));
        assertEquals(Undefined.instance, JSObject.get(obj, "foo"));
        assertEquals(Undefined.instance, JSObject.get(obj, Symbol.create("foo")));
    }

    @Test
    public void testSet() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        assertFalse(JSObject.set(obj, "id", 0));
        assertFalse(JSObject.set(obj, "name", 0));
        assertFalse(JSObject.set(obj, "name", "Larry Ellison"));
        assertFalse(JSObject.set(obj, 1, Undefined.instance));
    }

    @Test
    public void testDelete() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        assertFalse(JSObject.delete(obj, "id"));
        assertFalse(JSObject.delete(obj, "name"));
        assertTrue(JSObject.delete(obj, "foo"));
        assertTrue(JSObject.delete(obj, Symbol.create("foo")));
        assertTrue(JSObject.delete(obj, Symbol.create("id")));
        assertTrue(JSObject.delete(obj, 1L));
    }

    @Test
    public void testOwnPropertyKeys() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, SIMPLE_RECORD);

        List<Object> keys = JSObject.ownPropertyKeys(obj);
        assertEquals(2, keys.size());
        assertEquals("id", keys.get(0));
        assertEquals("name", keys.get(1));

        obj = JSRecord.create(context, EMPTY_RECORD);
        keys = JSObject.ownPropertyKeys(obj);
        assertEquals(0, keys.size());
    }

    @Test
    public void testPrototype() {
        JSContext context = testHelper.getJSContext();
        DynamicObject obj = JSRecord.create(context, EMPTY_RECORD);

        assertEquals(Null.instance, JSObject.getPrototype(obj));

        DynamicObject constructor = testHelper.getRealm().getRecordConstructor();

        PropertyDescriptor desc = JSObject.getOwnProperty(constructor, "prototype");
        assertFalse(desc.getWritable());
        assertFalse(desc.getEnumerable());
        assertFalse(desc.getConfigurable());
    }
}
