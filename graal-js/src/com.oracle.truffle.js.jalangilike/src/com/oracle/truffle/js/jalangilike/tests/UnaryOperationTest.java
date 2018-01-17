package com.oracle.truffle.js.jalangilike.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.FunctionCallTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyReadTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.UnaryOperationTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class UnaryOperationTest extends FineGrainedAccessTest {

    @Test
    public void typeof() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "var b = typeof Uint8Array;");

        assertEngineInit();

        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryOperationTag.class, (e2, unary) -> {
                enter(PropertyReadTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "Uint8Array");
                    prop.input((e4) -> {
                        assertTrue(JSObject.isJSObject(e4.val));
                    });
                }).exit();
                unary.input((e4) -> {
                    assertTrue(JSObject.isJSObject(e4.val));
                });
            }).exit();
            write.input("function");
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void voidMethod() {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", "void function foo() {}();");

        assertEngineInit();

        enter(VariableWriteTag.class, (e, var) -> {
            assertAttribute(e, NAME, "<return>");
            enter(UnaryOperationTag.class, (e2, unary) -> {
                enter(FunctionCallTag.class, (e3, call) -> {
                    enter(LiteralTag.class).exit();
                    call.input(assertUndefinedInput);
                    enter(LiteralTag.class).exit();
                    call.input(assertJSFunctionInput);
                    // lastly, read undefined to return it
                    enter(LiteralTag.class).exit(assertReturnValue(Undefined.instance));
                }).exit();
                unary.input(Undefined.instance);
            }).exit();
            var.input(Undefined.instance);
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void toInt() {
        assertBasicUnaryOperation("var x = true; var b = ~x;", -2);
    }

    @Test
    public void not() {
        assertBasicUnaryOperation("var x = true; var b = !x;", false);
    }

    @Test
    public void minus() {
        assertBasicUnaryOperation("var x = true; var b = -x;", -1);
    }

    @Test
    public void plus() {
        assertBasicUnaryOperation("var x = true; var b = +x;", 1);
    }

    private void assertBasicUnaryOperation(String src, Object expectedPostUnaryOpValue) {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);

        context.eval("js", src);

        assertEngineInit();

        assertGlobalVarDeclaration("x", true);

        enter(PropertyWriteTag.class, (e, write) -> {
            assertAttribute(e, KEY, "b");
            write.input(assertGlobalObjectInput);
            enter(UnaryOperationTag.class, (e2, unary) -> {
                enter(PropertyReadTag.class, (e3, prop) -> {
                    assertAttribute(e3, KEY, "x");
                    prop.input(assertGlobalObjectInput);
                }).exit();
                unary.input(true);
            }).exit();
            write.input(expectedPostUnaryOpValue);
        }).exit();

        disposeAgent(binding);
    }

}
