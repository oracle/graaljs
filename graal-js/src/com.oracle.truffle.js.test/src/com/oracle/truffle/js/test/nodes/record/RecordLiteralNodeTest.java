package com.oracle.truffle.js.test.nodes.record;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.test.JSSimpleTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RecordLiteralNodeTest extends JSSimpleTest {

    public RecordLiteralNodeTest() {
        super("record-literal-node-test");
        addOption(JSContextOptions.ECMASCRIPT_VERSION_NAME, "2022");
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
    @Test
    public void testSpread_Polyfill() {
        assertTrue(execute("#{...['test']} === #{'0': 'test'}").asBoolean());
    }
}
