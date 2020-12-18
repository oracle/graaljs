package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToDecoratorPropertyDescriptorTest extends DecoratorTest {

    @Test
    public void testNonCallableMethod() {
        for(String c : NON_CALLABLES) {
            String source = createElementDecoratorWithDataDescriptor(METHOD, KEY, STATIC, c, TRUE,null);
            testError(source, "Property method of property descriptor must be callable.");
        }
    }

    @Test
    public void testNonCallableGetter() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecoratorWithAccessorDescriptor(ACCESSOR, KEY, STATIC,c,null,null);
            testError(source, "Property get of property descriptor must be callable.");
        }
    }

    @Test
    public void testNonCallableSetter() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecoratorWithAccessorDescriptor(ACCESSOR, KEY, STATIC,null,c,null);
            testError(source, "Property set of property descriptor must be callable.");
        }
    }

    @Test
    public void testAccessorAndDataDescriptor(){
        String source = createElementDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC,EMPTY_METHOD,null, EMPTY_GETTER,null,null);
        testError(source, "Property descriptor can not be both accessor and data descriptor.");
        source  = createElementDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC, null, TRUE, EMPTY_GETTER, null, null);
        testError(source, "Property descriptor can not be both accessor and data descriptor.");
        source = createElementDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC, EMPTY_METHOD, null, null, EMPTY_SETTER, null);
        testError(source, "Property descriptor can not be both accessor and data descriptor.");
        source = createElementDecoratorWithPropertyDescriptor(METHOD, KEY, STATIC,null, TRUE, null, EMPTY_SETTER,null);
        testError(source, "Property descriptor can not be both accessor and data descriptor.");
    }
}
