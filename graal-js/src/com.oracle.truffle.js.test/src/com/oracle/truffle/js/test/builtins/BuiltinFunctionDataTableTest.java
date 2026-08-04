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
package com.oracle.truffle.js.test.builtins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import org.graalvm.polyglot.Context;
import org.junit.Test;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.test.JSTest;

public class BuiltinFunctionDataTableTest {

    private static final String BUILTINS_PACKAGE_PATH = "com/oracle/truffle/js/builtins/";
    private static final int MIN_EXPECTED_BUILTINS = 1000;
    private static final int MIN_FREE_SLOTS = (int) (0.05 * JSBuiltin.BUILTIN_FUNCTION_DATA_TABLE_SIZE);

    @Test
    public void testBuiltinFunctionDataTableCapacity() throws Exception {
        ClassLoader classLoader = JavaScriptLanguage.class.getClassLoader();
        Set<JSBuiltinsContainer> containers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (String className : findClassNames(BUILTINS_PACKAGE_PATH)) {
            Class<?> holderClass = Class.forName(className, false, classLoader);
            List<Field> containerFields = findContainerFields(holderClass);
            if (!containerFields.isEmpty()) {
                Class.forName(className, true, classLoader);
                for (Field field : containerFields) {
                    if (!field.trySetAccessible()) {
                        fail("Cannot access builtins container field " + field);
                    }
                    containers.add((JSBuiltinsContainer) field.get(null));
                }
            }
        }

        BitSet indices = new BitSet();
        for (JSBuiltinsContainer container : containers) {
            container.forEachBuiltin(builtin -> {
                indices.set(builtin.getIndex());
            });
            container.forEachAccessor((getter, setter) -> {
                if (getter != null) {
                    indices.set(getter.getIndex());
                }
                if (setter != null) {
                    indices.set(setter.getIndex());
                }
            });
        }

        try (Context context = JSTest.newContextBuilder().build()) {
            assertEquals(1337, context.eval(JavaScriptLanguage.ID, "Math.imul(7, 191)").asInt());
        }

        final int loadedCount = new JSBuiltin(null, Strings.fromJavaString("throwaway"), 0, 0, null).getIndex();
        final int builtinCount = indices.length();
        assertEquals("Builtin container discovery did not find every allocated builtin", loadedCount, indices.cardinality());
        assertTrue(String.format("Builtin function data table has fewer than %d free slots: %d of %d used. Increase the table size to ensure enough capacity.",
                        MIN_FREE_SLOTS, builtinCount, JSBuiltin.BUILTIN_FUNCTION_DATA_TABLE_SIZE),
                        builtinCount + MIN_FREE_SLOTS <= JSBuiltin.BUILTIN_FUNCTION_DATA_TABLE_SIZE);
        assertTrue(String.format("Builtin container discovery found only %d builtins, expected at least %d", builtinCount, MIN_EXPECTED_BUILTINS),
                        builtinCount >= MIN_EXPECTED_BUILTINS);

        testBuiltinFunctionDataTableFallback();
    }

    private static void testBuiltinFunctionDataTableFallback() {
        JSBuiltin overflowBuiltin;
        do {
            overflowBuiltin = new JSBuiltin(null, Strings.constant("overflow"), 0, 0, null);
        } while (overflowBuiltin.getIndex() < JSBuiltin.BUILTIN_FUNCTION_DATA_TABLE_SIZE);
        assertEquals(JSBuiltin.BUILTIN_FUNCTION_DATA_TABLE_SIZE, overflowBuiltin.getIndex());

        try (Context context = JSTest.newContextBuilder().build()) {
            JSContext jsContext = JavaScriptLanguage.getJSContext(context);
            assertNull(jsContext.getBuiltinFunctionData(overflowBuiltin));

            JSFunctionData first = JSFunctionData.create(jsContext, 0, Strings.constant("first"), false, false, false, true);
            JSFunctionData second = JSFunctionData.create(jsContext, 0, Strings.constant("second"), false, false, false, true);
            assertSame(first, jsContext.getOrInsertBuiltinFunctionData(overflowBuiltin, first));
            assertSame(first, jsContext.getOrInsertBuiltinFunctionData(overflowBuiltin, second));
            assertSame(first, jsContext.getBuiltinFunctionData(overflowBuiltin));
        }

        try (Context context = JSTest.newContextBuilder().build()) {
            JSContext jsContext = JavaScriptLanguage.getJSContext(context);
            assertNull(jsContext.getBuiltinFunctionData(overflowBuiltin));
            JSFunctionData functionData = JSFunctionData.create(jsContext, 0, Strings.constant("other context"), false, false, false, true);
            assertSame(functionData, jsContext.getOrInsertBuiltinFunctionData(overflowBuiltin, functionData));
        }
    }

    private static List<Field> findContainerFields(Class<?> holderClass) {
        List<Field> fields = new ArrayList<>();
        for (Field field : holderClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && JSBuiltinsContainer.class.isAssignableFrom(field.getType())) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static List<String> findClassNames(String basePath) throws IOException, URISyntaxException {
        Path codeSource = Path.of(JavaScriptLanguage.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classNames = new ArrayList<>();
        if (Files.isDirectory(codeSource)) {
            Path packageDirectory = codeSource.resolve(basePath);
            try (var paths = Files.walk(packageDirectory)) {
                paths.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                    classNames.add(toClassName(codeSource.relativize(path)));
                });
            }
        } else {
            try (JarFile jar = new JarFile(codeSource.toFile())) {
                jar.stream().filter(entry -> entry.getName().startsWith(basePath) && entry.getName().endsWith(".class")).forEach(entry -> {
                    classNames.add(toClassName(entry.getName()));
                });
            }
        }
        return classNames;
    }

    private static String toClassName(Path classFile) {
        return toClassName(classFile.toString().replace(classFile.getFileSystem().getSeparator(), "/"));
    }

    private static String toClassName(String classFile) {
        return classFile.substring(0, classFile.length() - ".class".length()).replace('/', '.');
    }
}
