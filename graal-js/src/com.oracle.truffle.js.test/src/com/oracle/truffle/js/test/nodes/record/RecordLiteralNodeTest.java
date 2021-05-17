package com.oracle.truffle.js.test.nodes.record;

import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSTest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RecordLiteralNodeTest {

    private static final String testName = "record-literal-node-test";

    private static Value execute(String sourceText) {
        try (Context context = JSTest.newContextBuilder()
                .option(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022")
                .build()) {
            return context.eval(Source.newBuilder(JavaScriptLanguage.ID, sourceText, testName).buildLiteral());
        }
    }

    @Test
    public void testSpread_Polyfill() {
        assertTrue(execute("#{...['test']} === #{'0': 'test'}").asBoolean());
    }

    // TODO: re-evaluate, check proposal for changes
    // let a = ['test'];
    // let b = {...a}
    // let c = #{...a}
    // console.log(a.length); // "1"
    // console.log(b.length); // "undefined"
    // console.log(c.length); // "1" according to proposal spec BUT "undefined" according to proposal polyfill
    @Ignore
    @Test
    public void testSpread_Spec() {
        assertTrue(execute("#{...['test']} === #{'0': 'test', length: 1}").asBoolean());
    }
}
