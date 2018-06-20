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
package com.oracle.truffle.js.stats.heap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.JavaClass;

import com.oracle.truffle.api.impl.DefaultCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCloneable;

public class HeapDumpAnalyzer {
    private static final String NODE = Node.class.getName();
    private static final String DEFAULT_CALL_TARGET = DefaultCallTarget.class.getName();
    private static final String OPTIMIZED_CALL_TARGET = "org.graalvm.compiler.truffle.OptimizedCallTarget";
    private static final String NODE_CLONEABLE = NodeCloneable.class.getName();

    public static void analyzeHeap(List<String> classNames, List<File> dumps) throws IOException {
        for (File dump : dumps) {
            String dumpName = dump.getName();
            Heap heap = HeapFactory.createHeap(dump);
            System.out.println(dumpName + "\tTotal.JavaHeap\tsize:\t" + heap.getSummary().getTotalLiveBytes());

            for (String className : classNames) {
                long instances = 0L;
                long size = 0L;

                JavaClass javaClass = heap.getJavaClassByName(className);
                if (javaClass != null) {
                    @SuppressWarnings("unchecked")
                    Collection<JavaClass> subClasses = javaClass.getSubClasses();
                    subClasses.add(javaClass);
                    for (JavaClass subClass : subClasses) {
                        instances += subClass.getInstancesCount();
                        size += subClass.getAllInstancesSize();
                    }

                    String prefix = dumpName + "\t" + className;
                    System.out.println(prefix + "\tinstances:\t" + instances);
                    System.out.println(prefix + "\tsize:\t" + size);
                }
            }
        }
    }

    private static void printUsageAndExit(int exitStatus) {
        System.out.println("Usage:");
        System.out.println("\tjava " + HeapDumpAnalyzer.class.getName() + " [-c <class name>]... dumps ...\n");
        System.out.println("positional arguments:");
        System.out.println("\theap dumps...\n");
        System.out.println("optional arguments:");
        System.out.println("\t-c <class name>, --class <class name>");
        System.out.println("\t\t\treport statistics about the subtypes of <className>");
        System.out.println("\t\t\tDefault: " + NODE);
        System.exit(exitStatus);
    }

    public static void main(String[] args) throws IOException {
        List<String> classNames = new LinkedList<>();
        List<File> dumps = new LinkedList<>();

        // Checkstyle: stop
        // - control variable 'i' is modified
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    printUsageAndExit(0);
                    break;
                case "-c":
                case "--class":
                    if (++i < args.length) {
                        classNames.add(args[i]);
                    } else {
                        printUsageAndExit(1);
                    }
                    break;
                default:
                    dumps.add(new File(args[i]));
                    break;
            }
        }
        // Checkstyle: resume

        if (classNames.size() == 0) {
            classNames.addAll(Arrays.asList(NODE, NODE_CLONEABLE, DEFAULT_CALL_TARGET, OPTIMIZED_CALL_TARGET));
        }
        if (dumps.size() == 0) {
            printUsageAndExit(2);
        }

        analyzeHeap(classNames, dumps);
    }
}
