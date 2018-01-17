package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ElementReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ElementWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;

public class ElementsAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = [1]; a[0];");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");

        enter(ElementReadTag.class, (e, elem) -> {
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralTag.class).exit();
            elem.input(0);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void nestedRead() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = [0]; a[a[0]];");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");

        enter(ElementReadTag.class, (e, elem) -> {
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            // nested read a[0]
            enter(ElementReadTag.class, (e1, elem1) -> {
                enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                elem1.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
                enter(LiteralTag.class).exit();
                elem1.input(0);
            }).exit();
            // outer read
            elem.input(0);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void write() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = []; a[1] = 'foo';");

        assertEngineInit();
        assertGlobalArrayLiteralDeclaration("a");
        // write element
        enter(ElementWriteTag.class, (e, elem) -> {
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
            elem.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
            enter(LiteralTag.class).exit();
            elem.input(1);
            enter(LiteralTag.class).exit();
            elem.input("foo");
        }).exit();

        disposeAgent(binding);
    }

}
