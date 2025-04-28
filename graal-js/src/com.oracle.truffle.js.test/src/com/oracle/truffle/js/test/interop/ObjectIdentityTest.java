/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import com.oracle.truffle.js.nodes.interop.ExportValueNode;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.interop.InteropBoundFunction;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignNull;

public class ObjectIdentityTest extends ParameterizedInteropLibraryTest {

    @Test
    public void proxyObjectIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyObject proxy1 = ProxyObject.fromMap(Collections.singletonMap("key", 42));
            ProxyObject proxy2 = ProxyObject.fromMap(Collections.singletonMap("key", 42));

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertTrue(equals.execute(proxy1, proxy1).asBoolean());
            assertTrue(identical.execute(proxy1, proxy1).asBoolean());
            assertFalse(equals.execute(proxy1, proxy2).asBoolean());
            assertFalse(identical.execute(proxy1, proxy2).asBoolean());
        }
    }

    @Test
    public void mixedObjectIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyObject proxy = ProxyObject.fromMap(Collections.singletonMap("key", 42));
            Value jsobj = context.eval(ID, "({})");

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertFalse(equals.execute(proxy, jsobj).asBoolean());
            assertFalse(identical.execute(proxy, jsobj).asBoolean());
            assertFalse(equals.execute(jsobj, proxy).asBoolean());
            assertFalse(identical.execute(jsobj, proxy).asBoolean());

            assertTrue(equals.execute(proxy, proxy).asBoolean());
            assertTrue(identical.execute(proxy, proxy).asBoolean());
            assertTrue(equals.execute(jsobj, jsobj).asBoolean());
            assertTrue(identical.execute(jsobj, jsobj).asBoolean());
        }
    }

    @Test
    public void foreignNullIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object null1 = new ForeignNull();
            Object null2 = new ForeignNull();
            Value jsnull = context.eval(ID, "null");

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            assertTrue(equals.execute(null1, null1).asBoolean());
            assertTrue(identical.execute(null1, null1).asBoolean());
            assertTrue(equals.execute(null1, null2).asBoolean());
            assertTrue(identical.execute(null1, null2).asBoolean());

            assertTrue(equals.execute(null1, jsnull).asBoolean());
            assertTrue(identical.execute(null1, jsnull).asBoolean());
            assertTrue(equals.execute(jsnull, null1).asBoolean());
            assertTrue(identical.execute(jsnull, null1).asBoolean());
        }
    }

    @Test
    public void testForeignObjectWithoutIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object value1 = ForeignBoxedObject.createNew(42);
            Object value2 = ForeignBoxedObject.createNew(42);
            Object value3 = ForeignBoxedObject.createNew(43);

            InteropLibrary interopVal1 = createInteropLibrary(value1);
            InteropLibrary interopVal2 = createInteropLibrary(value2);

            // Numbers have no identity.
            assertFalse(interopVal1.isIdentical(value1, value1, interopVal1));
            assertFalse(interopVal1.isIdentical(value1, value2, interopVal2));
            assertFalse(interopVal2.isIdentical(value2, value1, interopVal1));

            Value equals = context.eval(ID, "(function(a, b){return a == b;})");
            Value identical = context.eval(ID, "(function(a, b){return a === b;})");

            // Value equality comparison
            assertTrue(equals.execute(value1, value1).asBoolean());
            assertTrue(equals.execute(value1, value2).asBoolean());
            assertFalse(equals.execute(value2, value3).asBoolean());

            assertTrue(equals.execute(value1, 42).asBoolean());

            assertTrue(identical.execute(value1, value1).asBoolean());
            assertTrue(identical.execute(value1, value2).asBoolean());
            assertFalse(identical.execute(value2, value3).asBoolean());

            assertTrue(identical.execute(value1, 42).asBoolean());
        }
    }

    @Test
    public void testJSObjectIdentity() {
        try (TestHelper testHelper = new TestHelper()) {
            Object jsobj1 = testHelper.runNoPolyglot("({})");
            Object jsobj2 = testHelper.runNoPolyglot("({})");

            InteropLibrary interopObj1 = createInteropLibrary(jsobj1);
            InteropLibrary interopObj2 = createInteropLibrary(jsobj2);

            assertTrue(interopObj1.isIdentical(jsobj1, jsobj1, interopObj1));
            assertFalse(interopObj1.isIdentical(jsobj1, jsobj2, interopObj2));
            assertFalse(interopObj2.isIdentical(jsobj2, jsobj1, interopObj1));
        }
    }

    @Test
    public void testJSErrorIdentity() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value error = context.eval(ID, "try { throw new Error('expected'); } catch (e) { e }");
            assertTrue(error.toString(), error.isException());
            try {
                error.throwException();
                fail("should have thrown");
            } catch (PolyglotException ex) {
                Value exception = ex.getGuestObject();
                assertTrue("Exception and Error object have the same identity", exception.equals(error));
            }
        }
    }

    @Test
    public void testInteropFunctionIdentity() {
        try (TestHelper testHelper = new TestHelper()) {
            testHelper.getPolyglotContext().enter();

            Object jsfun = testHelper.runNoPolyglot("(function(){})");
            Object jsobj = testHelper.runNoPolyglot("({})");
            ExportValueNode exportValueNode = ExportValueNode.getUncached();
            Object interopBoundFunction = exportValueNode.execute(jsfun, jsobj, true);

            InteropLibrary interopJsFun = createInteropLibrary(jsfun);
            InteropLibrary interopBound = createInteropLibrary(interopBoundFunction);

            assertTrue(interopBoundFunction.getClass().getName(), interopBoundFunction instanceof InteropBoundFunction);
            assertTrue(interopBound.isIdentical(interopBoundFunction, jsfun, interopJsFun));
            assertTrue(interopJsFun.isIdentical(jsfun, interopBoundFunction, interopBound));

            testHelper.getPolyglotContext().leave();
        }
    }

    @Test
    public void testThrownIdentitylessValue() {
        try (TestHelper testHelper = new TestHelper()) {
            testHelper.getPolyglotContext().enter();

            Object value = null;
            Object ex1 = null;
            Object ex2 = null;
            try {
                testHelper.runNoPolyglot("throw 42;");
                fail("should have thrown");
            } catch (UserScriptException ex) {
                value = ex.getErrorObject();
                ex1 = ex;
            }
            Object jsobj = testHelper.runNoPolyglot("({})");

            InteropLibrary interopVal = createInteropLibrary(value);
            InteropLibrary interopEx1 = createInteropLibrary(ex1);
            InteropLibrary interopObj = createInteropLibrary(jsobj);

            assertFalse(interopVal.hasIdentity(value));
            assertTrue(interopObj.hasIdentity(jsobj));
            assertFalse(interopEx1.isIdentical(ex1, jsobj, interopObj));
            assertFalse(interopObj.isIdentical(jsobj, ex1, interopEx1));
            assertFalse(interopEx1.isIdentical(ex1, value, interopVal));
            assertFalse(interopVal.isIdentical(value, ex1, interopEx1));

            try {
                testHelper.runNoPolyglot("throw 42;");
                fail("should have thrown");
            } catch (UserScriptException ex) {
                ex2 = ex;
                assertSame(value, ex.getErrorObject());
            }

            InteropLibrary interopEx2 = createInteropLibrary(ex2);

            assertNotSame(ex1, ex2);
            assertFalse(interopEx2.isIdentical(ex2, value, interopVal));
            assertFalse(interopVal.isIdentical(value, ex2, interopEx2));
            assertFalse(interopEx1.isIdentical(ex1, ex2, interopEx2));

            Object jserror = testHelper.runNoPolyglot("try { throw new Error('expected'); } catch (e) { e }");
            InteropLibrary interopErr = createInteropLibrary(jserror);
            assertFalse(interopErr.isIdentical(jserror, ex1, interopEx1));

            testHelper.getPolyglotContext().leave();
        }
    }

    @Test
    public void testOneIdentityTwoExceptions() {
        try (TestHelper testHelper = new TestHelper()) {
            testHelper.getPolyglotContext().enter();

            Object value = null;
            Object ex1 = null;
            Object ex2 = null;
            try {
                testHelper.runNoPolyglot("throw null;");
                fail("should have thrown");
            } catch (UserScriptException ex) {
                value = ex.getErrorObject();
                ex1 = ex;
            }

            InteropLibrary interopVal = createInteropLibrary(value);
            InteropLibrary interopEx1 = createInteropLibrary(ex1);

            assertTrue(interopVal.hasIdentity(value));
            assertTrue(interopEx1.isIdentical(ex1, value, interopVal));
            assertTrue(interopVal.isIdentical(value, ex1, interopEx1));

            try {
                testHelper.runNoPolyglot("throw null;");
                fail("should have thrown");
            } catch (UserScriptException ex) {
                ex2 = ex;
                assertSame(value, ex.getErrorObject());
            }

            InteropLibrary interopEx2 = createInteropLibrary(ex2);

            assertNotSame(ex1, ex2);
            assertTrue(interopEx2.isIdentical(ex2, value, interopVal));
            assertTrue(interopVal.isIdentical(value, ex2, interopEx2));
            assertTrue(interopEx1.isIdentical(ex1, ex2, interopEx2));

            Object jserror = testHelper.runNoPolyglot("try { throw new Error('expected'); } catch (e) { e }");
            InteropLibrary interopErr = createInteropLibrary(jserror);
            assertFalse(interopErr.isIdentical(jserror, ex1, interopEx1));

            testHelper.getPolyglotContext().leave();
        }
    }

    @Test
    public void testIdentityPreservingWrapper() {
        try (TestHelper testHelper = new TestHelper()) {
            testHelper.getPolyglotContext().enter();

            JSException err = null;
            try {
                testHelper.runNoPolyglot("undefined.error");
                fail("should have thrown");
            } catch (JSException ex) {
                err = ex;
            }
            // We want to explicitly test the lazy error object case.
            assertNull(err.getErrorObjectLazy());

            Object wrapper = new IdentityPreservingWrapper(err);

            InteropLibrary interopErr = createInteropLibrary(err);
            InteropLibrary interopWrapper = createInteropLibrary(wrapper);

            assertTrue(interopErr.isIdentical(err, wrapper, interopWrapper));
            assertTrue(interopErr.hasIdentity(err));
            assertTrue(interopWrapper.isIdentical(wrapper, err, interopErr));
            assertTrue(interopWrapper.hasIdentity(wrapper));

            testHelper.getPolyglotContext().leave();
        }
    }

    @Test
    public void testNoExplicitIdentityObject() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Object value = new NoExplicitIdentityObject();

            Value identical = context.eval(ID, "(function(a, b){return a === b;})");
            Value equals = context.eval(ID, "(function(a, b){return a == b;})");

            assertFalse(identical.execute(value, value).asBoolean());
            assertFalse(equals.execute(value, value).asBoolean());

            Value singletonArrayIncludes = context.eval(ID, "(function(x, y){return [x].includes(y);})");

            assertFalse(singletonArrayIncludes.execute(42, value).asBoolean());
            assertFalse(singletonArrayIncludes.execute(value, value).asBoolean());
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class IdentityPreservingWrapper implements TruffleObject {
        final Object original;

        IdentityPreservingWrapper(Object original) {
            this.original = original;
        }

        @ExportMessage
        @TruffleBoundary
        TriState isIdenticalOrUndefined(Object other,
                        @CachedLibrary("this.original") InteropLibrary thisLib,
                        @CachedLibrary(limit = "3") InteropLibrary otherLib) {
            if (this == other) {
                return TriState.TRUE;
            } else if (otherLib.hasIdentity(other)) {
                return TriState.valueOf(thisLib.isIdentical(original, other, otherLib));
            } else {
                return TriState.UNDEFINED;
            }
        }

        @ExportMessage
        @TruffleBoundary
        int identityHashCode(@CachedLibrary("this.original") InteropLibrary thisLib) throws UnsupportedMessageException {
            return thisLib.identityHashCode(original);
        }
    }

    static final class NoExplicitIdentityObject implements TruffleObject {
    }

}
