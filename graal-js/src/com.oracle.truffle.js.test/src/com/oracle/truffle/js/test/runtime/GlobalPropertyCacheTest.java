/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

@RunWith(Parameterized.class)
public class GlobalPropertyCacheTest {

    @Parameter public boolean multiContext;

    @Parameters(name = "multiContext={0}")
    public static List<Boolean> data() {
        return List.of(false, true);
    }

    @Test
    public void instancePropertyProxyStorageAndShape() {
        Source source = Source.create("js", "function readLazyGlobal() { return lazyGlobal; } readLazyGlobal();");
        try (Engine engine = multiContext ? JSTest.newEngineBuilder().build() : null) {
            Shape expectedShape = null;
            for (int i = 0; i < 8; i++) {
                int value = i;
                try (Context context = newContextBuilder(engine).build()) {
                    context.initialize("js");
                    context.enter();
                    try {
                        var global = JavaScriptLanguage.getJSRealm(context).getGlobalObject();
                        JSObjectUtil.defineInstanceProxyProperty(global, Strings.fromJavaString("lazyGlobal"), new PropertyProxy() {
                            @Override
                            public Object get(JSDynamicObject store) {
                                return value;
                            }
                        }, JSAttributes.getDefault());
                        assertEquals(multiContext, JavaScriptLanguage.getJSRealm(context).getContext().isMultiContext());
                        assertEquals(!multiContext, global.getShape().getProperty(Strings.constant("lazyGlobal")).getLocation().isConstant());
                        assertEquals(value, context.eval(source).asInt());
                        if (multiContext) {
                            if (expectedShape != null) {
                                assertSame("Per-context proxy state must not be part of shared shapes", expectedShape, global.getShape());
                            }
                            expectedShape = global.getShape();
                        }
                    } finally {
                        context.leave();
                    }
                }
            }
        }
    }

    @Test
    public void instancePropertyProxyReadAndWrite() {
        Source source = Source.create("js", "(function writeAndRead(value) { lazyGlobal = value; return lazyGlobal; })");
        try (Engine engine = multiContext ? JSTest.newEngineBuilder().build() : null) {
            for (int i = 0; i < 3; i++) {
                try (Context context = newContextBuilder(engine).build()) {
                    context.initialize("js");
                    context.enter();
                    try {
                        var global = JavaScriptLanguage.getJSRealm(context).getGlobalObject();
                        PropertyProxy proxy = new PropertyProxy() {
                            private Object value = Undefined.instance;

                            @Override
                            public Object get(JSDynamicObject store) {
                                return value;
                            }

                            @Override
                            public boolean set(JSDynamicObject store, Object newValue) {
                                value = newValue;
                                return true;
                            }
                        };
                        JSObjectUtil.defineInstanceProxyProperty(global, Strings.fromJavaString("lazyGlobal"), proxy, JSAttributes.getDefault());
                        assertEquals(i, context.eval(source).execute(i).asInt());
                        assertEquals(i, proxy.get(global));
                    } finally {
                        context.leave();
                    }
                }
            }
        }
    }

    @Test
    public void lazyInstancePropertyProxyReplacesItself() {
        try (Engine engine = multiContext ? JSTest.newEngineBuilder().build() : null;
                        TestHelper helper = new TestHelper(newContextBuilder(engine))) {
            helper.enterContext();
            try {
                var global = helper.getGlobalObject();
                var key = Strings.fromJavaString("lazyGlobal");
                int[] calls = {0};
                JSObjectUtil.defineInstanceProxyProperty(global, key, new PropertyProxy() {
                    @Override
                    public Object get(JSDynamicObject store) {
                        calls[0]++;
                        JSObjectUtil.defineDataProperty(store, key, 42, JSAttributes.getDefault());
                        return 42;
                    }
                }, JSAttributes.getDefault());
                assertEquals(!multiContext, global.getShape().getProperty(key).getLocation().isConstant());
                var read = helper.runValue("(function readLazyGlobal() { return lazyGlobal; })");
                assertEquals(42, read.execute().asInt());
                assertEquals(42, read.execute().asInt());
                assertEquals(1, calls[0]);
                assertFalse(JSProperty.isProxy(global.getShape().getProperty(key)));
            } finally {
                helper.leaveContext();
            }
        }
    }

    @Test
    public void sharedPropertyProxyStorageAndShape() {
        try (Engine engine = multiContext ? JSTest.newEngineBuilder().build() : null) {
            Shape expectedShape = null;
            for (int i = 0; i < 3; i++) {
                try (TestHelper helper = new TestHelper(newContextBuilder(engine))) {
                    helper.enterContext();
                    try {
                        JSDynamicObject error = (JSDynamicObject) helper.runNoPolyglot("new Error('error " + i + "')");
                        var property = error.getShape().getProperty(JSError.STACK_NAME);
                        assertEquals(multiContext, helper.getJSContext().isMultiContext());
                        assertTrue(property.getLocation().isConstant());
                        assertSame(JSError.STACK_PROXY, JSProperty.getConstantProxy(property));
                        assertTrue(Strings.toJavaString((TruffleString) JSObject.get(error, JSError.STACK_NAME)).startsWith("Error: error " + i));
                        if (multiContext) {
                            if (expectedShape != null) {
                                assertSame("Shared proxies must retain shared shapes", expectedShape, error.getShape());
                            }
                            expectedShape = error.getShape();
                        }
                    } finally {
                        helper.leaveContext();
                    }
                }
            }
        }
    }

    private static Context.Builder newContextBuilder(Engine engine) {
        Context.Builder builder = JSTest.newContextBuilder();
        if (engine != null) {
            builder.engine(engine);
        }
        return builder;
    }

}
