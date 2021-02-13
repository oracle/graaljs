package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class HookTest extends DecoratorTest{
    @Test
    public void prototypeHookStartReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'prototype'," +
                "};" +
                "h.start = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Start of hook can not have a return value.");
    }

    @Test
    public void staticHookStartReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'static'," +
                "};" +
                "h.start = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Start of hook can not have a return value.");
    }

    @Test
    public void ownHookStartReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'own'," +
                "};" +
                "h.start = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Start of hook can not have a return value.");
    }

    @Test
    public void prototypeHookReplaceNotAConstructor() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'prototype'," +
                "};" +
                "h.replace = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Element descriptor property kind 'hook' and placement 'prototype' must not have property replace.");
    }

    @Test
    public void staticHookReplaceNotAConstructor() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'static'," +
                "};" +
                "h.replace = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Replace of hook must return a constructor.");
    }

    @Test
    public void ownHookReplaceNotAConstructor() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'own'," +
                "};" +
                "h.replace = function(){" +
                "console.log('test');" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Element descriptor property kind 'hook' and placement 'own' must not have properties replace and finish.");
    }

    @Test
    public void prototypehookFinishReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'prototype'," +
                "};" +
                "h.finish = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Finish of hook can not have a return value.");
    }

    @Test
    public void staticHookFinishReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'static'," +
                "};" +
                "h.finish = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        testError(source,"Finish of hook can not have a return value.");
    }

    @Test
    public void ownHookFinishReturnsNotUndefined() {
        String source = createClassDecorator(CLASS,null,null,"" +
                "let h = {" +
                "'kind':'hook'," +
                "'placement':'own'," +
                "};" +
                "h.finish = function(){" +
                "return 0;" +
                "};" +
                "d.elements = [];" +
                "d.elements.push(h);");
        source += "let c = new C();";
        testError(source,"Element descriptor property kind 'hook' and placement 'own' must not have properties replace and finish.");
    }
}
