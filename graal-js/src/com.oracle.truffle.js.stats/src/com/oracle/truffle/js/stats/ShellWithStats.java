/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.stats;

import static com.oracle.truffle.js.shell.JSLauncher.PreprocessResult.Consumed;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;

import com.oracle.truffle.js.builtins.helper.HeapDump;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.shell.RepeatingLauncher;
import com.oracle.truffle.js.stats.heap.HeapDumpAnalyzer;

public class ShellWithStats extends RepeatingLauncher {
    private boolean heapDump = JSTruffleOptions.DumpHeapOnExit;

    public static void main(String[] args) {
        new ShellWithStats().launch(args);
    }

    @Override
    protected PreprocessResult preprocessArgument(String argument, Map<String, String> polyglotOptions) {
        if (argument.equals("heap-dump")) {
            heapDump = true;
            return Consumed;
        }
        return super.preprocessArgument(argument, polyglotOptions);
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        super.printHelp(maxCategory);
        printOption("--heap-dump", "take a heap dump at the end of the execution");
    }

    @Override
    protected int executeScripts(Context.Builder contextBuilder) {
        int result = super.executeScripts(contextBuilder);
        if (!JSTruffleOptions.SubstrateVM && heapDump) {
            try {
                String dumpName = JSTruffleOptions.HeapDumpFileName == null ? HeapDump.defaultDumpName() : JSTruffleOptions.HeapDumpFileName;
                deleteIfExists(dumpName);
                System.out.println("Dumping the heap to: " + dumpName);
                HeapDump.dump(dumpName, true);
                HeapDumpAnalyzer.main(new String[]{dumpName});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private static void deleteIfExists(String dumpName) {
        File dumpFile = new File(dumpName);
        if (dumpFile.exists()) {
            System.out.println("Deleting existing file: " + dumpName);
            dumpFile.delete();
        }
    }
}
