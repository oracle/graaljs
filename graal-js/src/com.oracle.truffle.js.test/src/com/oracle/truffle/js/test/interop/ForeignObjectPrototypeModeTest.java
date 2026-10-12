/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.js.runtime.JSContextOptions.FOREIGN_OBJECT_PROTOTYPE_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.ForeignObjectPrototypeMode.DEFAULT;
import static com.oracle.truffle.js.runtime.JSContextOptions.ForeignObjectPrototypeMode.NONE;
import static com.oracle.truffle.js.runtime.JSContextOptions.ForeignObjectPrototypeMode.SPECIALIZED_ONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyIterable;
import org.graalvm.polyglot.proxy.ProxyIterator;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.runtime.JSContextOptions.ForeignObjectPrototypeMode;
import com.oracle.truffle.js.test.interop.ForeignObjectPrototypeTest.TestTruffleBoolean;
import com.oracle.truffle.js.test.interop.ForeignObjectPrototypeTest.TestTruffleHash;
import com.oracle.truffle.js.test.interop.ForeignObjectPrototypeTest.TestTruffleNumber;
import com.oracle.truffle.js.test.interop.ForeignObjectPrototypeTest.TestTruffleString;
import com.oracle.truffle.js.test.polyglot.ForeignNull;

@RunWith(Parameterized.class)
public class ForeignObjectPrototypeModeTest {

    @Parameters(name = "{0}")
    public static List<ForeignObjectPrototypeMode> data() {
        return Arrays.asList(null, NONE, DEFAULT, SPECIALIZED_ONLY);
    }

    @Parameter public ForeignObjectPrototypeMode mode;

    private static Context.Builder newContextBuilder(ForeignObjectPrototypeMode mode) {
        Context.Builder builder = Context.newBuilder(ID).allowExperimentalOptions(false);
        if (mode != null) {
            builder.option(FOREIGN_OBJECT_PROTOTYPE_NAME, mode.toString());
        }
        return builder;
    }

    private static boolean isEnabled(ForeignObjectPrototypeMode mode) {
        return mode != NONE;
    }

    private static void assertPrototype(Context context, ForeignObjectPrototypeMode mode, Object foreign) {
        Value result = context.eval(ID, """
                        (foreign) => {
                            const proto = Object.getPrototypeOf(foreign);
                            return [
                                proto === null,
                                proto === Object.prototype,
                                proto !== null && Object.getPrototypeOf(proto) === Object.prototype,
                                typeof foreign.hasOwnProperty === 'function'
                            ];
                        }
                        """).execute(foreign);
        assertEquals(!isEnabled(mode), result.getArrayElement(0).asBoolean());
        assertEquals(mode == SPECIALIZED_ONLY, result.getArrayElement(1).asBoolean());
        assertEquals(isEnabled(mode) && mode != SPECIALIZED_ONLY, result.getArrayElement(2).asBoolean());
        assertEquals(isEnabled(mode), result.getArrayElement(3).asBoolean());
    }

    /**
     * Checks generic foreign prototypes and plain-object detection in each option mode.
     */
    @Test
    public void testRecordLikeObject() {
        try (Context context = newContextBuilder(mode).build()) {
            ProxyObject foreign = ProxyObject.fromMap(new LinkedHashMap<>(Map.of("answer", 42)));
            assertPrototype(context, mode, foreign);
            Value plain = context.eval(ID, """
                            (value) => {
                                if (value === null || typeof value !== 'object') return false;
                                const proto = Object.getPrototypeOf(value);
                                return proto === null || proto === Object.prototype;
                            }
                            """);
            assertEquals(mode == NONE || mode == SPECIALIZED_ONLY, plain.execute(foreign).asBoolean());
            if (isEnabled(mode)) {
                assertTrue(context.eval(ID, "(o) => o.hasOwnProperty('answer') && !o.hasOwnProperty('missing')").execute(foreign).asBoolean());
            }
        }
    }

    /**
     * Checks that generic host objects follow the selected foreign prototype mode.
     */
    @Test
    public void testGenericHostObject() {
        try (Context context = newContextBuilder(mode).build()) {
            assertPrototype(context, mode, new Object());
        }
    }

    /**
     * Checks that Reflect.getPrototypeOf agrees with Object.getPrototypeOf and invokes proxy traps once.
     */
    @Test
    public void testReflectGetPrototypeOf() {
        try (Context context = newContextBuilder(mode).allowHostAccess(HostAccess.ALL).build()) {
            Value check = context.eval(ID, "(o) => Reflect.getPrototypeOf(o) === Object.getPrototypeOf(o)");
            for (Object foreign : List.of(ProxyObject.fromMap(Map.of("answer", 42)), new Object(),
                            ProxyArray.fromArray(1, 2), new int[]{1, 2}, Instant.now(), new TestTruffleHash(),
                            (ProxyExecutable) args -> 42, new RuntimeException("test"), ProxyIterable.from(List.of(1, 2)),
                            ProxyIterator.from(List.of(1, 2).iterator()))) {
                assertTrue(check.execute(foreign).asBoolean());
            }
            for (String expression : List.of("({})", "[]", "Object.create(null)", "Object(true)", "Object(1)", "Object('test')", "new Proxy({}, {})")) {
                assertTrue(check.execute(context.eval(ID, expression)).asBoolean());
            }
            assertTrue(context.eval(ID, """
                            let calls = 0;
                            const proto = {};
                            const proxy = new Proxy({}, {getPrototypeOf() { calls++; return proto; }});
                            Reflect.getPrototypeOf(proxy) === proto && calls === 1;
                            """).asBoolean());
        }
    }

    /**
     * Checks that Reflect.getPrototypeOf rejects JS and foreign primitives, as well as revoked proxies.
     */
    @Test
    public void testReflectGetPrototypeOfRejectsPrimitives() {
        try (Context context = newContextBuilder(mode).build()) {
            Value check = context.eval(ID, """
                            (value) => {
                                try {
                                    Reflect.getPrototypeOf(value);
                                    return false;
                                } catch (e) {
                                    return e instanceof TypeError;
                                }
                            }
                            """);
            for (String expression : List.of("null", "undefined", "true", "1", "'test'", "1n", "Symbol()")) {
                assertTrue(check.execute(context.eval(ID, expression)).asBoolean());
            }
            for (Object foreign : List.of(new ForeignNull(), new TestTruffleString(), new TestTruffleBoolean(), new TestTruffleNumber())) {
                assertTrue(check.execute(foreign).asBoolean());
            }
            assertTrue(check.execute(context.eval(ID, "const p = Proxy.revocable({}, {}); p.revoke(); p.proxy;")).asBoolean());
        }
    }

    /**
     * Checks the __proto__ getter for foreign and JS values, primitive boxing, and proxy traps.
     */
    @Test
    public void testProtoGetter() {
        try (Context context = newContextBuilder(mode).allowHostAccess(HostAccess.ALL).build()) {
            Value check = context.eval(ID, """
                            (o, enabled) => {
                                const getter = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__').get;
                                return getter.call(o) === Object.getPrototypeOf(o) &&
                                    (!enabled || o.__proto__ === Object.getPrototypeOf(o));
                            }
                            """);
            for (Object foreign : List.of(ProxyObject.fromMap(Map.of("answer", 42)), new Object(),
                            ProxyArray.fromArray(1, 2), new int[]{1, 2}, Instant.now(), ProxyHashMap.from(new LinkedHashMap<>()),
                            (ProxyExecutable) args -> 42, new RuntimeException("test"), ProxyIterable.from(List.of(1, 2)),
                            ProxyIterator.from(List.of(1, 2).iterator()))) {
                assertTrue(foreign.getClass().getName(), check.execute(foreign, isEnabled(mode)).asBoolean());
            }
            for (String expression : List.of("({})", "[]", "true", "1", "'test'", "1n", "Symbol()", "new Proxy({}, {})")) {
                assertTrue(check.execute(context.eval(ID, expression), true).asBoolean());
            }
            assertTrue(check.execute(context.eval(ID, "Object.create(null)"), false).asBoolean());
            Value checkBoxed = context.eval(ID, """
                            (o, expected) => Object.getOwnPropertyDescriptor(Object.prototype, '__proto__').get.call(o) === expected && o.__proto__ === expected
                            """);
            assertTrue(checkBoxed.execute(new TestTruffleString(), context.eval(ID, "String.prototype")).asBoolean());
            assertTrue(checkBoxed.execute(new TestTruffleBoolean(), context.eval(ID, "Boolean.prototype")).asBoolean());
            assertTrue(checkBoxed.execute(new TestTruffleNumber(), context.eval(ID, "Number.prototype")).asBoolean());
            assertTrue(context.eval(ID, """
                            let calls = 0;
                            const proto = {};
                            const proxy = new Proxy({}, {getPrototypeOf() { calls++; return proto; }});
                            proxy.__proto__ === proto && calls === 1;
                            """).asBoolean());
        }
    }

    /**
     * Checks that the __proto__ getter rejects JS and foreign nullish values and revoked proxies.
     */
    @Test
    public void testProtoGetterRejectsNullish() {
        try (Context context = newContextBuilder(mode).build()) {
            Value check = context.eval(ID, """
                            (value) => {
                                const getter = Object.getOwnPropertyDescriptor(Object.prototype, '__proto__').get;
                                try {
                                    getter.call(value);
                                    return false;
                                } catch (e) {
                                    return e instanceof TypeError;
                                }
                            }
                            """);
            for (String expression : List.of("null", "undefined")) {
                assertTrue(check.execute(context.eval(ID, expression)).asBoolean());
            }
            assertTrue(check.execute(new ForeignNull()).asBoolean());
            assertTrue(check.execute(context.eval(ID, "const p = Proxy.revocable({}, {}); p.revoke(); p.proxy;")).asBoolean());
        }
    }

    /**
     * Checks that an own __proto__ member shadows the getter without changing the foreign prototype.
     */
    @Test
    public void testProtoMemberShadowsGetter() {
        try (Context context = newContextBuilder(mode).build()) {
            ProxyObject foreign = ProxyObject.fromMap(Map.of("__proto__", "own member"));
            assertEquals("own member", context.eval(ID, "(o) => o.__proto__").execute(foreign).asString());
            assertPrototype(context, mode, foreign);
        }
    }

    /**
     * Checks that foreign members remain live and can shadow inherited methods.
     */
    @Test
    public void testLiveMembersAndShadowing() {
        Map<String, Object> members = new LinkedHashMap<>(Map.of("answer", 42));
        try (Context context = newContextBuilder(mode).build()) {
            context.getBindings(ID).putMember("foreign", ProxyObject.fromMap(members));
            assertEquals(42, context.eval(ID, "foreign.answer").asInt());
            members.put("answer", 43);
            assertEquals(43, context.eval(ID, "foreign.answer").asInt());
            context.eval(ID, "foreign.answer = 44");
            assertEquals(44, context.asValue(members.get("answer")).asInt());
            assertTrue(context.eval(ID, "Object.keys(foreign).includes('answer') && 'answer' in foreign").asBoolean());

            members.put("hasOwnProperty", (ProxyExecutable) args -> "own member");
            assertEquals("own member", context.eval(ID, "foreign.hasOwnProperty('answer')").asString());
            assertTrue(context.eval(ID, "Object.prototype.hasOwnProperty.call(foreign, 'answer')").asBoolean());
            members.remove("hasOwnProperty");
            assertEquals(isEnabled(mode), context.eval(ID, "typeof foreign.hasOwnProperty === 'function'").asBoolean());
        }
    }

    /**
     * Checks inherited getters, own-key enumeration, and shared prototype mutation in specialized-only mode.
     */
    @Test
    public void testPrototypeProperties() {
        try (Context context = newContextBuilder(mode).build()) {
            context.getBindings(ID).putMember("foreign", ProxyObject.fromMap(new LinkedHashMap<>(Map.of("answer", 42))));
            context.eval(ID, "Object.defineProperty(Object.prototype, 'inheritedAnswer', {get() { return this.answer; }});");
            assertEquals(isEnabled(mode), context.eval(ID, "'inheritedAnswer' in foreign").asBoolean());
            assertFalse(context.eval(ID, "Object.keys(foreign).includes('inheritedAnswer')").asBoolean());
            if (isEnabled(mode)) {
                assertEquals(42, context.eval(ID, "foreign.inheritedAnswer").asInt());
                context.eval(ID, "Object.getPrototypeOf(foreign).marker = 99");
                assertEquals(mode == SPECIALIZED_ONLY, context.eval(ID, "({}).marker === 99").asBoolean());
            } else {
                assertTrue(context.eval(ID, "foreign.inheritedAnswer === undefined").asBoolean());
            }
        }
    }

    /**
     * Checks own versus inherited properties, including symbols and shadowing, without invoking getters.
     */
    @Test
    public void testOwnPropertyChecks() {
        Map<String, Object> members = new LinkedHashMap<>(Map.of("answer", 42));
        try (Context context = newContextBuilder(mode).build()) {
            ProxyObject foreign = ProxyObject.fromMap(members);
            context.getBindings(ID).putMember("foreign", foreign);
            context.eval(ID, """
                            Object.defineProperty(Object.prototype, 'inheritedAnswer', {
                                get() { throw new Error('Property existence checks must not invoke getters'); }
                            });
                            Object.prototype[Symbol.iterator] = () => {};
                            """);
            Value check = context.eval(ID, """
                            (o, key, expected) => Object.hasOwn(o, key) === expected &&
                                Object.prototype.hasOwnProperty.call(o, key) === expected
                            """);
            assertTrue(check.execute(foreign, "answer", true).asBoolean());
            assertTrue(check.execute(foreign, "missing", false).asBoolean());
            assertTrue(check.execute(foreign, "inheritedAnswer", false).asBoolean());
            assertTrue(check.execute(foreign, "toString", false).asBoolean());
            assertTrue(check.execute(foreign, context.eval(ID, "Symbol.iterator"), false).asBoolean());
            assertEquals(isEnabled(mode), context.eval(ID, "'inheritedAnswer' in foreign").asBoolean());
            assertEquals(isEnabled(mode), context.eval(ID, "'toString' in foreign").asBoolean());
            assertEquals(isEnabled(mode), context.eval(ID, "Symbol.iterator in foreign").asBoolean());

            members.put("inheritedAnswer", 84);
            members.put("toString", "own member");
            assertTrue(check.execute(foreign, "inheritedAnswer", true).asBoolean());
            assertTrue(check.execute(foreign, "toString", true).asBoolean());
            assertEquals(84, context.eval(ID, "foreign.inheritedAnswer").asInt());
            members.remove("inheritedAnswer");
            assertTrue(check.execute(foreign, "inheritedAnswer", false).asBoolean());
            assertEquals(isEnabled(mode), context.eval(ID, "'inheritedAnswer' in foreign").asBoolean());

            assertTrue(check.execute(context.eval(ID, "Object.create({inherited: 42})"), "inherited", false).asBoolean());
            assertTrue(check.execute(context.eval(ID, "({own: 42})"), "own", true).asBoolean());
        }
    }

    /**
     * Checks that foreign arrays inherit methods and symbols without treating them as own properties.
     */
    @Test
    public void testArrayInheritedPropertiesNotOwn() {
        try (Context context = newContextBuilder(mode).build()) {
            context.getBindings(ID).putMember("foreign", ProxyArray.fromArray(1, 2));
            Value check = context.eval(ID, """
                            (key, enabled) => !Object.hasOwn(foreign, key) &&
                                !Object.prototype.hasOwnProperty.call(foreign, key) &&
                                (key in foreign) === enabled
                            """);
            assertTrue(check.execute("map", isEnabled(mode)).asBoolean());
            assertTrue(check.execute("toString", isEnabled(mode)).asBoolean());
            assertTrue(check.execute(context.eval(ID, "Symbol.iterator"), isEnabled(mode)).asBoolean());
        }
    }

    /**
     * Checks that specialized foreign prototype chains are preserved in specialized-only mode.
     */
    @Test
    public void testSpecializedPrototypes() {
        try (Context context = newContextBuilder(mode).allowHostAccess(HostAccess.ALL).build()) {
            assertSpecializedPrototype(context, ProxyArray.fromArray(1, 2), "Array.prototype");
            assertSpecializedPrototype(context, new int[]{1, 2}, "Array.prototype");
            assertSpecializedPrototype(context, Instant.now(), "Date.prototype");
            assertSpecializedPrototype(context, new TestTruffleHash(), "Map.prototype");
            assertSpecializedPrototype(context, new TestTruffleString(), "String.prototype");
            assertSpecializedPrototype(context, new TestTruffleBoolean(), "Boolean.prototype");
            assertSpecializedPrototype(context, new TestTruffleNumber(), "Number.prototype");
            assertSpecializedPrototype(context, (ProxyExecutable) args -> 42, "Function.prototype");
            assertSpecializedPrototype(context, new RuntimeException("test"), "Error.prototype");
            assertSpecializedPrototype(context, ProxyIterable.from(List.of(1, 2)), "Object.prototype");
            assertSpecializedPrototype(context, ProxyIterator.from(List.of(1, 2).iterator()), "Object.getPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]()))");
        }
    }

    private void assertSpecializedPrototype(Context context, Object foreign, String prototype) {
        if (isEnabled(mode)) {
            Value check = context.eval(ID, "(o, expected) => { const p = Object.getPrototypeOf(o); return p !== expected && Object.getPrototypeOf(p) === expected; }");
            assertTrue(prototype, check.execute(foreign, context.eval(ID, prototype)).asBoolean());
        } else {
            assertTrue(context.eval(ID, "(o) => Object.getPrototypeOf(o) === null").execute(foreign).asBoolean());
        }
    }

    /**
     * Checks that contexts sharing an engine retain their own prototype modes and realm properties.
     */
    @Test
    public void testSharedEngine() {
        Source source = Source.newBuilder(ID, "(o) => [Object.getPrototypeOf(o) === Object.prototype, o.contextMarker]", "foreign-prototype.js").buildLiteral();
        ProxyObject foreign = ProxyObject.fromMap(new LinkedHashMap<>());
        try (Engine engine = Engine.create()) {
            for (ForeignObjectPrototypeMode otherMode : data()) {
                try (Context first = newContextBuilder(mode).engine(engine).build();
                                Context second = newContextBuilder(otherMode).engine(engine).build()) {
                    first.eval(ID, "Object.prototype.contextMarker = 'first'");
                    second.eval(ID, "Object.prototype.contextMarker = 'second'");
                    Value firstFunction = first.eval(source);
                    Value secondFunction = second.eval(source);
                    for (int i = 0; i < 3; i++) {
                        assertContextResult(firstFunction.execute(foreign), mode, "first");
                        assertContextResult(secondFunction.execute(foreign), otherMode, "second");
                    }
                }
            }
        }
    }

    private static void assertContextResult(Value result, ForeignObjectPrototypeMode mode, String marker) {
        assertEquals(mode == SPECIALIZED_ONLY, result.getArrayElement(0).asBoolean());
        if (isEnabled(mode)) {
            assertEquals(marker, result.getArrayElement(1).asString());
        } else {
            assertTrue(result.getArrayElement(1).isNull());
        }
    }
}
