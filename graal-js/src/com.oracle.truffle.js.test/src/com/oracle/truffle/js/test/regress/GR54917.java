/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

@RunWith(Parameterized.class)
public class GR54917 {

    @Parameterized.Parameters(name = "{0}")
    public static List<Boolean> data() {
        return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
    }

    @Parameterized.Parameter(value = 0) public boolean arrayElementsAmongMembers;

    @Test
    public void testArray() {
        testIt("[]");
    }

    @Test
    public void testTypedArray() {
        testIt("new Uint8Array(2)");
    }

    @Test
    public void testArguments() {
        testIt("(function() { return arguments; })()");
    }

    private void testIt(String arrayInit) {
        try (Context ctx = JSTest.newContextBuilder().option(JSContextOptions.ARRAY_ELEMENTS_AMONG_MEMBERS_NAME, String.valueOf(arrayElementsAmongMembers)).allowAllAccess(true).build()) {
            Value array = ctx.eval("js", "var array = " + arrayInit + "; array[0] = 2; array[1] = 4; array.myFlag = true; array");

            Set<String> expected = arrayElementsAmongMembers ? Set.of("0", "1", "myFlag") : Set.of("myFlag");
            assertEquals(expected, array.getMemberKeys());

            checkMap(array.as(Map.class));

            ctx.getBindings("js").putMember("o", this);
            ctx.eval("js", "o.callback(array);");
        }
    }

    public void callback(Map<?, ?> map) {
        checkMap(map);
    }

    private void checkMap(Map<?, ?> map) {
        assertEquals(arrayElementsAmongMembers ? 3 : 1, map.size());
        assertEquals(true, map.get("myFlag"));
        if (arrayElementsAmongMembers) {
            assertEquals(2, ((Number) map.get("0")).intValue());
            assertEquals(4, ((Number) map.get("1")).intValue());
        }
    }

}
