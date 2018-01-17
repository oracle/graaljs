package com.oracle.truffle.js.jalangilike.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LocalsAccessTest extends FineGrainedAccessTest {

    @Test
    public void write() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "(function() { var a = 42; })();");

        assertEngineInit();

        enter(VariableWriteTag.class, (e, write) -> {
            enter(FunctionCallTag.class, (e1, call) -> {
                // fetch the target for the call (which is undefined)
                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                // fetch the function, i.e., read the literal
                enter(LiteralTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                enter(UnaryOperationTag.class, (e2, unary) -> {
                    enter(VariableWriteTag.class, (e3, var) -> {
                        enter(LiteralTag.class).exit();
                        var.input(42);
                    }).exit();
                    unary.input(42);
                }).exit();
            }).exit();
            write.input(Undefined.instance);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void writeScope() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "(function() { var level; (function() { (function() { level = 42; })(); })();})();");

        assertEngineInit();

        enter(VariableWriteTag.class, (e, write) -> {
            // first call
            enter(FunctionCallTag.class, (e1, call) -> {
                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                enter(LiteralTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                // second call
                enter(UnaryOperationTag.class, (e6, unary) -> {
                    enter(FunctionCallTag.class, (e2, call2) -> {
                        enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                        call2.input(assertUndefinedInput);
                        enter(LiteralTag.class).exit((e3) -> {
                            assertAttribute(e3, TYPE, LiteralTag.Type.FunctionLiteral.name());
                        });
                        call2.input(assertJSFunctionInput);
                        // third call
                        enter(UnaryOperationTag.class, (e7, unary2) -> {
                            enter(FunctionCallTag.class, (e3, call3) -> {
                                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                                call3.input(assertUndefinedInput);
                                enter(LiteralTag.class).exit((e4) -> {
                                    assertAttribute(e4, TYPE, LiteralTag.Type.FunctionLiteral.name());
                                });
                                call3.input(assertJSFunctionInput);
                                // TODO missing input event for arguments to FunctionCallTag
                                enter(UnaryOperationTag.class, (e8, unary3) -> {
                                    enter(VariableWriteTag.class, (e4, var) -> {
                                        enter(LiteralTag.class).exit();
                                        var.input(42);
                                    }).exit();
                                    unary3.input();
                                }).exit();
                            }).exit();
                            unary2.input();
                        }).exit();
                    }).exit();
                    unary.input();
                }).exit();
            }).exit();
            write.input(Undefined.instance);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void read() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "(function() { var a = 42; return a; })();");

        assertEngineInit();

        enter(VariableWriteTag.class, (e, write) -> {
            enter(FunctionCallTag.class, (e1, call) -> {
                // fetch the target for the call (which is undefined)
                enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                call.input(assertUndefinedInput);
                // get the function from the literal
                enter(LiteralTag.class).exit((e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.FunctionLiteral.name());
                });
                call.input(assertJSFunctionInput);
                // write 42
                enter(VariableWriteTag.class, (e2, var) -> {
                    enter(LiteralTag.class).exit();
                    var.input(42);
                }).exit();
                // return statement
                enter(VariableReadTag.class).exit();
            }).exit();
            write.input(42);
        }).exit();

        disposeAgent(binding);
    }

}
