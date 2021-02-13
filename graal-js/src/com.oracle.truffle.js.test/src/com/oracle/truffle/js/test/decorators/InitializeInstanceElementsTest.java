package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class InitializeInstanceElementsTest extends DecoratorTest{
    @Test
    public void prototypeHookStartReturnsUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'prototype'," +
                "};" +
                "h.start = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Start of hook can not have a return value.");
    }

    @Test
    public void staticHookStartReturnsUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'static'," +
                "};" +
                "h.start = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Start of hook can not have a return value.");
    }

    @Test
    public void ownHookStartReturnsUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'own'," +
                "};" +
                "h.start = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Start of hook can not have a return value.");
    }
}
