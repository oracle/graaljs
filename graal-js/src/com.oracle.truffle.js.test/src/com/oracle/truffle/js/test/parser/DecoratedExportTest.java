/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.parser;

import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.js.parser.ErrorManager;
import com.oracle.js.parser.Parser;
import com.oracle.js.parser.ParserException;
import com.oracle.js.parser.ScriptEnvironment;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class DecoratedExportTest {
    private static final String HEADER = """
                    export const events = [];
                    function d(label) {
                        events.push('evaluate:' + label);
                        return function(value, context) {
                            if (context.kind !== 'class') throw new Error('Expected a class decorator');
                            events.push('apply:' + label);
                            return value;
                        };
                    }
                    """;

    private static void checkModule(String declaration, String exportName, String className) {
        Context.Builder builder = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "staging");
        builder.option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true");
        try (Context context = builder.build()) {
            Source source = Source.newBuilder(JavaScriptLanguage.ID, HEADER + declaration, "decorator-export.mjs").mimeType(JavaScriptLanguage.MODULE_MIME_TYPE).buildLiteral();
            Value exports = context.eval(source);
            Value constructor = exports.getMember(exportName);
            Assert.assertEquals(className, constructor.getMember("name").asString());
            Assert.assertEquals(42, constructor.newInstance().getMember("value").asInt());
            Value events = exports.getMember("events");
            List<String> expected = List.of("evaluate:first", "evaluate:second", "apply:second", "apply:first");
            Assert.assertEquals(expected.size(), events.getArraySize());
            for (int i = 0; i < expected.size(); i++) {
                Assert.assertEquals(expected.get(i), events.getArrayElement(i).asString());
            }
        }
    }

    /** Both decorator placements must preserve named and default exports, class names, and order. */
    @Test
    public void testDecoratorPlacements() {
        String decorators = "@d('first') @d('second') ";
        checkModule(decorators + "export class C { value = 42; }", "C", "C");
        checkModule("export " + decorators + "class C { value = 42; }", "C", "C");
        checkModule(decorators + "export default class C { value = 42; }", "default", "C");
        checkModule("export default " + decorators + "class C { value = 42; }", "default", "C");
        checkModule(decorators + "export default class { value = 42; }", "default", "default");
        checkModule("export default " + decorators + "class { value = 42; }", "default", "default");
        checkModule(decorators + "class C { value = 42; } export {C};", "C", "C");
    }

    /** Leading decorator expressions retain the module's top-level-await grammar and execution. */
    @Test
    public void testAwaitInLeadingDecorator() {
        checkModule("@d(await Promise.resolve('first')) @d('second') export class C { value = 42; }", "C", "C");
    }

    /** Decorators cannot appear on both sides of export or decorate non-class export forms. */
    @Test
    public void testInvalidDecoratedExports() {
        ScriptEnvironment env = ScriptEnvironment.builder().strict(true).ecmaScriptVersion(ScriptEnvironment.ES_STAGING).build();
        for (String code : List.of(
                        "@d bogus C {}",
                        "@d export bogus C {}",
                        "@d export default bogus {}",
                        "@d export @d class C {}",
                        "@d export default @d class {}",
                        "@d export default @d class C {}",
                        "@d export function f() {}",
                        "@d export default function() {}",
                        "@d export default async function() {}",
                        "@d export const C = class {};",
                        "@d export default (class {});",
                        "@d export {C};",
                        "@d export * from 'other';")) {
            Parser parser = new Parser(env, com.oracle.js.parser.Source.sourceFor("test", code), new ErrorManager.ThrowErrorManager());
            Assert.assertThrows(ParserException.class, () -> parser.parseModule("test"));
        }
    }
}
