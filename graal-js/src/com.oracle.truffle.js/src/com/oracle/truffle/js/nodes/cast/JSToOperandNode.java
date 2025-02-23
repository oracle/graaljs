/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.cast;

import static com.oracle.truffle.js.builtins.OperatorsBuiltins.checkOverloadedOperatorsAllowed;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.cast.JSToPrimitiveNode.Hint;
import com.oracle.truffle.js.runtime.builtins.JSOverloadedOperatorsObject;

/**
 * Converts a value to an 'operand', which is a preliminary step when invoking an overloaded
 * operator. If the value is an object with overloaded operators, this ought to check that
 * overloaded operators for that object have been enabled. Otherwise, if the value doesn't feature
 * overloaded operators, it is coerced to a primitive using ToPrimitive.
 */
public abstract class JSToOperandNode extends JavaScriptBaseNode {

    protected final Hint hint;
    protected final boolean checkOperatorAllowed;

    protected JSToOperandNode(Hint hint, boolean checkOperatorAllowed) {
        this.hint = hint;
        this.checkOperatorAllowed = checkOperatorAllowed;
    }

    @NeverDefault
    public static JSToOperandNode create(Hint hint) {
        return JSToOperandNodeGen.create(hint, true);
    }

    @NeverDefault
    public static JSToOperandNode create(Hint hint, boolean checkOperatorAllowed) {
        return JSToOperandNodeGen.create(hint, checkOperatorAllowed);
    }

    public abstract Object execute(Object value);

    protected Hint getHint() {
        return hint;
    }

    @Specialization
    protected Object doOverloaded(JSOverloadedOperatorsObject arg) {
        if (checkOperatorAllowed) {
            checkOverloadedOperatorsAllowed(arg, this);
        }
        return arg;
    }

    @Specialization(guards = "!hasOverloadedOperators(arg)")
    protected Object doOther(Object arg,
                    @Cached JSToPrimitiveNode toPrimitiveNode) {
        return toPrimitiveNode.execute(arg, getHint());
    }
}
