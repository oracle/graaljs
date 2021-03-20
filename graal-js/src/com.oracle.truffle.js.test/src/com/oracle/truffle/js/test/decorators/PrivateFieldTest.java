package com.oracle.truffle.js.test.decorators;

import org.junit.Test;

public class PrivateFieldTest extends DecoratorTest{
    @Test
    public void readWriteonlyAccessor() {
        String source = "class C {" +
                "#message;" +
                "set #test(value) {" +
                "   this.#message = value;" +
                "}" +
                "b(){" +
                "   let a = this.#test;" +
                "}" +
                "}" +
                "let c = new C();" +
                "c.b();";
        testError(source, "Accessor #test has no getter.");
    }

    @Test
    public void writeReadonlyField() {
        String source = "" +
                "function readonly(d) {" +
                "d.writable = false;" +
                "return d;" +
                "}" +
                "class C {" +
                "@readonly" +
                "#message;" +
                "set test(value) {" +
                "   this.#message = value;" +
                "}" +
                "}" +
                "let c = new C();" +
                "c.test = 'test';";
        testError(source, "Field #message is not writable.");
    }

    @Test
    public void writeReadonlyMethod() {
        String source = "" +
                "function readonly(d) {" +
                "d.writeable = false;" +
                "return d;" +
                "}" +
                "class C {" +
                "@readonly" +
                "#message() {" +
                "" +
                "};" +
                "set test(value) {" +
                "   this.#message = function() { console.log(value); };" +
                "}" +
                "}" +
                "let c = new C();" +
                "c.test = 'test';";
        testError(source, "Method #message is not writable.");
    }

    @Test
    public void writeReadonlyAccessor() {
        String source = "" +
                "function readonly(d) {" +
                "d.writeable = false;" +
                "return d;" +
                "}" +
                "class C {" +
                "@readonly " +
                "get #message() {" +
                "return 0;" +
                "};" +
                "set test(value) {" +
                "   this.#message = value;" +
                "}" +
                "}" +
                "let c = new C();" +
                "c.test = 'test';";
        testError(source, "Accessor #message has no setter.");
    }
}
