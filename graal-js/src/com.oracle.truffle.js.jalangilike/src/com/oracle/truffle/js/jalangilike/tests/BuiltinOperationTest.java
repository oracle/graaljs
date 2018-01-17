package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BuiltinRootTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class BuiltinOperationTest extends FineGrainedAccessTest {

    @Test
    public void mathRandom() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = Math.random; a();");

        assertEngineInit();

        // var a = Math.random
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertGlobalObjectInput);
            enter(PropertyReadTag.class, (e1, read) -> {
                assertAttribute(e1, KEY, "random");
                enter(PropertyReadTag.class, (e2, prop) -> {
                    assertAttribute(e2, KEY, "Math");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                read.input(assertJSObjectInput);
            }).exit();
            write.input(assertJSObjectInput);
        }).exit();

        // a()
        enter(FunctionCallTag.class, (e, call) -> {
            // read target for 'a' (which is undefined)
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // read function 'a'
            enter(PropertyReadTag.class, assertPropertyReadName("a")).input().exit();
            call.input(assertJSFunctionInput);
            enter(BuiltinRootTag.class, (e2) -> {
                assertAttribute(e2, NAME, "Math.random");
            }).exit();
        }).exit();

        disposeAgent(binding);
    }

    @Ignore
    @Test
    public void objectDefineProp() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "const foo = {};" +
                        "Object.defineProperty(foo, 'foo', {" +
                        "  value: 42" +
                        "});");

        assertEngineInit();
        assertOn(Event.Kind.RETURN, LiteralTag.class);
        assertOn(Event.Kind.INPUT, VariableWriteTag.class);

        assertOn(Event.Kind.RETURN, VariableWriteTag.class, (e) -> {
            assertAttribute(e, NAME, "foo");
        });

        assertOn(Event.Kind.RETURN, PropertyReadTag.class, (e) -> {
            assertAttribute(e, KEY, "Object");
        });
        assertOn(Event.Kind.INPUT, PropertyReadTag.class);
        assertOn(Event.Kind.RETURN, PropertyReadTag.class, (e) -> {
            assertAttribute(e, KEY, "defineProperty");
        });
        assertOn(Event.Kind.INPUT, FunctionCallTag.class);

        assertOn(Event.Kind.RETURN, VariableReadTag.class, (e) -> {
            assertAttribute(e, NAME, "foo");
        });
        assertOn(Event.Kind.RETURN, LiteralTag.class);
        assertOn(Event.Kind.RETURN, LiteralTag.class);
        assertOn(Event.Kind.INPUT, LiteralTag.class);
        assertOn(Event.Kind.RETURN, LiteralTag.class);

        assertOn(Event.Kind.INPUT, FunctionCallTag.class, (e) -> {
            assertTrue(e.val instanceof Object[]);
            Object[] args = (Object[]) e.val;
            assertTrue(args.length == 5);
            assertTrue(JSObject.isJSObject(args[0]));
            assertTrue(JSFunction.isJSFunction(args[1]));
            assertTrue(JSObject.isJSObject(args[2]));
            assertEquals(args[3], "foo");
            assertTrue(JSObject.isJSObject(args[4]));
        });

        assertOn(Event.Kind.RETURN, BuiltinRootTag.class, (e) -> {
            assertAttribute(e, NAME, "Object.defineProperty");
        });
        assertOn(Event.Kind.RETURN, FunctionCallTag.class);

        disposeAgent(binding);
    }

}
