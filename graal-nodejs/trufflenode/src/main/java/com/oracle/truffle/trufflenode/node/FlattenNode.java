/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.trufflenode.node;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSLazyString;

@ImportStatic({JSRuntime.class})
abstract class FlattenNode extends JavaScriptBaseNode {
    protected abstract Object execute(Object value);

    @Specialization(guards = "isLazyString(value)")
    protected static String doLazyString(Object value) {
        return ((JSLazyString) value).toString();
    }

    @Specialization(guards = "!isLazyString(value)")
    protected static Object doOther(Object value) {
        return value;
    }
}
