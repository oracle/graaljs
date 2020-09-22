/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.jmh;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
public class JMHArrayInteropBenchmark {
    @State(Scope.Thread)
    public static class MyState {
        protected static final int ARRAY_SIZE = 10000;

        Context context;
        Source preSizedArraySource;
        Value preallocatedArray;
        Value preallocatedTypedArray;

        @Setup(Level.Trial)
        public void doSetup() {
            context = Context.create("js");
            preSizedArraySource = Source.create("js", "new Array(" + ARRAY_SIZE + ")");
            preallocatedArray = context.eval(Source.create("js", "new Array(" + ARRAY_SIZE + ").fill(0)"));
            preallocatedTypedArray = context.eval(Source.create("js", "new Int32Array(" + ARRAY_SIZE + ")"));
        }

        @TearDown(Level.Trial)
        public void doTearDown() {
            context.close();
        }
    }

    @Benchmark
    public Value testPopulateJSArrayFromJava(MyState state) {
        Value array = state.context.eval(state.preSizedArraySource);
        for (int i = 0; i < MyState.ARRAY_SIZE; i++) {
            array.setArrayElement(i, i);
        }
        return array;
    }

    @Benchmark
    public Value testWriteJSArrayFromJava(MyState state) {
        Value array = state.preallocatedArray;
        for (int i = 0; i < MyState.ARRAY_SIZE; i++) {
            array.setArrayElement(i, i);
        }
        return array;
    }

    @Benchmark
    public Value testReadJSArrayFromJava(MyState state, Blackhole blackhole) {
        Value array = state.preallocatedArray;
        for (int i = 0; i < MyState.ARRAY_SIZE; i++) {
            blackhole.consume(array.getArrayElement(i));
        }
        return array;
    }

    @Benchmark
    public Value testWriteJSTypedArrayFromJava(MyState state) {
        Value array = state.preallocatedTypedArray;
        for (int i = 0; i < MyState.ARRAY_SIZE; i++) {
            array.setArrayElement(i, i);
        }
        return array;
    }

    @Benchmark
    public Value testReadJSTypedArrayFromJava(MyState state, Blackhole blackhole) {
        Value array = state.preallocatedTypedArray;
        for (int i = 0; i < MyState.ARRAY_SIZE; i++) {
            blackhole.consume(array.getArrayElement(i));
        }
        return array;
    }
}
