package com.oracle.truffle.js.test.parser;

import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Assert;
import org.junit.Test;

public class ParserDiscardBindingTest {

    // Variable Declarations
    //@Test public void testUsingDiscard() { assertParses("using void = x;"); }
    //@Test public void testAwaitUsingDiscard() { assertParses("await using void = x;"); }
    @Test public void testVarTopLevelDiscard() { assertNotParses("var void = bar();"); }
    @Test public void testLetTopLevelDiscard() { assertNotParses("let void = bar();"); }
    @Test public void testConstTopLevelDiscard() { assertNotParses("const void = bar();"); }

    // Object Binding and Assignment Patterns
    @Test public void testObjectBindingDiscard() {
        assertParses("const { z: void, ...obj1 } = { x: 1, y: 2, z: 3 };");
    }

    @Test public void testObjectAssignmentDiscard() {
        assertParses("({ z: void, ...obj2 } = { x: 1, y: 2, z: 3 });");
    }

    // Array Binding and Assignment Patterns
    @Test public void testArrayBindingDiscard() { assertParses("const [a, void, void] = iter;"); }
    @Test public void testArrayAssignmentDiscard() { assertParses("[void, a, void] = x;"); }

    // Function and Arrow Parameters
    @Test public void testFunctionParamDiscard() { assertParses("function f(void, i) { return i; }"); }
    @Test public void testArrowParamDiscard() { assertParses("array.map((void, i) => i);"); }
    @Test public void testMultipleParamDiscards() { assertParses("foo((void, a, void, void, b) => {});"); }

    // Extractors and Pattern Matching (separate proposals)
    //@Test public void testExtractorDiscard() { assertParses("const Message(void, body) = msg;"); }
    //@Test public void testMatchPatternDiscard() {
    //    assertParses("match (obj) { when { x: void, y: void }: usePoint(obj); }");
    //}

    // Invalid Syntax (Edge Cases)
    @Test public void testBareVoidAssignment() { assertNotParses("void = x;"); }
    @Test public void testVoidAsExpressionValue() { assertNotParses("const x = void;"); }


    // --- HELPERS ---
    private void assertParses(String code) {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            ctx.parse("js", code);
        } catch (PolyglotException e) {
            Assert.fail("Expected code to parse successfully, but got: " + e.getMessage() + "\nCode: " + code);
        }
    }

    private void assertNotParses(String code) {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            ctx.parse("js", code);
            Assert.fail("Expected syntax error, but parsing succeeded: " + code);
        } catch (PolyglotException e) {
            Assert.assertTrue("Expected SyntaxError", e.isSyntaxError());
        }
    }


    @Test public void testVarArray() { assertParses("var [void] = x;"); }
    @Test public void testVarObject() { assertParses("var {x:void} = y;"); }
    @Test public void testLetArray() { assertParses("let [void] = x;"); }
    @Test public void testLetObject() { assertParses("let {x:void} = y;"); }
    @Test public void testConstArray() { assertParses("const [void] = x;"); }
    @Test public void testConstObject() { assertParses("const {x:void} = y;"); }
    @Test public void testFunctionParamIdentifierVoid() { assertParses("function f(void) {}"); }
    @Test public void testFunctionParamArrayVoid() { assertParses("function f([void]) {}"); }
    @Test public void testFunctionParamObjectVoid() { assertParses("function f({x:void}) {}"); }
    @Test public void testArrowParamIdentifierVoid() { assertParses("((void) => {});"); }
    @Test public void testArrowParamIdentifierName() { assertParses("((nice, test) => {});"); }
    @Test public void testArrowParamArrayVoid() { assertParses("(([void]) => {});"); }
    @Test public void testArrowParamObjectVoid() { assertParses("(({x:void}) => {});"); }
    // @Test public void testUsing() { assertParses("using void = x;"); }
    // @Test public void testAwaitUsing() { assertParses("await using void = x;"); }
    @Test public void testDestructuringAssignmentArray() { assertParses("[void] = x;"); }
    @Test public void testDestructuringAssignmentObject() { assertParses("({x:void} = y);"); }
}
