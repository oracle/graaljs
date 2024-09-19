/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.interop;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.test.JSTest;

/**
 * Tests of map iteration combined with entry deletion.
 */
@RunWith(Parameterized.class)
public class GR58285 {
    private Context context;

    @Parameters(name = "{0}")
    public static List<Boolean> data() {
        return List.of(Boolean.FALSE, Boolean.TRUE);
    }

    @Parameter(value = 0) public boolean mapWithTwoElements;

    @Before
    public void setUp() {
        context = JSTest.newContextBuilder().build();
    }

    @After
    public void tearDown() {
        context.close();
    }

    private Value createMap() {
        Value map = context.eval(ID, "var map = new Map(); map.set('foo', 'bar'); map");
        if (mapWithTwoElements) {
            context.eval(ID, "map.set(42, 211);");
        }
        return map;
    }

    private void testNextElement(Value iterator) {
        assertTrue(iterator.hasIteratorNextElement());
        context.eval(ID, "map.delete('foo')");
        try {
            iterator.getIteratorNextElement();
            assert mapWithTwoElements : "NoSuchElementException expected";
        } catch (NoSuchElementException nsex) {
            assert !mapWithTwoElements;
        }
    }

    private void testHasElement(Value iterator) {
        assertTrue(iterator.hasIteratorNextElement());
        context.eval(ID, "map.delete('foo')");
        assertTrue(iterator.hasIteratorNextElement() == mapWithTwoElements);
    }

    @Test
    public void testNextElementEntries() {
        testNextElement(createMap().getHashEntriesIterator());
    }

    @Test
    public void testNextElementKeys() {
        testNextElement(createMap().getHashKeysIterator());
    }

    @Test
    public void testNextElementValues() {
        testNextElement(createMap().getHashValuesIterator());
    }

    @Test
    public void testHasElementEntries() {
        testHasElement(createMap().getHashEntriesIterator());
    }

    @Test
    public void testHasElementKeys() {
        testHasElement(createMap().getHashKeysIterator());
    }

    @Test
    public void testHasElementValues() {
        testHasElement(createMap().getHashValuesIterator());
    }

}
