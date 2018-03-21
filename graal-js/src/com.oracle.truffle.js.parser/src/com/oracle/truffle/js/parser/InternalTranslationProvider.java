/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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

    private static final String[] internalFileNames = new String[]{"array.js", "annexb.js", "iterator.js", "promise.js", "string.js", "typedarray.js"};
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
            return Source.newBuilder(new InputStreamReader(stream)).name(JSRealm.INTERNAL_JS_FILE_NAME_PREFIX + fileName).mimeType(AbstractJavaScriptLanguage.APPLICATION_MIME_TYPE).internal().build();
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
