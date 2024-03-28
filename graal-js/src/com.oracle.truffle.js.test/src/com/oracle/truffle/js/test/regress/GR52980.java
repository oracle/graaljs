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
package com.oracle.truffle.js.test.regress;

import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * [GR-52980] Map.prototype.forEach does not convert java.lang.String key/value of foreign hash maps
 * to TruffleString.
 */
public class GR52980 {

    public static class Ctx1 {
        private final boolean javaMapForEach;

        public Ctx1(boolean javaMapForEach) {
            this.javaMapForEach = javaMapForEach;
        }

        @HostAccess.Export
        public Map<String, String> getMap() {
            return Map.of("key1", "value1");
        }

        @HostAccess.Export
        public void checkResult(String msg) {
            if (javaMapForEach) {
                Assert.assertEquals("value1 = key1", msg);
            } else {
                Assert.assertEquals("key1 = value1", msg);
            }
        }
    }

    public static class Ctx2 {
        @HostAccess.Export
        public Value getMap() {
            Map<Object, Object> map = Map.of("key1", "value1");
            return Value.asValue(ProxyHashMap.from(map));
        }

        @HostAccess.Export
        public void checkResult(String msg) {
            Assert.assertEquals("key1 = value1", msg);
        }
    }

    public static class Ctx3 {
        @HostAccess.Export
        public Object getMap() {
            Map<Object, Object> map = Map.of("key1", "value1");
            return new CustomProxyHashMap(map);
        }

        @HostAccess.Export
        public void checkResult(String msg) {
            Assert.assertEquals("key1 = value1", msg);
        }
    }

    @Test
    public void testGR52980() {
        Source testScript = Source.newBuilder("js", """
                        function test (ctx) {
                          const map = ctx.getMap();
                          map.forEach((val, key) => ctx.checkResult(`${key} = ${val}`));
                        }

                        ({test})
                        """, "name").buildLiteral();

        HostAccess hostAccessExplicit = HostAccess.newBuilder(HostAccess.EXPLICIT).//
                        allowArrayAccess(true).//
                        allowListAccess(true).//
                        allowMapAccess(true).//
                        allowAccessInheritance(true).//
                        build();
        for (HostAccess hostAccess : List.of(hostAccessExplicit, HostAccess.ALL, HostAccess.EXPLICIT)) {
            try (Context context = JSTest.newContextBuilder().allowHostAccess(hostAccess).build()) {
                Value testFun = context.eval(testScript).getMember("test");

                if (hostAccess != HostAccess.EXPLICIT) {
                    testFun.executeVoid(new Ctx1(hostAccess == HostAccess.ALL));
                }
                testFun.executeVoid(new Ctx2());
                testFun.executeVoid(new Ctx3());
            }
        }
    }

    static class CustomProxyHashMap implements ProxyHashMap, ProxyObject {
        private final ProxyHashMap hash;

        CustomProxyHashMap(Map<Object, Object> backingMap) {
            this.hash = ProxyHashMap.from(backingMap);
        }

        @Override
        public Object getMember(String member) {
            switch (member) {
                case "forEach":
                    return (ProxyExecutable) (args) -> {
                        if (args.length == 0) {
                            throw new IllegalArgumentException();
                        }
                        Value callback = args[0];

                        Value hashEntriesIterator = Value.asValue(hash.getHashEntriesIterator());
                        while (hashEntriesIterator.hasIteratorNextElement()) {
                            Value iteratorNextElement = hashEntriesIterator.getIteratorNextElement();
                            Value key = iteratorNextElement.getArrayElement(0);
                            Value value = iteratorNextElement.getArrayElement(1);
                            callback.execute(value, key, this);
                        }
                        return null;
                    };
                default:
                    return null;
            }
        }

        @Override
        public boolean hasMember(String member) {
            switch (member) {
                case "forEach":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public Object getMemberKeys() {
            return ProxyArray.fromArray("forEach");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("putMember() not supported.");
        }

        @Override
        public long getHashSize() {
            return hash.getHashSize();
        }

        @Override
        public boolean hasHashEntry(Value key) {
            return hash.hasHashEntry(key);
        }

        @Override
        public Object getHashValue(Value key) {
            return hash.getHashValue(key);
        }

        @Override
        public Object getHashEntriesIterator() {
            return hash.getHashEntriesIterator();
        }

        @Override
        public void putHashEntry(Value key, Value value) {
            throw new UnsupportedOperationException("putHashEntry() not supported.");
        }
    }
}
