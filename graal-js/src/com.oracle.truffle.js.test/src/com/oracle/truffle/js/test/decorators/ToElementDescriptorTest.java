package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToElementDescriptorTest extends DecoratorTest {
    @Test
    public void testInvalidKind()
    {
        String source = createElementDecorator("'invalid'", null, null, null);
        testError(source, "Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
        source = createElementDecorator("0", null, null, null);
        testError(source, "Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
        source = createElementDecorator("() => {}", null, null, null);
        testError(source, "Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
        source = createElementDecorator("true", null, null, null);
        testError(source, "Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
        source = createElementDecorator("null", null, null, null);
        testError(source, "Property kind of element descriptor must be one of 'hook', 'method', 'accessor' or 'field'.");
    }

    @Test
    public void testHookWithKey() {
        String source = createElementDecorator(HOOK,KEY,STATIC,null);
        testError(source, "Element descriptor with kind 'hook' must not have property key.");
        source = createElementDecorator(HOOK,KEY,OWN,null);
        testError(source, "Element descriptor with kind 'hook' must not have property key.");
        source = createElementDecorator(HOOK,KEY,PROTOTYPE,null);
        testError(source, "Element descriptor with kind 'hook' must not have property key.");
    }

    @Test
    public void testInvalidPlacement()
    {
        String source = createElementDecorator(METHOD,KEY,"'invalid'",null);
        testError(source, "Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
        source = createElementDecorator(METHOD,KEY,"0",null);
        testError(source, "Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
        source = createElementDecorator(METHOD,KEY,EMPTY_METHOD,null);
        testError(source, "Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
        source = createElementDecorator(METHOD,KEY,TRUE,null);
        testError(source, "Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
        source = createElementDecorator(METHOD,KEY,"null",null);
        testError(source, "Property placement of element descriptor must be one of 'static', 'prototype' or 'own'.");
    }

    @Test
    public void testPrivateKeyWithEnumerableTrue()
    {
        String source = createElementDecorator(METHOD,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
        source = createElementDecorator(ACCESSOR,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
        source = createElementDecorator(FIELD,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
    }

    @Test
    public void testPrivateKeyWithConfigurableTrue()
    {
        String source = createElementDecorator(METHOD, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
        source = createElementDecorator(ACCESSOR, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
        source = createElementDecorator(FIELD, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
    }

    @Test
    public void testPrivateKeyWithPlacementPrototype(){
        String source = createElementDecorator(METHOD,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
        source = createElementDecorator(ACCESSOR,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
        source = createElementDecorator(FIELD,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
    }

    @Test
    public void testHookOrAccessorWithDataDescriptor(){
        String source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,EMPTY_METHOD, null,null);
        testError(source, "Property descriptor of element descriptor must not be a data descriptor if property kind is 'accessor' or 'hook'.");
        source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,null, TRUE,null);
        testError(source, "Property descriptor of element descriptor must not be a data descriptor if property kind is 'accessor' or 'hook'.");
        source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,EMPTY_METHOD,null,null);
        testError(source, "Property descriptor of element descriptor must not be a data descriptor if property kind is 'accessor' or 'hook'.");
        source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,null, TRUE,null);
        testError(source, "Property descriptor of element descriptor must not be a data descriptor if property kind is 'accessor' or 'hook'.");
    }

    @Test
    public void testFieldMethodOrHookWithAccessorDescriptor() {
        String source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC, EMPTY_GETTER,null,null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC,null, EMPTY_SETTER,null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC,EMPTY_GETTER,null,null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC, null, EMPTY_SETTER, null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, EMPTY_GETTER, null, null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
        source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, null, EMPTY_SETTER, null);
        testError(source, "Property descriptor of element descriptor must not be an accessor descriptor of property kind is 'field', 'method' or 'hook'.");
    }

    @Test
    public void testFieldWithMethod() {
        String source = createElementDecoratorWithDataDescriptor(FIELD, KEY,STATIC, EMPTY_METHOD, null,null);
        testError(source, "Data descriptor of element descriptor must not have a value property if property kind is 'field'.");
    }

    @Test
    public void testNonCallableInitialize() {
        for(String c : NON_CALLABLES) {
            String source = createElementDecorator(FIELD, KEY, STATIC, "d.initialize = " + c + ";");
            testError(source, "Property initialize of element descriptor must be callable.");
        }
    }

    @Test
    public void testNonFieldWithInitialize() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor without kind 'field' must not have property initialize.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor without kind 'field' must not have property initialize.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor without kind 'field' must not have property initialize.");
    }

    @Test
    public void testNonCallableStart() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.start = " + c + ";");
            testError(source, "Property start of element descriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithStart() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property start.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property start.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property start.");
    }

    @Test
    public void testNonCallableReplace() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.replace = " + c + ";");
            testError(source, "Property replace of element descriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithReplace() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property replace.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property replace.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property replace.");
    }

    @Test
    public void testNonCallableFinish() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.finish = " + c + ";");
            testError(source, "Property finish of element descriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithFinish() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property finish.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property finish.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor without kind 'hook' must not have property finish.");
    }

    @Test
    public void testHookWithoutStartReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, null);
        testError(source, "Element descriptor with kind 'hook' must have at least one of property start, replace or finish.");
    }

    @Test
    public void testHookWithReplaceAndFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.replace = () => {}; d.finish = () => {};");
        testError(source, "Properties replace and finish cannot both be present.");
    }

    @Test
    public void testHookWithOwnReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, OWN, "d.replace = () => {};");
        testError(source, "Element descriptor with kind 'hook' and placement 'own' must not have properties replace and finish.");
        source = createElementDecorator(HOOK, null, OWN, "d.finish = () => {};");
        testError(source, "Element descriptor with kind 'hook' and placement 'own' must not have properties replace and finish.");
    }

    @Test
    public void testHookWithPrototypeAndReplace() {
        String source = createElementDecorator(HOOK, null, PROTOTYPE, "d.replace = () => {};");
        testError(source, "Element descriptor with kind 'hook' and placement 'own' must not have property replace.");
    }

    @Test
    public void testElementsPresent() {
        String source = createElementDecorator(METHOD, KEY, STATIC, "d.elements = {};");
        testError(source, "Element descriptor must not have property elements.");
        source = createElementDecorator(FIELD, KEY, STATIC, "d.elements = {};");
        testError(source, "Element descriptor must not have property elements.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.elements = {};");
        testError(source, "Element descriptor must not have property elements.");
        source = createElementDecorator(HOOK, null, STATIC, "d.elements = {}; d.start =" +EMPTY_METHOD + ";");
        testError(source, "Element descriptor must not have property elements.");
    }
}
