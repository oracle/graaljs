package com.oracle.truffle.js.test.decorators;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class AddElementPlacementTest extends DecoratorTest{
    @Test
    public void duplicatedKeyDefinition() {
        String source = createElementDecoratorWithDataDescriptor(METHOD, KEY, STATIC,EMPTY_METHOD,TRUE, "d.extras = [{" +
                "'kind':'field'," +
                "'key':'key'," +
                "'placement':'static'," +
                "}];");
        testError(source, "Duplicate key key.");
    }

    @Test
    public void validDefinition() {
        String source = createElementDecoratorWithDataDescriptor(METHOD, KEY, STATIC,EMPTY_METHOD,TRUE, "d.extras = [{" +
                "'kind':'field'," +
                "'key':'key2'," +
                "'placement':'static'," +
                "}];");
        try (Context context = JSTest.newContextBuilder().option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022").build()) {
            Value v = context.eval(JavaScriptLanguage.ID, source);
        } catch (Exception ex) {
            Assert.fail("should not have thrown: " + ex.getMessage());
        }
    }
}
