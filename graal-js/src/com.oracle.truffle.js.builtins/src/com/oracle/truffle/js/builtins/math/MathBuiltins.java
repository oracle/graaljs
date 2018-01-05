/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.builtins.math;

import java.util.EnumSet;

import com.oracle.truffle.js.builtins.JSBuiltinsContainer;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSMath;

/**
 * Contains builtins for math.
 */
public class MathBuiltins extends JSBuiltinsContainer.SwitchEnum<MathBuiltins.Math> {
    public MathBuiltins() {
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
        fround(1);

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
            default:
                return null;
        }
    }
}
