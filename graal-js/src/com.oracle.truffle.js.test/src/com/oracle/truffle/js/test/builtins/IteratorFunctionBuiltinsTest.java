package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class IteratorFunctionBuiltinsTest {
    @Test
    public void testConstructor() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            try {
                context.eval(JavaScriptLanguage.ID, "new Iterator()");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }

            Value result = context.eval(JavaScriptLanguage.ID, "new (class x extends Iterator{})()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertFalse(result.hasMember("next"));
            Assert.assertTrue(result.hasMember("map"));
            Assert.assertTrue(result.getMember("map").canExecute());
        }
    }

    @Test
    public void testFrom() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.from({[Symbol.iterator]: () => ({next: () => ({done: true})})})");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("next"));
            Assert.assertTrue(result.getMember("next").canExecute());
            Assert.assertTrue(result.hasMember("map"));
            Assert.assertTrue(result.getMember("map").canExecute());

            result = context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({done: true})})");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("next"));
            Assert.assertTrue(result.getMember("next").canExecute());
            Assert.assertTrue(result.hasMember("map"));
            Assert.assertTrue(result.getMember("map").canExecute());

            result = context.eval(JavaScriptLanguage.ID, "var x = [].values(); Iterator.from(x) === x");
            Assert.assertTrue(result.asBoolean());
        }
    }
}
