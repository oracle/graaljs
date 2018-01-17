package com.oracle.truffle.js.jalangilike.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;

public class BinaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void basic() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = 42; var b = 43; var c = a + b;");

        assertEngineInit();

        // var a = 42
        assertGlobalVarDeclaration("a", 42);
        // var a = 43
        assertGlobalVarDeclaration("b", 43);
        // var c = a + b;
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "c");
            write.input(assertGlobalObjectInput);

            enter(BinaryOperationTag.class, (e2, binary) -> {
                enter(PropertyReadTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "a");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                binary.input(42);

                enter(PropertyReadTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "b");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                binary.input(43);

            }).exit();

            write.input(85);
        }).exit();

        disposeAgent(binding);
    }

}
