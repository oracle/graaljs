/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.helper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.management.MBeanServer;

import com.sun.management.HotSpotDiagnosticMXBean;

public class HeapDump {
    // Derived from:
    // https://blogs.oracle.com/sundararajan/entry/programmatically_dumping_heap_from_java
    public static void dump(String fileName, boolean live) throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        hotspotMBean.dumpHeap(fileName, live);
    }

    public static String defaultDumpName() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();
        return "heapdump-" + dtf.format(now) + ".hprof";
    }
}
