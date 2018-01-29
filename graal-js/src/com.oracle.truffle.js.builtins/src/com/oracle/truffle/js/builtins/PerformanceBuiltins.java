/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.PerformanceBuiltinsFactory.JSPerformanceNowNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSPerformance;
import com.oracle.truffle.js.runtime.objects.JSAttributes;

public final class PerformanceBuiltins extends JSBuiltinsContainer.Lambda {
    private static final double NANOSECONDS_PER_MILLISECOND = 1000000;

    public PerformanceBuiltins() {
        super(JSPerformance.CLASS_NAME);
        defineFunction("now", 0, JSAttributes.getDefault(), (context, builtin) -> JSPerformanceNowNodeGen.create(context, builtin, args().fixedArgs(0).createArgumentNodes(context)));
    }

    public abstract static class JSPerformanceNowNode extends JSBuiltinNode {
        public JSPerformanceNowNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected double now() {
            long ns = System.nanoTime();
            if (!getContext().isOptionPreciseTime()) {
                long resolution = JSTruffleOptions.TimestampResolution;
                if (resolution > 0) {
                    ns = (ns / resolution) * resolution;
                }
            }
            return ns / NANOSECONDS_PER_MILLISECOND;
        }
    }
}
