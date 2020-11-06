package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalTimeFunctionBuiltinsFactory.JSTemporalTimeCompareNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTime;
import com.oracle.truffle.js.runtime.builtins.JSTemporalTimeObject;

public class TemporalTimeFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalTimeFunctionBuiltins.TemporalTimeFunction>{

    public static final JSBuiltinsContainer BUILTINS = new TemporalTimeFunctionBuiltins();

    protected TemporalTimeFunctionBuiltins() {
        super(JSTemporalTime.CLASS_NAME, TemporalTimeFunction.class);
    }

    public enum TemporalTimeFunction implements BuiltinEnum<TemporalTimeFunction> {
        compare(2);

        private final int length;

        TemporalTimeFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalTimeFunction builtinEnum) {
        switch (builtinEnum) {
            case compare:
                return JSTemporalTimeCompareNodeGen.create(context, builtin, args().fixedArgs(2).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalTimeCompareNode extends JSBuiltinNode {

        public JSTemporalTimeCompareNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isJSTemporalTime(obj1)", "isJSTemporalTime(obj2)"})
        protected int compare(DynamicObject obj1, DynamicObject obj2) {
            JSTemporalTimeObject time1 = (JSTemporalTimeObject) obj1;
            JSTemporalTimeObject time2 = (JSTemporalTimeObject) obj2;
            return JSTemporalTime.compareTemporalTime(
                    time1.getHours(), time1.getMinutes(), time1.getSeconds(),
                    time1.getMilliseconds(), time1.getMicroseconds(), time1.getNanoseconds(),
                    time2.getHours(), time2.getMinutes(), time2.getSeconds(),
                    time2.getMilliseconds(), time2.getMicroseconds(), time2.getNanoseconds()
            );
        }

        @Specialization(guards = "!isJSTemporalTime(obj1) || !isJSTemporalTime(obj2)")
        protected int cantCompare(Object obj1, Object obj2) {
            throw Errors.createTypeErrorTemporalTimeExpected();
        }

    }

}
