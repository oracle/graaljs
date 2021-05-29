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
package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TuplePrototypeBuiltinsTest {

    private static final String testName = "tuple-prototype-builtins-test";

    private static Value execute(String sourceText) {
        try (Context context = JSTest.newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .option(JSContextOptions.INTL_402_NAME, "true")
                .build()) {
            return context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, testName).buildLiteral());
        }
    }

    private static Value execute(String... sourceText) {
        return execute(String.join("\n", sourceText));
    }

    private static void expectError(String sourceText, String expectedMessage) {
        try (Context context = JSTest.newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, testName).buildLiteral());
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertTrue(
                    String.format("\"%s\" should contain \"%s\"", ex.getMessage(), expectedMessage),
                    ex.getMessage().contains(expectedMessage)
            );
        }
    }

    @Test
    public void testLength() {
        assertEquals(0, execute("#[].length").asInt());
        assertEquals(3, execute("#[1, 2, 3].length").asInt());
        assertEquals(3, execute("Object(#[1, 2, 3]).length").asInt());
    }

    @Test
    public void testValueOf() {
        assertTrue(execute("typeof #[].valueOf() === 'tuple'").asBoolean());
        assertTrue(execute("typeof Object(#[]).valueOf() === 'tuple'").asBoolean());
        expectError("Tuple.prototype.valueOf.call('test')", "be a Tuple");
    }

    @Test
    public void testPopped() {
        assertTrue(execute("#[1, 2].popped() === #[1]").asBoolean());
        assertTrue(execute("Object(#[1, 2]).popped() === #[1]").asBoolean());
        expectError("Tuple.prototype.popped.call('test')", "be a Tuple");
    }

    @Test
    public void testPushed() {
        assertTrue(execute("#[1].pushed(2, 3) === #[1, 2, 3]").asBoolean());
        assertTrue(execute("Object(#[1]).pushed(2, 3) === #[1, 2, 3]").asBoolean());
        expectError("#[].pushed(Object(1))", "non-primitive values");
        expectError("Tuple.prototype.pushed.call('test')", "be a Tuple");
    }

    @Test
    public void testReversed() {
        assertTrue(execute("#[1, 2].reversed() === #[2, 1]").asBoolean());
        assertTrue(execute("Object(#[1, 2]).reversed() === #[2, 1]").asBoolean());
        expectError("Tuple.prototype.reversed.call('test')", "be a Tuple");
    }

    @Test
    public void testShifted() {
        assertTrue(execute("#[1, 2].shifted() === #[2]").asBoolean());
        assertTrue(execute("Object(#[1, 2]).shifted() === #[2]").asBoolean());
        expectError("Tuple.prototype.shifted.call('test')", "be a Tuple");
    }

    @Test
    public void testSlice() {
        assertTrue(execute("#[1, 2, 3, 4, 5].slice(1, 4) === #[2, 3, 4]").asBoolean());
        assertTrue(execute("#[1, 2, 3, 4, 5].slice(1, 1) === #[]").asBoolean());
        assertTrue(execute("Object(#[1, 2]).slice(1, 2) === #[2]").asBoolean());
        expectError("Tuple.prototype.slice.call('test')", "be a Tuple");
    }

    @Test
    public void testSorted() {
        assertTrue(execute("#[5, 4, 3, 2, 1, 3].sorted() === #[1, 2, 3, 3, 4, 5]").asBoolean());
        assertTrue(execute("#[5, 4, 3, 2, 1, 3].sorted((a, b) => b - a) === #[5, 4, 3, 3, 2, 1]").asBoolean());
        assertTrue(execute("Object(#[2, 1]).sorted() === #[1, 2]").asBoolean());
        expectError("Tuple.prototype.sorted.call('test')", "be a Tuple");
    }

    @Test
    public void testSpliced() {
        assertTrue(execute("#[1, 7, 4].spliced(1, 1, 2, 3) === #[1, 2, 3, 4]").asBoolean());
        assertTrue(execute("Object(#[2, 1]).spliced(0, 1) === #[1]").asBoolean());
        assertTrue(execute("Object(#[2, 1]).spliced() === #[2, 1]").asBoolean());
        assertTrue(execute("Object(#[2, 1]).spliced(undefined, undefined) === #[2, 1]").asBoolean());
        assertTrue(execute("Object(#[2, 1]).spliced(undefined) === #[]").asBoolean());
        expectError("Tuple.prototype.spliced.call('test')", "be a Tuple");
    }

    @Test
    public void testConcat() {
        assertTrue(execute("#[1].concat(2, [3, 4], #[5, 6], 0) === #[1, 2, 3, 4, 5, 6, 0]").asBoolean());
        assertTrue(execute("Object(#[1, 2]).concat(3) === #[1, 2, 3]").asBoolean());
        expectError("Tuple.prototype.concat.call('test')", "be a Tuple");
    }

    @Test
    public void testIncludes() {
        assertTrue(execute("#[1, 2, 3].includes(1)").asBoolean());
        assertFalse(execute("#[1, 2, 3].includes(1, 1)").asBoolean());
        assertTrue(execute("Object(#[1, 2]).includes(2)").asBoolean());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testIncludes_Fallback() {
        expectError("Tuple.prototype.includes.call('test')", "be a Tuple");
    }

    @Test
    public void testIndexOf() {
        assertEquals(0, execute("#[1, 2, 3].indexOf(1)").asInt());
        assertEquals(-1, execute("#[1, 2, 3].indexOf(1, 1)").asInt());
        assertEquals(1, execute("Object(#[1, 2]).indexOf(2)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testIndexOf_Fallback() {
        expectError("Tuple.prototype.indexOf.call('test')", "be a Tuple");
    }

    @Test
    public void testJoin() {
        assertEquals("1,2,3", execute("#[1, 2, 3].join()").asString());
        assertEquals("123", execute("#[1, 2, 3].join('')").asString());
        assertEquals("1", execute("Object(#[1]).join('-')").asString());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testJoin_Fallback() {
        expectError("Tuple.prototype.join.call('test')", "be a Tuple");
    }

    @Test
    public void testLastIndexOf() {
        assertEquals(2, execute("#[1, 2, 1].lastIndexOf(1)").asInt());
        assertEquals(0, execute("#[1, 2, 1].lastIndexOf(1, 1)").asInt());
        assertEquals(1, execute("Object(#[1, 2]).lastIndexOf(2)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testLastIndexOf_Fallback() {
        expectError("Tuple.prototype.lastIndexOf.call('test')", "be a Tuple");
    }

    @Test
    public void testEntries() {
        assertTrue(execute(
                "var iterator = #['a', 'b'].entries();",
                "var values = [...iterator];",
                "values[0][0] === 0 && values[0][1] === 'a' && values[1][0] === 1 && values[1][1] === 'b';"
        ).asBoolean());
        assertTrue(execute("Object(#[]).entries().next().done;").asBoolean());
        expectError("Tuple.prototype.entries.call('test')", "be a Tuple");
    }

    @Test
    public void testEvery() {
        assertTrue(execute("#[1, 2, 3].every(it => it > 0)").asBoolean());
        assertFalse(execute("#[1, 2, 3].every(it => it < 2)").asBoolean());
        assertTrue(execute("Object(#[1, 1]).every(it => it === 1)").asBoolean());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testEvery_Fallback() {
        expectError("Tuple.prototype.every.call('test')", "be a Tuple");
    }

    @Test
    public void testFilter() {
        assertTrue(execute("#[1, 2, 3].filter(it => it !== 2) === #[1, 3]").asBoolean());
        assertTrue(execute("Object(#[1, 1]).filter(it => false) === #[]").asBoolean());
        expectError("Tuple.prototype.filter.call('test', it => false)", "be a Tuple");
    }

    @Test
    public void testFind() {
        assertEquals(2, execute("#[1, 2, 3].find(it => it > 1)").asInt());
        assertTrue(execute("#[1, 2, 3].find(it => it < 0) === undefined").asBoolean());
        assertEquals(1, execute("Object(#[1, 1]).find(it => it === 1)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testFind_Fallback() {
        expectError("Tuple.prototype.find.call('test')", "be a Tuple");
    }

    @Test
    public void testFindIndex() {
        assertEquals(1, execute("#[1, 2, 3].findIndex(it => it > 1)").asInt());
        assertEquals(-1, execute("#[1, 2, 3].findIndex(it => it < 0)").asInt());
        assertEquals(0, execute("Object(#[1, 1]).findIndex(it => it === 1)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testFindIndex_Fallback() {
        expectError("Tuple.prototype.findIndex.call('test')", "be a Tuple");
    }

    @Test
    public void testFlat() {
        assertTrue(execute("#[1, #[2, #[3]]].flat() === #[1, 2, #[3]]").asBoolean());
        assertTrue(execute("#[1, #[2, #[3]]].flat(2) === #[1, 2, 3]").asBoolean());
        expectError("Tuple.prototype.flat.call('test')", "be a Tuple");
    }

    @Test
    public void testFlatMap() {
        assertTrue(execute(
                "var mapper = it => typeof it === 'number' ? it * 10 : it;",
                "#[1, #[2, 3]].flatMap(mapper) === #[10, 20, 30];"
        ).asBoolean());
        expectError("Tuple.prototype.flatMap.call('test')", "be a Tuple");
    }

    @Test
    public void testForEach() {
        assertEquals(
                "123",
                execute("var text = '';",
                        "#[1, 2, 3].forEach(it => text += it);",
                        "text;"
                ).asString()
        );
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testForEach_Fallback() {
        expectError("Tuple.prototype.forEach.call('test')", "be a Tuple");
    }

    @Test
    public void testKeys() {
        assertTrue(execute("#[...#[1, 2, 3].keys()] === #[0, 1, 2];").asBoolean());
        expectError("Tuple.prototype.keys.call('test')", "be a Tuple");
    }

    @Test
    public void testMap() {
        assertTrue(execute("#[1, 2, 3].map(it => it * 10) === #[10, 20, 30]").asBoolean());
        expectError("#[1, 2, 3].map(it => Object(it))", "non-primitive values");
        expectError("Tuple.prototype.keys.call('test')", "be a Tuple");
    }

    @Test
    public void testReduce() {
        assertEquals("123", execute("#[1, 2, 3].reduce((acc, it) => acc += it, '')").asString());
        assertEquals(6, execute("Object(#[1, 2, 3]).reduce((acc, it) => acc += it)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testReduce_Fallback() {
        expectError("Tuple.prototype.reduce.call('test')", "be a Tuple");
    }

    @Test
    public void testReduceRight() {
        assertEquals("321", execute("#[1, 2, 3].reduceRight((acc, it) => acc += it, '')").asString());
        assertEquals(6, execute("Object(#[1, 2, 3]).reduceRight((acc, it) => acc += it)").asInt());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testReduceRight_Fallback() {
        expectError("Tuple.prototype.reduceRight.call('test')", "be a Tuple");
    }

    @Test
    public void testSome() {
        assertTrue(execute("#[1, 2, 3].some(it => it % 2 === 0)").asBoolean());
        assertFalse(execute("Object(#[1, 2, 3]).some(it => it < 0)").asBoolean());
    }

    @Ignore // TODO: re-evaluate, check proposal for changes
    @Test
    public void testSome_Fallback() {
        expectError("Tuple.prototype.some.call('test')", "be a Tuple");
    }

    @Test
    public void testUnshifted() {
        assertTrue(execute("#[1, 2, 3].unshifted(-1, 0) === #[-1, 0, 1, 2, 3]").asBoolean());
        assertTrue(execute("Object(#[1, 2, 3]).unshifted() === #[1, 2, 3]").asBoolean());
    }

    @Test
    public void testToLocaleString() {
        assertEquals("1.1", execute("#[1.1].toLocaleString('en')").asString());
        assertEquals("1,1", execute("#[1.1].toLocaleString('de')").asString());
        assertEquals("1,1", execute("Object(#[1.1]).toLocaleString('de')").asString());
    }

    @Test
    public void testToString() {
        assertEquals("1,2,3", execute("#[1, 2, 3].toString()").asString());
        assertEquals("1,2,3", execute("Object(#[1, 2, 3]).toString()").asString());
    }

    @Test
    public void testValues() {
        assertTrue(execute("#[...#[1, 2, 3].values()] === #[1, 2, 3];").asBoolean());
        assertTrue(execute("Object(#[]).values().next().done;").asBoolean());
        expectError("Tuple.prototype.values.call('test')", "be a Tuple");
    }

    @Test
    public void testWith() {
        assertTrue(execute("#[1, 2, 3].with(0, 4) === #[4, 2, 3]").asBoolean());
        assertTrue(execute("Object(#[1]).with(0, 0) === #[0]").asBoolean());
        assertTrue(execute("var x = #[1], y = x.with(0, 0); x === #[1] && y === #[0]").asBoolean());
        expectError("#[1, 2, 3].with(3, 4)", "out of range");
        expectError("#[1, 2, 3].with(0, {})", "non-primitive values");
        expectError("Tuple.prototype.with.call('test')", "be a Tuple");
    }
}
