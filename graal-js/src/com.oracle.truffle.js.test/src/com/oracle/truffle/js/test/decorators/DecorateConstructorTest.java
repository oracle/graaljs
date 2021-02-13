package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class DecorateConstructorTest extends DecoratorTest{
    @Test
    public void testDuplicateClassElement() {
        String source = createClassDecorator(CLASS, null, null, "d.elements = [{" +
                "'kind':" + METHOD +
                ",'key':" + KEY +
                ",'placement':" + STATIC +
                "},{" +
                "'kind':" + METHOD +
                ",'key':" + KEY +
                ",'placement':" + STATIC +
                "}];");
        testError(source, "Duplicate key key.");
    }
}
