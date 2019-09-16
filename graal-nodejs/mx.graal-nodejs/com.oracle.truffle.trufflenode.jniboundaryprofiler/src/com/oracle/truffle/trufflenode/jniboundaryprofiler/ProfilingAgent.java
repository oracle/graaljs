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
package com.oracle.truffle.trufflenode.jniboundaryprofiler;

import java.lang.instrument.Instrumentation;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSFunction;

public class ProfilingAgent {

    /* Dump the performance counters at the given interval. If zero, dumps only at VM exit. */
    public static final int DumpEvery = Integer.getInteger("node.native.profiler.interval", 0);

    /* Dump only the hottest methods. If zero, dumps all methods. */
    public static final int DumpOnlyTopMethods = Integer.getInteger("node.native.profiler.dumptop", 0);

    private static final Deque<String> callStack = new ArrayDeque<>();
    private static final Map<String, PerfCounter> bindingExecTimes = new HashMap<>(100);
    private static final Map<String, PerfCounter> bindingCalls = new HashMap<>(100);
    private static final Map<String, Map<String, PerfCounter>> jniExecTimes = new HashMap<>(100);
    private static final Map<String, Map<String, PerfCounter>> jniCalls = new HashMap<>(100);

    private static long last = System.nanoTime();
    private static long lastJniCallBegin = 0;

    private static Map<String, PerfCounter> currentJNICalls;
    private static Map<String, PerfCounter> currentJNIExecTimes;

    private static int jniMethodCallStack = 0;
    private static long firstBoundaryCrossedAt = 0;

    public static void premain(@SuppressWarnings("unused") String agentArgs, Instrumentation inst) {
        System.out.println("=== Native boundary profiling agent active ===");
        inst.addTransformer(new ProfilingTransformer());
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                dumpCounters();
            }
        });
    }

    private static class PerfCounter implements Comparable<PerfCounter> {

        private long value;

        PerfCounter() {
            this.value = 0;
        }

        public long longValue() {
            return value;
        }

        public void increment() {
            this.value++;
        }

        public void increment(long inc) {
            this.value += inc;
        }

        public int compareTo(PerfCounter o) {
            return (value < o.value) ? -1 : ((value == o.value) ? 0 : 1);
        }
    }

    /* For testing in node applications */
    public static long getNativeCalls(String lbl) {
        // Testing only: we assume the keys must exist as part of the test
        return bindingCalls.get(lbl).longValue();
    }

    /* For testing in node applications */
    public static long getJniCalls(String binding, String jniLabel) {
        // Testing only: we assume the keys must exist as part of the test
        return jniCalls.get(binding).get(jniLabel).longValue();
    }

    private static String getLabel(String apiName, Object label) throws AssertionError {
        String lbl;
        if (JSFunction.isJSFunction(label)) {
            lbl = JSFunction.getName((DynamicObject) label);
        } else if (label instanceof Object[]) {
            Object[] args = (Object[]) label;
            assert args.length > 1 && JSFunction.isJSFunction(args[1]);
            lbl = JSFunction.getName((DynamicObject) args[1]);
        } else {
            throw new AssertionError("Must instrument calls to JSFunction objects");
        }
        lbl = "".equals(lbl) ? apiName + ": <unknown>" : apiName + ": " + lbl;
        return lbl;
    }

    public static double getSamplingTime() {
        long elapsedTime = (System.nanoTime() - last);
        return elapsedTime / 1_000_000_000.0;
    }

    /**
     * Called by the instrumentation when some JS code calls a native C++ node.js binding via
     * {@code ExecuteNativeFunctionNode}'s "execute" method.
     */
    @TruffleBoundary
    public static void bindingCallBegin(String apiName, Object label) {
        String lbl = getLabel(apiName, label);
        if (callStack.size() == 0) {
            Map<String, PerfCounter> calls = jniCalls.get(lbl);
            if (calls == null) {
                calls = new HashMap<>();
                jniCalls.put(lbl, calls);
            }
            currentJNICalls = calls;

            Map<String, PerfCounter> times = jniExecTimes.get(lbl);
            if (times == null) {
                times = new HashMap<>();
                jniExecTimes.put(lbl, times);
            }
            currentJNIExecTimes = times;
            firstBoundaryCrossedAt = System.nanoTime();
        }
        callStack.push(lbl);
    }

    /**
     * Called by the instrumentation when a native C++ method returns to JS.
     */
    @TruffleBoundary
    public static void bindingCallEnd() {
        String lbl = callStack.pop();
        if (callStack.size() == 0) {
            if (jniMethodCallStack != 0) {
                // some exception was thrown and the instrumentation failed: cannot trust numbers
                throw new AssertionError("Broken instrumentation! (not all JNI method calls have returned: " + jniMethodCallStack + ")");
            }
            long end = System.nanoTime();

            PerfCounter totalHits = bindingCalls.get(lbl);
            if (totalHits == null) {
                totalHits = new PerfCounter();
                bindingCalls.put(lbl, totalHits);
            }
            totalHits.increment();

            long elapsedTime = end - firstBoundaryCrossedAt;
            PerfCounter total = bindingExecTimes.get(lbl);
            if (total == null) {
                total = new PerfCounter();
                bindingExecTimes.put(lbl, total);
            }
            total.increment(elapsedTime);
            jniExecTimes.put(lbl, currentJNIExecTimes);

            if (DumpEvery > 0 && getSamplingTime() > DumpEvery) {
                dumpCounters();
            }
        }
    }

    /**
     * Called by the instrumentation when a Java method is called during a C++ binding execution.
     * Since all Java calls happen via JNI, this gives an estimate of the number of JNI boundaries
     * that we cross via {@code GraalJSAccess}.
     */
    @TruffleBoundary
    public static void jniCallBegin(String lbl) {
        if (callStack.size() != 0) {
            if (jniMethodCallStack++ == 0) {
                PerfCounter totalHits = currentJNICalls.get(lbl);
                if (totalHits == null) {
                    totalHits = new PerfCounter();
                    currentJNICalls.put(lbl, totalHits);
                }
                totalHits.increment();
                lastJniCallBegin = System.nanoTime();
            }
        }
    }

    /**
     * Called by the instrumentation when Java method returns back to native C++.
     */
    @TruffleBoundary
    public static void jniCallEnd(String lbl) {
        if (callStack.size() != 0) {
            if (--jniMethodCallStack == 0) {
                long elapsedTime = System.nanoTime() - lastJniCallBegin;
                PerfCounter total = currentJNIExecTimes.get(lbl);
                if (total == null) {
                    total = new PerfCounter();
                    currentJNIExecTimes.put(lbl, total);
                }
                total.increment(elapsedTime);
            }
        }
    }

    @TruffleBoundary
    public static void dumpCounters() {
        double window = getSamplingTime();
        last = System.nanoTime();
        System.out.println("\n=== Sampling interval: " + window + " seconds ===");

        Map<String, PerfCounter> sortedTimes = bindingExecTimes.entrySet().stream().sorted(Collections.reverseOrder(Entry.comparingByValue())).collect(
                        Collectors.toMap(Entry::getKey, Entry::getValue,
                                        (e1, e2) -> e1, LinkedHashMap::new));

        System.out.println("\n=== Time spent in node.js native calls ===");
        for (Entry<String, PerfCounter> entry : sortedTimes.entrySet()) {
            double time = entry.getValue().longValue() / 1000000.0;
            double perc = (time / (window * 1000)) * 100;
            String line = String.format("[%6.2f %%] %-80s |time %10.3f ms |#calls %7d (JS->Cpp)", perc, entry.getKey(), time, bindingCalls.get(entry.getKey()).longValue());
            System.out.println(line);
        }

        System.out.println("\n=== Breakdown of Java methods executed during native calls (presumibly JNI calls) ===");

        int dumped = 0;
        for (Entry<String, PerfCounter> entry : sortedTimes.entrySet()) {
            double nativeTime = entry.getValue().longValue() / 1000000.0;
            double perc = (nativeTime / (window * 1000)) * 100;
            String header = String.format("[%6.2f %%] %-80s ", perc, entry.getKey());
            System.out.println(header);

            Map<String, PerfCounter> jniTime = jniExecTimes.get(entry.getKey());
            Map<String, PerfCounter> sortedCalls = jniCalls.get(entry.getKey()).entrySet().stream().sorted(Collections.reverseOrder(Entry.comparingByValue())).collect(
                            Collectors.toMap(Entry::getKey, Entry::getValue,
                                            (e1, e2) -> e1, LinkedHashMap::new));
            double total = 0;
            double totalTime = 0;
            for (Entry<String, PerfCounter> nestedentry : sortedCalls.entrySet()) {
                double time = jniTime.get(nestedentry.getKey()) == null ? 0 : jniTime.get(nestedentry.getKey()).longValue() / 1000000.0;
                double ratio = nestedentry.getValue().longValue() / (double) bindingCalls.get(entry.getKey()).longValue();
                String nestedLine = String.format("           %-91s |#calls %7d |time %10.3f ms |jni calls avg ~%4.1f (Cpp->JS)", nestedentry.getKey(), nestedentry.getValue().longValue(),
                                time,
                                ratio);
                System.out.println(nestedLine);
                total += ratio;
                totalTime += time;
            }

            System.out.println(String.format("\n          %92s |total native time               %10.3f ms", "", nativeTime));
            System.out.println(String.format("          %92s |total time in Java space (~)    %10.3f ms", "", totalTime));
            System.out.println(String.format("          %92s |total native calls                    %7d ", "", bindingCalls.get(entry.getKey()).longValue()));
            System.out.println(String.format("          %92s |avg JNI Java calls per native call (~)   %4.1f \n", "", total));

            if (DumpOnlyTopMethods > 0 && ++dumped == DumpOnlyTopMethods) {
                break;
            }
        }
        jniExecTimes.clear();
        jniCalls.clear();
        bindingCalls.clear();
        bindingExecTimes.clear();
    }

}
