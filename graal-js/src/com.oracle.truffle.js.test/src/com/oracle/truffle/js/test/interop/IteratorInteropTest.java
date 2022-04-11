/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.test.JSTest;

public class IteratorInteropTest {

    @Test
    public void testIterator() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value iterable = context.eval(ID, "[41, 42, 43]");
            assertTrue(iterable.hasIterator());
            Value iterator = iterable.getIterator();
            assertTrue(iterator.isIterator());

            assertTrue(iterator.hasIteratorNextElement());
            assertTrue(iterator.hasIteratorNextElement());
            assertEquals(41, iterator.getIteratorNextElement().asInt());
            assertTrue(iterator.hasIteratorNextElement());
            assertEquals(42, iterator.getIteratorNextElement().asInt());
            assertEquals(43, iterator.getIteratorNextElement().asInt());

            assertFalse(iterator.hasIteratorNextElement());
            assertThrows(() -> iterator.getIteratorNextElement(), NoSuchElementException.class);
            assertFalse(iterator.hasIteratorNextElement());
        }
    }

    @Test
    public void testIteratorDelegation() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value iterable = context.eval(ID, "[41, 42, 43]");
            assertTrue(iterable.hasIterator());
            Value iterator = iterable.getIterator();
            assertTrue(iterator.isIterator());

            assertTrue(iterator.hasMembers());
            assertTrue(iterator.hasMember("next"));
            assertFalse(iterator.hasMember("hasNext"));

            assertJSIteratorNext(iterator, context.asValue(41));

            assertEquals(42, iterator.getIteratorNextElement().asInt());
            assertEquals(43, iterator.getIteratorNextElement().asInt());

            assertJSIteratorDone(iterator);

            assertEquals("[object Array Iterator]", iterator.invokeMember("toString").asString());
        }
    }

    @Test
    public void testIteratorDesync() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value iterable = context.eval(ID, "[41, 42, 43]");
            assertTrue(iterable.hasIterator());
            Value iterator = iterable.getIterator();
            assertTrue(iterator.isIterator());

            assertJSIteratorNext(iterator, context.asValue(41));

            assertTrue(iterator.hasIteratorNextElement());

            assertJSIteratorNext(iterator, context.asValue(43));

            assertJSIteratorDone(iterator);

            assertTrue(iterator.hasIteratorNextElement());
            assertEquals(42, iterator.getIteratorNextElement().asInt());
            assertFalse(iterator.hasIteratorNextElement());
        }
    }

    @Test
    public void testInvalidIteratorResult() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value iterable = context.eval(ID, "({ [Symbol.iterator]() {return { next() {return 42;} };} })");
            assertTrue(iterable.hasIterator());
            Value badIterator = iterable.getIterator();
            assertTrue(badIterator.isIterator());

            assertThrowsTypeError(() -> badIterator.hasIteratorNextElement());
            assertThrowsTypeError(() -> badIterator.getIteratorNextElement());
        }
    }

    @Test
    public void testInvalidIterator() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value badIterable1 = context.eval(ID, "({ [Symbol.iterator]() {return { next: 42 };} })");
            assertTrue(badIterable1.hasIterator());
            assertThrowsTypeError(() -> badIterable1.getIterator());

            Value badIterable2 = context.eval(ID, "({ [Symbol.iterator]() {return {};} })");
            assertTrue(badIterable2.hasIterator());
            assertThrowsTypeError(() -> badIterable2.getIterator());

            Value badIterable3 = context.eval(ID, "({ [Symbol.iterator]() {return 42;} })");
            assertTrue(badIterable3.hasIterator());
            assertThrowsTypeError(() -> badIterable3.getIterator());
        }
    }

    @Test
    public void testUnsupportedGetIterator() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value object = context.eval(ID, "({})");
            assertFalse(object.hasIterator());
            assertThrows(() -> object.getIterator(), UnsupportedOperationException.class);
        }
    }

    private static void assertJSIteratorNext(Value iterator, Value expectedValue) {
        Value iterResult = iterator.invokeMember("next");
        assertTrue(iterResult.hasMembers());
        assertNotNull(iterResult.getMember("done"));
        assertFalse(iterResult.getMember("done").asBoolean());
        assertNotNull(iterResult.getMember("value"));
        assertEquals(expectedValue, iterResult.getMember("value"));
    }

    private static void assertJSIteratorDone(Value iterator) {
        Value iterResult = iterator.invokeMember("next");
        assertTrue(iterResult.hasMembers());
        assertNotNull(iterResult.getMember("done"));
        assertTrue(iterResult.getMember("done").asBoolean());
        assertNotNull(iterResult.getMember("value"));
        assertTrue(iterResult.getMember("value").isNull());
    }

    @Test
    public void testUncachedGetIterator() throws UnsupportedMessageException, UnknownIdentifierException, StopIterationException {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(ID, "a = [41, 42, 43]");

            context.enter();
            JSDynamicObject globalObject = JavaScriptLanguage.getJSRealm(context).getGlobalObject();
            Object iterable = InteropLibrary.getUncached(globalObject).readMember(globalObject, "a");
            Object iterator = InteropLibrary.getUncached(iterable).getIterator(iterable);
            Object nextElement;
            nextElement = InteropLibrary.getUncached(iterator).getIteratorNextElement(iterator);
            assertEquals(41, nextElement);
            nextElement = InteropLibrary.getUncached(iterator).getIteratorNextElement(iterator);
            assertEquals(42, nextElement);
            context.leave();
        }
    }

    private static void assertThrows(Runnable runnable, Class<? extends Throwable> expectedException) {
        try {
            runnable.run();
            fail("Expected " + expectedException.getName());
        } catch (Throwable e) {
            assertTrue("Expected " + expectedException.getName() + ", caught " + e.getClass().getName(), expectedException.isInstance(e));
        }
    }

    private static void assertThrowsTypeError(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected TypeError");
        } catch (PolyglotException e) {
            assertTrue(e.isGuestException());
            assertTrue(e.getMessage().startsWith("TypeError"));
        }
    }

}
