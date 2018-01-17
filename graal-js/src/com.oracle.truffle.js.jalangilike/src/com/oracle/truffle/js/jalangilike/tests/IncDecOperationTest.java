package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.JSObject;

public class IncDecOperationTest extends FineGrainedAccessTest {

    @Test
    public void inc() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = 42; a++;");

        assertEngineInit();
        assertGlobalVarDeclaration("a", 42);

        // Inc operation de-sugared to tmp = tmp + 1;
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input(assertJSObjectInput);
            enter(BinaryOperationTag.class, (e1, bin) -> {
                enter(VariableWriteTag.class, (e2, var) -> {
                    enter(UnaryOperationTag.class, (e3, un) -> {
                        enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit(assertReturnValue(42));
                        un.input(42);
                    }).exit();
                    var.input(42);
                }).exit();
                bin.input(42);
                enter(LiteralTag.class).exit(assertReturnValue(1));
                bin.input(1);
            }).exit();
            write.input(43);
        }).exit();
        enter(VariableReadTag.class, (e) -> {
            assertAttribute(e, NAME, "<tmp0>");
        }).exit(assertReturnValue(42));

        disposeAgent(binding);
    }

    @Test
    public void dec() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var a = 42; a--;");

        assertEngineInit();
        assertGlobalVarDeclaration("a", 42);

        // Dec operation de-sugared to tmp = tmp - 1;
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "a");
            write.input((e1) -> {
                assertTrue(JSObject.isDynamicObject(e1.val));
            });
            enter(BinaryOperationTag.class, (e1, bin) -> {
                enter(VariableWriteTag.class, (e2, var) -> {
                    enter(UnaryOperationTag.class, (e3, un) -> {
                        enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                        un.input(42);
                    }).exit();
                    var.input(42);
                }).exit();
                bin.input(42);
                enter(LiteralTag.class).exit();
                bin.input(1);
            }).exit();
            write.input(41);
        }).exit();
        enter(VariableReadTag.class, (e) -> {
            assertAttribute(e, NAME, "<tmp0>");
        }).exit();

        disposeAgent(binding);
    }

}
