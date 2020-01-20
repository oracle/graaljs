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

import java.util.Random;
import java.util.stream.IntStream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
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

@Warmup(iterations = 2)
@Measurement(iterations = 2)
@Fork(2)
public class JMHJsObjectInteropTest {
    @State(Scope.Thread)
    public static class MyState {
        public static final int MIN_PROPERTY_KEY_LENGHT = 3;
        public static final int MAX_PROPERTY_KEY_LENGHT = 10;
        public static final int MIN_PROPERTY_VALUE_LENGHT = 3;
        public static final int MAX_PROPERTY_VALUE_LENGHT = 50;
        public static final int PROPERTIES_COUNT = 10;
        public static final Character[] ALLOWED_CHARS = IntStream.range(0, 256).filter(i -> Character.isAlphabetic(i) || Character.isDigit(i)).mapToObj(i -> (char) i).toArray(Character[]::new);

        String generateString(int lenght) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lenght; i++) {
                sb.append(ALLOWED_CHARS[rnd.nextInt(ALLOWED_CHARS.length)]);
            }
            return sb.toString();
        }

        Engine engine;
        Context context;
        Source emptyObjectSource;
        String[] propertyKeys;
        Random rnd;

        @Setup(Level.Trial)
        public void doSetup() {
            engine = Engine.newBuilder().build();
            context = Context.newBuilder("js").engine(engine).build();
            emptyObjectSource = Source.create("js", "new Object()");
            rnd = new Random();
            propertyKeys = IntStream.range(0, PROPERTIES_COUNT).mapToObj(i -> generateString(3 + rnd.nextInt(MAX_PROPERTY_KEY_LENGHT - MIN_PROPERTY_KEY_LENGHT + 1))).toArray(
                            String[]::new);
        }

        @TearDown(Level.Trial)
        public void doTearDown() {
            context.close();
        }
    }

    @Benchmark
    public Value testPopulateJSObjectFromJava(MyState state) {
        Value object = state.context.eval(state.emptyObjectSource);
        for (int i = 0; i < MyState.PROPERTIES_COUNT; i++) {
            object.putMember(state.propertyKeys[i], state.generateString(3 + state.rnd.nextInt(MyState.MAX_PROPERTY_VALUE_LENGHT - MyState.MIN_PROPERTY_VALUE_LENGHT + 1)));
        }
        return object;
    }

}
