/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.js.runtime.JSContextOptions;

@RunWith(Parameterized.class)
public class SandboxPolicyOptionsTest {

    private record Option(String name, String value, SandboxPolicy strictestPolicy) {
    }

    @Parameter(value = 0) public SandboxPolicy policy;
    @Parameter(value = 1) public Option option;

    @Parameters(name = "{0} {1}")
    public static Collection<Object[]> parameters() {
        var policies = EnumSet.of(SandboxPolicy.TRUSTED, SandboxPolicy.CONSTRAINED);
        var options = List.of(
                        new Option(JSContextOptions.STRICT_NAME, "true", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.STRICT_NAME, "false", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2021", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "latest", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "staging", SandboxPolicy.TRUSTED),
                        new Option(JSContextOptions.CONSOLE_NAME, "true", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.CONSOLE_NAME, "false", SandboxPolicy.UNTRUSTED),
                        new Option(JSContextOptions.ALLOW_EVAL_NAME, "true", SandboxPolicy.CONSTRAINED),
                        new Option(JSContextOptions.ALLOW_EVAL_NAME, "false", SandboxPolicy.CONSTRAINED),
                        new Option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "none", SandboxPolicy.CONSTRAINED),
                        new Option(JSContextOptions.UNHANDLED_REJECTIONS_NAME, "throw", SandboxPolicy.CONSTRAINED),
                        new Option(JSContextOptions.TEXT_ENCODING_NAME, "true", SandboxPolicy.CONSTRAINED),
                        new Option(JSContextOptions.TEXT_ENCODING_NAME, "false", SandboxPolicy.CONSTRAINED));
        return policies.stream().flatMap(p -> options.stream().map(o -> new Object[]{p, o})).toList();
    }

    @SuppressWarnings("deprecation") @Rule public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSettableOptions() {
        var out = new ByteArrayOutputStream();
        var cb = Context.newBuilder("js").out(out).err(out);
        cb.sandbox(policy);
        cb.option(option.name, option.value);

        boolean shouldFailSandboxValidation = policy.isStricterThan(option.strictestPolicy);

        try (Context context = cb.build()) {
            // Make sure we can evaluate regular expressions in sandboxed mode.
            Value result = context.eval("js", "'  ok  '.replace(/\\s+/g, ' ').trim();");
            Assert.assertEquals("ok", result.asString());
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("option engine.")) {
                // Ignore failures due to engine options not being available in sandboxed mode.
                shouldFailSandboxValidation = false;
                Assume.assumeTrue("Ignore failures due to engine options", false);
            }
            throw ex;
        } finally {
            if (shouldFailSandboxValidation) {
                // Normal validation of sandbox policy throws an IllegalArgumentException.
                // Custom validation in JavaScriptLanguage is thrown as a PolyglotException.
                expectedException.expect(anyOf(instanceOf(IllegalArgumentException.class), instanceOf(PolyglotException.class)));
                expectedException.expectMessage(allOf(containsString("The validation for the given sandbox policy"), containsString("failed")));
            }
        }
    }
}
