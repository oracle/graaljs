/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.unary;

import static com.oracle.truffle.js.builtins.OperatorsBuiltins.checkOverloadedOperatorsAllowed;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;
import com.oracle.truffle.js.runtime.objects.OperatorSet;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * This node implements unary operators applied to an operand with overloaded operators. The logic
 * of this node mirrors that of the {@code JSOverloadedBinaryNode}, with some simplifications.
 */
@ImportStatic(OperatorSet.class)
public abstract class JSOverloadedUnaryNode extends JavaScriptBaseNode {

    static final int LIMIT = 3;

    private final TruffleString overloadedOperatorName;

    protected JSOverloadedUnaryNode(TruffleString overloadedOperatorName) {
        this.overloadedOperatorName = overloadedOperatorName;
    }

    public abstract Object execute(Object operand);

    @Specialization(guards = {"operand.matchesOperatorCounter(operatorCounter)"}, limit = "LIMIT")
    protected Object doCached(JSOverloadedOperatorsObject operand,
                    @Cached("operand.getOperatorCounter()") @SuppressWarnings("unused") int operatorCounter,
                    @Cached("getOperatorImplementation(operand, getOverloadedOperatorName())") Object operatorImplementation,
                    @Cached("createCall()") @Exclusive JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(operand, this);
        return performOverloaded(callNode, operatorImplementation, operand);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = {"doCached"})
    protected Object doGeneric(JSOverloadedOperatorsObject operand,
                    @Cached("createCall()") @Exclusive JSFunctionCallNode callNode) {
        checkOverloadedOperatorsAllowed(operand, this);
        Object operatorImplementation = OperatorSet.getOperatorImplementation(operand, getOverloadedOperatorName());
        return performOverloaded(callNode, operatorImplementation, operand);
    }

    private Object performOverloaded(JSFunctionCallNode callNode, Object operatorImplementation, Object operand) {
        if (operatorImplementation == null) {
            throw Errors.createTypeErrorNoOverloadFoundUnary(getOverloadedOperatorName(), operand, this);
        }
        // What should be the value of 'this' when invoking overloaded operators?
        // Currently, we set it to 'undefined'.
        return callNode.executeCall(JSArguments.create(Undefined.instance, operatorImplementation, operand));
    }

    protected TruffleString getOverloadedOperatorName() {
        return overloadedOperatorName;
    }
}
