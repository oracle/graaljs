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
package com.oracle.truffle.js.test.polyglot;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.util.UTS35Validator;

@RunWith(Parameterized.class)
public class LocaleTest {

    @Parameters(name = "{0}")

    public static List<String> localeIds() {
        return List.of("",
                        // well-formed
                        "und",
                        "cs",
                        "cs-CZ",
                        "de-AT",
                        "gsw-u-sd-chzh",
                        "en-t-jp",
                        "en-x-private",
                        "x-abc",
                        "und-x-abc",
                        "i-enochian",
                        "und-x-i-enochian",
                        "tr",
                        "lt",
                        // ill-formed
                        "de-", // Empty subtag
                        "de_AT", // Invalid subtag
                        "en-US-u-unsupported-xyz", // Incomplete extension 'u'
                        "en-u-ca-unsupported", // Invalid subtag: unsupported
                        "root" // not well-formed but accepted by setLanguageTag
        );
    }

    @Parameter(value = 0) public String localeId;

    @Test
    public void testInput() {
        boolean wellFormed = localeId.isEmpty() || UTS35Validator.isWellFormedUnicodeBCP47LocaleIdentifier(localeId);
        if (wellFormed) {
            // Should accept a superset of well-formed Unicode BCP 47 Locale Identifiers.
            Locale locale = !localeId.isEmpty() ? new Locale.Builder().setLanguageTag(localeId).build() : Locale.getDefault();

            try (Context context = Context.newBuilder(JavaScriptLanguage.ID).option(JSContextOptions.LOCALE_NAME, localeId).build()) {
                String upperCase = "HI\u00ccJ\u0303";
                Value result = context.eval("js", "'" + upperCase + "'.toLocaleLowerCase()");
                Assert.assertEquals(upperCase.toLowerCase(locale), result.asString());
            }
        } else {
            assertThrows(() -> {
                try (Context context = Context.newBuilder(JavaScriptLanguage.ID).option(JSContextOptions.LOCALE_NAME, localeId).build()) {
                    context.initialize(JavaScriptLanguage.ID);
                }
            }, IllegalArgumentException.class);
        }
    }

    private static void assertThrows(Runnable runnable, Class<? extends Throwable> expectedException) {
        try {
            runnable.run();
            fail("Expected " + expectedException.getName());
        } catch (Throwable e) {
            assertTrue("Expected " + expectedException.getName() + ", caught " + e.getClass().getName(), expectedException.isInstance(e));
        }
    }
}
