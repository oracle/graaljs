package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationAbsNodeGen;
import com.oracle.truffle.js.builtins.TemporalDurationPrototypeBuiltinsFactory.JSTemporalDurationNegatedNodeGen;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;

public class TemporalDurationPrototypeBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationPrototypeBuiltins.TemporalDurationPrototype> {

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationPrototypeBuiltins();

    protected TemporalDurationPrototypeBuiltins() {
        super(JSTemporalDuration.PROTOTYPE_NAME, TemporalDurationPrototype.class);
    }

    public enum TemporalDurationPrototype implements BuiltinEnum<TemporalDurationPrototype> {
        negated(0),
        abs(0);

        private final int length;

        TemporalDurationPrototype(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationPrototype builtinEnum) {
        switch (builtinEnum) {
            case negated:
                return JSTemporalDurationNegatedNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
            case abs:
                return JSTemporalDurationAbsNodeGen.create(context, builtin, args().withThis().createArgumentNodes(context));
        }
        return null;
    }

    // 7.3.16
    public abstract static class JSTemporalDurationNegated extends JSBuiltinNode {

        protected JSTemporalDurationNegated(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject negated(DynamicObject thisObj,
                                        @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            return JSTemporalDuration.createTemporalDurationFromInstance(duration,
                    -duration.getYears(), -duration.getMonths(), -duration.getWeeks(), -duration.getDays(),
                    -duration.getHours(), -duration.getMinutes(), -duration.getSeconds(), -duration.getMilliseconds(),
                    -duration.getMicroseconds(), -duration.getNanoseconds(), getContext().getRealm(), callNode);
        }
    }

    // 7.3.17
    public abstract static class JSTemporalDurationAbs extends JSBuiltinNode {

        protected JSTemporalDurationAbs(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected DynamicObject abs(DynamicObject thisObj,
                                    @Cached("createNew()") JSFunctionCallNode callNode) {
            JSTemporalDurationObject duration = (JSTemporalDurationObject) thisObj;
            return JSTemporalDuration.createTemporalDurationFromInstance(duration,
                    Math.abs(duration.getYears()), Math.abs(duration.getMonths()), Math.abs(duration.getWeeks()),
                    Math.abs(duration.getDays()), Math.abs(duration.getHours()), Math.abs(duration.getMinutes()),
                    Math.abs(duration.getSeconds()), Math.abs(duration.getMilliseconds()),
                    Math.abs(duration.getMicroseconds()), Math.abs(duration.getNanoseconds()),
                    getContext().getRealm(), callNode);
        }
    }
}
