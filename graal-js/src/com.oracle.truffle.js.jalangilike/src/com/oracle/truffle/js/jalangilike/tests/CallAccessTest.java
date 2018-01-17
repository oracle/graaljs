package com.oracle.truffle.js.jalangilike.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class CallAccessTest extends FineGrainedAccessTest {

    @Test
    public void callOneArg() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "function foo(a) {}; foo(42);");

        assertEngineInit();

        // declaration
        assertGlobalFunctionExpressionDeclaration("foo");

        // foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // target (which is undefined in this case) and function
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            // read 'foo' from the global object
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
            call.input(assertJSFunctionInput);
            // one argument
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void callTwoArgs() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "function foo(a,b) {}; foo(42,24);");

        assertEngineInit();

        // declaration
        assertGlobalFunctionExpressionDeclaration("foo");

        // foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // tead the target for 'foo', which is undefined
            enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
            call.input(assertUndefinedInput);
            enter(PropertyReadTag.class).input(assertGlobalObjectInput).exit();
            // target (which is undefined in this case) and function
            call.input(assertJSFunctionInput);
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
            enter(LiteralTag.class).exit(assertReturnValue(24));
            call.input(24);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void methodCall() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var foo = {x:function foo(a,b) {}}; foo.x(42,24);");

        assertEngineInit();

        // var foo = ...
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "foo");
            write.input(assertJSObjectInput);

            enter(LiteralTag.class, (e1, literal) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.FunctionLiteral.name());
                }).exit();
                literal.input(assertJSFunctionInput);
            }).exit();

            write.input(assertJSObjectInput);
        }).exit();

        // x.foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // read 'foo' from global
            enter(PropertyReadTag.class, (e1, prop) -> {
                assertAttribute(e1, KEY, "foo");
                prop.input(assertGlobalObjectInput);
            }).exit();
            // 1st argument to function is target
            call.input(assertJSObjectInput);
            // 2nd argument is the function itself
            enter(PropertyReadTag.class, assertPropertyReadName("x")).exit();
            call.input(assertJSFunctionInput);
            // arguments
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
            enter(LiteralTag.class).exit(assertReturnValue(24));
            call.input(24);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void methodCallOneArg() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var foo = {x:function foo(a,b) {}}; foo.x(42);");

        assertEngineInit();

        // var foo = ...
        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "foo");
            write.input(assertJSObjectInput);

            enter(LiteralTag.class, (e1, literal) -> {
                assertAttribute(e1, TYPE, LiteralTag.Type.ObjectLiteral.name());
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, LiteralTag.Type.FunctionLiteral.name());
                }).exit();
                literal.input(assertJSFunctionInput);
            }).exit();

            write.input(assertJSObjectInput);
        }).exit();

        // x.foo(1)
        enter(FunctionCallTag.class, (e, call) -> {
            // read 'foo' from global
            enter(PropertyReadTag.class, (e1, prop) -> {
                assertAttribute(e1, KEY, "foo");
                prop.input(assertGlobalObjectInput);
            }).exit();
            // 1st argument to function is target
            call.input(assertJSObjectInput);
            // 2nd argument is the function itself
            enter(PropertyReadTag.class, assertPropertyReadName("x")).exit();
            call.input(assertJSFunctionInput);
            // arguments
            enter(LiteralTag.class).exit(assertReturnValue(42));
            call.input(42);
        }).exit();

        disposeAgent(binding);
    }

}
