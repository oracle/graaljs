package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class CoalesceClassElementTest extends DecoratorTest{
    @Test
    public void testMethodWithMultipleDecorators() {
        String source = "function decorator1(d){} function decorator2(d){} class C { @decorator1 method(){} @decorator2 method(){} }";
        testError(source, "Overwritten and overwriting methods can not be decorated.");
    }

    @Test
    public void testGetterAndSetterDecorated() {
        String source = "function decorator1(d){} function decorator2(d){} class C { @decorator1 set test(v){} @decorator2 get test() {return 0;} }";
        testError(source, "Either getter or setter can be decorated, not both.");
    }
}
