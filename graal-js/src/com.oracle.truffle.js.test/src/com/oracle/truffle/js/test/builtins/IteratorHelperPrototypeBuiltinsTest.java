package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class IteratorHelperPrototypeBuiltinsTest {
    @Test
    public void testPrototype() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Object.getPrototypeOf(Object.getPrototypeOf([].values().drop(0))) === Iterator.prototype");
            Assert.assertTrue(result.asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "typeof Object.getPrototypeOf([].values().drop(0))");
            Assert.assertEquals("object", result.asString());
        }
    }

    @Test
    public void testNext() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "[1].values().drop(0).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertFalse(result.getMember("done").asBoolean());
            Assert.assertEquals(1, result.getMember("value").asInt());

            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().drop(0).next.call([2].values())");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
        }
    }

    @Test
    public void testReturn() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "function* test() {yield 1; yield 2;}; var x = test(); var y = x.drop(0); y.next(); y.return(); x.next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());


            result = context.eval(JavaScriptLanguage.ID, "function* test() {yield 1; yield 2;}; var x = test(); var y = [1].values().flatMap(() => x); y.next(); y.return(); x.next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertTrue(result.getMember("value").isNull());

            try {
                context.eval(JavaScriptLanguage.ID, "[1].values().drop(0).return.call([2].values())");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
        }
    }
}
