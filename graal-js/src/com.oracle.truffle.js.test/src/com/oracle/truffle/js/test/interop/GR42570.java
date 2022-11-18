/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Tests of foreign object support in ToPropertyDescriptor().
 */
public class GR42570 {

    @Test
    public void testData() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = "[true, false].forEach(" +
                            "enumerable => [true, false].forEach(" +
                            "configurable => [true, false].forEach(" +
                            "writable => {" +
                            "  descriptor.enumerable = enumerable;" +
                            "  descriptor.configurable = configurable;" +
                            "  descriptor.writable = writable;" +
                            "  descriptor.value = 42;" +
                            "  var o = {};" +
                            "  Object.defineProperty(o, 'key', descriptor);" +
                            "  var actual = Object.getOwnPropertyDescriptor(o, 'key');" +
                            "  if (enumerable !== actual.enumerable || configurable !== actual.configurable || writable !== actual.writable || 42 !== actual.value) {" +
                            "    throw new Error('Failure: ' + enumerable + ' ' + configurable + ' ' + writable);" +
                            "  }" +
                            "})))";
            context.getBindings(JavaScriptLanguage.ID).putMember("descriptor", ProxyObject.fromMap(new HashMap<>()));
            context.eval(JavaScriptLanguage.ID, code);
        }
    }

    @Test
    public void testAccessor() {
        try (Context context = JSTest.newContextBuilder().build()) {
            ProxyObject descriptor = ProxyObject.fromMap(new HashMap<>());
            Value foreignMethod = Value.asValue(new ProxyExecutable() {
                @Override
                public Object execute(Value... arguments) {
                    return 42;
                }
            });
            descriptor.putMember("set", foreignMethod);
            descriptor.putMember("get", foreignMethod);
            String code = "var o = {};" +
                            "Object.defineProperty(o, 'key', descriptor);" +
                            "var actual = Object.getOwnPropertyDescriptor(o, 'key');" +
                            "if (descriptor.set !== actual.set || descriptor.get !== actual.get) {" +
                            "  throw new Error();" +
                            "}";
            context.getBindings(JavaScriptLanguage.ID).putMember("descriptor", descriptor);
            context.eval(JavaScriptLanguage.ID, code);
        }
    }

}
