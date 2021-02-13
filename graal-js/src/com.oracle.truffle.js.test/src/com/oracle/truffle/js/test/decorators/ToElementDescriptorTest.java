package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToElementDescriptorTest extends DecoratorTest {
    @Test
    public void testInvalidKind()
    {
        for(String invalid: NON_VALIDS) {
            String source = createElementDecorator(invalid, null, null, null);
            testError(source, "Element descriptor property kind must be one of 'hook', 'method', 'accessor' or 'field'.");
        }
    }

    @Test
    public void testHookWithKey() {
        String source = createElementDecorator(HOOK,KEY,STATIC,null);
        testError(source, "Property descriptor of element descriptor with kind 'hook' must not have property key.");
        source = createElementDecorator(HOOK,KEY,OWN,null);
        testError(source, "Property descriptor of element descriptor with kind 'hook' must not have property key.");
        source = createElementDecorator(HOOK,KEY,PROTOTYPE,null);
        testError(source, "Property descriptor of element descriptor with kind 'hook' must not have property key.");
    }

    @Test
    public void testInvalidPlacement()
    {
        for(String invalid: NON_VALIDS) {
            String source = createElementDecorator(METHOD,KEY,invalid,null);
            testError(source, "Element descriptor property placement must be one of 'static', 'prototype' or 'own'.");
        }
    }

    @Test
    public void testPrivateKeyWithEnumerableTrue()
    {
        String source = createElementDecoratorWithPrivateMethod(METHOD,"#test",OWN,"d.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
        source = createElementDecoratorWithPrivateMethod(ACCESSOR,"#test",OWN,"d.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
        source = createElementDecoratorWithPrivateMethod(FIELD,"#test",OWN,"d.enumerable = true;");
        testError(source, "Element descriptor with private key must not be enumerable.");
    }

    @Test
    public void testPrivateKeyWithConfigurableTrue()
    {
        String source = createElementDecoratorWithPrivateMethod(METHOD, "#test", OWN, "d.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
        source = createElementDecoratorWithPrivateMethod(ACCESSOR, "#test", OWN, "d.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
        source = createElementDecoratorWithPrivateMethod(FIELD, "#test", OWN, "d.configurable = true;");
        testError(source, "Element descriptor with private key must not be configurable.");
    }

    @Test
    public void testPrivateKeyWithPlacementPrototype(){
        String source = createElementDecoratorWithPrivateMethod(METHOD,"#test",PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
        source = createElementDecoratorWithPrivateMethod(ACCESSOR,"#test",PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
        source = createElementDecoratorWithPrivateMethod(FIELD,"#test",PROTOTYPE,null);
        testError(source, "Element descriptor with private key must not have placement 'prototype'.");
    }

    @Test
    public void testHookOrAccessorWithDataDescriptor(){
        String source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,EMPTY_METHOD, null,null);
        testError(source, "Property descriptor of element descriptor with kind 'accessor' or 'hook' must not be a data descriptor.");
        source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,null, TRUE,null);
        testError(source, "Property descriptor of element descriptor with kind 'accessor' or 'hook' must not be a data descriptor.");
        source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,EMPTY_METHOD,null,null);
        testError(source, "Property descriptor of element descriptor with kind 'accessor' or 'hook' must not be a data descriptor.");
        source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,null, TRUE,null);
        testError(source, "Property descriptor of element descriptor with kind 'accessor' or 'hook' must not be a data descriptor.");
    }

    @Test
    public void testFieldMethodOrHookWithAccessorDescriptor() {
        String source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC, EMPTY_GETTER,null,null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC,null, EMPTY_SETTER,null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC,EMPTY_GETTER,null,null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC, null, EMPTY_SETTER, null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, EMPTY_GETTER, null, null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, null, EMPTY_SETTER, null);
        testError(source, "Property descriptor of element descriptor with kind 'field', 'method' or 'hook' must not be an accessor descriptor.");
    }

    @Test
    public void testFieldWithMethod() {
        String source = createElementDecoratorWithDataDescriptor(FIELD, KEY,STATIC, EMPTY_METHOD, null,null);
        testError(source, "Property descriptor of element descriptor with kind 'field' must not have a value property.");
    }

    @Test
    public void testNonCallableInitialize() {
        for(String c : NON_CALLABLES) {
            String source = createElementDecorator(FIELD, KEY, STATIC, "d.initialize = " + c + ";");
            testError(source, "Element descriptor property initialize with value other than undefined must be callable.");
        }
    }

    @Test
    public void testNonFieldWithInitialize() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor property initialize with value other than undefined requires element descriptor kind 'field'.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor property initialize with value other than undefined requires element descriptor kind 'field'.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "Element descriptor property initialize with value other than undefined requires element descriptor kind 'field'.");
    }

    @Test
    public void testNonCallableStart() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.start = " + c + ";");
            testError(source, "Element descriptor property start with value other than undefined must be callable.");
        }
    }

    @Test
    public void testNonHookWithStart() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor property start with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor property start with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.start = () => {};");
        testError(source, "Element descriptor property start with value other than undefined requires element descriptor kind 'hook'.");
    }

    @Test
    public void testNonCallableReplace() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.replace = " + c + ";");
            testError(source, "Element descriptor property replace with value other than undefined must be callable.");
        }
    }

    @Test
    public void testNonHookWithReplace() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor property replace with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor property replace with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.replace = () => {};");
        testError(source, "Element descriptor property replace with value other than undefined requires element descriptor kind 'hook'.");
    }

    @Test
    public void testNonCallableFinish() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.finish = " + c + ";");
            testError(source, "Element descriptor property finish with value other than undefined must be callable.");
        }
    }

    @Test
    public void testNonHookWithFinish() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor property finish with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor property finish with value other than undefined requires element descriptor kind 'hook'.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.finish = () => {};");
        testError(source, "Element descriptor property finish with value other than undefined requires element descriptor kind 'hook'.");
    }

    @Test
    public void testHookWithoutStartReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, null);
        testError(source, "Element descriptor property kind 'hook' must have at least one of start, replace or finish.");
    }

    @Test
    public void testHookWithReplaceAndFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.replace = () => {}; d.finish = () => {};");
        testError(source, "Properties replace and finish cannot both be present on element descriptor.");
    }

    @Test
    public void testHookWithOwnReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, OWN, "d.replace = () => {};");
        testError(source, "Element descriptor property kind 'hook' and placement 'own' must not have properties replace and finish.");
        source = createElementDecorator(HOOK, null, OWN, "d.finish = () => {};");
        testError(source, "Element descriptor property kind 'hook' and placement 'own' must not have properties replace and finish.");
    }

    @Test
    public void testHookWithPrototypeAndReplace() {
        String source = createElementDecorator(HOOK, null, PROTOTYPE, "d.replace = () => {};");
        testError(source, "Element descriptor property kind 'hook' and placement 'prototype' must not have property replace.");
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
