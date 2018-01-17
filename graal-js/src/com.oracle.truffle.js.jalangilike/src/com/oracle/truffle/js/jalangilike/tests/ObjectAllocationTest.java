package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ObjectAllocationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class ObjectAllocationTest extends FineGrainedAccessTest {

    @Test
    public void basic() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = new Object(); var b = {}; var c = [];");

        assertEngineInit();

        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);

            enter(ObjectAllocationTag.class, (e1, alloc) -> {
                enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                alloc.input((e2) -> {
                    assertTrue(JSFunction.isJSFunction(e2.val));
                });
                // TODO missing input event for arguments to ObjectAllocationTag
                enter(BuiltinRootTag.class, (e2) -> {
                    assertAttribute(e2, NAME, "Object");
                }).exit();
            }).exit();
            write.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "b");
            prop.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "c");
            prop.input(assertGlobalObjectInput);
            enter(LiteralTag.class).exit();
            prop.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void nested() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = {x:{}}; var b = [[]]; var c = {x:[]}");

        assertEngineInit();

        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "a");
            prop.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.ObjectLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSObject.isDynamicObject(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "b");
            prop.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.ArrayLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.ArrayLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSArray.isJSArray(e1.val));
            });
        }).exit();

        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "c");
            prop.input(assertGlobalObjectInput);
            enter(LiteralTag.class, (e1, lit) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.ArrayLiteral.name());
                }).exit();
                lit.input((e2) -> {
                    assertTrue(JSArray.isJSArray(e2.val));
                });
            }).exit();
            prop.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
        }).exit();

        disposeAgent(binding);
    }

}
