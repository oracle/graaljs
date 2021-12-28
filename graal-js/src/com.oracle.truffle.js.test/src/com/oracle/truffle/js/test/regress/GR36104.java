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

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class GR36104 {

    private static final Integer[] ECMASCRIPT_VERSIONS = new Integer[]{
                    null, // default
                    JSConfig.ECMAScript2021,
                    JSConfig.ECMAScript2022,
    };

    private static final Object[] TOP_LEVEL_AWAIT_FLAGS = new Object[]{
                    null, // default
                    true,
                    false,
    };

    @Parameters(name = "esVersion: {0}, tlaFlag: {1}")
    public static Iterable<Object[]> data() {
        List<Object[]> list = new ArrayList<>();
        for (Integer esVersion : ECMASCRIPT_VERSIONS) {
            for (Object tlaFlag : TOP_LEVEL_AWAIT_FLAGS) {
                list.add(new Object[]{esVersion, tlaFlag});
            }
        }
        return list;
    }

    @Parameter(0) public Integer esVersion;

    @Parameter(1) public Boolean tlaFlag;

    @Test
    public void test() {
        Context.Builder builder = JSTest.newContextBuilder();
        if (esVersion != null) {
            builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, esVersion.toString());
        }
        if (tlaFlag != null) {
            builder.option(JSContextOptions.TOP_LEVEL_AWAIT_NAME, tlaFlag.toString());
        }
        boolean tlaEnabled;
        if (tlaFlag == null) {
            tlaEnabled = (esVersion == null ? JSConfig.LatestECMAScriptVersion : esVersion) >= JSConfig.ECMAScript2022;
        } else {
            tlaEnabled = tlaFlag;
        }
        try (Context context = builder.build()) {
            Source source = Source.newBuilder(JavaScriptLanguage.ID, "await 42", "tla.mjs").buildLiteral();
            try {
                context.eval(source);
                Assert.assertTrue(tlaEnabled);
            } catch (PolyglotException pex) {
                Assert.assertFalse(tlaEnabled);
                Assert.assertTrue(pex.isSyntaxError());
            }
        }
    }
}
