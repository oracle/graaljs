/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
        // - ignore all through
        // - control variable 'i' is modified
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                case "--help":
                    printUsageAndExit(0);
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
