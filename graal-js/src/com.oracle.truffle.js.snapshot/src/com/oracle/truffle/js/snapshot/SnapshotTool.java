/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.IOAccess;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.JSRealm;

public class SnapshotTool {

    private boolean binary = true;
    private boolean wrapped = false;
    private String outDir = null;
    private String inDir = null;
    private List<String> srcFiles = new ArrayList<>();
    private int parallelism = Math.min(4, Runtime.getRuntime().availableProcessors());

    public SnapshotTool(String[] args) {
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
                } else if (arg.startsWith("--threads=")) {
                    parallelism = Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new SnapshotTool(args).run();
    }

    @SuppressWarnings("try")
    private void run() throws InterruptedException, ExecutionException {
        List<Callable<Void>> tasks = new ArrayList<>();
        if (!srcFiles.isEmpty() && outDir != null) {
            try (var timeStats = new TimeStats()) {
                for (String srcFileName : srcFiles) {
                    File sourceFile = inDir == null ? new File(srcFileName) : Paths.get(inDir, srcFileName).toFile();
                    File outputFile = Paths.get(outDir, srcFileName + (binary ? ".bin" : ".java")).toFile();
                    if (!sourceFile.isFile()) {
                        throw new IllegalArgumentException("Not a file: " + sourceFile);
                    }

                    tasks.add(() -> {
                        try (var timer = timeStats.file(srcFileName)) {
                            snapshotScriptFileTo(srcFileName, sourceFile, outputFile, binary, wrapped);
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException(String.join(": ", srcFileName, e.getMessage()), e);
                        }
                    });
                }

                var executor = new ForkJoinPool(parallelism, pool -> new JSContextWorkerThread(pool), null, true);
                var futures = executor.invokeAll(tasks);
                // Check that all tasks succeeded.
                for (var future : futures) {
                    future.get();
                }
            }
        } else {
            usage();
        }
    }

    private static Context newContext() {
        return Context.newBuilder(JavaScriptLanguage.ID).allowExperimentalOptions(true).//
                        allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build()).//
                        option(JSContextOptions.LAZY_TRANSLATION_NAME, "false").//
                        build();
    }

    private static final class JSContextWorkerThread extends ForkJoinWorkerThread {
        private Context polyglotContext;

        private JSContextWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

        @Override
        protected void onStart() {
            polyglotContext = newContext();
            polyglotContext.initialize(JavaScriptLanguage.ID);
            polyglotContext.enter();
        }

        @Override
        protected void onTermination(Throwable exception) {
            polyglotContext.leave();
            polyglotContext.close();
        }
    }

    private void usage() {
        System.out.println("Usage: [--java|--binary|--wrapped] --outdir=DIR [--indir=DIR] --file=FILE [--file=FILE ...] [--threads=%d]".formatted(parallelism));
    }

    private static String requireDirectory(String dir) {
        if (dir != null && !new File(dir).isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }
        return dir;
    }

    private static void snapshotScriptFileTo(String fileName, File sourceFile, File outputFile, boolean binary, boolean wrapped) throws IOException {
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
        final Recording rec = Recording.recordSource(source, context, false, prefix, suffix);
        outputFile.getParentFile().mkdirs();
        try (FileOutputStream outs = new FileOutputStream(outputFile)) {
            rec.saveToStream(fileName, outs, binary);
        }
    }

    private static final class TimeStats implements AutoCloseable {
        private final Map<String, Long> entries = Collections.synchronizedMap(new LinkedHashMap<>());
        private final long realStartTime = System.nanoTime();

        public AutoCloseable file(String fileName) {
            long startTime = System.nanoTime();
            return () -> {
                long endTime = System.nanoTime();
                entries.put(fileName, endTime - startTime);
            };
        }

        @Override
        public void close() {
            if (entries.isEmpty()) {
                return;
            }
            long total = 0;
            long realEndTime = System.nanoTime();
            for (Map.Entry<String, Long> entry : entries.entrySet()) {
                System.out.printf("%s: %.02f ms\n", entry.getKey(), ms(entry.getValue()));
                total += entry.getValue();
            }
            if (entries.size() > 1) {
                System.out.printf("Total: %.02f ms, real: %.02f ms\n", ms(total), ms(realEndTime - realStartTime));
            }
        }

        private static double ms(long ns) {
            return ns / 1e6;
        }
    }
}
