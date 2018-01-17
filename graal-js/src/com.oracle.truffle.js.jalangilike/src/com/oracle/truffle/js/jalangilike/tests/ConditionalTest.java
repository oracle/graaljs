package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.BinaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.ConditionalExpressionTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ConditionalTest extends FineGrainedAccessTest {

    @Test
    public void basic() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "if (!true) {};");

        assertEngineInit();

        // !true
        enter(VariableWriteTag.class, (e, write) -> {
            enter(ConditionalExpressionTag.class, (e1, cond) -> {
                enter(LiteralTag.class).exit();
                cond.input(false);
            }).exit();
            write.input((e2) -> {
                assertTrue(e2.val == Undefined.instance);
            });
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void instrumentable() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var i = 0;" +
                        "var f = 1;" +
                        "if (i < 1) {" +
                        "  f += i;" +
                        "};");

        assertEngineInit();

        assertGlobalVarDeclaration("i", 0);
        assertGlobalVarDeclaration("f", 1);

        enter(ConditionalExpressionTag.class, (e, cond) -> {
            // if (i < 1)
            enter(BinaryOperationTag.class, (e2, bin) -> {
                enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                bin.input(0);
                enter(LiteralTag.class).exit();
                bin.input(1);
            }).exit();
            // condition is true
            cond.input(true);
            // f += 1
            enter(VariableWriteTag.class, (e1, var) -> {
                enter(PropertyWriteTag.class, (e2, write) -> {
                    assertAttribute(e2, KEY, "f");
                    write.input(assertJSObjectInput);
                    enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
                    write.input(0);
                }).exit();
                var.input(1);
            }).exit();
            // the += statement returns
            cond.input(1);
        }).exit();

        disposeAgent(binding);
    }

    // Utility field to track the iteration counter during the test from lambdas
    static int it = 0;

    @Test
    public void forLoop() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "for(var x = 3; x < 6; x++){};");

        assertEngineInit();

        // var x = 3
        enter(PropertyWriteTag.class, (e, prop) -> {
            prop.input(assertJSObjectInput);
            assertAttribute(e, KEY, "x");
            enter(LiteralTag.class).exit();
            prop.input(3);
        }).exit();
        // First condition check: 3 < 6 ? true, so enter the loop
        enter(ConditionalExpressionTag.class, (e, cond) -> {
            enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
            // generated because this is a binary node too
            cond.input(3);
            enter(LiteralTag.class).exit();
            cond.input(6);
        }).exit(assertReturnValue(true));

        int iters = 0;
        for (it = 3; it < 6; it++) {
            iters++;
            // perform increment 'i++'
            enter(PropertyWriteTag.class, (e, prop) -> {
                assertAttribute(e, KEY, "x");
                prop.input(assertJSObjectInput);
                enter(BinaryOperationTag.class, (e1, bin) -> {
                    enter(VariableWriteTag.class, (e2, var) -> {
                        enter(UnaryOperationTag.class, (e3, un) -> {
                            enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
                            un.input(it);
                        }).exit();
                        var.input(it);
                    }).exit();
                    bin.input(it);
                    enter(LiteralTag.class).exit(assertReturnValue(1));
                    bin.input(1);
                }).exit();
                prop.input(it + 1);
            }).exit();
            // read incremented value
            enter(VariableReadTag.class, assertVarReadName("<tmp0>")).exit();

            boolean expectedConditionValue = it < 5;
            // check condition if 'it < 6'
            enter(ConditionalExpressionTag.class, (e, input) -> {
                enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
                input.input(it + 1);
                enter(LiteralTag.class).exit();
                input.input(6);
            }).exit(assertReturnValue(expectedConditionValue));
        }

        assert iters == 3;

        disposeAgent(binding);
    }

    @Test
    public void whileLoop() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var x = 3; while (x < 6) { x++; };");

        assertEngineInit();

        // var x = 3
        enter(PropertyWriteTag.class, (e, prop) -> {
            assertAttribute(e, KEY, "x");
            prop.input(assertJSObjectInput);
            enter(LiteralTag.class).exit();
            prop.input(3);
        }).exit();
        // First condition check: 3 < 6 ? true, so enter the loop
        enter(ConditionalExpressionTag.class, (e, cond) -> {
            enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
            // generated because this is a binary node too
            cond.input(3);
            enter(LiteralTag.class).exit();
            cond.input(6);
        }).exit(assertReturnValue(true));

        int iters = 0;
        for (it = 3; it < 6; it++) {
            iters++;
            // the while loop here returns the last value of 'x++'
            enter(VariableWriteTag.class, (v, ret) -> {
                assertAttribute(v, NAME, "<return>");
                // perform increment 'i++'
                enter(PropertyWriteTag.class, (e, prop) -> {
                    assertAttribute(e, KEY, "x");
                    prop.input(assertJSObjectInput);
                    enter(BinaryOperationTag.class, (e1, bin) -> {
                        enter(VariableWriteTag.class, (e2, var) -> {
                            enter(UnaryOperationTag.class, (e3, un) -> {
                                enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
                                un.input(it);
                            }).exit();
                            var.input(it);
                        }).exit();
                        bin.input(it);
                        enter(LiteralTag.class).exit(assertReturnValue(1));
                        bin.input(1);
                    }).exit();
                    prop.input(it + 1);
                }).exit();
                // read incremented value
                enter(VariableReadTag.class, assertVarReadName("<tmp0>")).exit();
                ret.input(it);
            }).exit();

            boolean expectedConditionValue = it < 5;
            // check condition if 'it < 6'
            enter(ConditionalExpressionTag.class, (e, input) -> {
                enter(PropertyReadTag.class, assertPropertyReadName("x")).input(assertGlobalObjectInput).exit();
                input.input(it + 1);
                enter(LiteralTag.class).exit();
                input.input(6);
            }).exit(assertReturnValue(expectedConditionValue));
        }

        assert iters == 3;

        disposeAgent(binding);
    }
}
