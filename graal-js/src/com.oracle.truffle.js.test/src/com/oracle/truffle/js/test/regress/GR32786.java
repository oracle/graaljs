/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.regress;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import com.oracle.truffle.js.test.JSTest;

/**
 * GitHub issue https://github.com/oracle/graaljs/issues/474.
 */
public class GR32786 extends JSTest {

    public static class AnyClass {
        public void fail() throws Exception {
            throw new Exception("expected message");
        }
    }

    @Test
    public void async() {
        try (Context context = Context.newBuilder().allowHostAccess(HostAccess.ALL).build()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            context.eval(Source.newBuilder("js", """
                            async function someJsFun() {
                                AnyClass.fail();
                            }
                            """, "async-snippet").buildLiteral());

            context.getBindings("js").putMember("AnyClass", new AnyClass());

            Value asyncFn = context.getBindings("js").getMember("someJsFun");
            Value asyncPromise = asyncFn.execute();

            Consumer<Object> javaThen = (error) -> {
                PolyglotException exception = context.asValue(error).as(PolyglotException.class);
                exception.printStackTrace(new PrintStream(out));
            };
            asyncPromise.invokeMember("catch", javaThen);

            assertThat(out.toString(), containsString("expected message"));
            assertThat(out.toString(), containsString("someJsFun(async-snippet"));
        }
    }

    @Test
    public void sync() {
        try (Context context = Context.newBuilder().allowHostAccess(HostAccess.ALL).build()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            context.eval(Source.newBuilder("js", """
                            function someJsFun() {
                                AnyClass.fail();
                            }
                            """, "sync-snippet").buildLiteral());

            context.getBindings("js").putMember("AnyClass", new AnyClass());

            Value syncFn = context.getBindings("js").getMember("someJsFun");
            try {
                syncFn.execute();
                fail("should have thrown a PolyglotException");
            } catch (PolyglotException ex) {
                ex.printStackTrace(new PrintStream(out));
                assertThat(out.toString(), containsString("expected message"));
                assertThat(out.toString(), containsString("someJsFun(sync-snippet"));
            }
        }
    }

}
