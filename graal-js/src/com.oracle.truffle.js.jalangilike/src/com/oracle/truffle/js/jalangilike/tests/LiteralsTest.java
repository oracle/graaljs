package com.oracle.truffle.js.jalangilike.tests;

import org.junit.Test;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.LiteralTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.PropertyWriteTag;
import com.oracle.truffle.js.nodes.tags.JSSpecificTags.VariableWriteTag;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class LiteralsTest extends FineGrainedAccessTest {

    protected void testLiteral(String src, LiteralTag.Type expectedTagType) {
        testLiteral(src, expectedTagType, null);
    }

    protected void testLiteral(String src, LiteralTag.Type expectedTagType, Object expectedValue) {
        EventBinding<ExecutionEventNodeFactory> binding = initAgent(allJSSpecificTags);
        context.eval("js", src);
        assertEngineInit();

        enter(VariableWriteTag.class, (e, var) -> {
            assertAttribute(e, NAME, "<return>");
            enter(PropertyWriteTag.class, (e1, prop) -> {
                prop.input(assertJSObjectInput);
                assertAttribute(e1, KEY, "x");
                enter(LiteralTag.class, (e2) -> {
                    assertAttribute(e2, TYPE, expectedTagType.name());
                }).exit();
                if (expectedValue != null) {
                    prop.input(expectedValue);
                } else {
                    prop.input();
                }
            }).exit();
            if (expectedValue != null) {
                var.input(expectedValue);
            } else {
                var.input();
            }
        }).exit();

        disposeAgent(binding);
    }

    @Test
    public void object() {
        testLiteral("x = {};", LiteralTag.Type.ObjectLiteral);
    }

    @Test
    public void array() {
        testLiteral("x = [];", LiteralTag.Type.ArrayLiteral);
    }

    @Test
    public void number() {
        testLiteral("x = 42;", LiteralTag.Type.NumericLiteral, 42);
    }

    @Test
    public void string() {
        testLiteral("x = \"foo\";", LiteralTag.Type.StringLiteral, "foo");
    }

    @Test
    public void bool() {
        testLiteral("x = true;", LiteralTag.Type.BooleanLiteral, true);
    }

    @Test
    public void nullLit() {
        testLiteral("x = null;", LiteralTag.Type.NullLiteral);
    }

    @Test
    public void undefined() {
        testLiteral("x = undefined;", LiteralTag.Type.UndefinedLiteral, Undefined.instance);
    }

    @Test
    public void regexp() {
        testLiteral("x = /\\w+/;", LiteralTag.Type.RegExpLiteral);
    }

    @Test
    public void function() {
        testLiteral("x = function foo(){};", LiteralTag.Type.FunctionLiteral);
    }

    @Test
    public void anonFunction() {
        testLiteral("x = () => {};", LiteralTag.Type.FunctionLiteral);
    }
}
