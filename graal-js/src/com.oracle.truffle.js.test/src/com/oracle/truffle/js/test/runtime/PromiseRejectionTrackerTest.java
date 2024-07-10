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
package com.oracle.truffle.js.test.runtime;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;
import static org.junit.Assert.assertEquals;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.PromiseRejectionTracker;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.test.JSTest;

public class PromiseRejectionTrackerTest {
    private Context ctx;

    @Before
    public void setup() {
        ctx = JSTest.newContextBuilder().build();
    }

    @After
    public void tearDown() {
        ctx.close();
    }

    @Test
    public void testBasic() {
        JSContext context = JavaScriptLanguage.getJSContext(ctx);
        final int[] rejected = new int[1];
        final int[] handled = new int[1];
        final int[] rejectAfterResolve = new int[1];
        final int[] resolveAfterResolve = new int[1];
        context.setPromiseRejectionTracker(new PromiseRejectionTracker() {
            @Override
            public void promiseRejected(JSDynamicObject promise, Object value) {
                rejected[0]++;
            }

            @Override
            public void promiseRejectionHandled(JSDynamicObject promise) {
                handled[0]++;
            }

            @Override
            public void promiseRejectedAfterResolved(JSDynamicObject promise, Object value) {
                rejectAfterResolve[0]++;
            }

            @Override
            public void promiseResolvedAfterResolved(JSDynamicObject promise, Object value) {
                resolveAfterResolve[0]++;
            }
        });

        ctx.eval(ID, "Promise.resolve().then(() => { throw new Error(); }).catch(() => {})");
        assertEquals(0, rejected[0]);
        assertEquals(0, handled[0]);

        Value promise1 = ctx.eval(ID, "Promise.reject(42)");
        assertEquals(1, rejected[0]);

        Value promise2 = ctx.eval(ID, "Promise.resolve('foo')");
        assertEquals(1, rejected[0]);

        Value promise3 = ctx.eval(ID, "new Promise((resolve, reject) => resolve('bar'))");
        assertEquals(1, rejected[0]);

        Value promise4 = ctx.eval(ID, "new Promise((resolve, reject) => reject('baz'))");
        assertEquals(2, rejected[0]);

        Value promise5 = ctx.eval(ID, "new Promise((resolve, reject) => { throw new Error() })");
        assertEquals(3, rejected[0]);
        assertEquals(0, handled[0]);

        Value catchAll = ctx.eval(ID, "promise => promise.catch(() => {})");
        catchAll.execute(promise1);
        assertEquals(1, handled[0]);

        catchAll.execute(promise2);
        assertEquals(1, handled[0]);

        catchAll.execute(promise3);
        assertEquals(1, handled[0]);

        catchAll.execute(promise4);
        assertEquals(2, handled[0]);

        catchAll.execute(promise5);
        assertEquals(3, handled[0]);

        catchAll.execute(promise5);
        assertEquals(3, handled[0]);
        assertEquals(3, rejected[0]);

        assertEquals(0, rejectAfterResolve[0]);
        assertEquals(0, resolveAfterResolve[0]);
        ctx.eval(ID, "new Promise((resolve, reject) => { resolve('foo'); resolve('bar'); })");
        assertEquals(0, rejectAfterResolve[0]);
        assertEquals(1, resolveAfterResolve[0]);

        ctx.eval(ID, "new Promise((resolve, reject) => { resolve('foo'); reject('bar'); })");
        assertEquals(1, rejectAfterResolve[0]);
        assertEquals(1, resolveAfterResolve[0]);

        ctx.eval(ID, "new Promise((resolve, reject) => { reject('foo'); reject('bar'); })");
        assertEquals(2, rejectAfterResolve[0]);
        assertEquals(1, resolveAfterResolve[0]);

        ctx.eval(ID, "new Promise((resolve, reject) => { reject('foo'); resolve('bar'); })");
        assertEquals(2, rejectAfterResolve[0]);
        assertEquals(2, resolveAfterResolve[0]);
    }

}
