/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import java.util.SplittableRandom;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.js.nodes.function.*;
import com.oracle.truffle.js.runtime.*;

public abstract class RandomNode extends JSBuiltinNode {

    private final SplittableRandom r = new SplittableRandom();

    public RandomNode(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected double random() {
        return r.nextDouble();
    }
}
