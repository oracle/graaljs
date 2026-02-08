package com.oracle.truffle.js.test.parser;

import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Test;

/**
 * This grammar should work
 * <pre>
 * var [void] = x;         // via: BindingPattern :: `void`
 * var {x:void};           // via: BindingPattern :: `void`
 *
 * let [void] = x;         // via: BindingPattern :: `void`
 * let {x:void};           // via: BindingPattern :: `void`
 *
 * const [void] = x;       // via: BindingPattern :: `void`
 * const {x:void} = x;     // via: BindingPattern :: `void`
 *
 * function f(void) {}     // via: BindingPattern :: `void`
 * function f([void]) {}   // via: BindingPattern :: `void`
 * function f({x:void}) {} // via: BindingPattern :: `void`
 *
 * ((void) => {});         // via: BindingPattern :: `void`
 * (([void]) => {});       // via: BindingPattern :: `void`
 * (({x:void}) => {});     // via: BindingPattern :: `void`
 *
 * using void = x;         // via: LexicalBinding : `void` Initializer
 * await using void = x;   // via: LexicalBinding : `void` Initializer
 *
 * [void] = x;             // via: DestructuringAssignmentTarget : `void`
 * ({x:void} = x);         // via: DestructuringAssignmentTarget : `void`
 * </pre>
 */
public class ParserDiscardBindingTest {

    private void parseSuccessfully(String code) {
        try (Context ctx = JSTest.newContextBuilder().build()) {
            ctx.parse("js", code);
        }
    }

    private void assertParseError(String code) {
        parseSuccessfully(code);
        Assert.fail("Expected syntax error, but parsing succeeded");
    }

    /*
    @Test
    public void testUsingVoidBinding() {
        parseSuccessfully("using void = foo();");
    }

    @Test
    public void testAwaitUsingVoidBinding() {
        parseSuccessfully("await using void = foo();");
    }
    */

    @Test
    public void testArrayBindingWithVoid() {
        parseSuccessfully("let [void] = x;");
    }

    @Test
    public void testObjectBindingWithVoid() {
        parseSuccessfully("const { a: void } = obj;");
    }

    @Test
    public void testObjectBindingWithVoidAndRest() {
        parseSuccessfully("let { a: void, b } = obj;");
    }

    @Test
    public void testArrayAssignmentPatternWithVoid() {
        parseSuccessfully("([void] = x);");
    }

    @Test
    public void testObjectAssignmentPatternWithVoid() {
        parseSuccessfully("({ a: void } = obj);");
    }

    @Test
    public void testFunctionParameterVoid() {
        parseSuccessfully("function f(void) {}");
    }

    @Test
    public void testArrowFunctionParameterVoid() {
        parseSuccessfully("(void) => {}");
    }

    @Test
    public void testArrayFunctionParameterWithVoid() {
        parseSuccessfully("function f([void]) {}");
    }

    @Test
    public void testObjectFunctionParameterWithVoid() {
        parseSuccessfully("function f({ a: void }) {}");
    }

    @Test
    public void testCatchParameterVoid() {
        parseSuccessfully("try { throw 1; } catch (void) {}");
    }

    @Test
    public void testForOfWithVoidBinding() {
        parseSuccessfully("for (const void of xs) {}");
    }

    @Test
    public void testForOfWithArrayVoidBinding() {
        parseSuccessfully("for (let [void] of xs) {}");
    }

    @Test
    public void testVoidIsNotAssignable() {
        assertParseError("void = x;");
    }

    @Test
    public void testVoidIsNotAnExpression() {
        assertParseError("const x = void;");
    }

    @Test
    public void testVoidIsNotReferenceable() {
        assertParseError("function f() { void; }");
    }

    @Test
    public void testVoidIsNotUpdateTarget() {
        assertParseError("void++;");
    }

    /*
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

     */
}
