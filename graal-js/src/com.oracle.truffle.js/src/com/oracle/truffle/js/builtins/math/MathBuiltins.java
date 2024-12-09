/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.builtins.math;

import java.util.EnumSet;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSMath;

/**
 * Contains builtins for math.
 */
public class MathBuiltins extends JSBuiltinsContainer.SwitchEnum<MathBuiltins.Math> {

    public static final JSBuiltinsContainer BUILTINS = new MathBuiltins();

    protected MathBuiltins() {
        super(JSMath.CLASS_NAME, Math.class);
    }

    public enum Math implements BuiltinEnum<Math> {
        abs(1),
        acos(1),
        asin(1),
        atan2(2),
        atan(1),
        ceil(1),
        cos(1),
        exp(1),
        floor(1),
        log(1),
        max(2),
        min(2),
        pow(2),
        random(0),
        round(1),
        sin(1),
        sqrt(1),
        tan(1),

        // ES6
        imul(2),
        sign(1),
        trunc(1),
        cbrt(1),
        expm1(1),
        hypot(2),
        log2(1),
        log10(1),
        log1p(1),
        clz32(1),
        cosh(1),
        sinh(1),
        tanh(1),
        acosh(1),
        asinh(1),
        atanh(1),
        fround(1),

        f16round(1),
        sumPrecise(1);

        private final int length;

        Math(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (EnumSet.range(imul, fround).contains(this)) {
                return 6;
            }
            if (EnumSet.range(f16round, sumPrecise).contains(this)) {
                return JSConfig.StagingECMAScriptVersion;
            }
            return BuiltinEnum.super.getECMAScriptVersion();
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, Math builtinEnum) {
        switch (builtinEnum) {
            case abs:
                return AbsNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case acos:
                return AcosNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case asin:
                return AsinNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case atan2:
                return Atan2NodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case atan:
                return AtanNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case ceil:
                return CeilNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case cos:
                return CosNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case exp:
                return ExpNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case floor:
                return FloorNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case log:
                return LogNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case max:
                return MaxNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case min:
                return MinNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case pow:
                return PowNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case random:
                return RandomNodeGen.create(context, builtin, args().createArgumentNodes(context));
            case round:
                return RoundNode.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case sin:
                return SinNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case sqrt:
                return SqrtNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case tan:
                return TanNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case imul:
                return ImulNode.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
            case sign:
                return SignNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case trunc:
                return TruncNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case cbrt:
                return CbrtNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case expm1:
                return Expm1NodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case hypot:
                return HypotNodeGen.create(context, builtin, args().varArgs().createArgumentNodes(context));
            case log2:
                return Log2NodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case log10:
                return Log10NodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case log1p:
                return Log1pNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case clz32:
                return Clz32NodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case cosh:
                return CoshNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case sinh:
                return SinhNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case tanh:
                return TanhNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case acosh:
                return AcoshNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case asinh:
                return AsinhNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case atanh:
                return AtanhNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case fround:
                return FroundNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case f16round:
                return F16roundNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            case sumPrecise:
                return SumPreciseNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
            default:
                return null;
        }
    }
}
