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
package com.oracle.truffle.js.test.regress;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GR30371 {
    @Test
    public void test01() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value arr = context.eval(JavaScriptLanguage.ID, "var a =[56363954];" + //
                            "a[15] = -708053262;" + //
                            "delete a[0];" + //
                            "a[15] = 1733912160;" + //
                            "a[14] = 2141293745;" + //
                            "a[13] = 1250715252;" + //
                            "delete a[17];" + //
                            "if(a[13] !== 1250715252) throw \"1250715252 expected\";" + //
                            "a;");

            assertEquals(16, arr.getArraySize());
            assertTrue(arr.hasArrayElements());
        }
    }

    @Test
    public void test02() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value arr = context.eval(JavaScriptLanguage.ID, "var a = new Array(-1111669833, -599160259, 1640048431, -1056923883, -25940143);" + //
                            "delete a[0];" + //
                            "a[1] = -1615921072;" + //
                            "delete a[8];" + //
                            "delete a[9];" + //
                            "if(a[4] !== -25940143) throw \"-25940143 expected\";" + //
                            "a;");

            assertTrue(arr.hasArrayElements());
        }
    }

    @Test
    public void test03() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value arr = context.eval(JavaScriptLanguage.ID, "var a = new Array(1,2);" + //
                            "delete a[0];" + //
                            "a[1] = 1889084888;" + //
                            "a[2] = 674859129;" + //
                            "a[3] = -670041941;" + //
                            "a[4] = 929814157;" + //
                            "a[5] = -877652534;" + //
                            "delete a[15];" + //
                            "if(a[5] !== -877652534) throw \"-877652534 expected\";" + //
                            "a;");

            assertTrue(arr.hasArrayElements());
        }
    }

    @Test
    public void test04() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value arr = context.eval(JavaScriptLanguage.ID, "var a = new Array(1,2,3);" + //
                            "delete a[0];" + //
                            "a[3] = -1789853761;" + //
                            "a[2] = 1;" + //
                            "a[24] = 2;" + //
                            "if(a[3] !== -1789853761) throw \"-1789853761 expected\";" + //
                            "a;");

            assertTrue(arr.hasArrayElements());
        }
    }
}
