/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.cast.JSToPropertyKeyNodeGen.JSToPropertyKeyWrapperNodeGen;
import com.oracle.truffle.js.nodes.unary.JSUnaryNode;
import com.oracle.truffle.js.runtime.Symbol;

/**
 * This implements the abstract operation ToPropertyKey(argument).
 */
public abstract class JSToPropertyKeyNode extends JavaScriptBaseNode {

    @NeverDefault
    public static JSToPropertyKeyNode create() {
        return JSToPropertyKeyNodeGen.create();
    }

    public abstract Object execute(Object operand);

    @Specialization
    protected TruffleString doTString(TruffleString value) {
        return value;
    }

    @Specialization
    protected Symbol doSymbol(Symbol value) {
        return value;
    }

    // !isString intentionally omitted
    @Specialization(guards = {"!isSymbol(value)"})
    protected Object doOther(Object value,
                    @Cached JSToPrimitiveNode toPrimitiveNode,
                    @Cached JSToStringNode toStringNode,
                    @Cached InlinedConditionProfile isSymbol) {
        Object key = toPrimitiveNode.executeHintString(value);
        if (isSymbol.profile(this, key instanceof Symbol)) {
            return key;
        } else {
            return toStringNode.executeString(key);
        }
    }

    public abstract static class JSToPropertyKeyWrapperNode extends JSUnaryNode {

        protected JSToPropertyKeyWrapperNode(JavaScriptNode operand) {
            super(operand);
        }

        public static JavaScriptNode create(JavaScriptNode key) {
            if (key.isResultAlwaysOfType(TruffleString.class) || key.isResultAlwaysOfType(Symbol.class)) {
                return key;
            }
            return JSToPropertyKeyWrapperNodeGen.create(key);
        }

        @Specialization
        protected static Object doDefault(Object value,
                        @Cached JSToPropertyKeyNode toPropertyKeyNode) {
            return toPropertyKeyNode.execute(value);
        }

        @Override
        protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
            return JSToPropertyKeyWrapperNodeGen.create(cloneUninitialized(getOperand(), materializedTags));
        }
    }
}
