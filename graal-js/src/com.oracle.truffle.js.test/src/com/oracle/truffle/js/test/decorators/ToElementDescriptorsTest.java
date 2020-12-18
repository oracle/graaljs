package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToElementDescriptorsTest extends DecoratorTest {

    @Test
    public void testExtraWithExtras() {
        String source = createElementDecorator(METHOD,KEY, STATIC,"d.extras = {" +
                "extras: {}" +
                "}");
        testError(source, "Extra elements must not have extras themselves.");
    }
}
