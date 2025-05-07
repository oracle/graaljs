/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;

public class NewSetMethodsTest {

    private static Context getNewSetMethodsContext() {
        return JSTest.newContextBuilder().option(JSContextOptions.NEW_SET_METHODS_NAME, "true").build();
    }

    @Test
    public void testNotAvailable() {
        String code = String.format("var set1 = %s; set1.union(set1)", createSetString(1, 2, 3, 4));

        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.NEW_SET_METHODS_NAME, "false").build()) {
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should fail with js.new-set-methods=false");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("union is not a function"));
        }
    }

    @Test
    public void testUnion() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.union(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(4, 5, 6, 7), createSetString(1, 2, 3, 4, 5, 6, 7),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testUnionEmptyInput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.union(set2); %s;",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testUnionIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.union.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIntersection() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.intersection(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6), createSetString(3, 4),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIntersectionEmptyInput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.intersection(set2); %s;",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIntersectionEmptyOutput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.intersection(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(5, 6, 7, 8), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIntersectionHasNotCallable() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.has = 17;" +
                            "var result = set1.intersection(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Non callable expected.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIntersectionIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.intersection.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testDifference() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.difference(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6), createSetString(1, 2),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDifferenceEmptyInput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.difference(set2); %s;",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDifferenceEmptyOutput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.difference(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(1, 2, 3, 4), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testDifferenceIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.difference.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testSymmetricDifference() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.symmetricDifference(set2); %s;",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6), createSetString(1, 2, 5, 6),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testSymmetricDifferenceEmptyInput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.symmetricDifference(set2); %s;",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testSymmetricDifferenceEmptyOutput() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; var expected = %s;" +
                            "var result = set1.symmetricDifference(set2); %s;",
                            createSetString(1, 3), createSetString(1, 3), createSetString(),
                            setEqualsString("result", "expected"));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testSymmetricDifferenceIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.symmetricDifference.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsSubsetOf() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSubsetOf(set2);",
                            createSetString(3, 5), createSetString(3, 4, 5, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotSubsetOf() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSubsetOf(set2);",
                            createSetString(3, 5, 7), createSetString(3, 4, 5, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsSubsetOfNoIterable() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var x = 0;" +
                            "set1.isSubsetOf(x);",
                            createSetString(3, 5));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set-like.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().startsWith("TypeError:"));
        }
    }

    @Test
    public void testIsSubsetOfIterableHasNotCallable() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var arr = [3, 4, 5, 6]; arr.size = arr.length; Array.prototype.has = 17;" +
                            "set1.isSubsetOf(arr);",
                            createSetString(3, 5));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Non callable expected.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIsSubsetOfIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.isSubsetOf.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsSupersetOf() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSupersetOf(set2);",
                            createSetString(3, 4, 5, 6), createSetString(3, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotSupersetOf() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSupersetOf(set2);",
                            createSetString(3, 4, 5, 6), createSetString(3, 6, 7));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsSupersetOfIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.isSupersetOf.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsDisjointFrom() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isDisjointFrom(set2);",
                            createSetString(1, 2), createSetString(3, 4));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotDisjointFrom() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isDisjointFrom(set2);",
                            createSetString(1, 2), createSetString(2, 3));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsDisjointFromHasNotCallable() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("var set1 = %s; var arr = [3, 4]; arr.size = arr.length; Set.prototype.has = 17;" +
                            "set1.isDisjointFrom(arr);",
                            createSetString(1, 2));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Non callable expected.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIsDisjointFromIsNotSet() {
        try (Context context = getNewSetMethodsContext()) {
            String code = String.format("Set.prototype.isDisjointFrom.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
            Assert.fail("Should not be Set.");
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    private static String setEqualsString(String varName1, String varName2) {
        return String.format("%s.size === %s.size && [...%s].every(value => %s.has(value));",
                        varName1, varName2, varName1, varName2);
    }

    private static String createSetString(int... values) {
        return "new Set(" + Arrays.toString(values) + ")";
    }
}
