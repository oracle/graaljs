package com.oracle.truffle.js.jalangilike.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.EvalCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class EvalTest extends FineGrainedAccessTest {

    @Test
    public void eval() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "eval('var a = 42;')");

        assertEngineInit();

        enter(VariableWriteTag.class, (v, var) -> {
            enter(EvalCallTag.class, (e, eval) -> {
                enter(PropertyReadTag.class, assertPropertyReadName("eval")).input(assertGlobalObjectInput).exit();
                eval.input(assertJSFunctionInput);
                enter(LiteralTag.class, assertLiteralType(LiteralTag.Type.StringLiteral)).exit();
                eval.input("var a = 42;");
                enter(PropertyWriteTag.class, (e1, prop) -> {
                    prop.input(assertJSObjectInput);
                    enter(LiteralTag.class).exit(assertReturnValue(42));
                    prop.input(42);
                }).exit();
            }).exit();
            var.input(Undefined.instance);
        }).exit();

        disposeAgent(binding);
    }

}
