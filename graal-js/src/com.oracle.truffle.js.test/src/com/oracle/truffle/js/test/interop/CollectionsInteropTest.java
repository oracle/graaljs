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
package com.oracle.truffle.js.test.interop;

import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.ForeignBoxedObject;
import com.oracle.truffle.js.test.polyglot.ForeignDynamicObject;
import com.oracle.truffle.js.test.polyglot.ForeignTestMap;

public class CollectionsInteropTest {

    private static void checkNormalization(Object object, Object normalizedObject) {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value fn = context.eval(JavaScriptLanguage.ID, "" +
                            "(function(object, normalizedObject) {\n" +
                            "    var set = new Set();\n" +
                            "    set.add(object);\n" +
                            "    return set.has(object) && set.has(normalizedObject);\n" +
                            "})");
            Value result = fn.execute(object, normalizedObject);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testBoxedNumberNormalization() {
        checkNormalization(ForeignBoxedObject.createNew(42), 42);
        checkNormalization(ForeignBoxedObject.createNew(4.2), 4.2);
    }

    @Test
    public void testBoxedBooleanNormalization() {
        checkNormalization(ForeignBoxedObject.createNew(true), true);
        checkNormalization(ForeignBoxedObject.createNew(false), false);
    }

    @Test
    public void testBoxedStringNormalization() {
        checkNormalization(ForeignBoxedObject.createNew("foo"), "foo");
    }

    @Test
    public void testBoxedNullNormalization() {
        ForeignTestMap object = new ForeignTestMap();
        object.getContainer().put("IS_NULL", true);
        checkNormalization(object, Null.instance);
    }

    @Test
    public void testForeignDynamicObjectNormalization() {
        DynamicObject object = new ForeignDynamicObject();
        checkNormalization(object, object);
    }

    @Test
    public void testHostObjectNormalization() {
        Object object = new java.awt.Point(42, 211);
        checkNormalization(object, object);
    }

}
