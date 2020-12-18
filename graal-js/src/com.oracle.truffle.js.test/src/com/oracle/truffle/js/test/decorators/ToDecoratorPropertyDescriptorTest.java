package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToDecoratorPropertyDescriptorTest extends DescriptorTest{

    @Test
    public void testNonCallableMethod() {
        for(String c : NON_CALLABLES) {
            String source = createDecoratorWithDataDescriptor(METHOD, KEY, STATIC, c, TRUE,null);
            testError(source, "Property method of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonCallableGetter() {
        for(String c: NON_CALLABLES) {
            String source = createDecoratorWithAccessorDescriptor(ACCESSOR, KEY, STATIC,c,null,null);
            testError(source, "Property get of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonCallableSetter() {
        for(String c: NON_CALLABLES) {
            String source = createDecoratorWithAccessorDescriptor(ACCESSOR, KEY, STATIC,null,c,null);
            testError(source, "Property set of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testAccessorAndDataDescriptor(){
        String source = createDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC,EMPTY_METHOD,null, EMPTY_GETTER,null,null);
        testError(source, "PropertyDescriptor must not be a accessor and data descriptor.");
        source  = createDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC, null, TRUE, EMPTY_GETTER, null, null);
        testError(source, "PropertyDescriptor must not be a accessor and data descriptor.");
        source = createDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC, EMPTY_METHOD, null, null, EMPTY_SETTER, null);
        testError(source, "PropertyDescriptor must not be a accessor and data descriptor.");
        source = createDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC,null, TRUE, null, EMPTY_SETTER,null);
        testError(source, "PropertyDescriptor must not be a accessor and data descriptor.");
    }
}
