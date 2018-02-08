/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.test.instrumentation;

import org.junit.Test;

import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadElementExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ObjectAllocationExpressionTag;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ReadPropertyExpressionTag;

public class LeftHandSideExpressions extends FineGrainedAccessTest {

    @Test
    public void member() {
        evalWithTags("var a = {x:42}; a.x;", new Class[]{ReadPropertyExpressionTag.class, ReadElementExpressionTag.class});

        enter(ReadPropertyExpressionTag.class, (e, g) -> {
            assertAttribute(e, KEY, "x");
            enter(ReadPropertyExpressionTag.class, (e2, l) -> {
                l.input(assertJSObjectInput);
                assertAttribute(e2, KEY, "a");
            }).exit();
            g.input(assertJSObjectInput);
        }).exit();
    }

    @Test
    public void memberElem() {
        evalWithTags("var a = {x:42}; a['x'];", new Class[]{ReadPropertyExpressionTag.class, ReadElementExpressionTag.class});

        enter(ReadElementExpressionTag.class, (e, g) -> {
            enter(ReadPropertyExpressionTag.class, (e2, p) -> {
                p.input(assertGlobalObjectInput);
                assertAttribute(e2, KEY, "a");
            }).exit();
            g.input(assertJSObjectInput);
            g.input("x");
        }).exit();
    }

    @Test
    public void newExpression() {
        evalWithTag("new Object()", ObjectAllocationExpressionTag.class);
        enter(ObjectAllocationExpressionTag.class).input(assertJSFunctionInput).exit();
    }

}
