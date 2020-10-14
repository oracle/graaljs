/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.polyglot.PolyglotBuiltinTest;

public class JSProxyTest {

    @Test
    public void testProxySet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String source = "var target= { isTarget: true};" +
                            "var handler={ set: function(t,v) { t.setCalled=true; } };" +
                            "var p = new Proxy(target, handler);" +
                            "function set(t,v) { t.value=v; };" +
                            "set({},38); set({},39); set({},40); set({}, 42); set(p,42);" +
                            "target.setCalled;";
            assertTrue(context.eval("js", source).asBoolean());
        }
    }

    @Test
    public void testProxyOwnKeysForeignTarget() {
        try (Context context = JSTest.newContextBuilder().allowPolyglotAccess(PolyglotAccess.ALL).option(JSContextOptions.DEBUG_BUILTIN_NAME, "true").build()) {
            PolyglotBuiltinTest.addTestPolyglotBuiltins(context);
            String source = "var target = Polyglot.createForeignObject();" +
                            "var handler = { ownKeys: function(t) { target.ownKeysCalled=true; return []; } };" +
                            "var p = new Proxy(target, handler);" +
                            "Object.keys(p);" +
                            "target.ownKeysCalled;";
            assertTrue(context.eval("js", source).asBoolean());
        }
    }

    @Test
    public void testProxyInstantiate() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value proxyFactory = context.eval(ID, "(target) => new Proxy(target, {});");
            Value constructorProxy;
            Value result;

            Value constructor = context.eval(ID, "(function(arg) {return {expected: arg};});");
            constructorProxy = proxyFactory.execute(constructor);
            result = constructorProxy.newInstance("hello");
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals("hello", result.getMember("expected").asString());

            constructorProxy = proxyFactory.execute(constructorProxy);
            result = constructorProxy.newInstance(42);
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals(42, result.getMember("expected").asInt());

            constructorProxy = proxyFactory.execute((ProxyInstantiable) args -> {
                assertEquals(1, args.length);
                return ProxyObject.fromMap(Collections.singletonMap("expected", 42));
            });
            result = constructorProxy.newInstance("hello");
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals(42, result.getMember("expected").asInt());
        }
    }

    @Test
    public void testProxyInstantiateWithTrap() {
        try (Context context = JSTest.newContextBuilder().build()) {
            Value proxyFactory = context.eval(ID, "" +
                            "(target) => new Proxy(target, {\n" +
                            "  construct(target, args) {return {expected: args[0]};}\n" +
                            "});\n");
            Value constructorProxy;
            Value result;

            constructorProxy = proxyFactory.execute(context.eval(ID, "(function(arg) {return {notExpected: arg};});"));
            result = constructorProxy.newInstance("hello");
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals("hello", result.getMember("expected").asString());

            constructorProxy = proxyFactory.execute(constructorProxy);
            result = constructorProxy.newInstance(42);
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals(42, result.getMember("expected").asInt());

            constructorProxy = proxyFactory.execute((ProxyInstantiable) args -> {
                assertEquals(1, args.length);
                return ProxyObject.fromMap(Collections.singletonMap("notExpected", 42));
            });
            result = constructorProxy.newInstance(42);
            assertTrue(result.hasMembers());
            assertTrue(result.hasMember("expected"));
            assertEquals(42, result.getMember("expected").asInt());
        }
    }

    @Test
    public void testProxy() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final Value proxy = ctx.eval("js", "" +
                            "(function(){\n" +
                            "return new Proxy(function() {}, {\n" +
                            "    apply: function(target, thisArg, argumentsList) {return 'a';}\n" +
                            "});\n" +
                            "})").execute();
            assertEquals("a", proxy.execute().asString());
        }
    }

    @Test
    public void testProxyConstruct() {
        try (Context ctx = JSTest.newContextBuilder().allowHostAccess(HostAccess.ALL).allowHostClassLookup(s -> true).build()) {
            String src = "const AL = Java.type('java.util.ArrayList');\n" +
                            "const PAL = new Proxy(AL, {});\n" +
                            "var ar = new PAL();\n" +
                            "ar.add('test');\n" +
                            "ar.size() === 1 && ar.get(0)==='test'";

            org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder("js", src, "testProxy").buildLiteral();
            final Value proxy = ctx.eval(source);
            assertTrue(proxy.isBoolean() && proxy.asBoolean() == true);
        }
    }

    @Test
    public void testForeignGetTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            Value value = ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { get: foreignTrap });\n" +
                            "return proxy.foo;\n" +
                            "})").execute((ProxyExecutable) args -> 42);
            assertTrue(value.isNumber());
            assertSame(42, value.asInt());
        }
    }

    @Test
    public void testForeignSetTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { set: foreignTrap });\n" +
                            "proxy.foo = 42;\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignHasTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { has: foreignTrap });\n" +
                            "with (proxy) foo;\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignConstructTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy(function() {}, { construct: foreignTrap });\n" +
                            "new proxy();\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return args[0];
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignApplyTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            Value value = ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy(function() {}, { apply: foreignTrap });\n" +
                            "return proxy();\n" +
                            "})").execute((ProxyExecutable) args -> {
                                return 42;
                            });
            assertTrue(value.isNumber());
            assertSame(42, value.asInt());
        }
    }

    @Test
    public void testForeignDeletePropertyTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { deleteProperty: foreignTrap });\n" +
                            "delete proxy.foo;\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignDefinePropertyTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { defineProperty: foreignTrap });\n" +
                            "proxy.foo = 42;\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignPreventExtensionsTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "  var proxy = new Proxy({}, { preventExtensions: foreignTrap });\n" +
                            "  try {\n" +
                            "    Object.freeze(proxy);\n" +
                            "    throw new Error('TypeError expected');\n" +
                            "  } catch (e) {\n" +
                            "    if (!(e instanceof TypeError)) {\n" +
                            "      throw e;\n" +
                            "    }\n" +
                            "  }\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return false;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignIsExtensibleTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { isExtensible: foreignTrap });\n" +
                            "Object.isExtensible(proxy);\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignGetPrototypeOfTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            Value objectPrototype = ctx.eval("js", "Object.prototype");
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { getPrototypeOf: foreignTrap });\n" +
                            "Object.getPrototypeOf(proxy);\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return objectPrototype;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignSetPrototypeOfTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { setPrototypeOf: foreignTrap });\n" +
                            "Object.setPrototypeOf(proxy, {});\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return true;
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignOwnKeysTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { ownKeys: foreignTrap });\n" +
                            "Object.keys(proxy);\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return args[0];
                            });
            assertTrue(called[0]);
        }
    }

    @Test
    public void testForeignGetOwnPropertyDescriptorTrap() {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            final boolean[] called = new boolean[1];
            Value descriptor = ctx.eval("js", "({ configurable: true })");
            ctx.eval("js", "(function(foreignTrap) {\n" +
                            "var proxy = new Proxy({}, { getOwnPropertyDescriptor: foreignTrap });\n" +
                            "Object.getOwnPropertyDescriptor(proxy, 'foo');\n" +
                            "})").execute((ProxyExecutable) args -> {
                                called[0] = true;
                                return descriptor;
                            });
            assertTrue(called[0]);
        }
    }

}
