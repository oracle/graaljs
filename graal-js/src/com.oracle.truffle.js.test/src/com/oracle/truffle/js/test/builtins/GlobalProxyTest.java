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
package com.oracle.truffle.js.test.builtins;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;

import org.graalvm.polyglot.PolyglotException;
import org.junit.Test;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

/**
 * Tests for the global builtin.
 */
public class GlobalProxyTest {

    @Test
    public void testGlobalSetMutableBindingCheck() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (TestHelper helper = new TestHelper(JSTest.newContextBuilder().out(out))) {
            helper.enterContext();

            JSContext context = helper.getJSContext();
            JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
            DynamicObject proxyHandler = JSOrdinary.create(context, realm);
            JSObject.set(proxyHandler, "has", helper.runNoPolyglot("" +
                            "(function() {\n" +
                            "  let Reflect = globalThis.Reflect;\n" +
                            "  return function has(target, key) {\n" +
                            "    var result = Reflect.has(target, key);\n" +
                            "    if (key == 'x') {\n" +
                            "      console.log(`has ${key} ${result}`);\n" +
                            "    }\n" +
                            "    return result;\n" +
                            "  };\n" +
                            "})()"));
            DynamicObject oldGlobal = realm.getGlobalObject();
            DynamicObject newGlobal = JSProxy.create(context, realm, oldGlobal, proxyHandler);
            realm.setGlobalObject(newGlobal);

            try {
                helper.getPolyglotContext().eval(JavaScriptLanguage.ID, "(function(){'use strict'; x = 42;})()");
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertThat(e.getMessage(), startsWith("ReferenceError"));
            }
            assertEquals("has x false\n", out.toString());

            out.reset();

            helper.run("x = (console.log('side effect'), 42);");
            assertEquals("has x false\nside effect\nhas x false\n", out.toString());

            out.reset();

            helper.run("x = (delete x, console.log('side effect'), 42);");
            assertEquals("has x true\nside effect\nhas x false\n", out.toString());

            out.reset();

            helper.run("x = (console.log('side effect'), 42);");
            assertEquals("has x true\nside effect\nhas x true\n", out.toString());

            out.reset();

            helper.run("x = 42;");
            assertEquals("has x true\nhas x true\n", out.toString());

            out.reset();

            helper.run("this.x = 1;");
            try {
                helper.getPolyglotContext().eval(JavaScriptLanguage.ID, "(function(){'use strict'; x = (delete globalThis.x, console.log('deleted'), 3);})()");
                fail("should have thrown");
            } catch (PolyglotException e) {
                assertThat(e.getMessage(), startsWith("ReferenceError"));
            }
            assertEquals("has x true\ndeleted\nhas x false\n", out.toString());

            helper.leaveContext();
        }
    }

}
