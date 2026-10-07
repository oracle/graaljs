/*
 * Copyright (c) 2019, 2026, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.js.parser.ir.Module.ImportPhase;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSModuleData;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

public class RealmBuiltinTest {

    @Test
    public void testRealmBuiltinNotShared() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "const id = Realm.create(); Realm.eval(id, 'Realm') === Realm");
            Assert.assertTrue(result.isBoolean());
            Assert.assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testRealmCurrent1() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "const id = Realm.create(); Realm.eval(id, 'Realm.current()') === id");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testRealmCurrent2() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "const id = Realm.create(); Realm.eval(id, 'Realm.current')()");
            Assert.assertTrue(result.fitsInInt());
            Assert.assertEquals(0, result.asInt());
        }
    }

    @Test
    public void testRealmShared() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Realm.shared === undefined");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());
            result = context.eval(JavaScriptLanguage.ID, "const id = Realm.create(); Realm.shared = 42; Realm.eval(id, 'Realm.shared')");
            Assert.assertTrue(result.isNumber());
            Assert.assertEquals(42, result.asInt());
            result = context.eval(JavaScriptLanguage.ID, "Realm.eval(id, 'Realm.shared = 211'); Realm.shared");
            Assert.assertTrue(result.isNumber());
            Assert.assertEquals(211, result.asInt());
        }
    }

    @Test
    public void testInvalidRealmIndex() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "let passed = false; try { Realm.eval(1, '6*7'); } catch (e) { passed = e instanceof TypeError; } passed");
            Assert.assertTrue(result.isBoolean());
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testFunctionDataRealmTransitionPolicy() {
        try (TestHelper helper = new TestHelper()) {
            helper.enterContext();
            try {
                JSObject functions = (JSObject) helper.parse("""
                                ({ordinary: function () {}, builtin: Array, nonConstructor: Math.abs, symbol: Symbol,
                                  boundOrdinary: (function () {}).bind(null), boundBuiltin: Array.bind(null)})
                                """).run(helper.getRealm());
                assertRealmTransitionPolicy(functions, "ordinary", true, false);
                assertRealmTransitionPolicy(functions, "builtin", true, true);
                assertRealmTransitionPolicy(functions, "nonConstructor", true, false);
                assertRealmTransitionPolicy(functions, "symbol", true, true);
                assertRealmTransitionPolicy(functions, "boundOrdinary", false, false);
                assertRealmTransitionPolicy(functions, "boundBuiltin", false, false);
            } finally {
                helper.leaveContext();
            }
        }
    }

    private static void assertRealmTransitionPolicy(JSObject functions, String name, boolean call, boolean construct) {
        JSFunctionObject function = (JSFunctionObject) JSObject.get(functions, Strings.fromJavaString(name));
        Assert.assertEquals(name, call, function.getFunctionData().callTargetNeedsRealmTransition());
        Assert.assertEquals(name, construct, function.getFunctionData().constructTargetNeedsRealmTransition());
    }

    @Test
    public void testModuleLinkRealm() {
        try (TestHelper helper = new TestHelper()) {
            helper.enterContext();
            try {
                JSRealm mainRealm = helper.getRealm();
                JSRealm childRealm = mainRealm.createChildRealm();
                Source source = Source.newBuilder(JavaScriptLanguage.ID, "export function f() { return globalThis; }", "realm-link.mjs").mimeType(JavaScriptLanguage.MODULE_MIME_TYPE).build();
                JSModuleData moduleData = helper.getJSContext().getEvaluator().envParseModule(childRealm, source);
                for (JSRealm moduleRealm : new JSRealm[]{childRealm, mainRealm}) {
                    JSRealm callerRealm = moduleRealm == childRealm ? mainRealm : childRealm;
                    JSRealm previousRealm = mainRealm.enterRealm(null, callerRealm);
                    try {
                        JSModuleRecord module = new JSModuleRecord(moduleData, moduleRealm.getModuleLoader());
                        module.loadRequestedModulesSync(moduleRealm, Undefined.instance);
                        module.link(moduleRealm);
                        Assert.assertSame(callerRealm, JSRealm.get(null));
                        JSFunctionObject function = (JSFunctionObject) JSObject.get(module.getModuleNamespace(ImportPhase.Evaluation), Strings.fromJavaString("f"));
                        Assert.assertSame(moduleRealm, JSFunction.getRealm(function));
                        Assert.assertSame(moduleRealm.getGlobalObject(), JSFunction.call(function, Undefined.instance, new Object[0]));
                        Assert.assertSame(callerRealm, JSRealm.get(null));
                    } finally {
                        mainRealm.leaveRealm(null, previousRealm);
                    }
                }
            } finally {
                helper.leaveContext();
            }
        }
    }

    @Test
    public void testExportedFunctionRealm() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value otherGlobal = context.eval(JavaScriptLanguage.ID, "var otherRealm = Realm.create(); Realm.global(otherRealm);");
            for (String suffix : new String[]{"", ".bind(null)"}) {
                Value function = context.eval(JavaScriptLanguage.ID, """
                                Realm.eval(otherRealm, '(function (fail) { if (fail) throw new Error(); return globalThis; })')%s
                                """.formatted(suffix));
                Assert.assertEquals(otherGlobal, function.execute(false));
                Assert.assertThrows(PolyglotException.class, () -> function.execute(true));
                Assert.assertEquals(0, context.eval(JavaScriptLanguage.ID, "Realm.current()").asInt());
            }
        }
    }

    @Test
    public void testExportedConstructorRealm() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            Value otherGlobal = context.eval(JavaScriptLanguage.ID, "var otherRealm = Realm.create(); Realm.global(otherRealm);");
            for (String suffix : new String[]{"", ".bind(null)"}) {
                Value constructor = context.eval(JavaScriptLanguage.ID, """
                                Realm.eval(otherRealm, '(function Constructor() { this.global = globalThis; })')%s
                                """.formatted(suffix));
                Assert.assertEquals(otherGlobal, constructor.newInstance().getMember("global"));
                Assert.assertEquals(0, context.eval(JavaScriptLanguage.ID, "Realm.current()").asInt());
            }
        }
    }

    @Test
    public void testAsyncResumptionRealm() {
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.V8_REALM_BUILTIN_NAME, "true").build()) {
            context.eval(JavaScriptLanguage.ID, """
                            var done = false;
                            var failure = null;
                            const child = Realm.create();
                            const other = Realm.global(child);
                            Realm.eval(child, `
                                async function f(reject) {
                                    const local = 42;
                                    await 0;
                                    if (reject) throw new Error('async function');
                                    return { object: {}, local: eval('local') };
                                }
                                async function* g(reject) {
                                    const local = 43;
                                    yield { object: {}, local: eval('local') };
                                    await 0;
                                    if (reject) throw new Error('async generator');
                                    return { object: {}, local: eval('local') };
                                }
                            `);
                            function check(value, local) {
                                if (Object.getPrototypeOf(value.object) !== other.Object.prototype ||
                                    value.local !== local || Realm.current() !== 0) {
                                    throw new Error('incorrect resumption realm');
                                }
                            }
                            (async function() {
                                check(await other.f(false), 42);
                                const iterator = other.g(false);
                                const first = await iterator.next();
                                check(first.value, 43);
                                const last = await iterator.next();
                                check(last.value, 43);
                                if (first.done || !last.done) throw new Error('incorrect generator state');
                                const rejectingIterator = other.g(true);
                                check((await rejectingIterator.next()).value, 43);
                                for (const promise of [other.f(true), rejectingIterator.next()]) {
                                    let rejected = false;
                                    try { await promise; } catch (error) {
                                        if (!(error instanceof other.Error)) throw error;
                                        rejected = true;
                                    }
                                    if (!rejected || Realm.current() !== 0) throw new Error('incorrect rejection realm');
                                }
                                done = true;
                            })().catch(error => { failure = error; });
                            """);
            Assert.assertTrue(context.getBindings(JavaScriptLanguage.ID).getMember("failure").toString(),
                            context.getBindings(JavaScriptLanguage.ID).getMember("done").asBoolean());
            Assert.assertEquals(0, context.eval(JavaScriptLanguage.ID, "Realm.current()").asInt());
        }
    }
}
