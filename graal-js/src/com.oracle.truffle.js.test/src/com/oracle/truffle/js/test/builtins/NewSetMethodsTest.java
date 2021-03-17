package com.oracle.truffle.js.test.builtins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Test;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;

public class NewSetMethodsTest {

    @Test
    public void testUnion() {
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.union.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIntersection() {
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.has = null;" +
                            "var result = set1.intersection(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIntersectionAddNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.add = null;" +
                            "var result = set1.intersection(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIntersectionIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.intersection.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testDifference() {
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
    public void testDifferenceDeleteNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.delete = null;" +
                            "set1.difference(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testDifferenceIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.difference.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testSymmetricDifference() {
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
        try (Context context = JSTest.newContextBuilder().build()) {
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
    public void testSymmetricDifferenceDeleteNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.delete = null;" +
                            "set1.symmetricDifference(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testSymmetricDifferenceAddNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; Set.prototype.add = 666;" +
                            "set1.symmetricDifference(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError:"));
        }
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; delete(Set.prototype.add);" +
                            "set1.symmetricDifference(set2);",
                            createSetString(1, 2, 3, 4), createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError:"));
        }
    }

    @Test
    public void testSymmetricDifferenceIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.symmetricDifference.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsSubsetOf() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSubsetOf(set2);",
                            createSetString(3, 5), createSetString(3, 4, 5, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotSubsetOf() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSubsetOf(set2);",
                            createSetString(3, 5, 7), createSetString(3, 4, 5, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsSubsetOfNoIterable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var x = 0; Array.prototype.has = null;" +
                            "set1.isSubsetOf(x);",
                            createSetString(3, 5));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: 0 is not iterable"));
        }
    }

    @Test
    public void testIsSubsetOfIterableHasNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var arr = [3, 4, 5, 6]; Array.prototype.has = null;" +
                            "set1.isSubsetOf(arr);",
                            createSetString(3, 5));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsSubsetOfIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.isSubsetOf.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsSupersetOf() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSupersetOf(set2);",
                            createSetString(3, 4, 5, 6), createSetString(3, 6));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotSupersetOf() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isSupersetOf(set2);",
                            createSetString(3, 4, 5, 6), createSetString(3, 6, 7));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsSupersetOfIterableHasNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var arr = [3, 6]; Set.prototype.has = null;" +
                            "set1.isSupersetOf(arr);",
                            createSetString(3, 4, 5, 6));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIsSupersetOfIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.isSupersetOf.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    @Test
    public void testIsDisjointedFrom() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isDisjointedFrom(set2);",
                            createSetString(1, 2), createSetString(3, 4));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testIsNotDisjointedFrom() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var set2 = %s; set1.isDisjointedFrom(set2);",
                            createSetString(1, 2), createSetString(2, 3));
            Value result = context.eval(JavaScriptLanguage.ID, code);
            assertTrue(result.isBoolean());
            assertFalse(result.asBoolean());
        }
    }

    @Test
    public void testIsDisjointedFromHasNotCallable() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("var set1 = %s; var arr = [3, 4]; Set.prototype.has = null;" +
                            "set1.isDisjointedFrom(arr);",
                            createSetString(1, 2));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Callable expected"));
        }
    }

    @Test
    public void testIsDisjointedFromIsNotSet() {
        try (Context context = JSTest.newContextBuilder().build()) {
            String code = String.format("Set.prototype.isDisjointedFrom.call(0, [1, 2, 3]);",
                            createSetString(), createSetString(), createSetString(),
                            setEqualsString("result", "expected"));
            context.eval(JavaScriptLanguage.ID, code);
        } catch (PolyglotException ex) {
            assertTrue(ex.getMessage().contains("TypeError: Set expected"));
        }
    }

    private static final String setEqualsString(String varName1, String varName2) {
        return String.format("%s.size === %s.size && [...%s].every(value => %s.has(value));",
                        varName1, varName2, varName1, varName2);
    }

    private static String createSetString(int... values) {
        return "new Set(" + Arrays.toString(values) + ")";
    }
}
