package com.oracle.truffle.js.test.builtins;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class WrapForIteratorPrototypeBuiltinsTest {
    @Test
    public void testPrototype() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Object.getPrototypeOf(Object.getPrototypeOf(Iterator.from({next: () => ({done: true})}))) === Iterator.prototype");
            Assert.assertTrue(result.asBoolean());
        }
    }

    @Test
    public void testNext() {
        Context.Builder builder = JSTest.newContextBuilder();
        builder.option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING);
        try (Context context = builder.build()) {
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true})}).next()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());
            Assert.assertEquals("object", result.getMember("value").asString());


            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true})}).next.call([].values())");
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
            Value result = context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: false}), return: () => ({done: true})}).return()");
            Assert.assertTrue(result.hasMembers());
            Assert.assertTrue(result.hasMember("done"));
            Assert.assertTrue(result.hasMember("value"));
            Assert.assertTrue(result.getMember("done").asBoolean());

            result = context.eval(JavaScriptLanguage.ID, "var called = false; Iterator.from({next: () => ({value: typeof this, done: false}), return: () => (called = true, {done: true})}).return(); called");
            Assert.assertTrue(result.asBoolean());

            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true}), return: () => 1}).return()");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
            
            try {
                context.eval(JavaScriptLanguage.ID, "Iterator.from({next: () => ({value: typeof this, done: true}), return: () => ({done: true})}).return.call([].values())");
                Assert.fail("No exception thrown");
            } catch (PolyglotException e) {
                Assert.assertEquals("TypeError: ", e.getMessage().substring(0, "TypeError: ".length()));
            }
        }
    }
}
