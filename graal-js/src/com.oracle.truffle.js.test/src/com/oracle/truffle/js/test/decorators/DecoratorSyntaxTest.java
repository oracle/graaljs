package com.oracle.truffle.js.test.decorators;

import com.oracle.js.parser.ParserException;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DecoratorSyntaxTest extends JSTest {

    private void testError(String sourceCode, String expectedMsg) {
        try (Context context = JSTest.newContextBuilder().build()) {
            context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceCode, "decorator-test").buildLiteral());
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains(expectedMsg));
        }
    }

    @Test
    public void testEmptyClassDecorator() {
        testError("@class DecoratedClass {}","Expected ident");
    }

    @Test
    public void testEmptyClassElementDecorator() {
        testError("class C { " +
                "@method(){}" +
                " }", "Expected ident");
    }

    @Test
    public void testArrayAccessInClassDecorator() {
        testError("@decorator[0] class DecoratedClass {}", "Expected ident");
    }

    @Test
    public void testArrayAccessInClassElementDecorator() {
        testError("class C {" +
                "@decorator[0] method(){}" +
                "}", "Expected (");
    }

    @Test
    public void testDecoratedConstructor() {
        testError("class C {" +
                "@decorator constructor() {}" +
                "}", "Class constructor must not have decorators.");
    }
}
