package com.oracle.truffle.js.test.decorators;

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
        testSuccess(source);
    }
}
