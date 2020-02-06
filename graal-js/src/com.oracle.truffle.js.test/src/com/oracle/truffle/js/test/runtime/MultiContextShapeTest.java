/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertSame;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;

public class MultiContextShapeTest {

    @Test
    public void ordinaryObjectShape() {
        testSameShapeAcrossContexts("({key: 'value'});");
        testSameShapeAcrossContexts("Object.create(null);");
        testSameShapeAcrossContexts("Object.create({});");
    }

    @Test
    public void arrayShape() {
        testSameShapeAcrossContexts("[3,1,4,1,5,9,2,6,5,3,5,9]");
    }

    @Test
    public void promiseShape() {
        testSameShapeAcrossContexts("Promise.resolve(42);");
    }

    @Test
    public void functionShape() {
        testSameShapeAcrossContexts("(function() {});");
        testSameShapeAcrossContexts("function foo() {} foo;");
    }

    @Test
    public void argumentsShape() {
        testSameShapeAcrossContexts("(function() {return arguments;})();");
        testSameShapeAcrossContexts("(function() {'use strict'; return arguments;})();");
    }

    @Test
    public void classShape() {
        testSameShapeAcrossContexts("class C {} C;");
        testSameShapeAcrossContexts("class C extends null {} C;");
    }

    private static void testSameShapeAcrossContexts(String source) {
        try (Engine engine = JSTest.newEngineBuilder().build()) {
            Shape lastShape = null;
            for (int i = 0; i < 2; i++) {
                try (Context c = JSTest.newContextBuilder().engine(engine).build()) {
                    Value object = c.eval(ID, source);
                    DynamicObject jsobject = unwrapJSObject(c, object);
                    Shape objShape = jsobject.getShape();
                    if (lastShape != null) {
                        assertSame(lastShape, objShape);
                    }
                    lastShape = objShape;
                }
            }
        }
    }

    private static DynamicObject unwrapJSObject(Context c, Value value) {
        final String key = "_testObject";
        c.getBindings(ID).putMember(key, value);
        return (DynamicObject) JSObject.get(JavaScriptLanguage.getJSRealm(c).getGlobalObject(), key);
    }

}
