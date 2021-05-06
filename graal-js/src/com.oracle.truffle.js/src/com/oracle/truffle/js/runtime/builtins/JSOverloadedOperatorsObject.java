/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.runtime.builtins;

import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSOrdinaryObject;
import com.oracle.truffle.js.runtime.objects.OperatorSet;

/**
 * This is the type of JavaScript objects that have overloaded operator semantics. This class
 * replicates JSOrdinaryObject.DefaultLayout, while adding an internal slot for the operator
 * information.
 */
public final class JSOverloadedOperatorsObject extends JSOrdinaryObject {

    @DynamicField Object o0;
    @DynamicField Object o1;
    @DynamicField Object o2;
    @DynamicField Object o3;
    @DynamicField long p0;
    @DynamicField long p1;
    @DynamicField long p2;

    private final OperatorSet operatorSet;

    private JSOverloadedOperatorsObject(Shape shape, OperatorSet operatorSet) {
        super(shape);
        this.operatorSet = operatorSet;
    }

    public OperatorSet getOperatorSet() {
        return operatorSet;
    }

    public int getOperatorCounter() {
        return getOperatorSet().getOperatorCounter();
    }

    public boolean matchesOperatorCounter(int operatorCounter) {
        return getOperatorSet().getOperatorCounter() == operatorCounter;
    }

    public static boolean hasOverloadedOperators(Object value) {
        return value instanceof JSOverloadedOperatorsObject;
    }

    public static JSOverloadedOperatorsObject create(JSContext context, Shape shape, OperatorSet operatorSet) {
        JSOverloadedOperatorsObject object = new JSOverloadedOperatorsObject(shape, operatorSet);
        return context.trackAllocation(object);
    }

    @Override
    protected JSObject copyWithoutProperties(Shape shape) {
        return new JSOverloadedOperatorsObject(shape, operatorSet);
    }
}
