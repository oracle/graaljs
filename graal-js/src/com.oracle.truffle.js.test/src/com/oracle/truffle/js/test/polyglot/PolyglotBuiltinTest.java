/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.Test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.test.JSTest;

/**
 * Tests for the Polyglot builtin.
 */
public class PolyglotBuiltinTest extends JSTest {

    private static String test(String sourceCode) {
        return test(sourceCode, null, true, null);
    }

    private static String test(String sourceCode, String failedMessage) {
        return test(sourceCode, failedMessage, true, null);
    }

    private static String test(String sourceCode, String failedMessage, boolean allowAllAccess, Object arg) {
        return test(sourceCode, failedMessage, allowAllAccess, arg, false);
    }

    private static String test(String sourceCode, String failedMessage, boolean allowAllAccess, Object arg, boolean nashornCompat) {
        try (Context context = JSTest.newContextBuilder().allowAllAccess(allowAllAccess).option(JSContextOptions.DEBUG_BUILTIN_NAME, "true").option(
                        JSContextOptions.NASHORN_COMPATIBILITY_MODE_NAME,
                        String.valueOf(nashornCompat)).build()) {
            if (arg != null) {
                context.getBindings("js").putMember("arg", arg);
            }
            addTestPolyglotBuiltins(context);
            Value result = context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "polyglot-test").buildLiteral());
            assertNull(failedMessage);
            return result.asString();
        } catch (Exception ex) {
            assertNotNull(failedMessage);
            assertTrue(ex.getMessage().contains(failedMessage));
            return "FAILED_AS_EXPECTED";
        }
    }

    public static void addTestPolyglotBuiltins(Context context) {
        Value polyglotObject = context.getBindings("js").getMember("Polyglot");
        polyglotObject.putMember("createForeignObject", (ProxyExecutable) (args) -> new ForeignTestMap());
        polyglotObject.putMember("createForeignDynamicObject", (ProxyExecutable) (args) -> new ForeignDynamicObject());
    }

    @Test
    public void testEval() {
        assertEquals("42", test("''+Polyglot.eval('js','40+2');"));
        assertEquals("42", test("''+Polyglot.eval('application/javascript','40+2');"));
    }

    @Test
    public void testExportImport() {
        assertEquals("foo", test("Polyglot.export('myName',{a:'foo'}); Polyglot.import('myName').a;"));

        test("Polyglot.export({},{a:'foo'});", "Invalid identifier");
        test("Polyglot.import({});", "Invalid identifier");
    }

    @Test
    public void testIsExecutable() {
        assertEquals("true", test("''+Polyglot.isExecutable(x=>x+1);"));

        assertEquals("false", test("''+Polyglot.isExecutable(1);"));
        assertEquals("false", test("''+Polyglot.isExecutable(true);"));
    }

    @Test
    public void testIsBoxed() {
        assertEquals("true", test("''+Polyglot.isBoxed(Object(42));"));
        assertEquals("false", test("''+Polyglot.isBoxed(x=>x+1);"));
        assertEquals("false", test("''+Polyglot.isBoxed(1);"));
        assertEquals("false", test("''+Polyglot.isBoxed('test');"));
        assertEquals("false", test("''+Polyglot.isBoxed(true);"));
    }

    @Test
    public void testIsNull() {
        assertEquals("false", test("''+Polyglot.isNull(x=>x+1);"));
        assertEquals("false", test("''+Polyglot.isNull(1);"));
        assertEquals("false", test("''+Polyglot.isNull('test');"));
        assertEquals("true", test("''+Polyglot.isNull(null);"));
    }

    @Test
    public void testHasSize() {
        assertEquals("false", test("''+Polyglot.hasSize(x=>x+1);"));
        assertEquals("false", test("''+Polyglot.hasSize(1);"));
        assertEquals("false", test("''+Polyglot.hasSize('test');"));
        assertEquals("true", test("''+Polyglot.hasSize([1,2,3]);"));
    }

    @Test
    public void testRead() {
        assertEquals("42", test("''+Polyglot.read([1,42,3],1);"));
        assertEquals("42", test("''+Polyglot.read({a:42},'a');"));
        assertEquals("null", test("''+Polyglot.read({a:42},{});"));
        test("''+Polyglot.read(false,{});", "non-interop object");
    }

    @Test
    public void testWrite() {
        assertEquals("true", test("var a = [1,2,3]; ''+(Polyglot.write(a,1,42) === 42 && a[1] === 42 && a.length === 3);"));
        assertEquals("true", test("var o = {a:1}; ''+(Polyglot.write(o,'b',42) === 42 && o.b === 42 && o.a === 1);"));
        assertEquals("null", test("''+Polyglot.write({a:42},{}, 42);"));
        test("''+Polyglot.write(false,0, {});", "non-interop object");
    }

    @Test
    public void testRemove() {
        assertEquals("1,3", test("var a = [1,2,3]; ''+(Polyglot.remove(a,1) && a);"));
        assertEquals("true", test("var o = {a:1, b:'foo'}; ''+(Polyglot.remove(o,'a') && o.a === undefined && o.b === 'foo');"));
        test("''+Polyglot.remove(false, 0);", "non-interop object");
    }

    @Test
    public void testUnbox() {
        assertEquals("42", test("''+Polyglot.unbox(Object(42));"));
        assertEquals("42", test("''+Polyglot.unbox(42);"));
        test("''+Polyglot.unbox({a:1});", "non-interop object");
        test("''+Polyglot.remove(false,0);", "non-interop object");
    }

    @Test
    public void testExecute() {
        assertEquals("42", test("''+Polyglot.execute(x=>x+1,41);"));
        test("''+Polyglot.execute({a:1});", "Message not supported");
        test("''+Polyglot.execute(false);", "non-interop object");
    }

    @Test
    public void testConstruct() {
        assertEquals("42", test("''+Polyglot.construct(Array,42).length;"));
        test("''+Polyglot.construct({a:1});", "Message not supported");
        test("''+Polyglot.construct(false);", "non-interop object");
    }

    @Test
    public void testGetSize() {
        assertEquals("42", test("''+Polyglot.getSize(new Array(42));"));
        assertEquals("null", test("''+Polyglot.getSize({a:1});"));
        test("''+Polyglot.getSize(false);", "non-interop object");
    }

    @Test
    public void testEvalFile() {
        test("''+Polyglot.evalFile('js','notfound.js');", "Cannot evaluate file");
        test("''+Polyglot.evalFile('js',{a:1});", "Expected arguments:");
    }

    @Test
    public void testHasKeys() {
        assertEquals("true", test("''+Polyglot.hasKeys([1,2,3]);"));
        assertEquals("true", test("''+Polyglot.hasKeys({a:1});"));
        assertEquals("false", test("''+Polyglot.hasKeys(1);"));
    }

    @Test
    public void testKeys() {
        assertEquals("0", test("''+Polyglot.keys([1,2,3]).length;"));
        assertEquals("1", test("''+Polyglot.keys({a:1}).length;"));
        test("''+Polyglot.keys(1);", "non-interop object");
    }

    @Test
    public void testIsInstantiable() {
        assertEquals("false", test("''+Polyglot.isInstantiable([1,2,3]);"));
        assertEquals("true", test("''+Polyglot.isInstantiable(Array);"));
        assertEquals("false", test("''+Polyglot.isInstantiable(1);"));
    }

    @Test
    public void testCreateForeignObject() {
        assertEquals("[object Object]", test("''+Polyglot.createForeignObject();"));
        assertEquals("[object Object]", test("''+Polyglot.createForeignDynamicObject();"));
    }

    @Test
    public void testToJSValue() {
        assertEquals("true", test("''+Polyglot.toJSValue(true);"));
        assertEquals("1", test("''+Polyglot.toJSValue(1);"));
        assertEquals("[object Object]", test("''+Polyglot.toJSValue({});"));
    }

    @Test
    public void testToPolyglotValue() {
        assertEquals("true", test("''+Polyglot.toPolyglotValue(true);"));
        assertEquals("1", test("''+Polyglot.toPolyglotValue(1);"));
    }

    @Test
    public void testDeniedExportImport() {
        try (Context context = JSTest.newContextBuilder(JavaScriptLanguage.ID, TestLanguage.ID).allowPolyglotAccess(
                        PolyglotAccess.newBuilder().allowEval(JavaScriptLanguage.ID, TestLanguage.ID).build()).option(JSContextOptions.POLYGLOT_BUILTIN_NAME, "true").build()) {
            context.getPolyglotBindings().putMember("fortyTwo", 42);
            try {
                context.eval(Source.create(JavaScriptLanguage.ID, "Polyglot.import('fortyTwo');"));
                fail("should have thrown");
            } catch (PolyglotException ex) {
                assertTrue(ex.isGuestException());
                assertFalse(ex.isInternalError());
            }
            try {
                context.eval(Source.create(JavaScriptLanguage.ID, "Polyglot.export('myName',{a:'foo'});"));
                fail("should have thrown");
            } catch (PolyglotException ex) {
                assertTrue(ex.isGuestException());
                assertFalse(ex.isInternalError());
            }
        }
    }

    @Test
    public void testEvalInternalLanguage() {
        try (Context context = JSTest.newContextBuilder().allowPolyglotAccess(PolyglotAccess.ALL).option(JSContextOptions.POLYGLOT_BUILTIN_NAME, "true").build()) {
            try {
                context.eval(Source.create(JavaScriptLanguage.ID, "Polyglot.eval('regex', 'sth');"));
                fail("should have thrown");
            } catch (PolyglotException ex) {
                assertTrue(ex.isGuestException());
                assertFalse(ex.isInternalError());
            }
        }
    }

    @SuppressWarnings("try")
    @Test
    public void testEvalReturnValue() throws Exception {
        try (AutoCloseable languageScope = TestLanguage.withTestLanguage(new TestLanguage() {
            @Override
            protected CallTarget parse(ParsingRequest request) throws Exception {
                String code = request.getSource().getCharacters().toString();
                if (code.startsWith("\"") && code.endsWith("\"")) {
                    String string = code.substring(1, code.length() - 1);
                    return RootNode.createConstantNode(string).getCallTarget();
                }
                throw Errors.createSyntaxError(code);
            }
        })) {
            IOAccess fileAccess = IOAccess.newBuilder().allowHostFileAccess(true).build();
            try (Context context = Context.newBuilder().allowIO(fileAccess).allowPolyglotAccess(PolyglotAccess.ALL).build()) {
                Value result = context.eval(Source.create(JavaScriptLanguage.ID, "Polyglot.eval('" + TestLanguage.ID + "', '\"something\"');"));
                assertTrue(result.isString());
                assertEquals("something", result.asString());

                File tmpFile = File.createTempFile("polyglot-evalfile-test", null);
                tmpFile.deleteOnExit();
                Files.writeString(tmpFile.toPath(), "\"nanika\"");
                result = context.eval(Source.create(JavaScriptLanguage.ID, "Polyglot.evalFile('" + TestLanguage.ID + "', " + JSRuntime.quote(tmpFile.getPath()) + ");"));
                assertTrue(result.isString());
                assertEquals("nanika", result.asString());
            }
        }
    }
}
