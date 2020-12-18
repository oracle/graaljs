package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class ToElementDescriptorTest extends DecoratorTest {
    @Test
    public void testInvalidKind()
    {
        String source = createElementDecorator("'invalid'", null, null, null);
        testError(source, "Invalid ElementDescriptor kind. Must be one of hook, method, accessor or field.");
        source = createElementDecorator("0", null, null, null);
        testError(source, "Invalid ElementDescriptor kind. Must be one of hook, method, accessor or field.");
        source = createElementDecorator("() => {}", null, null, null);
        testError(source, "Invalid ElementDescriptor kind. Must be one of hook, method, accessor or field.");
        source = createElementDecorator("true", null, null, null);
        testError(source, "Invalid ElementDescriptor kind. Must be one of hook, method, accessor or field.");
        source = createElementDecorator("null", null, null, null);
        testError(source, "Invalid ElementDescriptor kind. Must be one of hook, method, accessor or field.");
    }

    @Test
    public void testHookWithKey() {
        String source = createElementDecorator(HOOK,KEY,STATIC,null);
        testError(source, "ElementDescriptor with kind hook must not have a key.");
        source = createElementDecorator(HOOK,KEY,OWN,null);
        testError(source, "ElementDescriptor with kind hook must not have a key.");
    }

    @Test
    public void testInvalidPlacement()
    {
        String source = createElementDecorator(METHOD,KEY,"'invalid'",null);
        testError(source, "Invalid ElementDescriptor placement. Must be one of static, prototype or own.");
        source = createElementDecorator(METHOD,KEY,"0",null);
        testError(source, "Invalid ElementDescriptor placement. Must be one of static, prototype or own.");
        source = createElementDecorator(METHOD,KEY,EMPTY_METHOD,null);
        testError(source, "Invalid ElementDescriptor placement. Must be one of static, prototype or own.");
        source = createElementDecorator(METHOD,KEY,TRUE,null);
        testError(source, "Invalid ElementDescriptor placement. Must be one of static, prototype or own.");
        source = createElementDecorator(METHOD,KEY,"null",null);
        testError(source, "Invalid ElementDescriptor placement. Must be one of static, prototype or own.");
    }

    @Test
    public void testPrivateKeyWithEnumerableTrue()
    {
        String source = createElementDecorator(METHOD,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "ElementDescriptor with private key must not be enumerable.");
        source = createElementDecorator(ACCESSOR,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "ElementDescriptor with private key must not be enumerable.");
        source = createElementDecorator(FIELD,PRIVATE_KEY,OWN,"d.descriptor.enumerable = true;");
        testError(source, "ElementDescriptor with private key must not be enumerable.");
    }

    @Test
    public void testPrivateKeyWithConfigurableTrue()
    {
        String source = createElementDecorator(METHOD, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "ElementDescriptor with private key must not be configurable.");
        source = createElementDecorator(ACCESSOR, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "ElementDescriptor with private key must not be configurable.");
        source = createElementDecorator(FIELD, PRIVATE_KEY, OWN, "d.descriptor.configurable = true;");
        testError(source, "ElementDescriptor with private key must not be configurable.");
    }

    @Test
    public void testPrivateKeyWithPlacementPrototype(){
        String source = createElementDecorator(METHOD,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "ElementDescriptor with private key must not have placement prototype.");
        source = createElementDecorator(ACCESSOR,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "ElementDescriptor with private key must not have placement prototype.");
        source = createElementDecorator(FIELD,PRIVATE_KEY,PROTOTYPE,null);
        testError(source, "ElementDescriptor with private key must not have placement prototype.");
    }

    @Test
    public void testHookWithDataDescriptor(){
        String source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,EMPTY_METHOD, null,null);
        testError(source, "ElementDescriptor with kind accessor or hook must not have a data descriptor.");
        source = createElementDecoratorWithDataDescriptor(HOOK,null,STATIC,null, TRUE,null);
        testError(source, "ElementDescriptor with kind accessor or hook must not have a data descriptor.");
    }

    @Test
    public void testAccessorWithDataDescriptor() {
        String source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,EMPTY_METHOD,null,null);
        testError(source, "ElementDescriptor with kind accessor or hook must not have a data descriptor.");
        source = createElementDecoratorWithDataDescriptor(ACCESSOR,KEY,STATIC,null, TRUE,null);
        testError(source, "ElementDescriptor with kind accessor or hook must not have a data descriptor.");
    }

    @Test
    public void testFieldWithAccessorDescriptor() {
        String source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC, EMPTY_GETTER,null,null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(FIELD,KEY,STATIC,null, EMPTY_SETTER,null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
    }

    @Test
    public void testMethodWithAccessorDescriptor() {
        String source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC,EMPTY_GETTER,null,null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(METHOD,KEY,STATIC, null, EMPTY_SETTER, null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
    }

    @Test
    public void testHookWithAccessorDescriptor() {
        String source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, EMPTY_GETTER, null, null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
        source = createElementDecoratorWithAccessorDescriptor(HOOK, null, STATIC, null, EMPTY_SETTER, null);
        testError(source, "ElementDescriptor with kind field, method or hook must not have a accessor descriptor.");
    }

    @Test
    public void testFieldWithMethod() {
        String source = createElementDecoratorWithDataDescriptor(FIELD, KEY,STATIC, EMPTY_METHOD, null,null);
        testError(source, "ElementDescriptor with kind field must not have a method value.");
    }

    @Test
    public void testNonCallableInitialize() {
        for(String c : NON_CALLABLES) {
            String source = createElementDecorator(FIELD, KEY, STATIC, "d.initialize = " + c + ";");
            testError(source, "Property initialize of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonFieldWithInitialize() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.initialize = () => {};");
        testError(source, "ElementDescriptor without kind field must not have an initialize property.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "ElementDescriptor without kind field must not have an initialize property.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.initialize = () => {};");
        testError(source, "ElementDescriptor without kind field must not have an initialize property.");
    }

    @Test
    public void testNonCallableStart() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.start = " + c + ";");
            testError(source, "Property start of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithStart() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.start = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a start property.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.start = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a start property.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.start = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a start property.");
    }

    @Test
    public void testNonCallableReplace() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.replace = " + c + ";");
            testError(source, "Property replace of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithReplace() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a replace property.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.replace = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a replace property.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.replace = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a replace property.");
    }

    @Test
    public void testNonCallableFinish() {
        for(String c: NON_CALLABLES) {
            String source = createElementDecorator(HOOK, null, STATIC, "d.finish = " + c + ";");
            testError(source, "Property finish of ElementDescriptor must be callable.");
        }
    }

    @Test
    public void testNonHookWithFinish() {
        String source = createElementDecorator(FIELD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a finish property.");
        source = createElementDecorator(METHOD, KEY, STATIC, "d.finish = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a finish property.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.finish = () => {};");
        testError(source, "ElementDescriptor without kind hook must not have a finish property.");
    }

    @Test
    public void testHookWithoutStartReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, null);
        testError(source, "ElementDescriptor with kind hook must have at least one of start, replace or finish.");
    }

    @Test
    public void testHookWithReplaceAndFinish() {
        String source = createElementDecorator(HOOK, null, STATIC, "d.replace = () => {}; d.finish = () => {};");
        testError(source, "Properties replace and finish can not be present together.");
    }

    @Test
    public void testHookWithOwnReplaceOrFinish() {
        String source = createElementDecorator(HOOK, null, OWN, "d.replace = () => {};");
        testError(source, "ElementDescriptor with kind hook and placement own must not have a replace or finish property.");
        source = createElementDecorator(HOOK, null, OWN, "d.finish = () => {};");
        testError(source, "ElementDescriptor with kind hook and placement own must not have a replace or finish property.");
    }

    @Test
    public void testHookWithPrototypeAndReplace() {
        String source = createElementDecorator(HOOK, null, PROTOTYPE, "d.replace = () => {};");
        testError(source, "ElementDescriptor with kind hook and placement prototype must not have a replace property.");
    }

    @Test
    public void testElementsPresent() {
        String source = createElementDecorator(METHOD, KEY, STATIC, "d.elements = {};");
        testError(source, "ElementDescriptor must not have property elements.");
        source = createElementDecorator(FIELD, KEY, STATIC, "d.elements = {};");
        testError(source, "ElementDescriptor must not have property elements.");
        source = createElementDecorator(ACCESSOR, KEY, STATIC, "d.elements = {};");
        testError(source, "ElementDescriptor must not have property elements.");
        source = createElementDecorator(HOOK, null, STATIC, "d.elements = {};");
        testError(source, "ElementDescriptor must not have property elements.");
    }
}
