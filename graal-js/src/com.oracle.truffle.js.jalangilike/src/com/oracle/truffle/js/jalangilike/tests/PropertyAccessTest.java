package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag.Type;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class PropertyAccessTest extends FineGrainedAccessTest {

    @Test
    public void read() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = {x:42}; a.x;");

        assertEngineInit();

        // var a = {x:42}
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                assertAttribute(e2, TYPE, Type.ObjectLiteral.name());
                // num literal
                enter(LiteralTag.class).exit();
            }).input(42).exit();
        }).input((e) -> {
            assertTrue(JSObject.isJSObject(e.val));
        }).exit();
        // a.x;
        enter(PropertyReadTag.class, (e) -> {
            assertAttribute(e, KEY, "x");
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
        }).input((e) -> {
            assertTrue(JSObject.isJSObject(e.val));
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void nestedRead() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = {x:{y:42}}; a.x.y;");

        assertEngineInit();

        // var a = {x:{y:42}}
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e2) -> {
                enter(LiteralTag.class, (e3) -> {
                    assertAttribute(e3, TYPE, Type.ObjectLiteral.name());
                    // num literal
                    enter(LiteralTag.class).exit();
                }).input(42).exit();
            }).input().exit();
            write.input(assertJSObjectInput);
        }).exit();
        // a.x.y;
        enter(PropertyReadTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "y");
            // a.x
            enter(PropertyReadTag.class, (e1, read) -> {
                assertAttribute(e1, KEY, "x");
                enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                read.input(assertJSObjectInput);
            }).exit();
            prop.input(assertJSObjectInput);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void write() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = {}; a.x = 42;");

        assertEngineInit();

        // var a = {}
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            // {}
            enter(LiteralTag.class).exit();
            write.input(assertJSObjectInput);
        }).exit();

        // a.x = 42
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "x");
            // global read
            enter(PropertyReadTag.class, (e1, p) -> {
                assertAttribute(e1, KEY, "a");
                p.input(assertGlobalObjectInput);
            }).exit();
            write.input(assertJSObjectInput);
            enter(LiteralTag.class).exit();
            write.input(42);
        }).exit();

        disposeAgent(binding);
    }
}
