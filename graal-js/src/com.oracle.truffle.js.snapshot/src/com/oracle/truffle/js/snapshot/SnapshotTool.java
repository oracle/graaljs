/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;

public class SnapshotTool {
    private final TimeStats timeStats = new TimeStats();

    public SnapshotTool() {
    }

    public static void main(String[] args) throws IOException {
        boolean binary = true;
        boolean wrapped = false;
        String outDir = null;
        String inDir = null;
        List<String> srcFiles = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.equals("--java")) {
                    binary = false;
                } else if (arg.equals("--binary")) {
                    binary = true;
                } else if (arg.equals("--wrapped")) {
                    wrapped = true;
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
            try (Context polyglotContext = Context.newBuilder(JavaScriptLanguage.ID).allowIO(true).allowExperimentalOptions(true).option(JSContextOptions.CLASS_FIELDS_NAME, "true").option(
                            JSContextOptions.LAZY_TRANSLATION_NAME, "false").build()) {
                polyglotContext.initialize(JavaScriptLanguage.ID);
                polyglotContext.enter();
                for (String srcFile : srcFiles) {
                    File sourceFile = inDir == null ? new File(srcFile) : Paths.get(inDir, srcFile).toFile();
                    File outputFile = Paths.get(outDir, srcFile + (binary ? ".bin" : ".java")).toFile();
                    if (!sourceFile.isFile()) {
                        throw new IllegalArgumentException("Not a file: " + sourceFile);
                    }
                    snapshotTool.snapshotScriptFileTo(srcFile, sourceFile, outputFile, binary, wrapped);
                }
                snapshotTool.timeStats.print();
                polyglotContext.leave();
            }
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

    private void snapshotScriptFileTo(String fileName, File sourceFile, File outputFile, boolean binary, boolean wrapped) throws IOException {
        JSRealm realm = JavaScriptLanguage.getCurrentJSRealm();
        JSContext context = realm.getContext();
        Recording.logv("recording snapshot of %s", fileName);
        Source.SourceBuilder builder = Source.newBuilder(JavaScriptLanguage.ID, realm.getEnv().getPublicTruffleFile(sourceFile.getPath())).name(fileName);
        Source source = builder.build();
        String prefix;
        String suffix;
        if (wrapped) {
            // Wrapped source, i.e., source in the form
            // delimiter + prefix + delimiter + body + delimiter + suffix
            String code = source.getCharacters().toString();
            char delimiter = code.charAt(0);
            int prefixEnd = code.indexOf(delimiter, 1);
            int suffixStart = code.indexOf(delimiter, prefixEnd + 1);
            prefix = code.substring(1, prefixEnd);
            String body = code.substring(prefixEnd + 1, suffixStart);
            suffix = code.substring(suffixStart + 1);
            source = Source.newBuilder(JavaScriptLanguage.ID, body, fileName).build();
        } else {
            prefix = "";
            suffix = "";
        }
        try (TimerCloseable timer = timeStats.file(fileName)) {
            final Recording rec = Recording.recordSource(source, context, false, prefix, suffix);
            outputFile.getParentFile().mkdirs();
            try (FileOutputStream outs = new FileOutputStream(outputFile)) {
                rec.saveToStream(fileName, outs, binary);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(fileName, e);
        }
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
