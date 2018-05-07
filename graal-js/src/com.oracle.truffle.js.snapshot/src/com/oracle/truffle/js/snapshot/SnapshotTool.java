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
package com.oracle.truffle.js.snapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.nodes.NodeFactory;
import com.oracle.truffle.js.nodes.ScriptNode;
import com.oracle.truffle.js.parser.JSEngine;
import com.oracle.truffle.js.parser.JavaScriptTranslator;
import com.oracle.truffle.js.runtime.AbstractJavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;

public class SnapshotTool {
    static {
        System.setProperty("truffle.js.LazyTranslation", "false");
    }

    private final TimeStats timeStats = new TimeStats();
    private JSContext context;

    public SnapshotTool() {
    }

    static JSContext createDefaultContext() {
        return JSEngine.createJSContext();
    }

    public static void main(String[] args) throws IOException {
        assert !JSTruffleOptions.LazyTranslation;

        boolean binary = true;
        String outDir = null;
        String inDir = null;
        List<String> srcFiles = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equals("--java")) {
                    binary = false;
                } else if (arg.equals("--binary")) {
                    binary = true;
                } else if (arg.startsWith("--file=")) {
                    srcFiles.add(arg.substring(arg.indexOf('=') + 1));
                } else if (arg.startsWith("--outdir=")) {
                    outDir = requireDirectory(arg.substring(arg.indexOf('=') + 1));
                } else if (arg.startsWith("--indir=")) {
                    inDir = requireDirectory(arg.substring(arg.indexOf('=') + 1));
                }
            }
        }

        SnapshotTool snapshotTool = new SnapshotTool();
        if (!srcFiles.isEmpty() && outDir != null) {
            for (String srcFile : srcFiles) {
                File sourceFile = inDir == null ? new File(srcFile) : Paths.get(inDir, srcFile).toFile();
                File outputFile = Paths.get(outDir, srcFile + (binary ? ".bin" : ".java")).toFile();
                if (!sourceFile.isFile()) {
                    throw new IllegalArgumentException("Not a file: " + sourceFile);
                }
                snapshotTool.snapshotScriptFileTo(srcFile, sourceFile, outputFile, binary);
            }
            snapshotTool.timeStats.print();
        } else {
            System.out.println("Usage: [--java|--binary] --outdir=DIR [--indir=DIR] --file=FILE [--file=FILE ...]");
        }
    }

    private static String requireDirectory(String dir) {
        if (dir != null && !new File(dir).isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }
        return dir;
    }

    private void snapshotScriptFileTo(String fileName, File sourceFile, File outputFile, boolean binary) throws IOException {
        Recording.logv("recording snapshot of %s", fileName);
        Source source = Source.newBuilder(sourceFile).name(fileName).language(AbstractJavaScriptLanguage.ID).build();
        final Recording rec;
        try (TimerCloseable timer = timeStats.file(fileName)) {
            rec = new Recording();
            ScriptNode program = JavaScriptTranslator.translateScript(RecordingProxy.createRecordingNodeFactory(rec, NodeFactory.getInstance(getContext())), getContext(), source, false);
            rec.finish(program.getRootNode());
            outputFile.getParentFile().mkdirs();
            try (FileOutputStream outs = new FileOutputStream(outputFile)) {
                rec.saveToStream(fileName, outs, binary);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(fileName, e);
        }
    }

    private JSContext getContext() {
        if (context == null) {
            return context = createDefaultContext();
        }
        return context;
    }

    private interface TimerCloseable extends AutoCloseable {
        @Override
        void close();
    }

    private static class TimeStats {
        private final List<Map.Entry<String, Long>> entries = new ArrayList<>();

        public TimerCloseable file(String fileName) {
            long startTime = System.nanoTime();
            return () -> {
                long endTime = System.nanoTime();
                entries.add(new AbstractMap.SimpleImmutableEntry<>(fileName, endTime - startTime));
            };
        }

        public void print() {
            if (entries.isEmpty()) {
                return;
            }
            long total = 0;
            for (Map.Entry<String, Long> entry : entries) {
                System.out.printf("%s: %.02f ms\n", entry.getKey(), entry.getValue() / 1e6);
                total += entry.getValue();
            }
            System.out.printf("Total: %.02f ms\n", total / 1e6);
        }
    }
}
