/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.simd;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAllTrueNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAndNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDAnyTrueNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDCheckNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDExtractLaneNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDLoadNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDNotNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDOrNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDReplaceLaneNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSelectNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDShuffleNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSplatNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDStoreNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDSwizzleNode;
import com.oracle.truffle.js.builtins.simd.SIMDTypeFunctionBuiltins.SIMDXorNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.SIMDType;

public final class SIMDBoolFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<SIMDBoolFunctionBuiltins.SIMDTypeFunction> {
    public SIMDBoolFunctionBuiltins(String typeName, SIMDType simdContext) {
        super(typeName, SIMDTypeFunction.class);
        this.simdContext = simdContext;
    }

    protected final SIMDType simdContext;

    public enum SIMDTypeFunction implements BuiltinEnum<SIMDTypeFunction> {
        splat(1),
        check(1),
        and(2),
        xor(2),
        or(2),
        not(2),
        anyTrue(1),
        allTrue(1),
        select(3),
        extractLane(2),
        replaceLane(3),
        store(3),
        store1(3),
        store2(3),
        store3(3),
        load(2),
        load1(2),
        load2(2),
        load3(2),
        swizzle(1),
        shuffle(2);

        private final int length;

        SIMDTypeFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, SIMDTypeFunction builtinEnum) {
        switch (builtinEnum) {
            case allTrue:
                return SIMDAllTrueNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case and:
                return SIMDAndNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case anyTrue:
                return SIMDAnyTrueNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case check:
                return SIMDCheckNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case extractLane:
                return SIMDExtractLaneNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case load:
                return SIMDLoadNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case load1:
                return SIMDLoadNode.create(context, builtin, simdContext, 1, args().fixedArgs(2).createArgumentNodes(context));
            case load2:
                return SIMDLoadNode.create(context, builtin, simdContext, 2, args().fixedArgs(2).createArgumentNodes(context));
            case load3:
                return SIMDLoadNode.create(context, builtin, simdContext, 3, args().fixedArgs(2).createArgumentNodes(context));
            case not:
                return SIMDNotNode.create(context, builtin, simdContext, args().fixedArgs(1).createArgumentNodes(context));
            case or:
                return SIMDOrNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case replaceLane:
                return SIMDReplaceLaneNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case select:
                return SIMDSelectNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case shuffle:
                return SIMDShuffleNode.create(context, builtin, simdContext, args().varArgs().createArgumentNodes(context));
            case splat:
                return SIMDSplatNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
            case store:
                return SIMDStoreNode.create(context, builtin, simdContext, args().fixedArgs(3).createArgumentNodes(context));
            case store1:
                return SIMDStoreNode.create(context, builtin, simdContext, 1, args().fixedArgs(3).createArgumentNodes(context));
            case store2:
                return SIMDStoreNode.create(context, builtin, simdContext, 2, args().fixedArgs(3).createArgumentNodes(context));
            case store3:
                return SIMDStoreNode.create(context, builtin, simdContext, 3, args().fixedArgs(3).createArgumentNodes(context));
            case swizzle:
                return SIMDSwizzleNode.create(context, builtin, simdContext, args().varArgs().createArgumentNodes(context));
            case xor:
                return SIMDXorNode.create(context, builtin, simdContext, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }
}
