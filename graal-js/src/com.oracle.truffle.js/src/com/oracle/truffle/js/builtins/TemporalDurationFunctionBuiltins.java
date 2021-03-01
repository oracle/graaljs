package com.oracle.truffle.js.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDuration;
import com.oracle.truffle.js.runtime.builtins.JSTemporalDurationObject;

public class TemporalDurationFunctionBuiltins extends JSBuiltinsContainer.SwitchEnum<TemporalDurationFunctionBuiltins.TemporalDurationFunction>{

    public static final JSBuiltinsContainer BUILTINS = new TemporalDurationFunctionBuiltins();

    protected TemporalDurationFunctionBuiltins() {
        super(JSTemporalDuration.CLASS_NAME, TemporalDurationFunction.class);
    }

    public enum TemporalDurationFunction implements BuiltinEnum<TemporalDurationFunction> {
        from(1);

        private final int length;

        TemporalDurationFunction(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }
    }

    @Override
    protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, TemporalDurationFunction builtinEnum) {
        switch (builtinEnum) {
            case from:
                return TemporalDurationFunctionBuiltinsFactory.JSTemporalDurationFromNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public abstract static class JSTemporalDurationFrom extends JSBuiltinNode {

        protected JSTemporalDurationFrom(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(limit = "3")
        protected Object from(DynamicObject item,
                              @Cached("create()") IsObjectNode isObject,
                              @Cached("create()") IsConstructorNode isConstructor,
                              @Cached("create()") JSToIntegerAsLongNode toInt,
                              @Cached("create()") JSToStringNode toString,
                              @Cached("createNew()") JSFunctionCallNode callNode,
                              @CachedLibrary("item") DynamicObjectLibrary dol) {
            DynamicObject constructor = getContext().getRealm().getTemporalDurationConstructor();
            if(isObject.executeBoolean(item) && JSTemporalDuration.isJSTemporalDuration(item)) {
                JSTemporalDurationObject duration = (JSTemporalDurationObject) item;
                return JSTemporalDuration.createTemporalDurationFromStatic(constructor, duration.getYears(),
                        duration.getMonths(), duration.getWeeks(), duration.getDays(), duration.getHours(),
                        duration.getMinutes(), duration.getSeconds(), duration.getMilliseconds(),
                        duration.getMicroseconds(), duration.getNanoseconds(), isConstructor, callNode);
            }
            return JSTemporalDuration.toTemporalDuration(item, constructor, getContext().getRealm(), isObject,
                    toInt, dol, toString, isConstructor, callNode);
        }
    }

}
