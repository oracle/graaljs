/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.nodes.function.FunctionRootNode;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

/**
 * Provides ASTs for internal scripts.
 */
public class InternalTranslationProvider {
    private static final String RESOURCES_PATH = "resources/";

    private static final String[] internalFileNames = new String[]{"annexb.js", "promise.js", "string.js", "typedarray.js"};
    private static final Map<String, Source> internalSources;

    private static final String SNAPSHOT_CLASS_PREFIX = "com.oracle.truffle.js.parser.snapshots.Internal_";
    private static final String SNAPSHOT_PATH_PREFIX = "/com/oracle/truffle/js/parser/snapshots/Internal_";
    private static final Map<String, SnapshotProvider> internalSnapshots;

    static {
        internalSources = new HashMap<>();
        internalSnapshots = JSTruffleOptions.Snapshots ? new HashMap<>() : null;

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                for (String fileName : internalFileNames) {
                    internalSources.put(fileName, readInternalFileSource(fileName));

                    if (JSTruffleOptions.Snapshots) {
                        try (InputStream inputStream = InternalTranslationProvider.class.getResourceAsStream(binaryResourceNameFromFileName(fileName))) {
                            if (inputStream != null) {
                                internalSnapshots.put(fileName, new BinarySnapshotProvider(readAllBytes(inputStream)));
                                continue;
                            }
                        } catch (IOException e) {
                        }
                        try {
                            String className = classNameFromFileName(fileName);
                            Class<?> snapshotClass = Class.forName(className);
                            SnapshotProvider snapshot = (SnapshotProvider) snapshotClass.newInstance();
                            internalSnapshots.put(fileName, snapshot);
                            continue;
                        } catch (ClassNotFoundException e) {
                            // snapshot class not found
                        } catch (InstantiationException | IllegalAccessException e) {
                            // could not instantiate snapshot class
                        }
                    }
                }
                return null;
            }
        });
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        int bufferSize = inputStream.available();
        if (bufferSize <= 0) {
            bufferSize = 1024;
        }
        byte[] buffer = new byte[bufferSize];
        int bytesRead = 0;
        for (;;) {
            int n;
            while ((n = inputStream.read(buffer, bytesRead, bufferSize - bytesRead)) > 0) {
                bytesRead += n;
                if (bytesRead == bufferSize) {
                    break; // optional; skip useless read with len 0
                }
            }
            if (n < 0) {
                break; // EOF
            }
            // buffer full; check if we're already at EOF, else grow buffer
            assert bytesRead == bufferSize;
            int b = inputStream.read();
            if (b < 0) {
                break; // EOF
            } else {
                bufferSize = Math.addExact(bufferSize, bufferSize);
                buffer = Arrays.copyOf(buffer, bufferSize);
                buffer[bytesRead++] = (byte) b;
            }
        }
        if (bytesRead == bufferSize) {
            return buffer;
        } else {
            return Arrays.copyOf(buffer, bytesRead);
        }
    }

    public static String classNameFromFileName(String fileName) {
        return SNAPSHOT_CLASS_PREFIX + mangleFileName(fileName);
    }

    public static String binaryResourceNameFromFileName(String fileName) {
        return SNAPSHOT_PATH_PREFIX + mangleFileName(fileName) + ".bin";
    }

    /**
     * Replace non-word characters with {@code '_'}.
     */
    private static String mangleFileName(String fileName) {
        StringBuilder sb = null;
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);
            if (!isAsciiWordChar(ch)) {
                if (sb == null) {
                    sb = new StringBuilder(fileName);
                }
                sb.setCharAt(i, '_');
            }
        }
        return sb == null ? fileName : sb.toString();
    }

    private static boolean isAsciiWordChar(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || (ch == '_');
    }

    private static Source readInternalFileSource(String fileName) {
        InputStream stream = JSRealm.class.getResourceAsStream(RESOURCES_PATH + fileName);
        try {
            return Source.newBuilder(new InputStreamReader(stream)).name(JSRealm.INTERNAL_JS_FILE_NAME_PREFIX + fileName).language(AbstractJavaScriptLanguage.ID).internal().build();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Source getInternalFileSource(String fileName) {
        assert internalSources.containsKey(fileName) : "Internal source file not registered: " + fileName;
        return internalSources.get(fileName);
    }

    public static ScriptNode translateInternal(JSContext context, String fileName) {
        Source source = getInternalFileSource(fileName);
        if (JSTruffleOptions.Snapshots) {
            SnapshotProvider snapshotProvider = internalSnapshots.get(fileName);
            if (snapshotProvider != null) {
                return ScriptNode.fromFunctionRoot(context, (FunctionRootNode) snapshotProvider.apply(InternalTranslator.INTERNAL_NODE_FACTORY, context, source));
            }
        }

        try {
            return InternalTranslator.translateSource(context, source);
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage(), e, null);
        }
    }

    public static void forEachInternalSourceFile(Consumer<String> consumer) {
        Arrays.asList(internalFileNames).forEach(consumer);
    }

    public static ScriptNode interceptTranslation(JSContext context, String fileName, Function<NodeFactory, NodeFactory> nodeFactorySupplier) {
        try {
            return InternalTranslator.translateSource(nodeFactorySupplier.apply(InternalTranslator.INTERNAL_NODE_FACTORY), context, internalSources.get(fileName));
        } catch (com.oracle.js.parser.ParserException e) {
            throw Errors.createSyntaxError(e.getMessage(), e, null);
        }
    }
}
