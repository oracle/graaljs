/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.Test;

public class GR28474 {

    @Test
    public void testGH401() {
        try (Context ctx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).allowExperimentalOptions(true).option("js.nashorn-compat", "true").build()) {
            ctx.getBindings("js").putMember("self", new Bug401());

            assertEquals(20, ctx.eval("js", "self.longValue;").asLong());
            assertEquals(10, ctx.eval("js", "Math.min(self.longValue + 1, 10);").asLong());
            assertEquals(20, ctx.eval("js", "self.intValue;").asInt());
            assertEquals(10, ctx.eval("js", "Math.min(self.intValue + 1, 10);").asInt());
            assertEquals(20.0, ctx.eval("js", "self.doubleValue;").asDouble(), 0.0);
            assertEquals(10.0, ctx.eval("js", "Math.min(self.doubleValue + 1, 10);").asDouble(), 0.0);
            assertEquals(true, ctx.eval("js", "self.boolValue;").asBoolean());

            assertEquals(20f, ctx.eval("js", "self.floatValue;").asFloat(), 0f);
            assertEquals(10f, ctx.eval("js", "Math.min(self.floatValue + 1, 10);").asFloat(), 0f);
            assertEquals("a", ctx.eval("js", "self.charValue;").asString());
            assertEquals(20, ctx.eval("js", "self.shortValue;").asShort());
            assertEquals(10, ctx.eval("js", "Math.min(self.shortValue + 1, 10);").asShort());
            assertEquals(20, ctx.eval("js", "self.byteValue;").asByte());
            assertEquals(10, ctx.eval("js", "Math.min(self.shortValue + 1, 10);").asByte());
        }
    }

    public static class Bug401 {
        public long getLongValue() {
            return 20;
        }

        public int getIntValue() {
            return 20;
        }

        public double getDoubleValue() {
            return 20;
        }

        public float getFloatValue() {
            return 20;
        }

        public char getCharValue() {
            return 'a';
        }

        public short getShortValue() {
            return 20;
        }

        public byte getByteValue() {
            return 20;
        }

        public boolean getBoolValue() {
            return true;
        }
    }
}
