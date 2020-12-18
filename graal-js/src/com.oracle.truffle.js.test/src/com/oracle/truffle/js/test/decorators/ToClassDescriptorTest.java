package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToClassDescriptorTest extends DecoratorTest{
    @Test
    public void testKindDifferentFromClass() {
        String source = createClassDecorator(METHOD, null, null, null);
        testError(source, "Property kind of class descriptor must be 'class'.");
        source = createClassDecorator(FIELD, null, null, null);
        testError(source, "Property kind of class descriptor must be 'class'.");
        source = createClassDecorator(ACCESSOR, null, null, null);
        testError(source, "Property kind of class descriptor must be 'class'.");
        source = createClassDecorator(HOOK, null, null, null);
        testError(source, "Property kind of class descriptor must be 'class'.");
        source = createClassDecorator("'invalid'", null, null, null);
        testError(source, "Property kind of class descriptor must be 'class'.");
    }

    @Test
    public void testKeyPresent() {
        String source = createClassDecorator(CLASS, KEY, null,null);
        testError(source, "Class descriptor must not have property key.");
    }

    @Test
    public void testPlacementPresent() {
        String source = createClassDecorator(CLASS, null, STATIC, null);
        testError(source, "Class descriptor must not have property placement.");
        source = createClassDecorator(CLASS, null, OWN, null);
        testError(source, "Class descriptor must not have property placement.");
        source = createClassDecorator(CLASS, null, PROTOTYPE, null);
        testError(source, "Class descriptor must not have property placement.");
        source = createClassDecorator(CLASS, null, "'invalid'", null);
        testError(source, "Class descriptor must not have property placement.");
    }

    @Test
    public void testDescriptorPresent() {
        String source = createClassDecorator(CLASS,null,null, "d.descriptor.writable = " + TRUE + ";");
        testError(source, "Property descriptor of class descriptor must either be empty or undefined.");
        source = createClassDecorator(CLASS,null,null, "d.descriptor.value = " + EMPTY_METHOD + ";");
        testError(source, "Property descriptor of class descriptor must either be empty or undefined.");
        source = createClassDecorator(CLASS,null,null, "d.descriptor.get = " + EMPTY_GETTER + ";");
        testError(source, "Property descriptor of class descriptor must either be empty or undefined.");
        source = createClassDecorator(CLASS,null,null, "d.descriptor.set = " + EMPTY_SETTER + ";");
        testError(source, "Property descriptor of class descriptor must either be empty or undefined.");
    }

    @Test
    public void testInitializePresent() {
        String source = createClassDecorator(CLASS,null,null, "d.initialize = " + EMPTY_METHOD + ";");
        testError(source, "Class descriptor must not have property initialize.");
    }

    @Test
    public void testStartPresent() {
        String source = createClassDecorator(CLASS, null, null, "d.start = " + EMPTY_METHOD + ";");
        testError(source, "Class descriptor must not have property start.");
    }

    @Test
    public void testExtrasPresent() {
        String source = createClassDecorator(CLASS, null, null, "d.extras = {}");
        testError(source, "Class descriptor must not have property extras.");
    }

    @Test
    public void testFinishPresent() {
        String source = createClassDecorator(CLASS, null, null, "d.finish = " + EMPTY_METHOD + ";");
        testError(source, "Class descriptor must not have property finish.");
    }

    @Test
    public void testReplacePresent() {
        String source = createClassDecorator(CLASS, null, null, "d.replace = " + EMPTY_METHOD + ";");
        testError(source, "Class descriptor must not have property replace.");
    }
}
